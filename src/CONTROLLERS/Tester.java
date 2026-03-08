package CONTROLLERS;

import MODEL.Token;
import MODEL.SymbolTable;
import MODEL.ErrorReporter;
import MODEL.LexicalException;
import java.io.IOException;

public class Tester {
    private static SymbolTable symbolTable = new SymbolTable();
    private static ErrorReporter errorReporter = new ErrorReporter();

    public static void main(String[] args) {
        String filePath = "MCLANG/test/simple.txt"; 
        Scanner scanner = new Scanner(filePath);

        System.out.println("\nPHASE 2 COMPILER\n");

        try {
            Token token = null;
            boolean foundOrderEnd = false;  // Flag for ORDER_END

            do {
                try {
                    token = scanner.getNextToken();

                    // Error if there is anything after the first ORDER_END
                    if (foundOrderEnd && !token.tokenName.equals("[EOF]")) {
                        errorReporter.reportPostOrderEndError(token.line, token.col, token.lexeme);
                        System.out.print("<ERROR:" + errorReporter.getErrorCount() + "> ");
                        continue;   // Check for the next token immediately after an error
                    }

                    if (token.tokenName.equals("<end>")){
                        foundOrderEnd = true;
                    }

                    // Process valid tokens
                    processToken(token);

                } catch (LexicalException le) {
                    errorReporter.reportLexicalError(le.line, le.col, le.getMessage(), le.lexeme);
                    System.out.print("<ERROR:" + errorReporter.getErrorCount() + "> ");
                    
                    // Create dummy token to keep the loop going
                    token = new Token("<error>", le.lexeme, null, le.line, le.col);
                }

            } while (token == null || !token.tokenName.equals("[EOF]"));

            // Print summary of errors and identifiers
            errorReporter.printSummary();
            symbolTable.printTable();

        } catch (IOException e) {
            System.err.println("Fatal Error: " + e.getMessage());
        }
    }

    private static void processToken(Token token) {
        if (token.tokenName.equals("<id>")) {
            symbolTable.add(token.lexeme, "type: id");          // Adds id to the symbol table
            System.out.print(" " + token.lexeme + " ");                 // Prints the actual id written
        } else if (token.tokenName.equals("<numlit>") || token.tokenName.equals("<stringlit>")) {
            System.out.print(" " + token.lexeme + " ");                 // Prnts the actual literal written
        } else {
            formatText(token);                                          
        }
    }

    // For readability in the output
    private static void formatText(Token token) {
        if (token.tokenName.equals("[EOF]")) {
            System.out.println("\n[EOF]");
        } else if (isNewlineToken(token.tokenName)) {
            System.out.println(token.tokenName); 
        } else {        
            System.out.print(token.tokenName + " ");
        }
    }

    private static boolean isNewlineToken(String name) {
        return 
            name.equals("<semi>") ||
            name.equals("<l_brace>") || 
            name.equals("<r_brace>") ||
            name.equals("<start>") ||
            name.equals("<end>");
    }
}