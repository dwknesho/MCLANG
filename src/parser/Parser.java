package parser;

import lexer.Scanner; 
import lexer.Token;
import semantic.SymbolTable;
import errors.LexicalException;
import errors.ErrorReporter;
import grtree.Tree;
import grtree.TreeScrollFrame;
import java.util.Stack;
import java.util.Set;
import java.util.HashSet;

public class Parser {
    private Scanner scanner;
    private ParseTable table;
    private ErrorReporter reporter;
    private SymbolTable symTable;
    private int currentPrintLine = -1;

    // ---------------------------------------------------------------
    // These are the "noise" non-terminals that exist only to enforce
    // grammar structure but carry no semantic meaning on their own.
    // They will be PRUNED from the concrete tree to produce the AST.
    // Add / remove names here as your grammar evolves.
    // ---------------------------------------------------------------
 private static final Set<String> PASSTHROUGH_NODES = new HashSet<>();
static {

    // ── EXPRESSION CHAIN (precedence ladder, all single-production wrappers) ──
    PASSTHROUGH_NODES.add("EXPRESSION");        // always → LOGIC_OR only, pure wrapper
    PASSTHROUGH_NODES.add("LOGIC_OR");          // → LOGIC_AND LOGIC_OR'
    PASSTHROUGH_NODES.add("LOGIC_AND");         // → EQUALITY LOGIC_AND'
    PASSTHROUGH_NODES.add("EQUALITY");          // → RELATIONAL EQUALITY'
    PASSTHROUGH_NODES.add("RELATIONAL");        // → ADDITIVE RELATIONAL'
    PASSTHROUGH_NODES.add("ADDITIVE");          // → MULTIPLICATIVE ADDITIVE'
    PASSTHROUGH_NODES.add("MULTIPLICATIVE");    // → UNARY MULTIPLICATIVE'

    // ── PRIME NODES (left-recursion elimination helpers, never meaningful alone) ──
    PASSTHROUGH_NODES.add("LOGIC_OR'");
    PASSTHROUGH_NODES.add("LOGIC_AND'");
    PASSTHROUGH_NODES.add("EQUALITY'");
    PASSTHROUGH_NODES.add("RELATIONAL'");
    PASSTHROUGH_NODES.add("ADDITIVE'");
    PASSTHROUGH_NODES.add("MULTIPLICATIVE'");
    PASSTHROUGH_NODES.add("PRIMARY'");
    PASSTHROUGH_NODES.add("STATEMENT'");
    PASSTHROUGH_NODES.add("DECLARATION_STMT'");
    PASSTHROUGH_NODES.add("ASSIGNMENT_STMT'");
    PASSTHROUGH_NODES.add("L_VALUE'");
    PASSTHROUGH_NODES.add("FOR_UPDATE'");
    PASSTHROUGH_NODES.add("RETURN_STMT'");

    // ── PUNCTUATION TERMINALS (zero semantic value) ──
    PASSTHROUGH_NODES.add("semi");
    PASSTHROUGH_NODES.add("l_brace");
    PASSTHROUGH_NODES.add("r_brace");
    PASSTHROUGH_NODES.add("l_paren");
    PASSTHROUGH_NODES.add("r_paren");
    PASSTHROUGH_NODES.add("comma");
    PASSTHROUGH_NODES.add("colon");

    // ── SPECIAL GRAMMAR SYMBOLS ──
    PASSTHROUGH_NODES.add("ε");
    PASSTHROUGH_NODES.add("eof");
    PASSTHROUGH_NODES.add("start");
    PASSTHROUGH_NODES.add("end");
}
    public Parser(Scanner scanner, ParseTable table, ErrorReporter reporter, SymbolTable symTable) {
        this.scanner = scanner;
        this.table = table;
        this.reporter = reporter;
        this.symTable = symTable;
    }

    private Token fetchNextToken() {
        while (true) {
            try {
                Token t = scanner.getNextToken();
                
                if (t != null && !t.tokenName.equals("[EOF]")) {
                    printToken(t);
                }

                if (t != null && t.tokenName.equals("<id>")) {
                    if (symTable.getAttributes(t.lexeme) == null) {
                        symTable.declareVariable(t.lexeme, "Pending Phase 4");
                    }
                }
                
                return t;
            } catch (LexicalException le) {
                reporter.reportLexicalError(le.line, le.col, le.getMessage(), le.lexeme);
                System.out.print("<ERROR:" + reporter.getErrorCount() + "> ");
                
            } catch (Exception e) {
                return new Token("eof", "", null, -1, -1);
            }
        }
    }

