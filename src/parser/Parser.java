package parser;

import lexer.Scanner; 
import lexer.Token;
import semantic.SymbolTable;
import errors.SyntaxException;
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
    private Token lastPrintedToken = null; // Stops duplicate printing during Panic Mode

    // ---------------------------------------------------------------
    // These are the "noise" non-terminals that exist only to enforce
    // grammar structure but carry no semantic meaning on their own.
    // They will be PRUNED from the concrete tree to produce the AST.
    // ---------------------------------------------------------------
    private static final Set<String> PASSTHROUGH_NODES = new HashSet<>();
    static {
        // ── EXPRESSION CHAIN ──
        PASSTHROUGH_NODES.add("EXPRESSION");        
        PASSTHROUGH_NODES.add("LOGIC_OR");          
        PASSTHROUGH_NODES.add("LOGIC_AND");         
        PASSTHROUGH_NODES.add("EQUALITY");          
        PASSTHROUGH_NODES.add("RELATIONAL");        
        PASSTHROUGH_NODES.add("ADDITIVE");          
        PASSTHROUGH_NODES.add("MULTIPLICATIVE");    

        // ── PRIME NODES ──
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

        // ── PUNCTUATION TERMINALS ──
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

                // TEMPORARY PHASE 3 HACK: Add every <id> to the Symbol Table
                if (t != null && t.tokenName.equals("<id>")) {
                    if (symTable.getAttributes(t.lexeme) == null) {
                        symTable.declareVariable(t.lexeme, "Pending Phase 4");
                    }
                }
                
                return t;
            } catch (LexicalException le) {
                reporter.reportLexicalError(le.line, le.col, le.getMessage(), le.lexeme);
                // Print Lexical Error inline
                System.out.print("<ERROR " + reporter.getErrorCount() + ": " + le.lexeme + "> ");
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

        System.out.println("\nStarting Syntax Analysis...\n");
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
                    
                    // 1. NORMAL MATCH: Print normally!
                    printOnce(currentToken, false, 0);
                    
                    topNode.data = topSymbol + " (" + currentToken.lexeme + ")";
                    
                    if (!tokenSymbol.equals("eof")) {
                        currentToken = fetchNextToken(); 
                    }
                } else {
                    reporter.reportSyntaxError(currentToken.line, currentToken.col, 
                        "Expected '" + topSymbol + "' but found '" + currentToken.lexeme + "'");
                    
                    // 2. ERROR (Terminal Mismatch): Wrap in error tag!
                    printOnce(currentToken, true, reporter.getErrorCount());
                    
                    stack.pop(); 
                }
            } 
            else {
                String[] rhs = table.getRule(topSymbol, tokenSymbol);
                
                if (rhs == null) {
                    reporter.reportSyntaxError(currentToken.line, currentToken.col, 
                        "Unexpected token '" + currentToken.lexeme + "' while parsing " + topSymbol);
                    
                    // 3. ERROR (Blank Cell): Wrap in error tag!
                    printOnce(currentToken, true, reporter.getErrorCount());
                    
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
        
        System.out.println("\n\nParsing Complete!\n");

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
    // ---------------------------------------------------------------
    @SuppressWarnings("unchecked")
    private Tree toAST(Tree node) {
        if (node == null) return null;

        String rawLabel = rawLabel(node.data);

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
                return null;          
            } else if (astChildren.size() == 1) {
                return astChildren.get(0); 
            }
        }

        Tree astNode = new Tree(node.data);
        for (Tree c : astChildren) {
            astNode.addChild(c);
        }
        return astNode;
    }

    private boolean isPassthrough(String rawLabel) {
        if (PASSTHROUGH_NODES.contains(rawLabel)) return true;
        for (String p : PASSTHROUGH_NODES) {
            if (rawLabel.startsWith(p + " (")) return true;
        }
        return false;
    }

    private String rawLabel(String data) {
        int idx = data.indexOf(" (");
        return idx >= 0 ? data.substring(0, idx) : data;
    }

    private String normalizeToken(String tokenName) {
        if (tokenName.equals("[EOF]")) return "eof";
        return tokenName.replaceAll("[<>]", ""); 
    }

    private void printOnce(Token token, boolean asError, int errNum) {
        // If we already printed this exact token, or it's an EOF, skip it!
        if (token == lastPrintedToken || token.tokenName.equals("[EOF]") || token.tokenName.equals("eof")) {
            return;
        }
        lastPrintedToken = token; // Remember this token

        if (currentPrintLine != -1 && token.line > currentPrintLine) {
            System.out.println(); 
        }
        currentPrintLine = token.line;

        String name = token.tokenName.replaceAll("[<>]", ""); 
        String displayLabel = name;
        if (name.equals("id")) displayLabel = "identifier";
        if (name.equals("numlit")) displayLabel = "num_lit";
        if (name.equals("stringlit")) displayLabel = "string_lit";
        
        // Build the core token string
        String formattedStr;
        if (name.equals("id") || name.equals("numlit") || name.equals("stringlit")) {
            formattedStr = token.lexeme + " (" + displayLabel + ")";
        } else {
            formattedStr = name;
        }

        // Print it normally, or wrap it in the ERROR tag!
        if (asError) {
            System.out.print("<ERROR " + errNum + ": " + formattedStr + "> ");
        } else {
            System.out.print(formattedStr + " ");
        }
    }
}