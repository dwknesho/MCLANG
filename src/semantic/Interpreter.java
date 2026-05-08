package semantic;

import grtree.Tree;
import java.util.Scanner;

public class Interpreter {
    private SymbolTable symTable;
    private ExpressionEvaluator evaluator;
    private Scanner inputScanner; 

    public Interpreter(SymbolTable symTable) {
        this.symTable = symTable;
        this.evaluator = new ExpressionEvaluator(symTable);
        this.inputScanner = new Scanner(System.in);
    }

    public void execute(Tree root) {
        System.out.println("\n=======================================================");
        System.out.println("                 PROGRAM OUTPUT LOG                    ");
        System.out.println("=======================================================");
        
        try {
            walk(root);
        } catch (SemanticException e) {
            System.err.println(e.getMessage());
        } catch (Exception e) {
            System.err.println("\n[SYSTEM ERROR] Execution failed: " + e.getMessage());
            e.printStackTrace();
        }
        
        System.out.println("\n=======================================================\n");
    }

    private void walk(Tree node) {
        if (node == null) return;
        String label = node.data;

        // 1. DECLARATIONS (Updated for Arrays!)
        if (label.equals("DECLARATION_STMT")) {
            Tree typeNode = (Tree) node.children.get(0);
            String varType = getMcLangType(typeNode.children.isEmpty() ? typeNode.data : ((Tree)typeNode.children.get(0)).data);
            
            Tree idNode = (Tree) node.children.get(1);
            String varName = extractValue(idNode.data);

            symTable.declareVariable(varName, varType);

            if (node.children.size() > 2) {
                Tree thirdChild = (Tree) node.children.get(2);
                
                // ARRAY DECLARATION: PRICE scores[5];
                if (thirdChild.data.startsWith("l_brack")) {
                    int size = (int) evaluator.evaluate((Tree) node.children.get(3)).asDouble();
                    Object[] array = new Object[size];
                    
                    // Pre-fill the array with default values
                    for (int i = 0; i < size; i++) {
                        if (varType.equals("PRICE")) array[i] = 0.0;
                        else if (varType.equals("QUALITY")) array[i] = false;
                        else array[i] = "";
                    }
                    symTable.assignValue(varName, array);
                } 
                // NORMAL INITIALIZATION: PRICE i = 0;
                else if (!thirdChild.children.isEmpty() && !((Tree)thirdChild.children.get(0)).data.equals("ε")) {
                    Tree expr = (Tree) thirdChild.children.get(1);
                    RuntimeValue rhs = evaluator.evaluate(expr);
                    symTable.assignValue(varName, rhs.value);
                }
            }
        }

        // 2. ASSIGNMENTS
        else if (label.equals("ASSIGNMENT_STMT")) {
            Tree targetNode = (Tree) node.children.get(0); // This is L_VALUE
            Tree rightSide = (Tree) node.children.get(1); 
            
            if (rightSide.data.equals("ASSIGNMENT_STMT'") && rightSide.children.size() >= 2) {
                String assignOp = ((Tree) rightSide.children.get(0)).children.isEmpty() ? 
                    ((Tree) rightSide.children.get(0)).data : ((Tree) ((Tree) rightSide.children.get(0)).children.get(0)).data;
                
                RuntimeValue rhs = evaluator.evaluate((Tree) rightSide.children.get(1));
                
                if (assignOp.startsWith("assign_as")) {
                    assignToVariable(targetNode, rhs.value);
                } 
                else if (assignOp.startsWith("assign_add")) {
                    RuntimeValue currentVal = evaluator.evaluate(targetNode);
                    assignToVariable(targetNode, currentVal.asDouble() + rhs.asDouble());
                }
            }
        }

        // 3. I/O (ORDER / SERVE)
        else if (label.equals("IO_STMT")) {
            Tree ioType = (Tree) node.children.get(0);
            
            if (ioType.data.startsWith("output")) {
                RuntimeValue result = evaluator.evaluate((Tree) node.children.get(1));
                System.out.print(result.toString() + "\n");
            } 
            else if (ioType.data.startsWith("input")) {
                Tree lValue = (Tree) node.children.get(1);
                String varName = getVarNameSafely(lValue);
                
                System.out.print("> "); 
                String input = inputScanner.nextLine();
                
                SymbolTable.VariableAttributes attr = symTable.getAttributes(varName);
                if (attr.dataType.equals("PRICE")) assignToVariable(lValue, Double.parseDouble(input));
                else if (attr.dataType.equals("QUALITY")) assignToVariable(lValue, Boolean.parseBoolean(input));
                else assignToVariable(lValue, input);
            }
        }

        // 4. IF / ELSE STATEMENTS (WANT / ONLY)
        else if (label.equals("IF_STMT")) {
            Tree exprNode = (Tree) node.children.get(1);
            Tree blockNode = (Tree) node.children.get(2);
            Tree elsePart = (Tree) node.children.get(4); 
            
            if (evaluator.evaluate(exprNode).asBoolean()) {
                walk(blockNode); 
            } else {
                if (!elsePart.children.isEmpty() && !((Tree)elsePart.children.get(0)).data.equals("ε")) {
                    walk((Tree) elsePart.children.get(1));
                }
            }
        }

        // 5. SWITCH STATEMENTS (MENU / FOOD / SOLDOUT)
        else if (label.equals("SWITCH_STMT")) {
            Tree idNode = (Tree) node.children.get(1);
            Tree caseList = (Tree) node.children.get(2);
            Tree defaultCase = (Tree) node.children.get(3);

            String varName = extractValue(idNode.data);
            SymbolTable.VariableAttributes attr = symTable.getAttributes(varName);
            boolean caseMatched = false;
            
            try {
                Tree currentCaseList = caseList;
                while (!currentCaseList.children.isEmpty() && !((Tree)currentCaseList.children.get(0)).data.equals("ε")) {
                    Tree literalNode = (Tree) currentCaseList.children.get(1); 
                    Tree stmtList = (Tree) currentCaseList.children.get(2);
                    
                    if (!caseMatched) {
                        RuntimeValue caseVal = evaluator.evaluate(literalNode);
                        if (formatValue(attr.value).equals(formatValue(caseVal.value))) {
                            caseMatched = true;
                        }
                    }

                    if (caseMatched) walk(stmtList);
                    currentCaseList = (Tree) currentCaseList.children.get(3); 
                }

                if (!caseMatched && !defaultCase.children.isEmpty() && !((Tree)defaultCase.children.get(0)).data.equals("ε")) {
                    Tree defaultStmtList = (Tree) defaultCase.children.get(1);
                    walk(defaultStmtList);
                }
            } catch (BreakException e) {
                // Break Caught! Exit the switch silently.
            }
        }

        // 6. WHILE LOOPS (REFILL)
        else if (label.equals("WHILE_LOOP")) {
            Tree exprNode = (Tree) node.children.get(1);
            Tree blockNode = (Tree) node.children.get(2);
            
            try {
                while (evaluator.evaluate(exprNode).asBoolean()) {
                    walk(blockNode);
                }
            } catch (BreakException e) {}
        }
        
        // 7. --- FOR LOOPS (PREP) --- 
        else if (label.equals("FOR_LOOP")) {
            Tree initNode = (Tree) node.children.get(1);
            Tree conditionNode = (Tree) node.children.get(2);
            Tree updateNode = (Tree) node.children.get(3);
            Tree blockNode = (Tree) node.children.get(4);

            walk(initNode); 

            try {
                while (evaluator.evaluate(conditionNode).asBoolean()) {
                    walk(blockNode);  
                    walk(updateNode); 
                }
            } catch (BreakException e) {}
        }
        else if (label.equals("FOR_INIT")) {
            walk((Tree) node.children.get(0)); 
        }
        else if (label.equals("FOR_UPDATE")) {
            Tree lValue = (Tree) node.children.get(0);
            String op = ((Tree) node.children.get(1)).data; // Fixed to target the pruned child directly!
            
            RuntimeValue currentVal = evaluator.evaluate(lValue);
            if (op.startsWith("incre")) assignToVariable(lValue, currentVal.asDouble() + 1);
            else if (op.startsWith("decre")) assignToVariable(lValue, currentVal.asDouble() - 1);
        }

        // 8. BREAK (DONE)
        else if (label.equals("BREAK_STMT")) {
            throw new BreakException();
        }

        // 9. PREVENT BLIND FUNCTION EXECUTION 
        else if (label.equals("FUNCTION_DECL") || label.startsWith("function") || label.equals("TASK")) {
            return;
        }

        // DEFAULT: Traverse children 
        else {
            for (Object child : node.children) {
                walk((Tree) child);
            }
        }
    }

