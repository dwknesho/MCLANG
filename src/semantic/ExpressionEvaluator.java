package semantic;

import grtree.Tree;

public class ExpressionEvaluator {
    private SymbolTable symTable;

    public ExpressionEvaluator(SymbolTable symTable) {
        this.symTable = symTable;
    }

    public RuntimeValue evaluate(Tree node) {
        if (node == null) return new RuntimeValue("UNKNOWN", null);
        String label = node.data;

        // 1. Unwrap Structural Passthroughs
        if (label.equals("EXPRESSION") || label.equals("PRIMARY") || label.equals("LITERAL") || label.equals("UNARY")) {
            return evaluate((Tree) node.children.get(0));
        }

        // 2. Handle Right-Recursive Operator Chains (LL(1) unwrapping)
        if (label.equals("LOGIC_OR") || label.equals("LOGIC_AND") || label.equals("EQUALITY") || 
            label.equals("RELATIONAL") || label.equals("ADDITIVE") || label.equals("MULTIPLICATIVE")) {
            
            RuntimeValue leftVal = evaluate((Tree) node.children.get(0));
            if (node.children.size() > 1) {
                return evaluatePrime(leftVal, (Tree) node.children.get(1));
            }
            return leftVal;
        }

        // 3. Leaf Nodes (Values)
        if (label.startsWith("numlit")) {
            return new RuntimeValue("PRICE", Double.parseDouble(extractValue(label)));
        }
        if (label.startsWith("stringlit")) {
            String val = extractValue(label).replace("\"", "").replace("\\n", "\n"); 
            return new RuntimeValue("RECIPE", val);
        }
        if (label.startsWith("true") || label.startsWith("FRESH")) return new RuntimeValue("QUALITY", true);
        if (label.startsWith("false") || label.startsWith("EXPIRED")) return new RuntimeValue("QUALITY", false);
        
        // --- UPDATED IDENTIFIER & ARRAY READING ---
        if (label.startsWith("id") || label.equals("L_VALUE")) {
            // Unpack the ID whether it is raw or wrapped in an L_VALUE
            Tree idNode = label.equals("L_VALUE") ? (Tree) node.children.get(0) : node;
            String varName = extractValue(idNode.data);
            
            SymbolTable.VariableAttributes attr = symTable.getAttributes(varName);
            if (attr == null) throw new SemanticException("Variable '" + varName + "' is undeclared.");
            
            // Is it an Array Read? (e.g., scores[i])
            if (node.children != null && node.children.size() > 1 && ((Tree)node.children.get(1)).data.startsWith("l_brack")) {
                Tree indexExpr = (Tree) node.children.get(2);
                int index = (int) evaluate(indexExpr).asDouble();
                Object[] arr = (Object[]) attr.value;
                return new RuntimeValue(attr.dataType, arr[index]); // Return the specific slot
            }
            
            if (attr.value == null) throw new SemanticException("Variable '" + varName + "' is uninitialized.");
            return new RuntimeValue(attr.dataType, attr.value);
        }

        return new RuntimeValue("UNKNOWN", null);
    }

    // Handles left-to-right associativity for LL(1) Prime nodes
    private RuntimeValue evaluatePrime(RuntimeValue leftVal, Tree primeNode) {
        if (primeNode.children.isEmpty() || ((Tree)primeNode.children.get(0)).data.equals("ε")) {
            return leftVal;
        }

        String opNode = ((Tree) primeNode.children.get(0)).data; 
        RuntimeValue rightVal = evaluate((Tree) primeNode.children.get(1));
        RuntimeValue currentResult = performOperation(leftVal, opNode, rightVal);

        // If there is another operator in the chain (e.g., 5 + 3 + 2), keep going!
        if (primeNode.children.size() > 2) {
            return evaluatePrime(currentResult, (Tree) primeNode.children.get(2));
        }
        return currentResult;
    }

    private RuntimeValue performOperation(RuntimeValue left, String operator, RuntimeValue right) {
        // String Concatenation
        if (operator.startsWith("arith_add") && (left.type.equals("RECIPE") || right.type.equals("RECIPE"))) {
            return new RuntimeValue("RECIPE", left.asString() + right.asString());
        }

        // Math Operators (Level 3 & 4)
        if (operator.startsWith("arith_add")) return new RuntimeValue("PRICE", left.asDouble() + right.asDouble());
        if (operator.startsWith("arith_sub")) return new RuntimeValue("PRICE", left.asDouble() - right.asDouble());
        if (operator.startsWith("arith_mul")) return new RuntimeValue("PRICE", left.asDouble() * right.asDouble());
        if (operator.startsWith("arith_div")) return new RuntimeValue("PRICE", left.asDouble() / right.asDouble());
        if (operator.startsWith("arith_mod")) return new RuntimeValue("PRICE", left.asDouble() % right.asDouble());

        // Relational Operators (Level 5)
        if (operator.startsWith("rel_ls")) return new RuntimeValue("QUALITY", left.asDouble() < right.asDouble());
        if (operator.startsWith("rel_lse")) return new RuntimeValue("QUALITY", left.asDouble() <= right.asDouble());
        if (operator.startsWith("rel_gt")) return new RuntimeValue("QUALITY", left.asDouble() > right.asDouble());
        if (operator.startsWith("rel_gte")) return new RuntimeValue("QUALITY", left.asDouble() >= right.asDouble());

        // Equality Operators (Level 6)
        if (operator.startsWith("rel_eq")) return new RuntimeValue("QUALITY", left.value.equals(right.value));
        if (operator.startsWith("rel_neq")) return new RuntimeValue("QUALITY", !left.value.equals(right.value));

        // Logical Operators (Level 7 & 8)
        if (operator.startsWith("log_and")) return new RuntimeValue("QUALITY", left.asBoolean() && right.asBoolean());
        if (operator.startsWith("log_or")) return new RuntimeValue("QUALITY", left.asBoolean() || right.asBoolean());

        throw new SemanticException("Unknown Operator: " + operator);
    }

    private String extractValue(String nodeData) {
        int start = nodeData.indexOf('(') + 1;
        int end = nodeData.lastIndexOf(')');
        return start > 0 && end > start ? nodeData.substring(start, end) : nodeData;
    }
}