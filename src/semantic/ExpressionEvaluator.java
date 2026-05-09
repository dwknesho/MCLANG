package semantic;

import grtree.Tree;

public class ExpressionEvaluator {
    private SymbolTable symTable;
    private Interpreter interpreter; 

    public ExpressionEvaluator(SymbolTable symTable) {
        this.symTable = symTable;
    }

    public void setInterpreter(Interpreter interpreter) {
        this.interpreter = interpreter;
    }

    public RuntimeValue evaluate(Tree node) {
        if (node == null) return new RuntimeValue("UNKNOWN", null);
        String label = node.data;

        if (label.equals("EXPRESSION") || label.equals("LITERAL") || label.equals("UNARY")) {
            return evaluate((Tree) node.children.get(0));
        }

        if (label.equals("LOGIC_OR") || label.equals("LOGIC_AND") || label.equals("EQUALITY") || 
            label.equals("RELATIONAL") || label.equals("ADDITIVE") || label.equals("MULTIPLICATIVE")) {
            RuntimeValue leftVal = evaluate((Tree) node.children.get(0));
            if (node.children.size() > 1) return evaluatePrime(leftVal, (Tree) node.children.get(1));
            return leftVal;
        }

        if (label.startsWith("numlit")) return new RuntimeValue("PRICE", Double.parseDouble(extractValue(label)));
        if (label.startsWith("stringlit")) return new RuntimeValue("RECIPE", extractValue(label).replace("\"", "").replace("\\n", "\n").replace("\\t", "\t")); 
        if (label.startsWith("true") || label.startsWith("FRESH")) return new RuntimeValue("QUALITY", true);
        if (label.startsWith("false") || label.startsWith("EXPIRED")) return new RuntimeValue("QUALITY", false);
        
        // --- BULLETPROOF VARIABLE, FUNCTION, & ARRAY EXTRACTION ---
        if (label.equals("PRIMARY") || label.equals("L_VALUE")) {
            if (!node.children.isEmpty()) {
                Tree firstChild = (Tree) node.children.get(0);
                if (firstChild.data.startsWith("l_paren")) return evaluate((Tree) node.children.get(1));
                if (firstChild.data.startsWith("LITERAL")) return evaluate(firstChild);
            }

            Tree idNode = findNode(node, "id");
            if (idNode != null) {
                String varName = extractValue(idNode.data);
                SymbolTable.VariableAttributes attr = symTable.getAttributes(varName);
                
                // Function Call check (e.g. math(x, y))
                if (findNode(node, "l_paren") != null) {
                    Tree argList = findNode(node, "ARG_LIST"); 
                    return executeFunctionCall(varName, argList, attr);
                }

                // Array Read check (e.g. scores[i])
                if (findNode(node, "l_brack") != null) {
                    Tree indexExpr = null;
                    Tree searchNode = node;
                    Tree prime = findNode(node, "PRIMARY'");
                    if (prime == null) prime = findNode(node, "L_VALUE'");
                    if (prime != null) searchNode = prime;

                    boolean foundBrack = false;
                    for (Object c : searchNode.children) {
                        Tree t = (Tree) c;
                        if (foundBrack && !t.data.startsWith("r_brack")) {
                            indexExpr = t; break;
                        }
                        if (t.data.startsWith("l_brack")) foundBrack = true;
                    }
                    if (indexExpr == null) indexExpr = findNode(node, "EXPRESSION"); 
                    
                    if (indexExpr != null) {
                        int index = (int) evaluate(indexExpr).asDouble();
                        Object[] arr = (Object[]) attr.value;
                        return new RuntimeValue(attr.dataType, arr[index]);
                    }
                }

                if (attr.value == null) throw new RuntimeException("Semantic Error: Variable '" + varName + "' is uninitialized.");
                return new RuntimeValue(attr.dataType, attr.value);
            }
        }
        
        if (label.equals("CALL_STMT")) {
            Tree idNode = findNode(node, "id");
            if (idNode != null) {
                String varName = extractValue(idNode.data);
                SymbolTable.VariableAttributes attr = symTable.getAttributes(varName);
                Tree argList = findNode(node, "ARG_LIST"); 
                return executeFunctionCall(varName, argList, attr);
            }
        }

        return new RuntimeValue("UNKNOWN", null);
    }

