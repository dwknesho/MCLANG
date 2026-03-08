package MODEL;

import java.util.LinkedHashMap;
import java.util.Map;

public class SymbolTable {
    private Map<String, String> table;

    public SymbolTable() {
        this.table = new LinkedHashMap<>();
    }

    // Add a new identifier to the table
    public void add(String name, String details) {
        if (!table.containsKey(name)) {
            table.put(name, details);
        }
    }

    // Look up an identifier
    public String get(String name) {
        return table.get(name);
    }

    // Check if an identifier exists
    public boolean contains(String name) {
        return table.containsKey(name);
    }

    // Print the symbol table
    public void printTable() {
        System.out.println("\nSymbol Table\n");
        for (String key : table.keySet()) {
            System.out.println("Identifier: " + key);
        }
    }
}
