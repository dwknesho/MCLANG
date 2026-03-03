import java.io.IOException;

public class Tester {
    public static void main(String[] args) {
        // Path to your test file (relative to the project root)
        String testFilePath = "test/simple.txt";

        try {
            Scanner scanner = new Scanner(testFilePath);

            System.out.println("Scanning file: " + testFilePath);
            System.out.println("---------------------------------------------------------------------------");
            System.out.printf("%-20s | %-15s | %-10s | %-5s | %-5s%n", 
                              "TOKEN NAME", "LEXEME", "VALUE", "LINE", "COL");
            System.out.println("---------------------------------------------------------------------------");

            Token token;
            // Loop until we hit the EOF token 
            while (true) {
                token = scanner.getNextToken();
                
                // Format the output using the public fields from the Token class
                System.out.printf("%-20s | %-15s | %-10s | %-5d | %-5d%n", 
                    token.tokenName, 
                    token.lexeme, 
                    (token.value == null ? "null" : token.value), 
                    token.line, 
                    token.col);

                // Break the loop if the scanner returns the [EOF] token
                if (token.tokenName.equals("[EOF]")) {
                    break;
                }
            }

            System.out.println("---------------------------------------------------------------------------");
            System.out.println("Scan complete.");

        } catch (IOException e) {
            System.err.println("Error reading the file: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Unexpected error during scanning: " + e.getMessage());
            e.printStackTrace();
        }
    }
}