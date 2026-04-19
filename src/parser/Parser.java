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
import java.util.List;
import java.util.ArrayList;

public class Parser {
    private Scanner scanner;
    private ParseTable table;
    private ErrorReporter reporter;
    private SymbolTable symTable;
    
    private int currentPrintLine = -1; // Tracks which source line we last printed on, so we know when to add a newline
    private Token lastPrintedToken = null; // Stops duplicate printing during Panic Mode

     // This are the "noise' non terminals that will be PRUNED from the concrete tree to produce the AST.
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
     //Keeps asking the scanner for the next token until it gets a valid one.
    private Token fetchNextToken() {
        while (true) {
            try {
                Token t = scanner.getNextToken();

                // Add Identifiers to the Symbol Table
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
      //Is the main LL(1) parsing loop. It works like a stack machine
    public void parse() throws Exception {
        Stack<Tree> stack = new Stack<>();
        Tree root = new Tree("PROGRAM"); 
         // Push in reverse order: eof goes on first (bottom), PROGRAM on top
        stack.push(new Tree("eof")); 
        stack.push(root);            

        System.out.println("\nStarting Syntax Analysis...\n");
        Token currentToken = fetchNextToken();

        // Setup the trace log and step counter
        int step = 1;
        List<String> parseTrace = new ArrayList<>();

        while (!stack.isEmpty()) {
            Tree topNode = stack.peek(); 
            String topSymbol = topNode.data;

            // Capture current state for the Trace Table
            String currentStackStr = getStackString(stack);
            String inputStr = currentToken.lexeme;
            if (inputStr.isEmpty() && (currentToken.tokenName.equals("eof") || currentToken.tokenName.equals("[EOF]"))) {
                inputStr = "EOF";
            }
            String actionStr = "";

            if (topSymbol.equals("ε")) {
                actionStr = "Pop ε";
                stack.pop();
                parseTrace.add(String.format("%-4d | %-70s | %-15s | %s", step++, truncate(currentStackStr, 70), inputStr, actionStr));
                continue; 
            }

            String tokenSymbol = normalizeToken(currentToken.tokenName);

            if (table.isTerminal(topSymbol)) {
                if (topSymbol.equals(tokenSymbol)) {
                    actionStr = "Match '" + currentToken.lexeme + "'"; 
                    stack.pop(); 
                    
                    printOnce(currentToken, false, 0);
                    topNode.data = topSymbol + " (" + currentToken.lexeme + ")";
                    
                    if (!tokenSymbol.equals("eof")) {
                        currentToken = fetchNextToken(); 
                    }
                } else {
                    actionStr = "Error: Expected '" + topSymbol + "'";
                    reporter.reportSyntaxError(currentToken.line, currentToken.col, 
                        "Expected '" + topSymbol + "' but found '" + currentToken.lexeme + "'");
                    
                    printOnce(currentToken, true, reporter.getErrorCount());
                    stack.pop(); 
                }
            } 
            else {
                // Top of stack is a non-terminal — look up what rule to expand it with
                String[] rhs = table.getRule(topSymbol, tokenSymbol);
                 //No rule exists for this (non-terminal, token) combination.
                if (rhs == null) {
                    actionStr = "Error: No rule for " + topSymbol;
                    reporter.reportSyntaxError(currentToken.line, currentToken.col, 
                        "Unexpected token '" + currentToken.lexeme + "' while parsing " + topSymbol);
                    
                    printOnce(currentToken, true, reporter.getErrorCount());
                    
                    if (!tokenSymbol.equals("eof")) {
                        currentToken = fetchNextToken();
                    } else {
                         // Can't advance past EOF — pop the stuck non-terminal instead
                        stack.pop(); 
                    }
                } else {
                    actionStr = "Predict " + topSymbol + " -> " + String.join(" ", rhs);
                     // Found a valid rule. Pop the non-terminal, add its children to the tree,
                    stack.pop(); 
                    Tree[] children = new Tree[rhs.length];
                    for (int i = 0; i < rhs.length; i++) {
                        children[i] = new Tree(rhs[i]);
                        topNode.addChild(children[i]); 
                    }
                    // Push them onto the stack in REVERSE order so the leftmost symbol
                    for (int i = rhs.length - 1; i >= 0; i--) {
                        stack.push(children[i]);
                    }
                }
            }
            
            // Save the row to the trace table
            parseTrace.add(String.format("%-4d | %-70s | %-15s | %s", step++, truncate(currentStackStr, 70), inputStr, actionStr));
        }
        
        System.out.println("\n\nParsing Complete!\n");

        // Print the massive Trace Table before the AST
        System.out.println("=======================================================================================================================");
        System.out.println("                                               LL(1) PARSE TRACE                                                       ");
        System.out.println("=======================================================================================================================");
        System.out.printf("%-4s | %-70s | %-15s | %s\n", "STEP", "STACK (Top -> Bottom)", "INPUT", "ACTION");
        System.out.println("-----------------------------------------------------------------------------------------------------------------------");
        for (String row : parseTrace) {
            System.out.println(row);
        }
        System.out.println("=======================================================================================================================\n");

        // POST-PROCESS: Convert the concrete parse tree into an AST
        Tree ast = toAST(root);
        if (ast == null) ast = new Tree("PROGRAM"); 

        // ONLY draw the AST window if there are absolutely no errors!
        if (!reporter.hasErrors()) {
            System.out.println("Abstract Syntax Tree built successfully.\n");
            new TreeScrollFrame(ast);
        } else {
            System.out.println("[!] Syntax or Lexical errors detected. AST visualization skipped.\n");
        }
    }

 
    @SuppressWarnings("unchecked")
      // Recursively converts the concrete parse tree into an AST by removing all the passthrough nodes defined in PASSTHROUGH_NODES.
    private Tree toAST(Tree node) {
        if (node == null) return null;

        String rawLabel = rawLabel(node.data);
     //Recursively build AST versions of all children.
        List<Tree> astChildren = new ArrayList<>();
        for (Object childObj : node.children) {
            Tree child = (Tree) childObj;
            Tree astChild = toAST(child);
            if (astChild != null) {
                astChildren.add(astChild);
            }
        }

        boolean isPassthrough = isPassthrough(rawLabel);

        if (isPassthrough) {
            if (astChildren.isEmpty()) {
                return null;  // nothing meaningful here, drop it            
            } else if (astChildren.size() == 1) {
                return astChildren.get(0);   // just pass the single child up, skip this wrapper
            }
        }
          // Rebuild this node with only the meaningful children attached
        Tree astNode = new Tree(node.data);
        for (Tree c : astChildren) {
            astNode.addChild(c);
        }
        return astNode;
    }
   // Checks if a label is in our passthrough set.
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

    // Token names from the scanner come with angle brackets like "<if>" or "[EOF]".
    // The parse table uses clean names like "if" and "eof", so we strip the bracket
    private String normalizeToken(String tokenName) {
        if (tokenName.equals("[EOF]")) return "eof";
        return tokenName.replaceAll("[<>]", ""); 
    }

     // Prints a token to the console, making sure we never print the same token twice
    private void printOnce(Token token, boolean asError, int errNum) {
        // If we already printed this exact token, or it's an EOF, skip it!
        if (token == lastPrintedToken || token.tokenName.equals("[EOF]") || token.tokenName.equals("eof")) {
            return;
        }
        lastPrintedToken = token; 
        // Start a new line when we move to a new source line
        if (currentPrintLine != -1 && token.line > currentPrintLine) {
            System.out.println(); 
        }
        currentPrintLine = token.line;
          // Use friendlier display names for the three literal types
        String name = token.tokenName.replaceAll("[<>]", ""); 
        String displayLabel = name;
        if (name.equals("id")) displayLabel = "identifier";
        if (name.equals("numlit")) displayLabel = "num_lit";
        if (name.equals("stringlit")) displayLabel = "string_lit";
        
        // Literals show the actual written value; keywords/operators just show the token name
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

    // Converts the Stack into a clean string (Top of stack on the left)
    private String getStackString(Stack<Tree> stack) {
        StringBuilder sb = new StringBuilder();
        for (int i = stack.size() - 1; i >= 0; i--) {
            sb.append(rawLabel(stack.get(i).data)).append(" ");
        }
        return sb.toString().trim();
    }

    // Prevents massive stacks from breaking the table layout
    private String truncate(String str, int len) {
        if (str.length() <= len) return str;
        return str.substring(0, len - 3) + "...";
    }
}