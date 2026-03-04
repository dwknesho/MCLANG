import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;

public class Tester {

    private static LinkedHashMap<String, String> symbolTable = new LinkedHashMap<>();

    public static void main(String[] args) {

        String filePath = "C:\\\\Users\\\\mirai\\\\Documents\\\\compiler\\\\MCLANG\\\\test\\\\simple.txt"; 
        Scanner scanner = new Scanner(filePath);

        try {
            Token token;
            do {
                token = scanner.getNextToken();
                
                // 1. Error Handling Requirement: Print errors if found
                if (token.tokenName.equals("<error>")) {
                    System.out.println("\nLEXICAL ERROR at line " + token.line + 
                                       ", col " + token.col + ": " + token.value + 
                                       " ('" + token.lexeme + "')");
                    continue; // Skip printing normal token formatting for errors
                }

                // 2. Identifier Requirement: Print lexeme and add to Symbol Table
                if (token.tokenName.equals("<id>")) {
                    // Check if the id is already in the symbol table, if not, add it
                    if (!symbolTable.containsKey(token.lexeme)) {
                        symbolTable.put(token.lexeme, "type: id"); 
                    }
                    System.out.print(" " + token.lexeme + " ");
                } 
                // 3. Constant Values Requirement: Print their actual values
                else if (token.tokenName.equals("<numlit>") || token.tokenName.equals("<stringlit>")) {
                    System.out.print(" " + token.value + " ");
                } 
                // 4. Reserved Words & Operators Requirement: Print token names
                else {
                    // Formatting logic to make the console output look cleaner
                    if (token.tokenName.equals("[EOF]")) {
                        System.out.println("\n[EOF]");
                    } else if (token.tokenName.equals("<semi>") || token.tokenName.equals("<l_brace>") || token.tokenName.equals("<r_brace>")) {
                        // Drop a new line after semicolons or brackets so it reads like code
                        System.out.println(token.tokenName); 
                    } else {
                        System.out.print(token.tokenName + " ");
                    }
                }
            } while (!token.tokenName.equals("[EOF]"));

            // Print the final symbol table so the professor can verify it works
            System.out.println("\nSymbol Table");
            for (String key : symbolTable.keySet()) {
                System.out.println("Identifier: " + key + " | Details: " + symbolTable.get(key));
            }

        } catch (IOException e) {
            System.err.println("Error reading tokens: " + e.getMessage());
        }
    }
}