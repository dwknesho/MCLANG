package semantic;

import java.util.LinkedHashMap;
import java.util.Map;

public class SymbolTable {
    // Map the Lexeme (Variable Name) to its Attributes (Type and Value)
    private Map<String, VariableAttributes> table = new LinkedHashMap<>();

    // 1. Called when a variable is declared (e.g., PRICE x;)
    public void declareVariable(String lexeme, String dataType) {
        if (table.containsKey(lexeme)) {
            throw new RuntimeException("Semantic Error: Variable '" + lexeme + "' is already declared!");
        }
        // Initialize with the data type and a null value
        table.put(lexeme, new VariableAttributes(dataType, null));
    }

    // 2. Called when a variable is assigned a value (e.g., x = 5;)
    public void assignValue(String lexeme, Object value) {
        if (!table.containsKey(lexeme)) {
            throw new RuntimeException("Semantic Error: Variable '" + lexeme + "' has not been declared!");
        }
        table.get(lexeme).value = value;
    }

    // 3. Look up a variable's details
    public VariableAttributes getAttributes(String lexeme) {
        return table.get(lexeme);
    }

    // 4. Print the formatted table
    public void printTable() {
        System.out.println("\n=======================================================");
        System.out.println("                     SYMBOL TABLE                      ");
        System.out.println("=======================================================");
        System.out.printf("%-20s | %-12s | %-15s\n", "IDENTIFIER (Lexeme)", "DATA TYPE", "VALUE");
        System.out.println("-------------------------------------------------------");
        for (Map.Entry<String, VariableAttributes> entry : table.entrySet()) {
            System.out.printf("%-20s | %-12s | %-15s\n", 
                entry.getKey(), 
                entry.getValue().dataType, 
                entry.getValue().value == null ? "null" : entry.getValue().value.toString());
        }
        System.out.println("=======================================================\n");
    }

    // Helper Class to store multiple attributes per identifier
    public class VariableAttributes {
        public String dataType; // e.g., "PRICE", "RECIPE"
        public Object value;    // e.g., 150.5, "Hello"

        public VariableAttributes(String dataType, Object value) {
            this.dataType = dataType;
            this.value = value;
        }
    }
}