    // --- ARRAY & VARIABLE MEMORY HELPERS ---

    // Safely handles storing data in both single variables AND array indexes!
    private void assignToVariable(Tree lValueNode, Object newValue) {
        String varName = getVarNameSafely(lValueNode);
        
        // Is it an array assignment? (e.g. scores[i] = 100)
        if (lValueNode.children.size() > 1 && ((Tree)lValueNode.children.get(1)).data.startsWith("l_brack")) {
            int index = (int) evaluator.evaluate((Tree) lValueNode.children.get(2)).asDouble();
            Object[] array = (Object[]) symTable.getAttributes(varName).value;
            array[index] = newValue;
        } else {
            symTable.assignValue(varName, newValue);
        }
    }

    private String getVarNameSafely(Tree node) {
        if (node.data.startsWith("id")) return extractValue(node.data);
        if (node.data.equals("L_VALUE")) return extractValue(((Tree)node.children.get(0)).data);
        return extractValue(node.data);
    }

    private String getMcLangType(String tokenData) {
        if (tokenData.startsWith("num_type")) return "PRICE";
        if (tokenData.startsWith("string_type")) return "RECIPE";
        if (tokenData.startsWith("bool_type")) return "QUALITY";
        return "UNKNOWN";
    }

    private String extractValue(String nodeData) {
        int start = nodeData.indexOf('(') + 1;
        int end = nodeData.lastIndexOf(')');
        return start > 0 && end > start ? nodeData.substring(start, end) : nodeData;
    }

    // Helper to format Double values smoothly (e.g. 1.0 -> "1") so switch cases match easily
    private String formatValue(Object val) {
        if (val instanceof Double) {
            double d = (Double) val;
            if (d == (long) d) return String.format("%d", (long) d);
        }
        return val != null ? val.toString() : "null";
    }
}