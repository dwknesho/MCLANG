import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ArrayList;

public class Tester {

    private static LinkedHashMap<String, String> symbolTable = new LinkedHashMap<>();
    private static List<String> errorList = new ArrayList<>(); // To store error details

    public static void main(String[] args) {
    
        String filePath = "MCLANG/test/simple.txt";
        Scanner scanner = new Scanner(filePath);
        int errorCounter = 0;

        System.out.println("\nPHASE 2 COMPILER\n");

        try {
            Token token;
            boolean foundOrderEnd = false; //tracks eof
            do {
                token = scanner.getNextToken();
                //to make sure nothing is written after ORDER_END
                if (foundOrderEnd && !token.tokenName.equals("[EOF]")) {
                    errorCounter++;
                    System.out.print("!!ERROR " + errorCounter + "!! ");
                    String detail = "Error " + errorCounter + " [Line " + token.line + ", Col " + token.col + "]: " +
                                    "Nothing is allowed after ORDER_END ('" + token.lexeme + "')";
                    errorList.add(detail);
                    
                    continue;
                }
                
                // Print errors if found
                if (token.tokenName.equals("<error>")) {
                    errorCounter++;
                    // Print the placeholder in the code output
                    System.out.print("!!ERROR " + errorCounter + "!! ");
                    
                    // Save the detailed message for the list at the bottom
                    String detail = "Error " + errorCounter + " [Line " + token.line + ", Col " + token.col + "]: " + 
                                    token.errorMessage + " ('" + token.lexeme + "')";
                    errorList.add(detail);
                    continue;
                }

                if (token.tokenName.equals("<end>")){
                    foundOrderEnd = true;
                }

                //Print lexeme and add to Symbol Table
                if (token.tokenName.equals("<id>")) {
                    // Check if the id is already in the symbol table, if not, add it
                    if (!symbolTable.containsKey(token.lexeme)) {
                        symbolTable.put(token.lexeme, "type: id"); 
                    }
                    System.out.print(" " + token.lexeme + " ");
                } 
                //Print actual values
                else if (token.tokenName.equals("<numlit>") || token.tokenName.equals("<stringlit>")) {
                    Object val = token.value;
                    if (val instanceof Double && ((Double) val) == Math.floor((Double) val)) {
                        System.out.print(" " + (int)(double)(Double) val + " ");
                    } else {
                        System.out.print(" " + val + " ");
                    }
                } 
                // Print token names
                else {
                    // Formatting to make the console output look cleaner
                    if (token.tokenName.equals("[EOF]")) {
                        System.out.println("\n[EOF]");
                    } else if (token.tokenName.equals("<semi>") || 
                            token.tokenName.equals("<l_brace>") || 
                            token.tokenName.equals("<r_brace>") || 
                            token.tokenName.equals("<start>") || 
                            token.tokenName.equals("<end>")) {
                            // new line after semicolons, brackets, start, or end
                        System.out.println(token.tokenName); 
                    } else {        
                        System.out.print(token.tokenName + " ");
                    }
                }
            } while (!token.tokenName.equals("[EOF]"));

            if (!errorList.isEmpty()) {
                System.out.println("\nLEXICAL ERRORS");
                for (String error : errorList) {
                    System.out.println(error);
                }
            }

            // Print the final symbol table
            System.out.println("\nSymbol Table\n");
            for (String key : symbolTable.keySet()) {
                System.out.println("Identifier: " + key + " | Details: " + symbolTable.get(key));
            }

        } catch (IOException e) {
            System.err.println("Error reading tokens: " + e.getMessage());
        }
    }
}