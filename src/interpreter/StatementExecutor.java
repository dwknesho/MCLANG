package interpreter;

import errors.InterpreterExceptions;
import grtree.Tree;
import runtime.SymbolTable;

import java.util.Scanner;

//Executes imperative statements
public class StatementExecutor {
    private final SymbolTable symTable;
    private final ExpressionEvaluator evaluator;
    private final Scanner consoleInput;

    // Set by Interpreter after construction
    private Interpreter interpreter;

    public StatementExecutor(SymbolTable symTable, ExpressionEvaluator evaluator, Scanner consoleInput) {
        this.symTable     = symTable;
        this.evaluator    = evaluator;
        this.consoleInput = consoleInput;
    }

    public void setInterpreter(Interpreter interpreter) {
        this.interpreter = interpreter;
    }

    // Declaration
    public void executeDeclaration(Tree node) {
        String mclangType = mapDataType(rawLabel(((Tree) ((Tree) node.children.get(0)).children.get(0)).data));
        String varName    = extractLexeme(((Tree) node.children.get(1)).data);
        Tree   declPrime  = (Tree) node.children.get(2);
        String primeLabel = rawLabel(declPrime.data);

        if (primeLabel.equals("VAR_INIT")) {
            symTable.declareVariable(varName, mclangType);
            if (!declPrime.children.isEmpty()
                    && !rawLabel(((Tree) declPrime.children.get(0)).data).equals("ε")) {
                symTable.assignValue(varName, evaluator.evaluate((Tree) declPrime.children.get(1)));
            }
        } else if (primeLabel.equals("DECLARATION_STMT'")) {
            Object sizeObj = evaluator.evaluate((Tree) declPrime.children.get(1));
            if (!(sizeObj instanceof Double)) {
                throw new RuntimeException("Semantic Error: Array size must be a PRICE.");
            }
            symTable.declareArray(varName, mclangType, ((Double) sizeObj).intValue());
        }
    }

    // Assignment
    public void executeAssignment(String varName, Tree stmtPrime) {
        boolean isArray   = false;
        int     index     = -1;
        Tree    assignPart = stmtPrime;

        if (rawLabel(stmtPrime.data).equals("STATEMENT'")) {
            Tree lValuePrime = (Tree) stmtPrime.children.get(0);
            assignPart = (Tree) stmtPrime.children.get(1);
            if (!lValuePrime.children.isEmpty()
                    && !rawLabel(((Tree) lValuePrime.children.get(0)).data).equals("ε")
                    && rawLabel(((Tree) lValuePrime.children.get(0)).data).equals("l_brack")) {
                isArray = true;
                Object idxObj = evaluator.evaluate((Tree) lValuePrime.children.get(1));
                if (!(idxObj instanceof Double)) {
                    throw new RuntimeException("Semantic Error: Array index must be a PRICE.");
                }
                index = ((Double) idxObj).intValue();
            }
        }

        String assignLabel = rawLabel(assignPart.data);
        String exactOp     = "";
        Object rhsValue    = null;

        if (assignLabel.equals("incre") || assignLabel.equals("decre")) {
            exactOp = assignLabel;
        } else {
            exactOp  = rawLabel(((Tree) ((Tree) assignPart.children.get(0)).children.get(0)).data);
            rhsValue = evaluator.evaluate((Tree) assignPart.children.get(1));
        }

        applyAssignOp(varName, isArray, index, exactOp, rhsValue);
    }

