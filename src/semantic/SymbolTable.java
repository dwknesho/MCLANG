package semantic;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Stack;
import java.util.ArrayList;
import java.util.List;
import grtree.Tree;
// Phase 4 upgrade: SymbolTable now supports scoping.
// Each time we enter a block (WANT, REFILL, PREP, etc.) we push a new scope.
// When we leave the block we pop it. Variable lookup walks the scope stack
// from innermost to outermost, which gives us proper shadowing for free.
public class SymbolTable {

    // Each scope is its own map. The stack grows when we enter a block.
    private Stack<Map<String, VariableAttributes>> scopeStack = new Stack<>();

    // Function signatures are stored separately — they live at global scope always
    private Map<String, FunctionAttributes> functionTable = new LinkedHashMap<>();

    public SymbolTable() {
        // Push the global scope immediately so there's always one scope available
        scopeStack.push(new LinkedHashMap<>());
    }

    // Call this when entering any block: WANT, ONLY, SIDE, REFILL, PREP, STIR, TASK body
    public void pushScope() {
        scopeStack.push(new LinkedHashMap<>());
    }

    // Call this when leaving a block
    public void popScope() {
        if (scopeStack.size() > 1) {
            scopeStack.pop();
        }
    }

    // Declares a variable in the CURRENT (innermost) scope
    public void declareVariable(String lexeme, String dataType) {
        Map<String, VariableAttributes> currentScope = scopeStack.peek();
        if (currentScope.containsKey(lexeme)) {
            throw new SemanticException("Variable '" + lexeme + "' is already declared in this scope");
        }
        currentScope.put(lexeme, new VariableAttributes(dataType, null));
    }

    // Assigns a value — walks scopes from inner to outer to find the variable
    public void assignValue(String lexeme, Object value) {
        VariableAttributes attr = getAttributes(lexeme);
        if (attr == null) {
            throw new SemanticException("Variable '" + lexeme + "' has not been declared");
        }
        attr.value = value;
    }

    // Looks up a variable — searches from innermost scope outward
    public VariableAttributes getAttributes(String lexeme) {
        for (int i = scopeStack.size() - 1; i >= 0; i--) {
            if (scopeStack.get(i).containsKey(lexeme)) {
                return scopeStack.get(i).get(lexeme);
            }
        }
        return null; // not found in any scope
    }

    // Registers a function signature
    public void declareFunction(String name, String returnType, List<String> paramTypes, List<String> paramNames) {
        if (functionTable.containsKey(name)) {
            throw new SemanticException("Function '" + name + "' is already declared");
        }
        functionTable.put(name, new FunctionAttributes(returnType, paramTypes, paramNames));
    }

    public FunctionAttributes getFunctionAttributes(String name) {
        return functionTable.get(name);
    }

    // Prints the full symbol table (all scopes flattened + functions)
    public void printTable() {
        System.out.println("\n=======================================================");
        System.out.println("                     SYMBOL TABLE                      ");
        System.out.println("=======================================================");
        System.out.printf("%-20s | %-12s | %-15s\n", "IDENTIFIER", "DATA TYPE", "VALUE");
        System.out.println("-------------------------------------------------------");

        // Print all variables from global scope (index 0)
        for (Map.Entry<String, VariableAttributes> entry : scopeStack.get(0).entrySet()) {
            System.out.printf("%-20s | %-12s | %-15s\n",
                entry.getKey(),
                entry.getValue().dataType,
                entry.getValue().value == null ? "null" : entry.getValue().value.toString());
        }

        // Print functions
        if (!functionTable.isEmpty()) {
            System.out.println("-------------------------------------------------------");
            System.out.printf("%-20s | %-12s | %-15s\n", "FUNCTION", "RETURN TYPE", "PARAMS");
            System.out.println("-------------------------------------------------------");
            for (Map.Entry<String, FunctionAttributes> entry : functionTable.entrySet()) {
                System.out.printf("%-20s | %-12s | %-15s\n",
                    entry.getKey(),
                    entry.getValue().returnType,
                    entry.getValue().paramTypes.toString());
            }
        }

        System.out.println("=======================================================\n");
    }

    // Variable attributes: type + current value
    public static class VariableAttributes {
        public String dataType;
        public Object value;

        public VariableAttributes(String dataType, Object value) {
            this.dataType = dataType;
            this.value = value;
        }
    }

    // Function attributes: return type + ordered param types and names
    public static class FunctionAttributes {
        public String returnType;
        public List<String> paramTypes;
        public List<String> paramNames;
        public Tree body; // the AST node for the function body — stored so we can call it

        public FunctionAttributes(String returnType, List<String> paramTypes, List<String> paramNames) {
            this.returnType = returnType;
            this.paramTypes = paramTypes;
            this.paramNames = paramNames;
        }
    }
}