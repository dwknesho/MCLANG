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
import java.util.List;
import java.util.ArrayList;

public class Parser {
    private Scanner scanner;
    private ParseTable table;
    private ErrorReporter reporter;
    
    private int currentPrintLine = -1; 
    private Token lastPrintedToken = null; 

    private static final Set<String> PASSTHROUGH_NODES = new HashSet<>();
    static {
        PASSTHROUGH_NODES.add("EXPRESSION");        
        PASSTHROUGH_NODES.add("LOGIC_OR");          
        PASSTHROUGH_NODES.add("LOGIC_AND");         
        PASSTHROUGH_NODES.add("EQUALITY");          
        PASSTHROUGH_NODES.add("RELATIONAL");        
        PASSTHROUGH_NODES.add("ADDITIVE");          
        PASSTHROUGH_NODES.add("MULTIPLICATIVE");    
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
        PASSTHROUGH_NODES.add("semi");
        PASSTHROUGH_NODES.add("l_brace");
        PASSTHROUGH_NODES.add("r_brace");
        PASSTHROUGH_NODES.add("l_paren");
        PASSTHROUGH_NODES.add("r_paren");
        PASSTHROUGH_NODES.add("comma");
        PASSTHROUGH_NODES.add("colon");
        PASSTHROUGH_NODES.add("ε");
        PASSTHROUGH_NODES.add("eof");
        PASSTHROUGH_NODES.add("start");
        PASSTHROUGH_NODES.add("end");
    }

    public Parser(Scanner scanner, ParseTable table, ErrorReporter reporter) {
        this.scanner = scanner;
        this.table = table;
        this.reporter = reporter;
    }
    
    // FIX: Removed the SymbolTable memory peek that caused the EOF crash!
    private Token fetchNextToken() {
        while (true) {
            try {
                return scanner.getNextToken();
            } catch (LexicalException le) {     
                reporter.reportLexicalError(le.line, le.col, le.getMessage(), le.lexeme);
                System.out.print("<ERROR " + reporter.getErrorCount() + ": " + le.lexeme + "> ");
            } catch (Exception e) {
                System.err.println("\n[SCANNER CRASH] " + e.getMessage());
                return new Token("eof", "", null, -1, -1);
            }
        }
    }

    // FIX: Returns the Tree!
    public Tree parse() throws Exception {
        Stack<Tree> stack = new Stack<>();
        Tree root = new Tree("PROGRAM"); 
        stack.push(root);            

        System.out.println("\nStarting Syntax Analysis...\n");
        Token currentToken = fetchNextToken();

        int step = 1;
        List<String> parseTrace = new ArrayList<>();    

        while (!stack.isEmpty()) {
            Tree topNode = stack.peek();                
            String topSymbol = topNode.data;

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
                    if (!tokenSymbol.equals("eof")) currentToken = fetchNextToken(); 
                } else {
                    actionStr = "Error: Expected '" + topSymbol + "'";
                    reporter.reportSyntaxError(currentToken.line, currentToken.col, 
                        "Expected '" + topSymbol + "' but found '" + currentToken.lexeme + "'");
                    printOnce(currentToken, true, reporter.getErrorCount());
                    stack.pop(); 
                }
            } else {
                String[] rhs = table.getRule(topSymbol, tokenSymbol);
                if (rhs == null) {
                    actionStr = "Error: No rule for " + topSymbol;
                    reporter.reportSyntaxError(currentToken.line, currentToken.col, 
                        "Unexpected token '" + currentToken.lexeme + "' while parsing " + topSymbol);
                    printOnce(currentToken, true, reporter.getErrorCount());
                    if (!tokenSymbol.equals("eof")) currentToken = fetchNextToken();
                    else stack.pop(); 
                } else {
                    actionStr = "Predict " + topSymbol + " -> " + String.join(" ", rhs);
                    stack.pop(); 
                    Tree[] children = new Tree[rhs.length];
                    for (int i = 0; i < rhs.length; i++) {
                        children[i] = new Tree(rhs[i]);
                        topNode.addChild(children[i]); 
                    }
                    for (int i = rhs.length - 1; i >= 0; i--) stack.push(children[i]);
                }
            }
            parseTrace.add(String.format("%-4d | %-70s | %-15s | %s", step++, truncate(currentStackStr, 70), inputStr, actionStr));
        }
        
        System.out.println("\n\nParsing Complete!\n");
        Tree ast = toAST(root);
        if (ast == null) ast = new Tree("PROGRAM"); 

        System.out.println("Abstract Syntax Tree built successfully.\n");
        new TreeScrollFrame(ast);
        
        return ast; // Return the tree to Tester!
    }

    // (Keep your existing toAST, isPassthrough, rawLabel, normalizeToken, printOnce, getStackString, truncate methods here)
    private Tree toAST(Tree node) {
        if (node == null) return null;
        String rawLabel = rawLabel(node.data);
        List<Tree> astChildren = new ArrayList<>();
        for (Object childObj : node.children) {
            Tree astChild = toAST((Tree) childObj);
            if (astChild != null) astChildren.add(astChild);
        }
        if (isPassthrough(rawLabel)) {
            if (astChildren.isEmpty()) return null;              
            else if (astChildren.size() == 1) return astChildren.get(0);   
        }
        Tree astNode = new Tree(node.data);
        for (Tree c : astChildren) astNode.addChild(c);
        return astNode;
    }

    private boolean isPassthrough(String rawLabel) {
        if (PASSTHROUGH_NODES.contains(rawLabel)) return true;
        for (String p : PASSTHROUGH_NODES) if (rawLabel.startsWith(p + " (")) return true;
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
        if (token == lastPrintedToken || token.tokenName.equals("[EOF]") || token.tokenName.equals("eof")) return;
        lastPrintedToken = token; 
        if (currentPrintLine != -1 && token.line > currentPrintLine) System.out.println(); 
        currentPrintLine = token.line;
        String name = token.tokenName.replaceAll("[<>]", ""); 
        String displayLabel = name;
        if (name.equals("id")) displayLabel = "identifier";
        if (name.equals("numlit")) displayLabel = "num_lit";
        if (name.equals("stringlit")) displayLabel = "string_lit";
        
        String formattedStr = (name.equals("id") || name.equals("numlit") || name.equals("stringlit")) ? 
            token.lexeme + " (" + displayLabel + ")" : name;

        if (asError) System.out.print("<ERROR " + errNum + ": " + formattedStr + "> ");
        else System.out.print(formattedStr + " ");
    }

    private String getStackString(Stack<Tree> stack) {
        StringBuilder sb = new StringBuilder();
        for (int i = stack.size() - 1; i >= 0; i--) sb.append(rawLabel(stack.get(i).data)).append(" ");
        return sb.toString().trim();
    }

    private String truncate(String str, int len) {
        return str.length() <= len ? str : str.substring(0, len - 3) + "...";
    }
}