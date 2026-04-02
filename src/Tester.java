import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ArrayList;

public class Tester {

    // ── ONLY CHANGE 1: SymbolEntry replaces the plain String value ──
    static class SymbolEntry {
        String type;   // num, string, bool
        String value;  // literal value or "uninitialized"
        String scope;  // "global" or "local (level N)"

        SymbolEntry(String type, String value, String scope) {
            this.type  = type;
            this.value = value;
            this.scope = scope;
        }
    }

    // ── ONLY CHANGE 2: map uses SymbolEntry instead of String ──
    private static LinkedHashMap<String, SymbolEntry> symbolTable = new LinkedHashMap<>();
    private static List<String> errorList = new ArrayList<>();

    public static void main(String[] args) {
    
        String filePath = "C:\\\\\\\\Users\\\\\\\\mirai\\\\\\\\Documents\\\\\\\\Compiler\\\\\\\\MCLANG\\\\\\\\test\\\\\\\\program1.txt";
        Scanner scanner = new Scanner(filePath);
        int errorCounter = 0;

        // ── ONLY CHANGE 3: track scope and last seen type for symbol table ──
        int currentScope = 0;
        String pendingType = null;  // holds the type keyword when we see PRICE/RECIPE/QUALITY

        System.out.println("\nPHASE 2 COMPILER\n");

        try {
            Token token;
            Token prevToken = null; // track previous token for type+id pairing
            boolean foundOrderEnd = false;
            do {
                token = scanner.getNextToken();

                // to make sure nothing is written after ORDER_END
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
                    System.out.print("!!ERROR " + errorCounter + "!! ");
                    String detail = "Error " + errorCounter + " [Line " + token.line + ", Col " + token.col + "]: " + 
                                    token.errorMessage + " ('" + token.lexeme + "')";
                    errorList.add(detail);
                    continue;
                }

                if (token.tokenName.equals("<end>")){
                    foundOrderEnd = true;
                }

                // ── ONLY CHANGE 4: scope tracking on braces ──
                if (token.tokenName.equals("<l_brace>")) currentScope++;
                if (token.tokenName.equals("<r_brace>") && currentScope > 0) currentScope--;

                // ── ONLY CHANGE 5: detect type keyword to prepare for id ──
                if (isTypeToken(token.tokenName)) {
                    pendingType = token.tokenName;
                }

                // Print lexeme and add to Symbol Table
                if (token.tokenName.equals("<id>")) {
                    if (!symbolTable.containsKey(token.lexeme)) {
                        // ── ONLY CHANGE 6: use pendingType and currentScope ──
                        String type  = pendingType != null ? mapType(pendingType) : "unknown";
                        String scope = currentScope == 0 ? "global" : "local (level " + currentScope + ")";
                        symbolTable.put(token.lexeme, new SymbolEntry(type, "uninitialized", scope));
                    }
                    pendingType = null; // reset after use
                    System.out.print(" " + token.lexeme + " ");
                }
                // Print actual values ( for literals)
                else if (token.tokenName.equals("<numlit>") || token.tokenName.equals("<stringlit>")) {
                    Object val = token.value;

                    // ── ONLY CHANGE 7: if prev was assign_as, update last id's value ──
                    if (prevToken != null && prevToken.tokenName.equals("<assign_as>")) {
                        // find the last added id and update its value
                        String lastId = getLastId();
                        if (lastId != null) {
                            symbolTable.get(lastId).value = formatValue(token);
                        }
                    }
                   // if 5 print as 5, otherwise print as it 
                    if (val instanceof Double && ((Double) val) == Math.floor((Double) val)) {
                        System.out.print(" " + (int)(double)(Double) val + " ");
                    } else {
                        System.out.print(" " + val + " ");
                    }
                }
                // ── ONLY CHANGE 8: capture FRESH/EXPIRED as bool value ── ( for boolean)
                else if (token.tokenName.equals("<true>") || token.tokenName.equals("<false>")) {
                    if (prevToken != null && prevToken.tokenName.equals("<assign_as>")) {
                        String lastId = getLastId();
                        if (lastId != null) {
                            symbolTable.get(lastId).value = token.lexeme;
                        }
                    }
                    System.out.print(token.tokenName + " ");
                }
                // Print token names 
                else {
                    if (token.tokenName.equals("[EOF]")) {
                        System.out.println("\n[EOF]");
                    } else if (token.tokenName.equals("<semi>") || 
                            token.tokenName.equals("<l_brace>") || 
                            token.tokenName.equals("<r_brace>") || 
                            token.tokenName.equals("<start>") || 
                            token.tokenName.equals("<end>")) {
                        System.out.println(token.tokenName); 
                    } else {        
                        System.out.print(token.tokenName + " ");
                    }
                }

                prevToken = token;

            } while (!token.tokenName.equals("[EOF]"));

            if (!errorList.isEmpty()) {
                System.out.println("\nLEXICAL ERRORS");
                for (String error : errorList) {
                    System.out.println(error);
                }
            }

            // ── ONLY CHANGE 9: updated symbol table print format ──
            System.out.println("\nSymbol Table\n");
            System.out.printf("%-20s %-10s %-20s %-15s%n", "Identifier", "Type", "Value", "Scope");
            System.out.println("--------------------------------------------------------------------");
            for (String key : symbolTable.keySet()) {
                SymbolEntry e = symbolTable.get(key);
                System.out.printf("%-20s %-10s %-20s %-15s%n", key, e.type, e.value, e.scope);
            }

        } catch (IOException e) {
            System.err.println("Error reading tokens: " + e.getMessage());
        }
    }

    // Returns the last inserted key in the symbol table
    private static String getLastId() {
        String last = null;
        for (String key : symbolTable.keySet()) last = key;
        return last;
    }

    // Formats a literal token's value as a string
    private static String formatValue(Token tok) {
        if (tok.tokenName.equals("<numlit>")) {
            Object v = tok.value;
            if (v instanceof Double && (Double) v == Math.floor((Double) v)) {
                return String.valueOf((int)(double)(Double) v);
            }
            return String.valueOf(v);
        }
        if (tok.tokenName.equals("<stringlit>")) return tok.lexeme;
        return "uninitialized";
    }

    // Maps type token to readable string
    private static String mapType(String tokenName) {
        switch (tokenName) {
            case "<num_type>":    return "int";
            case "<string_type>": return "string";
            case "<bool_type>":   return "bool";
            case "<void>":        return "void";
            default:              return "unknown";
        }
    }

    private static boolean isTypeToken(String tokenName) {
        return tokenName.equals("<num_type>") ||
               tokenName.equals("<string_type>") ||
               tokenName.equals("<bool_type>") ||
               tokenName.equals("<void>");
    }
}