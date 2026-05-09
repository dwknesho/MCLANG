package interpreter;

import grtree.Tree;
import semantic.SymbolTable;
import semantic.MCLangFunction;
import errors.ErrorReporter;

import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class Interpreter {
    private SymbolTable symTable;
    private ErrorReporter reporter;
    private Scanner consoleInput; 
    private ExpressionEvaluator evaluator; 

    private Map<String, MCLangFunction> functions = new HashMap<>();

    public Interpreter(SymbolTable symTable, ErrorReporter reporter) {
        this.symTable = symTable;
        this.reporter = reporter;
        this.consoleInput = new Scanner(System.in);
        this.evaluator = new ExpressionEvaluator(symTable, this); 
    }

    public boolean hasFunction(String name) {
        return functions.containsKey(name);
    }

    public void execute(Tree ast) {
        System.out.println("\n--- MCLANG INTERPRETER OUTPUT ---");
        try {
            if (ast.data.equals("PROGRAM") && !ast.children.isEmpty()) {
                executeStatementList((Tree) ast.children.get(0));
            }
        } catch (RuntimeException e) {
            System.err.println("\n[RUNTIME EXCEPTION] " + e.getMessage());
        }
        System.out.println("---------------------------------");
    }

    private void executeStatementList(Tree node) {
        if (node == null || node.children.isEmpty() || rawLabel(node.data).equals("ε")) return;

        executeStatement((Tree) node.children.get(0));

        if (node.children.size() > 1) {
            executeStatementList((Tree) node.children.get(1));
        }
    }

    private void executeStatement(Tree stmtNode) {
        if (stmtNode == null || stmtNode.children.isEmpty() || rawLabel(stmtNode.data).equals("ε")) return;

        Tree actualStmt = (Tree) stmtNode.children.get(0);
        String stmtType = rawLabel(actualStmt.data);

        switch (stmtType) {
            case "DECLARATION_STMT": executeDeclaration(actualStmt); break;
            case "IO_STMT": executeIO(actualStmt); break;
            case "IF_STMT": executeIf(actualStmt); break;
            case "SWITCH_STMT": executeSwitch(actualStmt); break;
            case "LOOP_STMT": executeLoop(actualStmt); break;
            case "BREAK_STMT": throw new BreakException();
            case "CONTINUE_STMT": throw new ContinueException();
            case "TRY_CATCH_STMT": executeTryCatch(actualStmt); break;
            case "THROW_STMT": executeThrow(actualStmt); break;
            case "FUNCTION_DECL": executeFunctionDeclaration(actualStmt); break;
            case "RETURN_STMT": executeReturn(actualStmt); break;
            case "id":
                String varOrFuncName = extractLexeme(actualStmt.data);
                
                if (stmtNode.children.size() == 1) {
                    executeFunctionCall(varOrFuncName, null);
                } else {
                    Tree stmtPrime = (Tree) stmtNode.children.get(1);
                    if (hasFunction(varOrFuncName) || rawLabel(stmtPrime.data).equals("ARG_LIST")) {
                        executeFunctionCall(varOrFuncName, stmtPrime);
                    } else {
                        executeAssignment(varOrFuncName, stmtPrime);
                    }
                }
                break;
            default: break;
        }
    }

    private void executeDeclaration(Tree node) {
        String mclangType = mapDataType(rawLabel(((Tree) ((Tree) node.children.get(0)).children.get(0)).data)); 
        String varName = extractLexeme(((Tree) node.children.get(1)).data);
        Tree declPrime = (Tree) node.children.get(2);
        String primeLabel = rawLabel(declPrime.data);

        if (primeLabel.equals("VAR_INIT")) {
            symTable.declareVariable(varName, mclangType);
            if (!declPrime.children.isEmpty() && !rawLabel(((Tree) declPrime.children.get(0)).data).equals("ε")) {
                symTable.assignValue(varName, evaluator.evaluate((Tree) declPrime.children.get(1)));
            }
        } else if (primeLabel.equals("DECLARATION_STMT'")) {
            Object sizeObj = evaluator.evaluate((Tree) declPrime.children.get(1));
            if (!(sizeObj instanceof Double)) throw new RuntimeException("Semantic Error: Array size must be a PRICE.");
            symTable.declareArray(varName, mclangType, ((Double) sizeObj).intValue());
        }
    }

    private void executeAssignment(String varName, Tree stmtPrime) {
        boolean isArray = false;
        int index = -1;
        Tree assignPart = stmtPrime;
        String primeLabel = rawLabel(stmtPrime.data);

        if (primeLabel.equals("STATEMENT'")) {
            Tree lValuePrime = (Tree) stmtPrime.children.get(0);
            assignPart = (Tree) stmtPrime.children.get(1);
            if (!lValuePrime.children.isEmpty() && !rawLabel(((Tree) lValuePrime.children.get(0)).data).equals("ε")) {
                if (rawLabel(((Tree) lValuePrime.children.get(0)).data).equals("l_brack")) {
                    isArray = true;
                    Object idxObj = evaluator.evaluate((Tree) lValuePrime.children.get(1));
                    if (!(idxObj instanceof Double)) throw new RuntimeException("Semantic Error: Array index must be a PRICE.");
                    index = ((Double) idxObj).intValue();
                }
            }
        }

        String assignLabel = rawLabel(assignPart.data);
        String exactOp = "";
        Object rhsValue = null;

        if (assignLabel.equals("incre") || assignLabel.equals("decre")) {
            exactOp = assignLabel;
        } else {
            exactOp = rawLabel(((Tree) ((Tree) assignPart.children.get(0)).children.get(0)).data);
            rhsValue = evaluator.evaluate((Tree) assignPart.children.get(1));
        }

        if (exactOp.equals("incre") || exactOp.equals("decre")) {
            Object currentVal = isArray ? getArrayValue(varName, index) : symTable.getAttributes(varName).value;
            if (!(currentVal instanceof Double)) throw new RuntimeException("Semantic Error: Increment/Decrement requires a PRICE.");
            double newVal = exactOp.equals("incre") ? (Double) currentVal + 1 : (Double) currentVal - 1;
            if (isArray) symTable.assignArrayValue(varName, index, newVal);
            else symTable.assignValue(varName, newVal);
        } else {
            if (exactOp.equals("assign_as")) {
                 if (isArray) symTable.assignArrayValue(varName, index, rhsValue);
                 else symTable.assignValue(varName, rhsValue);
            } else {
                Object currentVal = isArray ? getArrayValue(varName, index) : symTable.getAttributes(varName).value;
                if (!(currentVal instanceof Double) || !(rhsValue instanceof Double)) throw new RuntimeException("Semantic Error: Compound assignment requires PRICE values.");
                double cv = (Double) currentVal, rv = (Double) rhsValue, result = 0;
                switch (exactOp) {
                    case "assign_add": result = cv + rv; break;
                    case "assign_min": result = cv - rv; break;
                    case "assign_mul": result = cv * rv; break;
                    case "assign_div": 
                        if (rv == 0) throw new RuntimeException("Runtime Error: Division by zero.");
                        result = cv / rv; break;
                    case "assign_mod": 
                        if (rv == 0) throw new RuntimeException("Runtime Error: Modulo by zero.");
                        result = cv % rv; break;
                }
                if (isArray) symTable.assignArrayValue(varName, index, result);
                else symTable.assignValue(varName, result);
            }
        }
    }

    private void executeIO(Tree node) {
        String ioType = rawLabel(((Tree) node.children.get(0)).data);
        if (ioType.equals("output")) {
            Object value = evaluator.evaluate((Tree) node.children.get(1));
            if (value instanceof Double && ((Double) value) % 1 == 0) System.out.print(((Double) value).intValue());
            else System.out.print(value); 
        } else if (ioType.equals("input")) {
            Tree lValue = (Tree) node.children.get(1);
            String varName = extractLexeme(((Tree) lValue.children.get(0)).data);
            
            boolean isArray = false;
            int index = -1;
            if (lValue.children.size() > 1) {
                Tree lValuePrime = (Tree) lValue.children.get(1);
                if (!lValuePrime.children.isEmpty() && rawLabel(((Tree) lValuePrime.children.get(0)).data).equals("l_brack")) {
                    isArray = true;
                    Object idxObj = evaluator.evaluate((Tree) lValuePrime.children.get(1));
                    index = ((Double) idxObj).intValue();
                }
            }

            SymbolTable.VariableAttributes attr = symTable.getAttributes(varName);
            if (attr == null) throw new RuntimeException("Semantic Error: Variable '" + varName + "' not declared for ORDER.");

            String userInput = consoleInput.nextLine();
            try {
                Object val = null;
                if (attr.dataType.equals("PRICE")) val = Double.parseDouble(userInput);
                else if (attr.dataType.equals("QUALITY")) val = userInput.trim().equalsIgnoreCase("FRESH");
                else val = userInput; 
                
                if (isArray) symTable.assignArrayValue(varName, index, val);
                else symTable.assignValue(varName, val);
            } catch (NumberFormatException e) { throw new RuntimeException("Runtime Error: Invalid input. Expected a PRICE (number)."); }
        }
    }

    private void executeBlock(Tree blockNode) {
        // FULL-PROOF FIX: The statement list shifted to index 0 after brace pruning
        if (!blockNode.children.isEmpty() && !rawLabel(((Tree) blockNode.children.get(0)).data).equals("ε")) {
            executeStatementList((Tree) blockNode.children.get(0));
        }
    }

    private void executeIf(Tree node) {
        symTable.enterScope();
        try {
            Object cond = evaluator.evaluate((Tree) node.children.get(1));
            if (!(cond instanceof Boolean)) throw new RuntimeException("Semantic Error: WANT condition must evaluate to QUALITY.");
            boolean executed = false;
            
            if ((Boolean) cond) {
                executeBlock((Tree) node.children.get(2));
                executed = true;
            } else {
                Tree elifList = (Tree) node.children.get(3);
                while (!elifList.children.isEmpty() && !rawLabel(((Tree) elifList.children.get(0)).data).equals("ε")) {
                    Object elifCond = evaluator.evaluate((Tree) elifList.children.get(1));
                    if ((Boolean) elifCond) {
                        executeBlock((Tree) elifList.children.get(2));
                        executed = true;
                        break;
                    }
                    elifList = (Tree) elifList.children.get(3);
                }
                if (!executed) {
                    Tree elsePart = (Tree) node.children.get(4);
                    if (!elsePart.children.isEmpty() && !rawLabel(((Tree) elsePart.children.get(0)).data).equals("ε")) {
                        executeBlock((Tree) elsePart.children.get(1));
                    }
                }
            }
        } finally { symTable.exitScope(); }
    }

    private void executeSwitch(Tree node) {
        symTable.enterScope();
        try {
            String switchVar = extractLexeme(((Tree) node.children.get(1)).data);
            SymbolTable.VariableAttributes attr = symTable.getAttributes(switchVar);
            if (attr == null) throw new RuntimeException("Semantic Error: Variable '" + switchVar + "' in MENU not declared.");
            
            Object switchValue = attr.value;
            boolean matched = false;
            
            Tree caseList = (Tree) node.children.get(2);
            try {
                while (!caseList.children.isEmpty() && !rawLabel(((Tree) caseList.children.get(0)).data).equals("ε")) {
                    Object caseValue = evaluator.evaluate((Tree) caseList.children.get(1));
                    if (matched || switchValue.equals(caseValue)) {
                        matched = true;
                        executeStatementList((Tree) caseList.children.get(2));
                    }
                    caseList = (Tree) caseList.children.get(3); 
                }
                Tree defaultCase = (Tree) node.children.get(3);
                if (!defaultCase.children.isEmpty() && !rawLabel(((Tree) defaultCase.children.get(0)).data).equals("ε")) {
                    executeStatementList((Tree) defaultCase.children.get(1));
                }
            } catch (BreakException e) { }
            
        } finally { symTable.exitScope(); }
    }

    private void executeLoop(Tree node) {
        Tree actualLoop = (Tree) node.children.get(0);
        String loopType = rawLabel(actualLoop.data);

        symTable.enterScope(); 
        try {
            switch (loopType) {
                case "WHILE_LOOP":
                    while (true) {
                        Object cond = evaluator.evaluate((Tree) actualLoop.children.get(1));
                        if (!(cond instanceof Boolean)) throw new RuntimeException("Semantic Error: REFILL condition must evaluate to QUALITY.");
                        if (!(Boolean) cond) break;
                        try { symTable.enterScope(); executeBlock((Tree) actualLoop.children.get(2)); } 
                        catch (ContinueException e) {} catch (BreakException e) { break; } finally { symTable.exitScope(); }
                    }
                    break;
                case "DO_WHILE_LOOP":
                    do {
                        try { symTable.enterScope(); executeBlock((Tree) actualLoop.children.get(1)); } 
                        catch (ContinueException e) {} catch (BreakException e) { break; } finally { symTable.exitScope(); }
                        Object cond = evaluator.evaluate((Tree) actualLoop.children.get(3));
                        if (!(cond instanceof Boolean)) throw new RuntimeException("Semantic Error: STIR condition must evaluate to QUALITY.");
                        if (!(Boolean) cond) break;
                    } while (true);
                    break;
                case "FOR_LOOP":
                    Tree initNode = (Tree) actualLoop.children.get(1);
                    Tree initChild = (Tree) initNode.children.get(0);
                    if (rawLabel(initChild.data).equals("DECLARATION_STMT")) executeDeclaration(initChild);
                    else {
                        Tree lValue = (Tree) initChild.children.get(0);
                        String varName = extractLexeme(((Tree) lValue.children.get(0)).data); 
                        Tree lValuePrime = lValue.children.size() > 1 ? (Tree) lValue.children.get(1) : null;
                        Tree assignPrime = (Tree) initChild.children.get(1);
                        
                        if (lValuePrime == null) executeAssignment(varName, assignPrime);
                        else {
                            Tree dummyStmtPrime = new Tree("STATEMENT'");
                            dummyStmtPrime.addChild(lValuePrime); dummyStmtPrime.addChild(assignPrime);
                            executeAssignment(varName, dummyStmtPrime); 
                        }
                    }
                    while (true) {
                        Object cond = evaluator.evaluate((Tree) actualLoop.children.get(2));
                        if (!(cond instanceof Boolean)) throw new RuntimeException("Semantic Error: PREP condition must evaluate to QUALITY.");
                        if (!(Boolean) cond) break;
                        try { symTable.enterScope(); executeBlock((Tree) actualLoop.children.get(4)); } 
                        catch (ContinueException e) {} catch (BreakException e) { break; } finally { symTable.exitScope(); }
                        executeForUpdate((Tree) actualLoop.children.get(3));
                    }
                    break;
            }
        } finally { symTable.exitScope(); }
    }

    private void executeForUpdate(Tree node) {
        Tree lValue = (Tree) node.children.get(0);
        String varName = extractLexeme(((Tree) lValue.children.get(0)).data);
        
        boolean isArray = false;
        int index = -1;
        if (lValue.children.size() > 1) {
            Tree lValuePrime = (Tree) lValue.children.get(1);
            if (!lValuePrime.children.isEmpty() && rawLabel(((Tree) lValuePrime.children.get(0)).data).equals("l_brack")) {
                isArray = true;
                index = ((Double) evaluator.evaluate((Tree) lValuePrime.children.get(1))).intValue();
            }
        }

        Tree updatePrime = (Tree) node.children.get(1);
        String opLabel = rawLabel(updatePrime.data);

        if (opLabel.equals("incre") || opLabel.equals("decre")) {
            Object currentVal = isArray ? getArrayValue(varName, index) : symTable.getAttributes(varName).value;
            double newVal = opLabel.equals("incre") ? (Double) currentVal + 1 : (Double) currentVal - 1;
            if (isArray) symTable.assignArrayValue(varName, index, newVal);
            else symTable.assignValue(varName, newVal);
        } else {
            String exactOp = rawLabel(((Tree) ((Tree) updatePrime.children.get(0)).children.get(0)).data);
            Object rhsValue = evaluator.evaluate((Tree) updatePrime.children.get(1));
            double cv = (Double) (isArray ? getArrayValue(varName, index) : symTable.getAttributes(varName).value);
            double rv = (Double) rhsValue, result = 0;
            
            switch (exactOp) {
                case "assign_add": result = cv + rv; break;
                case "assign_min": result = cv - rv; break;
                case "assign_mul": result = cv * rv; break;
                case "assign_div": result = cv / rv; break;
                case "assign_mod": result = cv % rv; break;
            }
            if (isArray) symTable.assignArrayValue(varName, index, result);
            else symTable.assignValue(varName, result);
        }
    }

    private void executeTryCatch(Tree node) {
        Tree checkBlock = (Tree) node.children.get(1);
        String errorVarName = extractLexeme(((Tree) node.children.get(3)).data);
        Tree handleBlock = (Tree) node.children.get(4);
        Tree finallyPart = (Tree) node.children.get(5);

        try {
            symTable.enterScope();
            try { executeBlock(checkBlock); } finally { symTable.exitScope(); }
        } catch (SpillException e) {
            symTable.enterScope();
            try {
                String errType = (e.thrownValue instanceof Double) ? "PRICE" : (e.thrownValue instanceof Boolean) ? "QUALITY" : "RECIPE";
                symTable.declareVariable(errorVarName, errType);
                symTable.assignValue(errorVarName, e.thrownValue);
                executeBlock(handleBlock);
            } finally { symTable.exitScope(); }
        } finally {
            if (!finallyPart.children.isEmpty() && !rawLabel(((Tree) finallyPart.children.get(0)).data).equals("ε")) {
                symTable.enterScope();
                try { executeBlock((Tree) finallyPart.children.get(1)); } finally { symTable.exitScope(); }
            }
        }
    }

    private void executeThrow(Tree node) {
        throw new SpillException(evaluator.evaluate((Tree) node.children.get(1)));
    }

    private void executeFunctionDeclaration(Tree node) {
        String returnType = mapDataType(rawLabel(((Tree) ((Tree) node.children.get(1)).children.get(0)).data));
        String funcName = extractLexeme(((Tree) node.children.get(2)).data);
        MCLangFunction function = new MCLangFunction(returnType, funcName, (Tree) node.children.get(4));
        extractParameters((Tree) node.children.get(3), function);
        functions.put(funcName, function);
    }

    private void extractParameters(Tree paramNode, MCLangFunction function) {
        if (paramNode == null || paramNode.children.isEmpty() || rawLabel(((Tree) paramNode.children.get(0)).data).equals("ε")) return;
        String label = rawLabel(paramNode.data);
        if (label.equals("PARAM_LIST") || label.equals("MORE_PARAMS")) {
            String pType = mapDataType(rawLabel(((Tree)((Tree) paramNode.children.get(0)).children.get(0)).data));
            String pName = extractLexeme(((Tree) paramNode.children.get(1)).data);
            function.addParameter(pType, pName);
            if (paramNode.children.size() > 2) extractParameters((Tree) paramNode.children.get(2), function);
        }
    }

    private void executeReturn(Tree node) {
        if (node.children.size() == 1) throw new ReturnException(null); 
        else throw new ReturnException(evaluator.evaluate((Tree) node.children.get(1)));
    }

    public Object executeFunctionCall(String functionName, Tree argList) {
        MCLangFunction func = functions.get(functionName);
        if (func == null) throw new RuntimeException("Semantic Error: Task '" + functionName + "' is not defined.");

        java.util.List<Object> evaluatedArgs = new java.util.ArrayList<>();
        Tree currentArgList = argList;
        
        while (currentArgList != null && !currentArgList.children.isEmpty() && !rawLabel(currentArgList.data).equals("ε")) {
            String label = rawLabel(currentArgList.data);
            if (label.equals("ARG_LIST") || label.equals("MORE_ARGS")) {
                evaluatedArgs.add(evaluator.evaluate((Tree) currentArgList.children.get(0)));
                currentArgList = currentArgList.children.size() > 1 ? (Tree) currentArgList.children.get(1) : null;
            } else if (label.equals("PRIMARY'") || label.equals("STATEMENT'")) {
                currentArgList = (Tree) currentArgList.children.get(0);
            } else {
                evaluatedArgs.add(evaluator.evaluate(currentArgList));
                currentArgList = null;
            }
        }

        if (evaluatedArgs.size() != func.parameters.size()) throw new RuntimeException("Semantic Error: Task '" + functionName + "' expects " + func.parameters.size() + " arguments.");

        SymbolTable.Environment callerEnv = symTable.getCurrentEnv();
        symTable.setCurrentEnv(symTable.getGlobalEnv()); 
        symTable.enterScope(); 

        try {
            for (int i = 0; i < func.parameters.size(); i++) {
                MCLangFunction.Parameter p = func.parameters.get(i);
                symTable.declareVariable(p.name, p.type);
                symTable.assignValue(p.name, evaluatedArgs.get(i));
            }
            executeBlock(func.bodyNode);

            if (!func.returnType.equals("EMPTY")) throw new RuntimeException("Semantic Error: Task '" + functionName + "' must YIELD a value.");
            return null;

        } catch (ReturnException e) {
            if (func.returnType.equals("EMPTY") && e.returnValue != null) throw new RuntimeException("Semantic Error: EMPTY Task cannot YIELD a value.");
            if (e.returnValue != null) {
                boolean typeMatch = (func.returnType.equals("PRICE") && e.returnValue instanceof Double) ||
                                    (func.returnType.equals("QUALITY") && e.returnValue instanceof Boolean) ||
                                    (func.returnType.equals("RECIPE") && e.returnValue instanceof String);
                if (!typeMatch) throw new RuntimeException("Semantic Error: Task '" + functionName + "' returned the wrong data type.");
            }
            return e.returnValue;
        } finally {
            symTable.setCurrentEnv(callerEnv);
        }
    }
    
    private String rawLabel(String data) {
        int idx = data.indexOf(" (");
        return idx >= 0 ? data.substring(0, idx).trim() : data.trim();
    }

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

    private String mapDataType(String tokenType) {
        switch (tokenType) {
            case "num_type": return "PRICE";
            case "string_type": return "RECIPE";
            case "bool_type": return "QUALITY";
            default: return "EMPTY";
        }
    }

    private Object getArrayValue(String name, int index) {
        SymbolTable.VariableAttributes attr = symTable.getAttributes(name);
        if (attr == null || !attr.isArray) throw new RuntimeException("Semantic Error: Invalid array access.");
        return attr.arrayValues[index];
    }
}

class BreakException extends RuntimeException {}
class ContinueException extends RuntimeException {}
class SpillException extends RuntimeException {
    public Object thrownValue;
    public SpillException(Object thrownValue) { super(thrownValue.toString()); this.thrownValue = thrownValue; }
}
class ReturnException extends RuntimeException {
    public Object returnValue;
    public ReturnException(Object returnValue) { super("Return", null, false, false); this.returnValue = returnValue; }
}