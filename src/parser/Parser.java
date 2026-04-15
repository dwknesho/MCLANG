package parser;

import lexer.Scanner;
import lexer.Token;
import errors.SyntaxException;
import grtree.Tree;
import grtree.TreeScrollFrame;
import java.util.Stack;

public class Parser {
    private Scanner scanner;
    private ParseTable table;

    public Parser(Scanner scanner, ParseTable table) {
        this.scanner = scanner;
        this.table = table;
    }

    public void parse() throws Exception {
        Stack<Tree> stack = new Stack<>();
        
        // 1. Initialize the Root and Stack
        Tree root = new Tree("PROGRAM"); 
        
        stack.push(new Tree("eof")); // Push End of File marker
        stack.push(root);            // Push the Start Symbol

        Token currentToken = scanner.getNextToken();
        System.out.println("Starting Syntax Analysis...");

        // 2. The Parsing Loop
        while (!stack.isEmpty()) {
            Tree topNode = stack.pop();
            String topSymbol = topNode.data;

            // Ignore Epsilon (it just disappears from the stack)
            if (topSymbol.equals("ε")) {
                continue; 
            }

            // Normalize the Lexer's token name (e.g., "<id>" -> "id", "[EOF]" -> "eof")
            String tokenSymbol = normalizeToken(currentToken.tokenName);

            // 3. Match Terminals
            if (table.isTerminal(topSymbol)) {
                if (topSymbol.equals(tokenSymbol)) {

                    printToken(currentToken);
                    
                    // Update the tree node to show the actual word typed in the code!
                    topNode.data = topSymbol + " (" + currentToken.lexeme + ")";
                    
                    if (!tokenSymbol.equals("eof")) {
                        currentToken = scanner.getNextToken(); // Move to the next token
                    }
                // ...
                } else {
                    throw new SyntaxException("Expected '" + topSymbol + "' but found '" + currentToken.lexeme + "'", 
                                            currentToken.lexeme, currentToken.line, currentToken.col);
                }
            } 
            // 4. Derive Non-Terminals
            else {
                String[] rhs = table.getRule(topSymbol, tokenSymbol);
                
                if (rhs == null) {
                    throw new SyntaxException("Unexpected token '" + currentToken.lexeme + "' while parsing " + topSymbol, 
                                            currentToken.lexeme, currentToken.line, currentToken.col);
                }

                // Create child Tree nodes and link them to the parent
                Tree[] children = new Tree[rhs.length];
                for (int i = 0; i < rhs.length; i++) {
                    children[i] = new Tree(rhs[i]);
                    topNode.addChild(children[i]); 
                }

                // Push children to stack in REVERSE order
                for (int i = rhs.length - 1; i >= 0; i--) {
                    stack.push(children[i]);
                }
            }
        }
        
        System.out.println("Parsing Successful!");
        
        // 5. Trigger the Professor's Visualizer!
        new TreeScrollFrame(root);
    }

    private String normalizeToken(String tokenName) {
        if (tokenName.equals("[EOF]")) return "eof";
        return tokenName.replaceAll("[<>]", ""); // Strip the angle brackets your scanner makes
    }

    private void printToken(Token token) {
    String name = token.tokenName.replaceAll("[<>]", ""); // Strip brackets
    
    // If it's an ID or Literal, print the actual typed lexeme
    if (name.equals("id") || name.equals("numlit") || name.equals("stringlit")) {
        System.out.print(token.lexeme + " ");
    } 
    // Add line breaks after semicolons and brackets for readability
    else if (name.equals("semi") || name.equals("l_brace") || name.equals("r_brace") || name.equals("start") || name.equals("end")) {
        System.out.println(name);
    } 
    // Otherwise, just print the token name
    else {
        System.out.print(name + " ");
    }
}
}