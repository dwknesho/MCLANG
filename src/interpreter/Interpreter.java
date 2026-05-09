package interpreter;

import errors.ErrorReporter;
import errors.InterpreterExceptions;
import grtree.Tree;
import java.util.Scanner;
import semantic.SymbolTable;

public class Interpreter {
    private final SymbolTable         symTable;
    private final ErrorReporter       reporter;
    private final ExpressionEvaluator evaluator;
    private final StatementExecutor   stmtExec;
    private final ControlFlowExecutor ctrlExec;
    private final FunctionExecutor    funcExec;

    public Interpreter(SymbolTable symTable, ErrorReporter reporter) {
        this.symTable  = symTable;
        this.reporter  = reporter;

        Scanner consoleInput = new Scanner(System.in);

        // Build the sub-executors
        this.evaluator = new ExpressionEvaluator(symTable, this);
        this.stmtExec  = new StatementExecutor(symTable, evaluator, consoleInput);
        this.ctrlExec  = new ControlFlowExecutor(symTable, evaluator);
        this.funcExec  = new FunctionExecutor(symTable, evaluator);

        // Wire back-references so sub-executors can call each other via Interpreter
        this.stmtExec.setInterpreter(this);
        this.ctrlExec.setInterpreter(this);
        this.funcExec.setInterpreter(this);
    }

    

    // Called by Tester after a successful parse.
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

    public boolean hasFunction(String name) {
        return funcExec.hasFunction(name);
    }

    public Object executeFunctionCall(String functionName, Tree argList) {
        return funcExec.executeFunctionCall(functionName, argList);
    }

    // Statement list / block 

    public void executeStatementList(Tree node) {
        if (node == null || node.children.isEmpty() || rawLabel(node.data).equals("ε")) return;
        executeStatement((Tree) node.children.get(0));
        if (node.children.size() > 1) {
            executeStatementList((Tree) node.children.get(1));
        }
    }

    public void executeBlock(Tree blockNode) {
        if (!blockNode.children.isEmpty()
                && !rawLabel(((Tree) blockNode.children.get(0)).data).equals("ε")) {
            executeStatementList((Tree) blockNode.children.get(0));
        }
    }

    // Statement dispatch

    private void executeStatement(Tree stmtNode) {
        if (stmtNode == null || stmtNode.children.isEmpty()
                || rawLabel(stmtNode.data).equals("ε")) return;

        Tree   actualStmt = (Tree) stmtNode.children.get(0);
        String stmtType   = rawLabel(actualStmt.data);

        switch (stmtType) {
            case "DECLARATION_STMT": 
                stmtExec.executeDeclaration(actualStmt);         
                 break;
            case "IO_STMT":          
                stmtExec.executeIO(actualStmt);                  
                break;
            case "LOOP_STMT":        
                stmtExec.executeLoop(actualStmt);                
                break;
            case "IF_STMT":          
                ctrlExec.executeIf(actualStmt);                  
                break;
            case "SWITCH_STMT":      
                ctrlExec.executeSwitch(actualStmt);              
                break;
            case "TRY_CATCH_STMT":   
                ctrlExec.executeTryCatch(actualStmt);            
                break;
            case "THROW_STMT":       
                ctrlExec.executeThrow(actualStmt);               
                break;
            case "RETURN_STMT":      
                ctrlExec.executeReturn(actualStmt);              
                break;
            case "FUNCTION_DECL":    
                funcExec.executeFunctionDeclaration(actualStmt); 
                break;
            case "BREAK_STMT":       
                throw new InterpreterExceptions.BreakException();
            case "CONTINUE_STMT":    
                throw new InterpreterExceptions.ContinueException();

            case "id":
                String name = extractLexeme(actualStmt.data);
                if (stmtNode.children.size() == 1) {
                    funcExec.executeFunctionCall(name, null);
                } else {
                    Tree stmtPrime = (Tree) stmtNode.children.get(1);
                    if (hasFunction(name) || rawLabel(stmtPrime.data).equals("ARG_LIST")) {
                        funcExec.executeFunctionCall(name, stmtPrime);
                    } else {
                        stmtExec.executeAssignment(name, stmtPrime);
                    }
                }
                break;

            default: break;
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