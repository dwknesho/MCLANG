package interpreter;

import grtree.Tree;
import semantic.MCLangFunction;
import semantic.SymbolTable;
import errors.InterpreterExceptions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Handles TASK (function) declarations, parameter binding, and call execution.
 * Manages the function registry and call stack scoping.
 */
public class FunctionExecutor {
    private final SymbolTable symTable;
    private final Map<String, MCLangFunction> functions = new HashMap<>();
    private final ExpressionEvaluator evaluator;

    // Set by Interpreter after construction (circular dependency resolution)
    private Interpreter interpreter;

    public FunctionExecutor(SymbolTable symTable, ExpressionEvaluator evaluator) {
        this.symTable = symTable;
        this.evaluator = evaluator;
    }

    public void setInterpreter(Interpreter interpreter) {
        this.interpreter = interpreter;
    }

    // Registry

    public boolean hasFunction(String name) {
        return functions.containsKey(name);
    }

    // Declaration

    public void executeFunctionDeclaration(Tree node) {
        String returnType = mapDataType(rawLabel(((Tree) ((Tree) node.children.get(1)).children.get(0)).data));
        String funcName = extractLexeme(((Tree) node.children.get(2)).data);
        MCLangFunction function = new MCLangFunction(returnType, funcName, (Tree) node.children.get(4));
        extractParameters((Tree) node.children.get(3), function);
        functions.put(funcName, function);
    }

    private void extractParameters(Tree paramNode, MCLangFunction function) {
        if (paramNode == null || paramNode.children.isEmpty()
                || rawLabel(((Tree) paramNode.children.get(0)).data).equals("ε")) return;

        String label = rawLabel(paramNode.data);
        if (label.equals("PARAM_LIST") || label.equals("MORE_PARAMS")) {
            String pType = mapDataType(rawLabel(((Tree) ((Tree) paramNode.children.get(0)).children.get(0)).data));
            String pName = extractLexeme(((Tree) paramNode.children.get(1)).data);
            function.addParameter(pType, pName);
            if (paramNode.children.size() > 2) {
                extractParameters((Tree) paramNode.children.get(2), function);
            }
        }
    }

    // Call Execution

    public Object executeFunctionCall(String functionName, Tree argList) {
        MCLangFunction func = functions.get(functionName);
        if (func == null) {
            throw new RuntimeException("Semantic Error: Task '" + functionName + "' is not defined.");
        }

        List<Object> evaluatedArgs = buildArgList(argList);

        if (evaluatedArgs.size() != func.parameters.size()) {
            throw new RuntimeException("Semantic Error: Task '" + functionName + "' expects "
                    + func.parameters.size() + " arguments.");
        }

        // Switch to a fresh scope rooted at global (lexical scoping)
        SymbolTable.Environment callerEnv = symTable.getCurrentEnv();
        symTable.setCurrentEnv(symTable.getGlobalEnv());
        symTable.enterScope();

        try {
            // Bind parameters
            for (int i = 0; i < func.parameters.size(); i++) {
                MCLangFunction.Parameter p = func.parameters.get(i);
                symTable.declareVariable(p.name, p.type);
                symTable.assignValue(p.name, evaluatedArgs.get(i));
            }

            interpreter.executeBlock(func.bodyNode);

            if (!func.returnType.equals("EMPTY")) {
                throw new RuntimeException("Semantic Error: Task '" + functionName + "' must YIELD a value.");
            }
            return null;

        } catch (InterpreterExceptions.ReturnException e) {
            if (func.returnType.equals("EMPTY") && e.returnValue != null) {
                throw new RuntimeException("Semantic Error: EMPTY Task cannot YIELD a value.");
            }
            if (e.returnValue != null) {
                boolean typeMatch =
                        (func.returnType.equals("PRICE")   && e.returnValue instanceof Double)  ||
                        (func.returnType.equals("QUALITY") && e.returnValue instanceof Boolean) ||
                        (func.returnType.equals("RECIPE")  && e.returnValue instanceof String);
                if (!typeMatch) {
                    throw new RuntimeException(
                            "Semantic Error: Task '" + functionName + "' returned the wrong data type.");
                }
            }
            return e.returnValue;
        } finally {
            symTable.setCurrentEnv(callerEnv);
        }
    }

    private List<Object> buildArgList(Tree argList) {
        List<Object> args = new ArrayList<>();
        Tree current = argList;
        while (current != null && !current.children.isEmpty() && !rawLabel(current.data).equals("ε")) {
            String label = rawLabel(current.data);
            if (label.equals("ARG_LIST") || label.equals("MORE_ARGS")) {
                args.add(evaluator.evaluate((Tree) current.children.get(0)));
                current = current.children.size() > 1 ? (Tree) current.children.get(1) : null;
            } else if (label.equals("PRIMARY'") || label.equals("STATEMENT'")) {
                current = (Tree) current.children.get(0);
            } else {
                args.add(evaluator.evaluate(current));
                current = null;
            }
        }
        return args;
    }

    // Helpers

    public String mapDataType(String tokenType) {
        switch (tokenType) {
            case "num_type":    return "PRICE";
            case "string_type": return "RECIPE";
            case "bool_type":   return "QUALITY";
            default:            return "EMPTY";
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