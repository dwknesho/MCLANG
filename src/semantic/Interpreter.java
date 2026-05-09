package semantic;

import grtree.Tree;
import java.util.Scanner;
import errors.BreakException;
import errors.ContinueException;
import errors.SpillException;
import errors.ReturnException;

public class Interpreter {
    private SymbolTable symTable;
    private ExpressionEvaluator evaluator;
    private Scanner inputScanner; 

    public Interpreter(SymbolTable symTable) {
        this.symTable = symTable;
        this.evaluator = new ExpressionEvaluator(symTable);
        this.evaluator.setInterpreter(this);
        this.inputScanner = new Scanner(System.in);
    }

    public void execute(Tree root) {
        System.out.println("\n=======================================================");
        System.out.println("                 PROGRAM OUTPUT LOG                    ");
        System.out.println("=======================================================");
        try { walk(root); } 
        catch (Exception e) {
            System.err.println("\n[RUNTIME ERROR] Execution failed: " + e.getMessage());
            e.printStackTrace();
        }
        System.out.println("\n=======================================================\n");
    }

    public void walk(Tree node) {
        if (node == null) return;
        String label = node.data;

        if (label.equals("BLOCK")) {
            symTable.enterScope(); 
            try { for (Object child : node.children) walk((Tree) child); } 
            finally { symTable.exitScope(); }
            return;
        }

        if (label.equals("DECLARATION_STMT")) {
            Tree typeNode = (Tree) node.children.get(0);
            String varType = getMcLangType(typeNode.children.isEmpty() ? typeNode.data : ((Tree)typeNode.children.get(0)).data);
            Tree idNode = (Tree) node.children.get(1);
            String varName = extractValue(idNode.data);
            symTable.declareVariable(varName, varType);

            Tree varInit = findNode(node, "VAR_INIT");
            if (varInit != null && varInit.children.size() > 1) {
                Tree expr = (Tree) varInit.children.get(1);
                symTable.assignValue(varName, evaluator.evaluate(expr).value);
                return;
            }
            
            Tree targetNode = node;
            Tree decPrime = findNode(node, "DECLARATION_STMT'");
            if (decPrime != null) targetNode = decPrime; 

            for (int i = 0; i < targetNode.children.size(); i++) {
                if (((Tree)targetNode.children.get(i)).data.startsWith("l_brack")) {
                    Tree expr = (Tree) targetNode.children.get(i+1);
                    int size = (int) evaluator.evaluate(expr).asDouble();
                    Object[] array = new Object[size];
                    for (int j = 0; j < size; j++) array[j] = varType.equals("PRICE") ? 0.0 : varType.equals("QUALITY") ? false : "";
                    symTable.assignValue(varName, array);
                    return;
                }
            }
            return;
        }

        if (label.equals("STATEMENT")) {
            Tree first = (Tree) node.children.get(0);
            if (first.data.startsWith("id")) {
                Tree stmtPrime = (Tree) node.children.get(1);
                if (stmtPrime.children.isEmpty()) return;
                
                if (findNode(stmtPrime, "l_paren") != null) {
                    Tree fakeCall = new Tree("CALL_STMT");
                    fakeCall.addChild(first);
                    for(Object c : stmtPrime.children) fakeCall.addChild((Tree)c);
                    evaluator.evaluate(fakeCall);
                    return;
                }
                
                Tree assignPrime = findNode(stmtPrime, "ASSIGNMENT_STMT'");
                if (assignPrime != null) {
                    Tree opNode = (Tree) assignPrime.children.get(0); 
                    String op = opNode.children.isEmpty() ? opNode.data : ((Tree)opNode.children.get(0)).data;
                    RuntimeValue rhs = evaluator.evaluate((Tree) assignPrime.children.get(1));
                    
                    Tree lValuePrime = findNode(stmtPrime, "L_VALUE'");
                    Tree fakeLValue = new Tree("L_VALUE");
                    fakeLValue.addChild(first); 
                    if (lValuePrime != null) fakeLValue.addChild(lValuePrime); 
                    
                    if (op.startsWith("assign_as")) assignToVariable(fakeLValue, rhs.value);
                    else if (op.startsWith("assign_add")) assignToVariable(fakeLValue, evaluator.evaluate(fakeLValue).asDouble() + rhs.asDouble());
                    else if (op.startsWith("assign_sub")) assignToVariable(fakeLValue, evaluator.evaluate(fakeLValue).asDouble() - rhs.asDouble());
                    else if (op.startsWith("assign_mul")) assignToVariable(fakeLValue, evaluator.evaluate(fakeLValue).asDouble() * rhs.asDouble());
                    else if (op.startsWith("assign_div")) assignToVariable(fakeLValue, evaluator.evaluate(fakeLValue).asDouble() / rhs.asDouble());
                    return;
                }
            }
        }

        if (label.equals("ASSIGNMENT_STMT")) {
            Tree lValue = (Tree) node.children.get(0);
            Tree assignPrime = findNode(node, "ASSIGNMENT_STMT'");
            if (assignPrime != null) {
                Tree opNode = (Tree) assignPrime.children.get(0);
                String op = opNode.children.isEmpty() ? opNode.data : ((Tree)opNode.children.get(0)).data;
                RuntimeValue rhs = evaluator.evaluate((Tree) assignPrime.children.get(1));
                if (op.startsWith("assign_as")) assignToVariable(lValue, rhs.value);
            }
            return;
        }

        if (label.equals("FOR_UPDATE")) {
            Tree lValue = findNode(node, "L_VALUE");
            if (lValue != null) {
                RuntimeValue currentVal = evaluator.evaluate(lValue);
                if (findNode(node, "incre") != null) assignToVariable(lValue, currentVal.asDouble() + 1);
                else if (findNode(node, "decre") != null) assignToVariable(lValue, currentVal.asDouble() - 1);
            }
            return;
        }

        if (label.equals("IO_STMT")) {
            Tree ioType = (Tree) node.children.get(0);
            if (ioType.data.startsWith("output")) {
                System.out.print(evaluator.evaluate((Tree) node.children.get(1)).toString() + "\n");
            } else if (ioType.data.startsWith("input")) {
                Tree lValue = (Tree) node.children.get(1);
                System.out.print("> "); 
                String input = inputScanner.nextLine();
                
                Tree idNode = findNode(lValue, "id");
                if (idNode != null) {
                    SymbolTable.VariableAttributes attr = symTable.getAttributes(extractValue(idNode.data));
                    try {
                        if (attr.dataType.equals("PRICE")) assignToVariable(lValue, Double.parseDouble(input));
                        else if (attr.dataType.equals("QUALITY")) assignToVariable(lValue, Boolean.parseBoolean(input));
                        else assignToVariable(lValue, input);
                    } catch (NumberFormatException e) {
                        System.out.println("[McLang Warning] Blank or invalid number entered. Defaulting to 0.");
                        assignToVariable(lValue, 0.0);
                    }
                }
            }
            return;
        }

        if (label.equals("IF_STMT")) {
            if (evaluator.evaluate((Tree) node.children.get(1)).asBoolean()) {
                walk((Tree) node.children.get(2)); 
            } else {
                Tree elifList = (Tree) node.children.get(3);
                boolean matchedElif = false;
                
                Tree currElif = elifList;
                while (currElif != null && !currElif.children.isEmpty() && !((Tree)currElif.children.get(0)).data.equals("ε")) {
                    Tree elifExpr = (Tree) currElif.children.get(1);
                    Tree elifBlock = (Tree) currElif.children.get(2);
                    if (evaluator.evaluate(elifExpr).asBoolean()) {
                        walk(elifBlock);
                        matchedElif = true;
                        break; 
                    }
                    currElif = (Tree) currElif.children.get(3); 
                }

                if (!matchedElif) {
                    Tree elsePart = (Tree) node.children.get(4); 
                    if (!elsePart.children.isEmpty() && !((Tree)elsePart.children.get(0)).data.equals("ε")) {
                        walk((Tree) elsePart.children.get(1));
                    }
                }
            }
            return;
        }

        if (label.equals("BREAK_STMT")) throw new BreakException();
        if (label.equals("CONTINUE_STMT")) throw new ContinueException();

        if (label.equals("WHILE_LOOP")) {
            Tree exprNode = (Tree) node.children.get(1);
            Tree blockNode = (Tree) node.children.get(2);
            try { while (evaluator.evaluate(exprNode).asBoolean()) try { walk(blockNode); } catch (ContinueException e) { } } 
            catch (BreakException e) { }
            return;
        }
        
        if (label.equals("DO_WHILE_LOOP")) {
            Tree blockNode = (Tree) node.children.get(1);
            Tree exprNode = (Tree) node.children.get(node.children.size() - 1);
            try { do { try { walk(blockNode); } catch (ContinueException e) { } } while (evaluator.evaluate(exprNode).asBoolean()); } 
            catch (BreakException e) { }
            return;
        }

        if (label.equals("FOR_LOOP")) {
            Tree init = (Tree) node.children.get(1);
            Tree cond = (Tree) node.children.get(2);
            Tree update = (Tree) node.children.get(3);
            Tree block = (Tree) node.children.get(4);

            walk(init); 
            try { 
                while (evaluator.evaluate(cond).asBoolean()) { 
                    try { walk(block); } catch (ContinueException e) {} 
                    walk(update); 
                } 
            } catch (BreakException e) {}
            return;
        }

        if (label.equals("THROW_STMT")) {
            RuntimeValue spillVal = evaluator.evaluate((Tree) node.children.get(1));
            throw new SpillException(spillVal);
        }

        if (label.equals("TRY_CATCH_STMT")) {
            Tree tryBlock = (Tree) node.children.get(1);
            Tree catchIdNode = (Tree) node.children.get(4);
            Tree catchBlock = (Tree) node.children.get(6);
            Tree finallyPart = (Tree) node.children.get(7); 
            
            try { walk(tryBlock); } 
            catch (SpillException e) {
                symTable.enterScope(); 
                String errVarName = extractValue(catchIdNode.data);
                symTable.declareVariable(errVarName, e.spilledValue.type);
                symTable.assignValue(errVarName, e.spilledValue.value);
                walk(catchBlock); 
                symTable.exitScope();
            } finally {
                if (!finallyPart.children.isEmpty() && !((Tree)finallyPart.children.get(0)).data.equals("ε")) walk((Tree) finallyPart.children.get(1)); 
            }
            return;
        }

        if (label.equals("FUNCTION_DECL")) {
            String varName = extractValue(((Tree)node.children.get(2)).data);
            symTable.declareVariable(varName, "TASK");
            symTable.assignValue(varName, node); 
            return;
        }

        if (label.equals("RETURN_STMT")) {
            Tree exprNode = node.children.size() > 1 ? (Tree) node.children.get(1) : null;
            if (exprNode == null || exprNode.data.equals("semi") || exprNode.data.equals("ε")) {
                throw new ReturnException(new RuntimeValue("EMPTY", null)); 
            } else {
                throw new ReturnException(evaluator.evaluate(exprNode)); 
            }
        }

        for (Object child : node.children) walk((Tree) child);
    }

    private void assignToVariable(Tree lValueNode, Object newValue) {
        Tree idNode = findNode(lValueNode, "id");
        if (idNode == null) return;
        String varName = extractValue(idNode.data);
        
        if (findNode(lValueNode, "l_brack") != null) {
            Tree indexExpr = null;
            Tree searchNode = lValueNode;
            Tree lvp = findNode(lValueNode, "L_VALUE'");
            if (lvp != null) searchNode = lvp;

            boolean foundBrack = false;
            for (Object c : searchNode.children) {
                Tree t = (Tree) c;
                if (foundBrack && !t.data.startsWith("r_brack")) {
                    indexExpr = t; break;
                }
                if (t.data.startsWith("l_brack")) foundBrack = true;
            }
            if (indexExpr == null) indexExpr = findNode(lValueNode, "EXPRESSION");

            if (indexExpr != null) {
                int index = (int) evaluator.evaluate(indexExpr).asDouble();
                Object[] array = (Object[]) symTable.getAttributes(varName).value;
                array[index] = newValue;
                return;
            }
        }
        symTable.assignValue(varName, newValue);
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
}