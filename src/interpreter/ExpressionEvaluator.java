package interpreter;

import grtree.Tree;
import semantic.SymbolTable;

public class ExpressionEvaluator {
    private SymbolTable symTable;
    private Interpreter interpreter;

    public ExpressionEvaluator(SymbolTable symTable, Interpreter interpreter) {
        this.symTable = symTable;
        this.interpreter = interpreter;
    }

    public Object evaluate(Tree node) {
        if (node == null) return null;
        String label = rawLabel(node.data);

        switch(label) {
            case "EXPRESSION": case "LOGIC_OR": case "LOGIC_AND": case "EQUALITY":
            case "RELATIONAL": case "ADDITIVE": case "MULTIPLICATIVE":
                Object leftVal = evaluate((Tree) node.children.get(0));
                
                // Safely check for prime nodes and ignore empty epsilon (ε) nodes
                if (node.children.size() > 1) {
                    Tree prime = (Tree) node.children.get(1);
                    if (!rawLabel(prime.data).equals("ε")) {
                        return evaluatePrime(prime, leftVal);
                    }
                }
                return leftVal;

            case "UNARY":
                if (node.children.size() == 2) { 
                    return applyUnary(rawLabel(((Tree) node.children.get(0)).data), evaluate((Tree) node.children.get(1)));
                } else return evaluate((Tree) node.children.get(0));

            case "PRIMARY":
                Tree firstChild = (Tree) node.children.get(0);
                String firstLabel = rawLabel(firstChild.data);

                if (firstLabel.equals("id")) {
                    String varName = extractLexeme(firstChild.data);
                    if (node.children.size() > 1) {
                        Tree prime = (Tree) node.children.get(1);
                        if (!rawLabel(prime.data).equals("ε")) {
                            return evaluatePrimaryPrime(prime, varName);
                        }
                    } 
                    return getVariableValue(varName);

                } else if (firstLabel.equals("LITERAL")) {
                    return evaluate(firstChild);
                } else if (firstLabel.equals("CALL_STMT")) {
                    return interpreter.executeFunctionCall(extractLexeme(((Tree)firstChild.children.get(0)).data), (Tree)firstChild.children.get(2));
                }
                break;

            case "LITERAL":
                Tree litChild = (Tree) node.children.get(0);
                String litType = rawLabel(litChild.data);
                String lexeme = extractLexeme(litChild.data);
                
                if (litType.equals("numlit")) return Double.parseDouble(lexeme);
                if (litType.equals("stringlit")) return lexeme; 
                if (litType.equals("true")) return true;
                if (litType.equals("false")) return false;
                break;
        }

        if (node.children.size() == 1) return evaluate((Tree) node.children.get(0));
        throw new RuntimeException("Interpreter Error: Unrecognized expression node: " + label);
    }

    private Object evaluatePrime(Tree node, Object leftVal) {
        if (node == null || node.children.isEmpty() || rawLabel(node.data).equals("ε")) return leftVal;

        Object currentResult = applyBinary(rawLabel(((Tree) node.children.get(0)).data), leftVal, evaluate((Tree) node.children.get(1)));
        
        if (node.children.size() > 2) {
            Tree nextPrime = (Tree) node.children.get(2);
            if (!rawLabel(nextPrime.data).equals("ε")) {
                return evaluatePrime(nextPrime, currentResult);
            }
        }
        return currentResult;
    }

    private Object evaluatePrimaryPrime(Tree primeNode, String varName) {
        if (primeNode == null || primeNode.children.isEmpty() || rawLabel(primeNode.data).equals("ε")) {
            return getVariableValue(varName);
        }

        Tree firstChild = (Tree) primeNode.children.get(0);
        String firstLabel = rawLabel(firstChild.data);
        
        if (firstLabel.equals("ε")) {
            return getVariableValue(varName);
        }

        if (firstLabel.equals("l_brack")) {
            // FIXED: We extract the expression safely from the primeNode itself
            Object indexObj = evaluate((Tree) primeNode.children.get(1));
            if (!(indexObj instanceof Double)) throw new RuntimeException("Semantic Error: Array index for '" + varName + "' must be a PRICE.");
            return getArrayValue(varName, ((Double) indexObj).intValue());
        } 

        if (firstLabel.equals("ARG_LIST") || firstLabel.equals("MORE_ARGS")) {
            return interpreter.executeFunctionCall(varName, firstChild);
        }

        return interpreter.executeFunctionCall(varName, primeNode);
    }

