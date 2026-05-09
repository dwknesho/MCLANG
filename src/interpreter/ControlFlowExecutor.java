package interpreter;

import errors.InterpreterExceptions;
import grtree.Tree;
import semantic.SymbolTable;


//Executes conditional and exception-handling statements
 
public class ControlFlowExecutor {
    private final SymbolTable symTable;
    private final ExpressionEvaluator evaluator;

    // Set by Interpreter after construction
    private Interpreter interpreter;

    public ControlFlowExecutor(SymbolTable symTable, ExpressionEvaluator evaluator) {
        this.symTable = symTable;
        this.evaluator = evaluator;
    }

    public void setInterpreter(Interpreter interpreter) {
        this.interpreter = interpreter;
    }

    // WANT / SIDE / ONLY (if / elseif / else)

    public void executeIf(Tree node) {
        symTable.enterScope();
        try {
            Object cond = evaluator.evaluate((Tree) node.children.get(1));
            if (!(cond instanceof Boolean)) {
                throw new RuntimeException("Semantic Error: WANT condition must evaluate to QUALITY.");
            }

            boolean executed = false;
            if ((Boolean) cond) {
                interpreter.executeBlock((Tree) node.children.get(2));
                executed = true;
            } else {
                // Walk ELSEIF chain
                Tree elifList = (Tree) node.children.get(3);
                while (!elifList.children.isEmpty()
                        && !rawLabel(((Tree) elifList.children.get(0)).data).equals("ε")) {
                    Object elifCond = evaluator.evaluate((Tree) elifList.children.get(1));
                    if ((Boolean) elifCond) {
                        interpreter.executeBlock((Tree) elifList.children.get(2));
                        executed = true;
                        break;
                    }
                    elifList = (Tree) elifList.children.get(3);
                }

                // ONLY (else) block
                if (!executed) {
                    Tree elsePart = (Tree) node.children.get(4);
                    if (!elsePart.children.isEmpty()
                            && !rawLabel(((Tree) elsePart.children.get(0)).data).equals("ε")) {
                        interpreter.executeBlock((Tree) elsePart.children.get(1));
                    }
                }
            }
        } finally {
            symTable.exitScope();
        }
    }

    // MENU / FOOD / SOLDOUT (switch / case / default)

    public void executeSwitch(Tree node) {
        symTable.enterScope();
        try {
            String switchVar = extractLexeme(((Tree) node.children.get(1)).data);
            SymbolTable.VariableAttributes attr = symTable.getAttributes(switchVar);
            if (attr == null) {
                throw new RuntimeException("Semantic Error: Variable '" + switchVar + "' in MENU not declared.");
            }

            Object switchValue = attr.value;
            boolean matched = false;

            Tree caseList = (Tree) node.children.get(2);
            try {
                while (!caseList.children.isEmpty()
                        && !rawLabel(((Tree) caseList.children.get(0)).data).equals("ε")) {
                    Object caseValue = evaluator.evaluate((Tree) caseList.children.get(1));
                    if (matched || switchValue.equals(caseValue)) {
                        matched = true;
                        interpreter.executeStatementList((Tree) caseList.children.get(2));
                    }
                    caseList = (Tree) caseList.children.get(3);
                }

                // SOLDOUT (default) block
                Tree defaultCase = (Tree) node.children.get(3);
                if (!defaultCase.children.isEmpty()
                        && !rawLabel(((Tree) defaultCase.children.get(0)).data).equals("ε")) {
                    interpreter.executeStatementList((Tree) defaultCase.children.get(1));
                }
            } catch (InterpreterExceptions.BreakException ignored) { }

        } finally {
            symTable.exitScope();
        }
    }

    // CHECK / HANDLE / CLEAN  (try / catch / finally)

    public void executeTryCatch(Tree node) {
        Tree checkBlock   = (Tree) node.children.get(1);
        String errorVar   = extractLexeme(((Tree) node.children.get(3)).data);
        Tree handleBlock  = (Tree) node.children.get(4);
        Tree finallyPart  = (Tree) node.children.get(5);

        try {
            symTable.enterScope();
            try {
                interpreter.executeBlock(checkBlock);
            } finally {
                symTable.exitScope();
            }
        } catch (InterpreterExceptions.SpillException e) {
            symTable.enterScope();
            try {
                String errType = (e.thrownValue instanceof Double)  ? "PRICE"
                               : (e.thrownValue instanceof Boolean) ? "QUALITY"
                               : "RECIPE";
                symTable.declareVariable(errorVar, errType);
                symTable.assignValue(errorVar, e.thrownValue);
                interpreter.executeBlock(handleBlock);
            } finally {
                symTable.exitScope();
            }
        } finally {
            if (!finallyPart.children.isEmpty()
                    && !rawLabel(((Tree) finallyPart.children.get(0)).data).equals("ε")) {
                symTable.enterScope();
                try {
                    interpreter.executeBlock((Tree) finallyPart.children.get(1));
                } finally {
                    symTable.exitScope();
                }
            }
        }
    }

    // SPILL (throw)

    public void executeThrow(Tree node) {
        throw new InterpreterExceptions.SpillException(evaluator.evaluate((Tree) node.children.get(1)));
    }

    // YIELD (return) 

    public void executeReturn(Tree node) {
        if (node.children.size() == 1) {
            throw new InterpreterExceptions.ReturnException(null);
        } else {
            throw new InterpreterExceptions.ReturnException(evaluator.evaluate((Tree) node.children.get(1)));
        }
    }

    // Helpers

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