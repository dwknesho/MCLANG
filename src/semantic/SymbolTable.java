package semantic;

import java.util.LinkedHashMap;
import java.util.Map;

public class SymbolTable {

    // Helper Class to store attributes, including Array configurations
    public class VariableAttributes {
        public String dataType;  // "PRICE", "RECIPE", "QUALITY", "TASK"
        public Object value;     // Value for scalar variables
        
        // Array specific fields
        public boolean isArray;
        public int size;
        public Object[] arrayValues;

        // Constructor for SCALAR variables
        public VariableAttributes(String dataType, Object value) {
            this.dataType = dataType;
            this.value = value;
            this.isArray = false;
        }

        // Constructor for ARRAY variables
        public VariableAttributes(String dataType, int size) {
            this.dataType = dataType;
            this.isArray = true;
            this.size = size;
            this.arrayValues = new Object[size];
            
            // Initialize arrays with default values based on MCLang types
            for(int i = 0; i < size; i++) {
                if(dataType.equals("PRICE")) arrayValues[i] = 0.0;
                else if(dataType.equals("QUALITY")) arrayValues[i] = false; // EXPIRED
                else if(dataType.equals("RECIPE")) arrayValues[i] = "";
            }
        }
    }

    // Node representing a single Lexical Scope
    public class Environment {
        public Environment parent; // Enclosing outer scope
        public Map<String, VariableAttributes> variables = new LinkedHashMap<>();

        public Environment(Environment parent) {
            this.parent = parent;
        }

        public void declareVariable(String name, String type) {
            if (variables.containsKey(name)) {
                throw new RuntimeException("Semantic Error: Variable '" + name + "' is already declared in this scope!");
            }
            // Set default scalar values
            Object defVal = null;
            if(type.equals("PRICE")) defVal = 0.0;
            else if(type.equals("QUALITY")) defVal = false;
            else if(type.equals("RECIPE")) defVal = "";
            
            variables.put(name, new VariableAttributes(type, defVal));
        }

        public void declareArray(String name, String type, int size) {
            if (variables.containsKey(name)) {
                throw new RuntimeException("Semantic Error: Array '" + name + "' is already declared in this scope!");
            }
            if (size <= 0) {
                throw new RuntimeException("Semantic Error: Array '" + name + "' size must be greater than 0!");
            }
            variables.put(name, new VariableAttributes(type, size));
        }

        // Looks up the variable in current scope, then climbs the tree to parents
        public VariableAttributes resolve(String name) {
            if (variables.containsKey(name)) {
                return variables.get(name);
            }
            if (parent != null) {
                return parent.resolve(name);
            }
            return null;
        }
    }

    private Environment globalEnv;
    private Environment currentEnv;

    public SymbolTable() {
        globalEnv = new Environment(null);
        currentEnv = globalEnv;
    }

    // --- SCOPE MANAGEMENT FOR INTERPRETER ---
    
    public void enterScope() {
        currentEnv = new Environment(currentEnv);
    }

    public void exitScope() {
        if (currentEnv.parent != null) {
            currentEnv = currentEnv.parent;
        }
    }
    
    public Environment getCurrentEnv() { return currentEnv; }
    
    // --- VARIABLE MANAGEMENT ---
    
    public void declareVariable(String lexeme, String dataType) {
        currentEnv.declareVariable(lexeme, dataType);
    }

    public void declareArray(String lexeme, String dataType, int size) {
        currentEnv.declareArray(lexeme, dataType, size);
    }

    public void assignValue(String lexeme, Object value) {
        VariableAttributes attr = currentEnv.resolve(lexeme);
        if (attr == null) {
            throw new RuntimeException("Semantic Error: Variable '" + lexeme + "' has not been declared!");
        }
        if (attr.isArray) {
            throw new RuntimeException("Semantic Error: '" + lexeme + "' is an array and requires an index for assignment.");
        }
        attr.value = value;
    }

    public void assignArrayValue(String lexeme, int index, Object value) {
        VariableAttributes attr = currentEnv.resolve(lexeme);
        if (attr == null) {
            throw new RuntimeException("Semantic Error: Array '" + lexeme + "' has not been declared!");
        }
        if (!attr.isArray) {
            throw new RuntimeException("Semantic Error: '" + lexeme + "' is not an array.");
        }
        if (index < 0 || index >= attr.size) {
            throw new RuntimeException("Runtime Error: Array index out of bounds for '" + lexeme + "' at index " + index);
        }
        attr.arrayValues[index] = value;
    }

    public VariableAttributes getAttributes(String lexeme) {
        return currentEnv.resolve(lexeme);
    }

    // --- CRITICAL PHASE 4 METHOD ---
    // The parser tentatively adds tokens to the symbol table. 
    // We must call reset() before interpreting the AST so we can declare them properly at runtime.
    public void reset() {
        globalEnv = new Environment(null);
        currentEnv = globalEnv;
    }

    // --- PRINTING ---
    public void printTable() {
        System.out.println("\n=====================================================================");
        System.out.println("                     SYMBOL TABLE (GLOBAL SCOPE)                     ");
        System.out.println("=====================================================================");
        System.out.printf("%-20s | %-12s | %-10s | %-15s\n", "IDENTIFIER", "DATA TYPE", "IS ARRAY", "VALUE/SIZE");
        System.out.println("---------------------------------------------------------------------");
        for (Map.Entry<String, VariableAttributes> entry : globalEnv.variables.entrySet()) {
            VariableAttributes attr = entry.getValue();
            String valStr;
            if (attr.isArray) {
                valStr = "Array[" + attr.size + "]";
            } else {
                valStr = attr.value == null ? "null" : attr.value.toString();
            }
            System.out.printf("%-20s | %-12s | %-10s | %-15s\n", 
                entry.getKey(), attr.dataType, attr.isArray, valStr);
        }
        System.out.println("=====================================================================\n");
    }

    public Environment getGlobalEnv() { 
        return globalEnv; 
    }
    
    public void setCurrentEnv(Environment env) { 
        this.currentEnv = env; 
    }
}