    private RuntimeValue executeFunctionCall(String funcName, Tree argList, SymbolTable.VariableAttributes attr) {
        if (attr == null || !attr.dataType.equals("TASK")) throw new RuntimeException("Semantic Error: Function '" + funcName + "' is undefined.");
        Tree funcNode = (Tree) attr.value;
        Tree paramList = findNode(funcNode, "PARAM_LIST");
        Tree blockNode = findNode(funcNode, "BLOCK");

        java.util.List<RuntimeValue> argVals = new java.util.ArrayList<>();
        Tree currArg = argList;
        if (currArg != null && !currArg.children.isEmpty() && !((Tree)currArg.children.get(0)).data.equals("ε")) {
            argVals.add(evaluate((Tree) currArg.children.get(0)));
            Tree moreArgs = (Tree) currArg.children.get(1);
            while (moreArgs != null && !moreArgs.children.isEmpty() && !((Tree)moreArgs.children.get(0)).data.equals("ε")) {
                argVals.add(evaluate((Tree) moreArgs.children.get(1)));
                moreArgs = (Tree) moreArgs.children.get(2);
            }
        }

        java.util.List<String> paramNames = new java.util.ArrayList<>();
        java.util.List<String> paramTypes = new java.util.ArrayList<>();
        Tree currParam = paramList;
        if (currParam != null && !currParam.children.isEmpty() && !((Tree)currParam.children.get(0)).data.equals("ε")) {
            Tree pType = (Tree) currParam.children.get(0);
            paramTypes.add(pType.children.isEmpty() ? pType.data : ((Tree)pType.children.get(0)).data);
            paramNames.add(extractValue(((Tree) currParam.children.get(1)).data));
            Tree moreParams = (Tree) currParam.children.get(2);
            while (moreParams != null && !moreParams.children.isEmpty() && !((Tree)moreParams.children.get(0)).data.equals("ε")) {
                Tree mpType = (Tree) moreParams.children.get(1);
                paramTypes.add(mpType.children.isEmpty() ? mpType.data : ((Tree)mpType.children.get(0)).data);
                paramNames.add(extractValue(((Tree) moreParams.children.get(2)).data));
                moreParams = (Tree) moreParams.children.get(3);
            }
        }

        if (argVals.size() != paramNames.size()) throw new RuntimeException("Semantic Error: '" + funcName + "' expects " + paramNames.size() + " arguments.");

        symTable.enterScope();
        for (int i = 0; i < paramNames.size(); i++) {
            String pType = paramTypes.get(i).startsWith("num") ? "PRICE" : paramTypes.get(i).startsWith("str") ? "RECIPE" : "QUALITY";
            symTable.declareVariable(paramNames.get(i), pType);
            symTable.assignValue(paramNames.get(i), argVals.get(i).value);
        }

        RuntimeValue returnValue = new RuntimeValue("UNKNOWN", null);
        try { interpreter.walk(blockNode); } 
        catch (errors.ReturnException e) { returnValue = e.returnValue; } 
        finally { symTable.exitScope(); }
        return returnValue;
    }

    private RuntimeValue evaluatePrime(RuntimeValue leftVal, Tree primeNode) {
        if (primeNode.children.isEmpty() || ((Tree)primeNode.children.get(0)).data.equals("ε")) return leftVal;
        String opNode = ((Tree) primeNode.children.get(0)).data; 
        RuntimeValue rightVal = evaluate((Tree) primeNode.children.get(1));
        RuntimeValue currentResult = performOperation(leftVal, opNode, rightVal);
        if (primeNode.children.size() > 2) return evaluatePrime(currentResult, (Tree) primeNode.children.get(2));
        return currentResult;
    }

    private RuntimeValue performOperation(RuntimeValue left, String operator, RuntimeValue right) {
        if (operator.startsWith("arith_add") && (left.type.equals("RECIPE") || right.type.equals("RECIPE"))) return new RuntimeValue("RECIPE", left.asString() + right.asString());
        if (operator.startsWith("arith_add")) return new RuntimeValue("PRICE", left.asDouble() + right.asDouble());
        if (operator.startsWith("arith_sub")) return new RuntimeValue("PRICE", left.asDouble() - right.asDouble());
        if (operator.startsWith("arith_mul")) return new RuntimeValue("PRICE", left.asDouble() * right.asDouble());
        if (operator.startsWith("arith_div")) {
            if (right.asDouble() == 0.0) throw new RuntimeException("Runtime Error: Division by zero!");
            return new RuntimeValue("PRICE", left.asDouble() / right.asDouble());
        }
        if (operator.startsWith("arith_mod")) {
            if (right.asDouble() == 0.0) throw new RuntimeException("Runtime Error: Modulo by zero!");
            return new RuntimeValue("PRICE", left.asDouble() % right.asDouble());
        }
        if (operator.startsWith("rel_ls")) return new RuntimeValue("QUALITY", left.asDouble() < right.asDouble());
        if (operator.startsWith("rel_lse")) return new RuntimeValue("QUALITY", left.asDouble() <= right.asDouble());
        if (operator.startsWith("rel_gt")) return new RuntimeValue("QUALITY", left.asDouble() > right.asDouble());
        if (operator.startsWith("rel_gte")) return new RuntimeValue("QUALITY", left.asDouble() >= right.asDouble());
        if (operator.startsWith("rel_eq")) {
            if (!left.type.equals(right.type)) throw new RuntimeException("Semantic Error: Cannot compare " + left.type + " with " + right.type);
            return new RuntimeValue("QUALITY", left.value.equals(right.value));
        }
        if (operator.startsWith("rel_neq")) {
            if (!left.type.equals(right.type)) throw new RuntimeException("Semantic Error: Cannot compare " + left.type + " with " + right.type);
            return new RuntimeValue("QUALITY", !left.value.equals(right.value));
        }
        if (operator.startsWith("log_and")) return new RuntimeValue("QUALITY", left.asBoolean() && right.asBoolean());
        if (operator.startsWith("log_or")) return new RuntimeValue("QUALITY", left.asBoolean() || right.asBoolean());
        throw new RuntimeException("Semantic Error: Unknown Operator " + operator);
    }

    private Tree findNode(Tree root, String prefix) {
        if (root == null) return null;
        if (root.data.startsWith(prefix)) return root;
        for (Object c : root.children) {
            Tree found = findNode((Tree) c, prefix);
            if (found != null) return found;
        }
        return null;
    }

    private String extractValue(String nodeData) {
        int start = nodeData.indexOf('(') + 1;
        int end = nodeData.lastIndexOf(')');
        return start > 0 && end > start ? nodeData.substring(start, end) : nodeData;
    }
}