    public void parse() throws Exception {
        Stack<Tree> stack = new Stack<>();
        Tree root = new Tree("PROGRAM"); 
        
        stack.push(new Tree("eof")); 
        stack.push(root);            

        System.out.println("\n\n\nStarting Syntax Analysis...\n");
        Token currentToken = fetchNextToken();

        while (!stack.isEmpty()) {
            Tree topNode = stack.peek(); 
            String topSymbol = topNode.data;

            if (topSymbol.equals("ε")) {
                stack.pop();
                continue; 
            }

            String tokenSymbol = normalizeToken(currentToken.tokenName);

            if (table.isTerminal(topSymbol)) {
                if (topSymbol.equals(tokenSymbol)) {
                    stack.pop(); 
                    topNode.data = topSymbol + " (" + currentToken.lexeme + ")";
                    
                    if (!tokenSymbol.equals("eof")) {
                        currentToken = fetchNextToken(); 
                    }
                } else {
                    reporter.reportSyntaxError(currentToken.line, currentToken.col, 
                        "Expected '" + topSymbol + "' but found '" + currentToken.lexeme + "'");
                    stack.pop(); 
                }
            } 
            else {
                String[] rhs = table.getRule(topSymbol, tokenSymbol);
                
                if (rhs == null) {
                    reporter.reportSyntaxError(currentToken.line, currentToken.col, 
                        "Unexpected token '" + currentToken.lexeme + "' while parsing " + topSymbol);
                    
                    if (!tokenSymbol.equals("eof")) {
                        currentToken = fetchNextToken();
                    } else {
                        stack.pop(); 
                    }
                } else {
                    stack.pop(); 
                    Tree[] children = new Tree[rhs.length];
                    for (int i = 0; i < rhs.length; i++) {
                        children[i] = new Tree(rhs[i]);
                        topNode.addChild(children[i]); 
                    }

                    for (int i = rhs.length - 1; i >= 0; i--) {
                        stack.push(children[i]);
                    }
                }
            }
        }
        
        System.out.println("\n\n\n\n\nParsing Complete!\n\n\n\n");

        // ----------------------------------------------------------
        // POST-PROCESS: Convert the concrete parse tree into an AST
        // by pruning all passthrough / noise nodes.
        // ----------------------------------------------------------
        Tree ast = toAST(root);
        if (ast == null) ast = new Tree("PROGRAM"); // safety fallback

        // ONLY draw the AST window if there are absolutely no errors!
        if (!reporter.hasErrors()) {
            System.out.println("Abstract Syntax Tree built successfully.\n");
            new TreeScrollFrame(ast);
        } else {
            System.out.println("[!] Syntax or Lexical errors detected. AST visualization skipped.\n");
        }
    }

    // ---------------------------------------------------------------
    // AST BUILDER
    // Recursively walks the concrete parse tree.
    // Rules:
    //   1. If a node is in PASSTHROUGH_NODES AND has exactly 1 meaningful
    //      child -> skip the node, return the child directly (chain collapse).
    //   2. If a node is in PASSTHROUGH_NODES AND has 0 meaningful children
    //      -> drop it entirely (return null).
    //   3. If a node is in PASSTHROUGH_NODES AND has 2+ meaningful children
    //      -> keep it (it is doing real structural work, e.g. a list).
    //   4. If a node is NOT in PASSTHROUGH_NODES -> keep it, but still
    //      prune its children recursively.
    // ---------------------------------------------------------------
    @SuppressWarnings("unchecked")
    private Tree toAST(Tree node) {
        if (node == null) return null;

        // Get the raw label (strip the " (lexeme)" part if it's a terminal)
        String rawLabel = rawLabel(node.data);

        // Recursively build AST children first
        java.util.List<Tree> astChildren = new java.util.ArrayList<>();
        for (Object childObj : node.children) {
            Tree child = (Tree) childObj;
            Tree astChild = toAST(child);
            if (astChild != null) {
                astChildren.add(astChild);
            }
        }

        boolean isPassthrough = isPassthrough(rawLabel);

        if (isPassthrough) {
            if (astChildren.size() == 0) {
                return null;          // pure noise, drop it
            } else if (astChildren.size() == 1) {
                return astChildren.get(0); // transparent wrapper, skip it
            }
            // 2+ children: keep this node as a structural container
        }

        // Build the pruned node with only meaningful children
        Tree astNode = new Tree(node.data);
        for (Tree c : astChildren) {
            astNode.addChild(c);
        }
        return astNode;
    }

    // Check if a label belongs to the passthrough set.
    // Handles both raw non-terminal names and terminal labels like "semi (;)"
    private boolean isPassthrough(String rawLabel) {
        if (PASSTHROUGH_NODES.contains(rawLabel)) return true;
        // Also drop pure punctuation terminals by their token name
        // e.g. "semi (;)", "l_brace ({)"
        for (String p : PASSTHROUGH_NODES) {
            if (rawLabel.startsWith(p + " (")) return true;
        }
        return false;
    }

    // Extract just the label part before " (" for terminals like "id (x)"
    private String rawLabel(String data) {
        int idx = data.indexOf(" (");
        return idx >= 0 ? data.substring(0, idx) : data;
    }

    private String normalizeToken(String tokenName) {
        if (tokenName.equals("[EOF]")) return "eof";
        return tokenName.replaceAll("[<>]", ""); 
    }

    private void printToken(Token token) {
        if (currentPrintLine != -1 && token.line > currentPrintLine) {
            System.out.println(); 
        }
        currentPrintLine = token.line;

        String name = token.tokenName.replaceAll("[<>]", ""); 
        
        if (name.equals("id") || name.equals("numlit") || name.equals("stringlit")) {
            String displayLabel = name;
            if (name.equals("id")) displayLabel = "identifier";
            if (name.equals("numlit")) displayLabel = "num_lit";
            if (name.equals("stringlit")) displayLabel = "string_lit";
            
            System.out.print(token.lexeme + " (" + displayLabel + ") ");
        } else {
            System.out.print(name + " ");
        }
    }
}