    private Object getVariableValue(String varName) {
        if (interpreter.hasFunction(varName)) return interpreter.executeFunctionCall(varName, null);
        
        SymbolTable.VariableAttributes attr = symTable.getAttributes(varName);
        if (attr == null) throw new RuntimeException("Semantic Error: Variable '" + varName + "' not declared.");
        if (attr.isArray) throw new RuntimeException("Semantic Error: Array '" + varName + "' requires an index.");
        if (attr.value == null) throw new RuntimeException("Semantic Error: Variable '" + varName + "' is uninitialized.");
        
        return attr.value;
    }

    private Object getArrayValue(String varName, int index) {
        SymbolTable.VariableAttributes attr = symTable.getAttributes(varName);
        if (attr == null || !attr.isArray) throw new RuntimeException("Semantic Error: Array '" + varName + "' not declared.");
        if (index < 0 || index >= attr.size) throw new RuntimeException("Runtime Error: Index " + index + " out of bounds for '" + varName + "'.");
        return attr.arrayValues[index];
    }

    private Object applyBinary(String op, Object left, Object right) {
        switch(op) {
            case "arith_mul": requirePrice(left, right, "*"); return (Double) left * (Double) right;
            case "arith_div": requirePrice(left, right, "/"); if ((Double) right == 0.0) throw new RuntimeException("Runtime Error: Division by zero."); return (Double) left / (Double) right;
            case "arith_mod": requirePrice(left, right, "%"); if ((Double) right == 0.0) throw new RuntimeException("Runtime Error: Modulo by zero."); return (Double) left % (Double) right;
            case "arith_add":
                if (left instanceof String || right instanceof String) return left.toString() + right.toString(); 
                requirePrice(left, right, "+"); return (Double) left + (Double) right;
            case "arith_sub": requirePrice(left, right, "-"); return (Double) left - (Double) right;
            case "rel_ls": requirePrice(left, right, "<"); return (Double) left < (Double) right;
            case "rel_lse": requirePrice(left, right, "<="); return (Double) left <= (Double) right;
            case "rel_gt": requirePrice(left, right, ">"); return (Double) left > (Double) right;
            case "rel_gte": requirePrice(left, right, ">="); return (Double) left >= (Double) right;
            case "rel_eq": requireSameType(left, right, "=="); return left.equals(right);
            case "rel_neq": requireSameType(left, right, "!="); return !left.equals(right);
            case "log_and": requireQuality(left, right, "&&"); return (Boolean) left && (Boolean) right;
            case "log_or": requireQuality(left, right, "||"); return (Boolean) left || (Boolean) right;
            default: throw new RuntimeException("Interpreter Error: Unknown operator " + op);
        }
    }

    private Object applyUnary(String op, Object operand) {
        if (op.equals("log_not")) { if (!(operand instanceof Boolean)) throw new RuntimeException("Semantic Error: '!' requires QUALITY."); return !((Boolean) operand); }
        if (op.equals("arith_sub")) { if (!(operand instanceof Double)) throw new RuntimeException("Semantic Error: Unary '-' requires PRICE."); return -((Double) operand); }
        if (op.equals("arith_add")) { if (!(operand instanceof Double)) throw new RuntimeException("Semantic Error: Unary '+' requires PRICE."); return (Double) operand; }
        throw new RuntimeException("Interpreter Error: Unknown unary operator " + op);
    }

    private void requirePrice(Object left, Object right, String op) { if (!(left instanceof Double) || !(right instanceof Double)) throw new RuntimeException("Semantic Error: Operator '" + op + "' requires PRICE operands."); }
    private void requireQuality(Object left, Object right, String op) { if (!(left instanceof Boolean) || !(right instanceof Boolean)) throw new RuntimeException("Semantic Error: Operator '" + op + "' requires QUALITY operands."); }
    private void requireSameType(Object left, Object right, String op) { if (left.getClass() != right.getClass()) throw new RuntimeException("Semantic Error: Operator '" + op + "' requires operands of the exact same data type."); }
    private String rawLabel(String data) { int idx = data.indexOf(" ("); return idx >= 0 ? data.substring(0, idx).trim() : data.trim(); }
    private String extractLexeme(String data) {
        int start = data.indexOf("(");
        int end = data.lastIndexOf(")");
        if (start != -1 && end != -1) {
            String lexeme = data.substring(start + 1, end);
            if (lexeme.startsWith("\"") && lexeme.endsWith("\"")) {
                // Remove the surrounding quotes
                String innerString = lexeme.substring(1, lexeme.length() - 1);
                // Translate raw escape sequences into actual Java characters
                innerString = innerString.replace("\\n", "\n")
                                         .replace("\\t", "\t")
                                         .replace("\\\"", "\"")
                                         .replace("\\\\", "\\");
                return innerString;
            }
            return lexeme;
        }
        return data; 
    }
}