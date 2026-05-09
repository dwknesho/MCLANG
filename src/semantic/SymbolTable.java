package semantic;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Stack;

public class SymbolTable {
    
    // UPGRADE: A Stack of maps to handle infinite layers of nested { } blocks
    private Stack<Map<String, VariableAttributes>> scopes;

    public SymbolTable() {
        scopes = new Stack<>();
        // Create the Global Scope immediately when the table is created
        enterScope(); 
    }

    // --- SCOPE MANAGEMENT ---

    // Called when the interpreter hits a '{' (Start of a block)
    public void enterScope() {
        scopes.push(new LinkedHashMap<>());
    }

    // Called when the interpreter hits a '}' (End of a block)
    public void exitScope() {
        if (scopes.size() > 1) {
            scopes.pop(); // Destroys all local variables in this block
        }
    }

    // --- VARIABLE MANAGEMENT ---

    // 1. Called when a variable is declared (e.g., PRICE x;)
    public void declareVariable(String lexeme, String dataType) {
        // We only check the CURRENT (top) scope to allow variable shadowing
        Map<String, VariableAttributes> currentScope = scopes.peek();
        
        if (currentScope.containsKey(lexeme)) {
            throw new RuntimeException("Semantic Error: Variable '" + lexeme + "' is already declared in this scope!");
        }
        
        // Initialize with the data type and a null value
        currentScope.put(lexeme, new VariableAttributes(dataType, null));
    }

    // 2. Called when a variable is assigned a value (e.g., x = 5;)
    public void assignValue(String lexeme, Object value) {
        Map<String, VariableAttributes> targetScope = findScopeContaining(lexeme);
        
        if (targetScope == null) {
            throw new RuntimeException("Semantic Error: Variable '" + lexeme + "' has not been declared!");
        }
        targetScope.get(lexeme).value = value;
    }

    // 3. Look up a variable's details
    public VariableAttributes getAttributes(String lexeme) {
        Map<String, VariableAttributes> targetScope = findScopeContaining(lexeme);
        
        if (targetScope == null) {
            throw new RuntimeException("Semantic Error: Variable '" + lexeme + "' has not been declared!");
        }
        return targetScope.get(lexeme);
    }

    // Searches from the innermost scope (top of stack) down to the global scope
    private Map<String, VariableAttributes> findScopeContaining(String lexeme) {
        for (int i = scopes.size() - 1; i >= 0; i--) {
            Map<String, VariableAttributes> scope = scopes.get(i);
            if (scope.containsKey(lexeme)) {
                return scope;
            }
        }
        return null; // Not found anywhere
    }

    // 4. Print the formatted table
    public void printTable() {
        System.out.println("\n=======================================================");
        System.out.println("                     SYMBOL TABLE                      ");
        System.out.println("=======================================================");
        System.out.printf("%-20s | %-12s | %-15s\n", "IDENTIFIER (Lexeme)", "DATA TYPE", "VALUE");
        System.out.println("-------------------------------------------------------");
        
        // Iterate through all active scopes
        for (int i = 0; i < scopes.size(); i++) {
            for (Map.Entry<String, VariableAttributes> entry : scopes.get(i).entrySet()) {
                System.out.printf("%-20s | %-12s | %-15s\n", 
                    entry.getKey(), 
                    entry.getValue().dataType, 
                    entry.getValue().value == null ? "null" : entry.getValue().value.toString());
            }
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