    private void applyAssignOp(String varName, boolean isArray, int index, String op, Object rhs) {
        if (op.equals("incre") || op.equals("decre")) {
            Object cur = isArray ? getArrayValue(varName, index) : symTable.getAttributes(varName).value;
            if (!(cur instanceof Double)) {
                throw new RuntimeException("Semantic Error: Increment/Decrement requires a PRICE.");
            }
            double newVal = op.equals("incre") ? (Double) cur + 1 : (Double) cur - 1;
            if (isArray) symTable.assignArrayValue(varName, index, newVal);
            else         symTable.assignValue(varName, newVal);
            return;
        }

        if (op.equals("assign_as")) {
            if (isArray) symTable.assignArrayValue(varName, index, rhs);
            else         symTable.assignValue(varName, rhs);
            return;
        }

        // Compound assignment
        Object cur = isArray ? getArrayValue(varName, index) : symTable.getAttributes(varName).value;
        if (!(cur instanceof Double) || !(rhs instanceof Double)) {
            throw new RuntimeException("Semantic Error: Compound assignment requires PRICE values.");
        }
        double cv = (Double) cur, rv = (Double) rhs, result = 0;
        switch (op) {
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
        else         symTable.assignValue(varName, result);
    }

    // IO  (ORDER / SERVE)
    public void executeIO(Tree node) {
        String ioType = rawLabel(((Tree) node.children.get(0)).data);

        if (ioType.equals("output")) {
            Object value = evaluator.evaluate((Tree) node.children.get(1));
            if (value instanceof Double && ((Double) value) % 1 == 0) {
                System.out.print(((Double) value).intValue());
            } else {
                System.out.print(value);
            }

        } else if (ioType.equals("input")) {
            Tree   lValue  = (Tree) node.children.get(1);
            String varName = extractLexeme(((Tree) lValue.children.get(0)).data);

            boolean isArray = false;
            int     index   = -1;
            if (lValue.children.size() > 1) {
                Tree lValuePrime = (Tree) lValue.children.get(1);
                if (!lValuePrime.children.isEmpty()
                        && rawLabel(((Tree) lValuePrime.children.get(0)).data).equals("l_brack")) {
                    isArray = true;
                    index   = ((Double) evaluator.evaluate((Tree) lValuePrime.children.get(1))).intValue();
                }
            }

            SymbolTable.VariableAttributes attr = symTable.getAttributes(varName);
            if (attr == null) {
                throw new RuntimeException("Semantic Error: Variable '" + varName + "' not declared for ORDER.");
            }

            String userInput = consoleInput.nextLine();
            try {
                Object val;
                if      (attr.dataType.equals("PRICE"))   val = Double.parseDouble(userInput);
                else if (attr.dataType.equals("QUALITY")) val = userInput.trim().equalsIgnoreCase("FRESH");
                else                                       val = userInput;

                if (isArray) symTable.assignArrayValue(varName, index, val);
                else         symTable.assignValue(varName, val);
            } catch (NumberFormatException e) {
                throw new RuntimeException("Runtime Error: Invalid input. Expected a PRICE (number).");
            }
        }
    }

    // Loops  (REFILL / PREP / STIR) 
    public void executeLoop(Tree node) {
        Tree   actualLoop = (Tree) node.children.get(0);
        String loopType   = rawLabel(actualLoop.data);

        symTable.enterScope();
        try {
            switch (loopType) {
                case "WHILE_LOOP":   executeWhile(actualLoop);  break;
                case "DO_WHILE_LOOP": executeDoWhile(actualLoop); break;
                case "FOR_LOOP":     executeFor(actualLoop);    break;
            }
        } finally {
            symTable.exitScope();
        }
    }

    private void executeWhile(Tree node) {
        while (true) {
            Object cond = evaluator.evaluate((Tree) node.children.get(1));
            if (!(cond instanceof Boolean)) {
                throw new RuntimeException("Semantic Error: REFILL condition must evaluate to QUALITY.");
            }
            if (!(Boolean) cond) break;
            try {
                symTable.enterScope();
                interpreter.executeBlock((Tree) node.children.get(2));
            } catch (InterpreterExceptions.ContinueException ignored) {
            } catch (InterpreterExceptions.BreakException e) {
                break;
            } finally {
                symTable.exitScope();
            }
        }
    }

    private void executeDoWhile(Tree node) {
        do {
            try {
                symTable.enterScope();
                interpreter.executeBlock((Tree) node.children.get(1));
            } catch (InterpreterExceptions.ContinueException ignored) {
            } catch (InterpreterExceptions.BreakException e) {
                break;
            } finally {
                symTable.exitScope();
            }
            Object cond = evaluator.evaluate((Tree) node.children.get(3));
            if (!(cond instanceof Boolean)) {
                throw new RuntimeException("Semantic Error: STIR condition must evaluate to QUALITY.");
            }
            if (!(Boolean) cond) break;
        } while (true);
    }

    private void executeFor(Tree node) {
        Tree initNode  = (Tree) node.children.get(1);
        Tree initChild = (Tree) initNode.children.get(0);
        if (rawLabel(initChild.data).equals("DECLARATION_STMT")) {
            executeDeclaration(initChild);
        } else {
            Tree   lValue   = (Tree) initChild.children.get(0);
            String varName  = extractLexeme(((Tree) lValue.children.get(0)).data);
            Tree   lVPrime  = lValue.children.size() > 1 ? (Tree) lValue.children.get(1) : null;
            Tree   assignPrime = (Tree) initChild.children.get(1);

            if (lVPrime == null) {
                executeAssignment(varName, assignPrime);
            } else {
                Tree dummyStmtPrime = new Tree("STATEMENT'");
                dummyStmtPrime.addChild(lVPrime);
                dummyStmtPrime.addChild(assignPrime);
                executeAssignment(varName, dummyStmtPrime);
            }
        }

        // Condition + body + update
        while (true) {
            Object cond = evaluator.evaluate((Tree) node.children.get(2));
            if (!(cond instanceof Boolean)) {
                throw new RuntimeException("Semantic Error: PREP condition must evaluate to QUALITY.");
            }
            if (!(Boolean) cond) break;
            try {
                symTable.enterScope();
                interpreter.executeBlock((Tree) node.children.get(4));
            } catch (InterpreterExceptions.ContinueException ignored) {
            } catch (InterpreterExceptions.BreakException e) {
                break;
            } finally {
                symTable.exitScope();
            }
            executeForUpdate((Tree) node.children.get(3));
        }
    }

    private void executeForUpdate(Tree node) {
        Tree   lValue  = (Tree) node.children.get(0);
        String varName = extractLexeme(((Tree) lValue.children.get(0)).data);

        boolean isArray = false;
        int     index   = -1;
        if (lValue.children.size() > 1) {
            Tree lvp = (Tree) lValue.children.get(1);
            if (!lvp.children.isEmpty()
                    && rawLabel(((Tree) lvp.children.get(0)).data).equals("l_brack")) {
                isArray = true;
                index   = ((Double) evaluator.evaluate((Tree) lvp.children.get(1))).intValue();
            }
        }

        Tree   updatePrime = (Tree) node.children.get(1);
        String opLabel     = rawLabel(updatePrime.data);

        if (opLabel.equals("incre") || opLabel.equals("decre")) {
            Object cur    = isArray ? getArrayValue(varName, index) : symTable.getAttributes(varName).value;
            double newVal = opLabel.equals("incre") ? (Double) cur + 1 : (Double) cur - 1;
            if (isArray) symTable.assignArrayValue(varName, index, newVal);
            else         symTable.assignValue(varName, newVal);
        } else {
            String exactOp = rawLabel(((Tree) ((Tree) updatePrime.children.get(0)).children.get(0)).data);
            double rhs     = (Double) evaluator.evaluate((Tree) updatePrime.children.get(1));
            double cv      = (Double) (isArray ? getArrayValue(varName, index) : symTable.getAttributes(varName).value);
            double result  = 0;
            switch (exactOp) {
                case "assign_add": 
                result = cv + rhs; 
                break;
                case "assign_min": 
                result = cv - rhs; 
                break;
                case "assign_mul": 
                result = cv * rhs; 
                break;
                case "assign_div": 
                result = cv / rhs; 
                break;
                case "assign_mod": 
                result = cv % rhs; 
                break;
            }
            if (isArray) symTable.assignArrayValue(varName, index, result);
            else         symTable.assignValue(varName, result);
        }
    }

    // Shared helpers
    private Object getArrayValue(String name, int index) {
        SymbolTable.VariableAttributes attr = symTable.getAttributes(name);
        if (attr == null || !attr.isArray) {
            throw new RuntimeException("Semantic Error: Invalid array access for '" + name + "'.");
        }
        return attr.arrayValues[index];
    }

    public String mapDataType(String tokenType) {
        switch (tokenType) {
            case "num_type":    
            return "PRICE";
            case "string_type": 
            return "RECIPE";
            case "bool_type":   
            return "QUALITY";
            default:            
            return "EMPTY";
        }
    }

    private String rawLabel(String data) {
        int idx = data.indexOf(" (");
        return idx >= 0 ? data.substring(0, idx).trim() : data.trim();
    }

    private String extractLexeme(String data) {
        int start = data.indexOf("(");
        int end   = data.lastIndexOf(")");
        if (start != -1 && end != -1) {
            String lexeme = data.substring(start + 1, end);
            if (lexeme.startsWith("\"") && lexeme.endsWith("\"")) {
                String inner = lexeme.substring(1, lexeme.length() - 1);
                return inner.replace("\\n",  "\n")
                            .replace("\\t",  "\t")
                            .replace("\\\"", "\"")
                            .replace("\\\\", "\\");
            }
            return lexeme;
        }
        return data;
    }
}