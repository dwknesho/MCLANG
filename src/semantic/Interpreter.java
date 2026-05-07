package semantic;

import grtree.Tree;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

// The interpreter walks the AST produced by the parser and:
//   1. Type-checks every expression and statement (semantic rules)
//   2. Executes each statement — assignments, I/O, loops, conditionals, etc.
//   3. Keeps the symbol table updated with real types and values
//
// We use a simple "return value wrapper" pattern: every eval method returns
// a Value object that carries both the Java value and the McLang type string.
public class Interpreter {

    private SymbolTable symTable;
    private java.io.PrintStream out;
    private Scanner inputScanner;

    // Used to implement DONE (break) and NEXT (continue) — we throw a
    // lightweight signal object instead of using Java exceptions for flow,
    // which keeps the code cleaner and faster than try/catch everywhere
    private static class BreakSignal    extends RuntimeException { public BreakSignal()    { super(null,null,true,false); } }
    private static class ContinueSignal extends RuntimeException { public ContinueSignal() { super(null,null,true,false); } }

    // Return signal carries the actual returned value so the function call
    // site can pick it up after the signal unwinds the call stack
    private static class ReturnSignal extends RuntimeException {
        public final Value value;
        public ReturnSignal(Value v) { super(null,null,true,false); this.value = v; }
    }

    // Every expression evaluation returns one of these
    public static class Value {
        public static final String NUM    = "PRICE";
        public static final String BOOL   = "QUALITY";
        public static final String STRING = "RECIPE";
        public static final String VOID   = "EMPTY";

        public final String type;   // "PRICE", "QUALITY", "RECIPE", "EMPTY"
        public final Object raw;    // Double, Boolean, or String

        public Value(String type, Object raw) {
            this.type = type;
            this.raw  = raw;
        }

        public double asNum()    { return ((Double) raw); }
        public boolean asBool()  { return ((Boolean) raw); }
        public String asString() { return raw.toString(); }

        @Override
        public String toString() {
            if (raw == null) return "null";
            // Print numbers without trailing .0 when they're whole
            if (type.equals(NUM)) {
                double d = asNum();
                if (d == Math.floor(d) && !Double.isInfinite(d)) {
                    return String.valueOf((long) d);
                }
                return String.valueOf(d);
            }
            return raw.toString();
        }
    }

    public Interpreter(SymbolTable symTable) {
        this.symTable     = symTable;
        this.out          = System.out;
        this.inputScanner = new Scanner(System.in);
    }

    // -----------------------------------------------------------------------
    // Entry point — called by Tester after the parser finishes
    // -----------------------------------------------------------------------
    public void interpret(Tree ast) {
        if (ast == null) {
            semanticError("AST is null — nothing to interpret", -1);
            return;
        }
        try {
            execProgram(ast);
        } catch (SemanticException e) {
            if (e.line > 0) {
                System.out.println("\n[Semantic Error, Line " + e.line + "]: " + e.getMessage());
            } else {
                System.out.println("\n[Semantic Error]: " + e.getMessage());
            }
        }
    }

    // -----------------------------------------------------------------------
    // PROGRAM -> start STATEMENT_LIST end eof
    // -----------------------------------------------------------------------
    private void execProgram(Tree node) {
        // The AST root is PROGRAM; its only meaningful child is STATEMENT_LIST
        for (Object child : node.children) {
            Tree c = (Tree) child;
            String lbl = label(c);
            if (lbl.equals("STATEMENT_LIST") || lbl.equals("STATEMENT")) {
                execStatementList(c);
            }
        }
    }

    // -----------------------------------------------------------------------
    // STATEMENT_LIST -> STATEMENT STATEMENT_LIST | epsilon
    // -----------------------------------------------------------------------
    private void execStatementList(Tree node) {
        if (node == null) return;
        String lbl = label(node);

        if (lbl.equals("STATEMENT_LIST")) {
            for (Object child : node.children) {
                execStatement((Tree) child);
            }
        } else {
            // The AST sometimes collapses STATEMENT_LIST to a single STATEMENT
            execStatement(node);
        }
    }

    // -----------------------------------------------------------------------
    // Route a STATEMENT node to the right handler based on its first child
    // -----------------------------------------------------------------------
    private void execStatement(Tree node) {
        if (node == null) return;
        String lbl = label(node);

        switch (lbl) {
            case "DECLARATION_STMT": execDeclaration(node);   break;
            case "IO_STMT":          execIO(node);             break;
            case "IF_STMT":          execIf(node);             break;
            case "SWITCH_STMT":      execSwitch(node);         break;
            case "WHILE_LOOP":       execWhile(node);          break;
            case "FOR_LOOP":         execFor(node);            break;
            case "DO_WHILE_LOOP":    execDoWhile(node);        break;
            case "TRY_CATCH_STMT":   execTryCatch(node);       break;
            case "THROW_STMT":       execThrow(node);          break;
            case "FUNCTION_DECL":    execFunctionDecl(node);   break;
            case "RETURN_STMT":      execReturn(node);         break;
            case "BREAK_STMT":       throw new BreakSignal();
            case "CONTINUE_STMT":    throw new ContinueSignal();
            case "ASSIGNMENT_STMT":  execAssignment(node);     break;
            case "STATEMENT_LIST":   execStatementList(node);  break;
            case "STATEMENT":
                // Unwrap one level if the AST kept a STATEMENT wrapper
                if (!node.children.isEmpty()) {
                    execStatement((Tree) node.children.get(0));
                }
                break;
            default:
                // Function call used as a statement: id (args)
                if (lbl.startsWith("id (")) {
                    evalFunctionCall(node);
                }
                break;
        }
    }

    // -----------------------------------------------------------------------
    // DECLARATION_STMT -> TYPE id DECLARATION_STMT'
    //   DECLARATION_STMT' -> VAR_INIT semi | l_brack EXPRESSION r_brack semi
    //   VAR_INIT -> assign_as EXPRESSION | epsilon
    // -----------------------------------------------------------------------
    private void execDeclaration(Tree node) {
        String typeStr  = extractType(node);
        String varName  = extractId(node);
        int    lineNum  = extractLine(node);

        // Array declaration: TYPE id [ EXPRESSION ]
        if (hasChild(node, "VAR_INIT") == false && hasChildLabel(node, "l_brack")) {
            Tree sizeExpr = findChildByLabel(node, "EXPRESSION");
            if (sizeExpr == null) sizeExpr = findDeepExpression(node);
            Value sizeVal = evalExpression(sizeExpr);

            if (!sizeVal.type.equals(Value.NUM)) {
                semanticError("Array size must be a PRICE (number)", lineNum);
            }
            int size = (int) sizeVal.asNum();
            if (size <= 0) {
                semanticError("Array size must be a positive integer, got " + size, lineNum);
            }
            // Store array as a Double[] in the symbol table
            Double[] arr = new Double[size];
            symTable.declareVariable(varName, typeStr + "[]");
            symTable.assignValue(varName, arr);
            return;
        }

        // Regular variable declaration
        symTable.declareVariable(varName, typeStr);

        // If there's an initializer, evaluate it and assign
        Tree varInit = findChildByLabel(node, "VAR_INIT");
        if (varInit != null && !varInit.children.isEmpty()) {
            // VAR_INIT has children only when there's an assign_as + EXPRESSION
            Tree exprNode = findDeepExpression(varInit);
            if (exprNode != null) {
                Value val = evalExpression(exprNode);
                checkTypeMatch(typeStr, val, lineNum);
                symTable.assignValue(varName, val.raw);
            }
        }
    }

    // -----------------------------------------------------------------------
    // ASSIGNMENT_STMT -> L_VALUE ASSIGNMENT_STMT'
    //   ASSIGNMENT_STMT' -> ASSIGN_OP EXPRESSION semi | incre semi | decre semi
    // -----------------------------------------------------------------------
    private void execAssignment(Tree node) {
        // Get the variable name (first id terminal in this subtree)
        String varName = extractId(node);
        int    lineNum = extractLine(node);

        SymbolTable.VariableAttributes attr = symTable.getAttributes(varName);
        if (attr == null) {
            semanticError("Variable '" + varName + "' used before declaration", lineNum);
            return;
        }

        // Check for array index access: id [ EXPRESSION ]
        Tree lvalPrime = findChildByLabel(node, "L_VALUE'");
        boolean isArrayAccess = lvalPrime != null && hasChildLabel(lvalPrime, "l_brack");
        int arrayIndex = -1;
        if (isArrayAccess) {
            Tree idxExpr = findDeepExpression(lvalPrime);
            Value idxVal = evalExpression(idxExpr);
            if (!idxVal.type.equals(Value.NUM)) {
                semanticError("Array index must be PRICE (number)", lineNum);
            }
            arrayIndex = (int) idxVal.asNum();
        }

        // Find the operator and handle incre/decre
        Tree stmtPrime = findChildByLabel(node, "ASSIGNMENT_STMT'");
        if (stmtPrime == null) return;

        String firstChildLabel = label((Tree) stmtPrime.children.get(0));

        if (firstChildLabel.startsWith("incre")) {
            // x++
            double cur = getNumVar(varName, lineNum);
            symTable.assignValue(varName, cur + 1.0);
            return;
        }
        if (firstChildLabel.startsWith("decre")) {
            // x--
            double cur = getNumVar(varName, lineNum);
            symTable.assignValue(varName, cur - 1.0);
            return;
        }

        // Regular assignment: find operator and expression
        String op = firstChildLabel.contains("(") 
            ? firstChildLabel.substring(0, firstChildLabel.indexOf(" ("))
            : firstChildLabel;

        Tree exprNode = findDeepExpression(stmtPrime);
        Value rhs = evalExpression(exprNode);

        // For compound ops, we need the current value
        Object newVal = computeAssignment(op, varName, rhs, attr.dataType, lineNum);

        if (isArrayAccess) {
            Double[] arr = (Double[]) attr.value;
            if (arrayIndex < 0 || arrayIndex >= arr.length) {
                semanticError("Array index " + arrayIndex + " out of bounds for '" + varName + "' (size " + arr.length + ")", lineNum);
            }
            arr[arrayIndex] = (Double) newVal;
        } else {
            checkTypeMatch(attr.dataType, rhs, lineNum);
            symTable.assignValue(varName, newVal);
        }
    }

    private Object computeAssignment(String op, String varName, Value rhs, String declaredType, int line) {
        switch (op) {
            case "assign_as":  return rhs.raw;
            case "assign_add": {
                if (declaredType.equals("RECIPE")) {
                    // String concatenation
                    String cur = (String) symTable.getAttributes(varName).value;
                    return (cur == null ? "" : cur) + rhs.asString();
                }
                return getNumVar(varName, line) + toNum(rhs, line);
            }
            case "assign_min": return getNumVar(varName, line) - toNum(rhs, line);
            case "assign_mul": return getNumVar(varName, line) * toNum(rhs, line);
            case "assign_div": {
                double divisor = toNum(rhs, line);
                if (divisor == 0) semanticError("Division by zero in /= assignment", line);
                return getNumVar(varName, line) / divisor;
            }
            case "assign_mod": {
                double mod = toNum(rhs, line);
                if (mod == 0) semanticError("Division by zero in %= assignment", line);
                return getNumVar(varName, line) % mod;
            }
            default: return rhs.raw;
        }
    }

    // -----------------------------------------------------------------------
    // IO_STMT -> input L_VALUE semi | output EXPRESSION semi
    // -----------------------------------------------------------------------
    private void execIO(Tree node) {
        int lineNum = extractLine(node);

        // Figure out if it's input or output from the first child's label
        Tree firstChild = (Tree) node.children.get(0);
        String kind = label(firstChild);

        if (kind.startsWith("input")) {
            // ORDER varName;
            String varName = extractId(node);
            SymbolTable.VariableAttributes attr = symTable.getAttributes(varName);
            if (attr == null) {
                semanticError("Variable '" + varName + "' used before declaration in ORDER", lineNum);
                return;
            }
            String raw = inputScanner.nextLine().trim();
            try {
                if (attr.dataType.equals("PRICE")) {
                    symTable.assignValue(varName, Double.parseDouble(raw));
                } else if (attr.dataType.equals("QUALITY")) {
                    symTable.assignValue(varName, raw.equalsIgnoreCase("FRESH") || raw.equalsIgnoreCase("true"));
                } else {
                    symTable.assignValue(varName, raw);
                }
            } catch (NumberFormatException e) {
                semanticError("Cannot convert input '" + raw + "' to PRICE for variable '" + varName + "'", lineNum);
            }

        } else {
            // SERVE expression;
            Tree exprNode = findDeepExpression(node);
            if (exprNode != null) {
                Value val = evalExpression(exprNode);
                out.println(val.toString());
            }
        }
    }

    // -----------------------------------------------------------------------
    // IF_STMT -> if l_paren EXPRESSION r_paren BLOCK ELIF_LIST ELSE_PART
    // -----------------------------------------------------------------------
    private void execIf(Tree node) {
        int lineNum = extractLine(node);
        List<Tree> children = getChildren(node);

        int i = 0;
        // Skip the "if" terminal
        while (i < children.size() && label(children.get(i)).startsWith("if")) i++;

        // Evaluate condition
        Tree condExpr = findDeepExpressionFrom(children, i);
        Value cond = evalExpression(condExpr);
        if (!cond.type.equals(Value.BOOL)) {
            semanticError("WANT condition must be QUALITY (boolean), got " + cond.type, lineNum);
        }

        // Find the BLOCK for the if branch
        Tree ifBlock = findChildByLabel(node, "BLOCK");

        if (cond.asBool()) {
            execBlock(ifBlock);
            return;
        }

        // Try ELIF_LIST
        Tree elifList = findChildByLabel(node, "ELIF_LIST");
        if (elifList != null && execElif(elifList, lineNum)) return;

        // Try ELSE_PART
        Tree elsePart = findChildByLabel(node, "ELSE_PART");
        if (elsePart != null && !elsePart.children.isEmpty()) {
            Tree elseBlock = findChildByLabel(elsePart, "BLOCK");
            if (elseBlock != null) execBlock(elseBlock);
        }
    }

    // Returns true if a branch was taken
    private boolean execElif(Tree node, int lineNum) {
        if (node == null || node.children.isEmpty()) return false;

        // Find the condition expression inside this ELIF_LIST
        Tree condExpr = findDeepExpression(node);
        Value cond = evalExpression(condExpr);
        if (!cond.type.equals(Value.BOOL)) {
            semanticError("SIDE condition must be QUALITY (boolean), got " + cond.type, lineNum);
        }

        Tree block = findChildByLabel(node, "BLOCK");

        if (cond.asBool()) {
            execBlock(block);
            return true;
        }

        // Check nested ELIF_LIST
        Tree nextElif = findChildByLabel(node, "ELIF_LIST");
        return execElif(nextElif, lineNum);
    }

    // -----------------------------------------------------------------------
    // SWITCH_STMT -> switch l_paren id r_paren l_brace CASE_LIST DEFAULT_CASE r_brace
    // -----------------------------------------------------------------------
    private void execSwitch(Tree node) {
        int lineNum = extractLine(node);
        String varName = extractId(node);
        SymbolTable.VariableAttributes attr = symTable.getAttributes(varName);
        if (attr == null) {
            semanticError("Variable '" + varName + "' not declared in MENU", lineNum);
            return;
        }
        Value switchVal = new Value(attr.dataType, attr.value);

        Tree caseList = findChildByLabel(node, "CASE_LIST");
        boolean matched = execCaseList(caseList, switchVal, lineNum);

        if (!matched) {
            Tree defaultCase = findChildByLabel(node, "DEFAULT_CASE");
            if (defaultCase != null && !defaultCase.children.isEmpty()) {
                Tree stmtList = findChildByLabel(defaultCase, "STATEMENT_LIST");
                try {
                    execStatementList(stmtList);
                } catch (BreakSignal b) { /* done */ }
            }
        }
    }

    private boolean execCaseList(Tree node, Value switchVal, int lineNum) {
        if (node == null || node.children.isEmpty()) return false;

        // CASE_LIST -> case LITERAL colon STATEMENT_LIST CASE_LIST
        Tree literalNode = findChildByLabel(node, "LITERAL");
        Value caseVal = evalLiteral(literalNode);

        boolean match = valuesEqual(switchVal, caseVal);
        if (match) {
            Tree stmtList = findChildByLabel(node, "STATEMENT_LIST");
            try {
                execStatementList(stmtList);
            } catch (BreakSignal b) {
                return true;
            }
            return true;
        }

        Tree nextCaseList = findChildByLabel(node, "CASE_LIST");
        return execCaseList(nextCaseList, switchVal, lineNum);
    }

    // -----------------------------------------------------------------------
    // WHILE_LOOP -> while l_paren EXPRESSION r_paren BLOCK
    // -----------------------------------------------------------------------
    private void execWhile(Tree node) {
        int lineNum = extractLine(node);
        Tree condExpr = findDeepExpression(node);
        Tree block    = findChildByLabel(node, "BLOCK");

        while (true) {
            Value cond = evalExpression(condExpr);
            if (!cond.type.equals(Value.BOOL)) {
                semanticError("REFILL condition must be QUALITY (boolean), got " + cond.type, lineNum);
            }
            if (!cond.asBool()) break;
            try {
                execBlock(block);
            } catch (BreakSignal b)    { break; }
              catch (ContinueSignal c) { /* just re-check condition */ }
        }
    }

    // -----------------------------------------------------------------------
    // FOR_LOOP -> for l_paren FOR_INIT EXPRESSION semi FOR_UPDATE r_paren BLOCK
    // -----------------------------------------------------------------------
    private void execFor(Tree node) {
        int lineNum = extractLine(node);
        symTable.pushScope();

        // FOR_INIT: either a DECLARATION_STMT or ASSIGNMENT_STMT
        Tree forInit = findChildByLabel(node, "FOR_INIT");
        if (forInit != null && !forInit.children.isEmpty()) {
            Tree initChild = (Tree) forInit.children.get(0);
            String initLbl = label(initChild);
            if (initLbl.equals("DECLARATION_STMT")) execDeclaration(initChild);
            else                                     execAssignment(initChild);
        }

        // Condition: the EXPRESSION directly under FOR_LOOP (not inside FOR_INIT or FOR_UPDATE)
        Tree condExpr = findDirectExpression(node);

        // FOR_UPDATE: L_VALUE FOR_UPDATE' — like an assignment without semi
        Tree forUpdate = findChildByLabel(node, "FOR_UPDATE");

        Tree block = findChildByLabel(node, "BLOCK");

        while (true) {
            Value cond = evalExpression(condExpr);
            if (!cond.type.equals(Value.BOOL)) {
                semanticError("PREP condition must be QUALITY (boolean), got " + cond.type, lineNum);
            }
            if (!cond.asBool()) break;

            try {
                execBlock(block);
            } catch (BreakSignal b)    { break; }
              catch (ContinueSignal c) { /* fall through to update */ }

            execForUpdate(forUpdate, lineNum);
        }

        symTable.popScope();
    }

    private void execForUpdate(Tree node, int lineNum) {
        if (node == null) return;
        String varName = extractId(node);
        Tree updatePrime = findChildByLabel(node, "FOR_UPDATE'");
        if (updatePrime == null || updatePrime.children.isEmpty()) return;

        String firstLabel = label((Tree) updatePrime.children.get(0));

        if (firstLabel.startsWith("incre")) { symTable.assignValue(varName, getNumVar(varName, lineNum) + 1); return; }
        if (firstLabel.startsWith("decre")) { symTable.assignValue(varName, getNumVar(varName, lineNum) - 1); return; }

        String op = firstLabel.contains("(") ? firstLabel.substring(0, firstLabel.indexOf(" (")) : firstLabel;
        Tree exprNode = findDeepExpression(updatePrime);
        Value rhs = evalExpression(exprNode);
        Object newVal = computeAssignment(op, varName, rhs, "PRICE", lineNum);
        symTable.assignValue(varName, newVal);
    }

    // -----------------------------------------------------------------------
    // DO_WHILE_LOOP -> do_while BLOCK while l_paren EXPRESSION r_paren semi
    // -----------------------------------------------------------------------
    private void execDoWhile(Tree node) {
        int lineNum = extractLine(node);
        Tree block    = findChildByLabel(node, "BLOCK");
        Tree condExpr = findDeepExpression(node);

        do {
            try {
                execBlock(block);
            } catch (BreakSignal b)    { break; }
              catch (ContinueSignal c) { /* re-check condition */ }

            Value cond = evalExpression(condExpr);
            if (!cond.type.equals(Value.BOOL)) {
                semanticError("STIR condition must be QUALITY (boolean), got " + cond.type, lineNum);
            }
            if (!cond.asBool()) break;
        } while (true);
    }

    // -----------------------------------------------------------------------
    // TRY_CATCH_STMT -> try BLOCK catch l_paren id r_paren BLOCK FINALLY_PART
    // -----------------------------------------------------------------------
    private void execTryCatch(Tree node) {
        Tree tryBlock     = (Tree) node.children.get(0);
        String catchVar   = extractId(node);
        Tree catchBlock   = node.children.size() > 1 ? (Tree) node.children.get(1) : null;
        Tree finallyPart  = findChildByLabel(node, "FINALLY_PART");

        try {
            execBlock(tryBlock);
        } catch ( RuntimeException e) {
            // Bind the exception message to the catch variable
            if (catchVar != null && catchBlock != null) {
                symTable.pushScope();
                symTable.declareVariable(catchVar, "RECIPE");
                symTable.assignValue(catchVar, e.getMessage());
                execBlock(catchBlock);
                symTable.popScope();
            }
        } finally {
            if (finallyPart != null && !finallyPart.children.isEmpty()) {
                Tree finallyBlock = findChildByLabel(finallyPart, "BLOCK");
                if (finallyBlock != null) execBlock(finallyBlock);
            }
        }
    }

    // -----------------------------------------------------------------------
    // THROW_STMT -> throw EXPRESSION semi
    // -----------------------------------------------------------------------
    private void execThrow(Tree node) {
        int lineNum = extractLine(node);
        Tree exprNode = findDeepExpression(node);
        Value val = evalExpression(exprNode);
        throw new SemanticException("SPILL: " + val.toString(), lineNum);
    }

    // -----------------------------------------------------------------------
    // FUNCTION_DECL -> function TYPE id l_paren PARAM_LIST r_paren BLOCK
    // -----------------------------------------------------------------------
    private void execFunctionDecl(Tree node) {
        String returnType = extractType(node);
        String funcName   = extractId(node);

        List<String> paramTypes = new ArrayList<>();
        List<String> paramNames = new ArrayList<>();

        Tree paramList = findChildByLabel(node, "PARAM_LIST");
        collectParams(paramList, paramTypes, paramNames);

        symTable.declareFunction(funcName, returnType, paramTypes, paramNames);

        // Store the BLOCK node on the function so we can execute it when called
        Tree block = findChildByLabel(node, "BLOCK");
        SymbolTable.FunctionAttributes fa = symTable.getFunctionAttributes(funcName);
        fa.body = block;
    }

    private void collectParams(Tree node, List<String> types, List<String> names) {
        if (node == null || node.children.isEmpty()) return;
        // PARAM_LIST -> TYPE id MORE_PARAMS
        String type = null; String name = null;
        for (Object child : node.children) {
            Tree c = (Tree) child;
            String lbl = label(c);
            if (lbl.startsWith("num_type") || lbl.startsWith("string_type") || lbl.startsWith("bool_type") || lbl.startsWith("void")) {
                type = tokenTypeToMcLang(lbl);
            } else if (lbl.startsWith("id (")) {
                name = extractLexeme(c);
            } else if (lbl.equals("MORE_PARAMS")) {
                if (type != null && name != null) { types.add(type); names.add(name); }
                collectParams(c, types, names);
                return;
            }
        }
        if (type != null && name != null) { types.add(type); names.add(name); }
    }

    // -----------------------------------------------------------------------
    // RETURN_STMT -> return RETURN_STMT'
    // -----------------------------------------------------------------------
    private void execReturn(Tree node) {
        Tree returnPrime = findChildByLabel(node, "RETURN_STMT'");
        if (returnPrime == null || returnPrime.children.isEmpty()) {
            throw new ReturnSignal(new Value(Value.VOID, null));
        }
        // If RETURN_STMT' has only a semi child, it's an empty return
        if (returnPrime.children.size() == 1) {
            throw new ReturnSignal(new Value(Value.VOID, null));
        }
        Tree exprNode = findDeepExpression(returnPrime);
        Value val = evalExpression(exprNode);
        throw new ReturnSignal(val);
    }

    // -----------------------------------------------------------------------
    // BLOCK -> l_brace STATEMENT_LIST r_brace
    // -----------------------------------------------------------------------
    private void execBlock(Tree node) {
        if (node == null) return;
        symTable.pushScope();
        Tree stmtList = findChildByLabel(node, "STATEMENT_LIST");
        try {
            execStatementList(stmtList);
        } finally {
            symTable.popScope();
        }
    }

    // -----------------------------------------------------------------------
    // EXPRESSION EVALUATION
    // All eval methods return a Value with type and raw Java value
    // -----------------------------------------------------------------------
    private Value evalExpression(Tree node) {
        if (node == null) return new Value(Value.VOID, null);

        String lbl = label(node);

        // Literal terminals
        if (lbl.startsWith("numlit ("))    return new Value(Value.NUM,    Double.parseDouble(extractLexeme(node)));
        if (lbl.startsWith("stringlit (")) return new Value(Value.STRING, stripQuotes(extractLexeme(node)));
        if (lbl.startsWith("true"))        return new Value(Value.BOOL,   true);
        if (lbl.startsWith("false"))       return new Value(Value.BOOL,   false);

        // Identifier: look up value
        if (lbl.startsWith("id (")) {
            String varName = extractLexeme(node);
            SymbolTable.VariableAttributes attr = symTable.getAttributes(varName);
            if (attr == null) {
                semanticError("Variable '" + varName + "' used before declaration", -1);
            }
            if (attr.value == null) {
                semanticError("Variable '" + varName + "' is used but has no value assigned", -1);
            }
            return new Value(attr.dataType, attr.value);
        }

        // LITERAL node — delegate to its single child
        if (lbl.equals("LITERAL")) return evalExpression(firstChild(node));

        // PRIMARY node
        if (lbl.equals("PRIMARY")) return evalPrimary(node);

        // UNARY node
        if (lbl.equals("UNARY")) return evalUnary(node);

        // Binary operator nodes — these have the form: leftVal op rightVal
        // They show up when ADDITIVE', MULTIPLICATIVE', etc. have 2+ children after pruning
        return evalBinaryOrChain(node);
    }

    private Value evalPrimary(Tree node) {
        List<Tree> kids = getChildren(node);
        if (kids.isEmpty()) return new Value(Value.VOID, null);

        Tree first = kids.get(0);
        String firstLbl = label(first);

        // Parenthesized expression
        if (firstLbl.startsWith("l_paren")) {
            Tree exprNode = findDeepExpression(node);
            return evalExpression(exprNode);
        }

        // Literal
        if (firstLbl.equals("LITERAL")) return evalLiteral(first);

        // id — variable or function call
        if (firstLbl.startsWith("id (")) {
            String varName = extractLexeme(first);
            // Check if there's a PRIMARY' with l_paren -> function call
            Tree primaryPrime = findChildByLabel(node, "PRIMARY'");
            if (primaryPrime != null && hasChildLabel(primaryPrime, "l_paren")) {
                return evalFunctionCall(node);
            }
            // Array access: id [ EXPRESSION ]
            if (primaryPrime != null && hasChildLabel(primaryPrime, "l_brack")) {
                Tree idxExpr = findDeepExpression(primaryPrime);
                Value idxVal = evalExpression(idxExpr);
                int idx = (int) idxVal.asNum();
                SymbolTable.VariableAttributes attr = symTable.getAttributes(varName);
                if (attr == null) semanticError("Array '" + varName + "' not declared", -1);
                Double[] arr = (Double[]) attr.value;
                if (idx < 0 || idx >= arr.length) semanticError("Array index " + idx + " out of bounds", -1);
                return new Value(Value.NUM, arr[idx]);
            }
            // Plain variable
            SymbolTable.VariableAttributes attr = symTable.getAttributes(varName);
            if (attr == null) semanticError("Variable '" + varName + "' not declared", -1);
            if (attr.value == null) semanticError("Variable '" + varName + "' has no value", -1);
            return new Value(attr.dataType, attr.value);
        }

        return evalExpression(first);
    }

    private Value evalUnary(Tree node) {
        List<Tree> kids = getChildren(node);
        if (kids.isEmpty()) return new Value(Value.VOID, null);
        String op = label(kids.get(0));
        Tree operand = kids.size() > 1 ? kids.get(1) : null;

        if (op.startsWith("log_not")) {
            Value v = evalExpression(operand);
            if (!v.type.equals(Value.BOOL)) semanticError("! requires QUALITY operand", -1);
            return new Value(Value.BOOL, !v.asBool());
        }
        if (op.startsWith("arith_sub")) {
            Value v = evalExpression(operand);
            if (!v.type.equals(Value.NUM)) semanticError("Unary - requires PRICE operand", -1);
            return new Value(Value.NUM, -v.asNum());
        }
        if (op.startsWith("arith_add")) {
            Value v = evalExpression(operand);
            if (!v.type.equals(Value.NUM)) semanticError("Unary + requires PRICE operand", -1);
            return new Value(Value.NUM, v.asNum());
        }
        return evalExpression(kids.get(0));
    }

    // Handles binary operators that survived AST pruning as multi-child nodes
    private Value evalBinaryOrChain(Tree node) {
        List<Tree> kids = getChildren(node);
        if (kids.isEmpty()) return new Value(Value.VOID, null);
        if (kids.size() == 1) return evalExpression(kids.get(0));

        // kids[0] = left operand, kids[1] = operator terminal, kids[2] = right operand
        Value left = evalExpression(kids.get(0));
        if (kids.size() >= 3) {
            String op  = label(kids.get(1));
            Value right = evalExpression(kids.get(2));
            Value result = applyBinaryOp(op, left, right, -1);

            // Handle chained operations (e.g. a + b + c has more children)
            for (int i = 3; i + 1 < kids.size(); i += 2) {
                op    = label(kids.get(i));
                right = evalExpression(kids.get(i + 1));
                result = applyBinaryOp(op, result, right, -1);
            }
            return result;
        }
        return left;
    }

    private Value applyBinaryOp(String op, Value left, Value right, int line) {
        // Strip the lexeme part if present: "arith_add (+)" -> "arith_add"
        if (op.contains(" (")) op = op.substring(0, op.indexOf(" ("));

        switch (op) {
            // Arithmetic
            case "arith_add": {
                if (left.type.equals(Value.STRING) || right.type.equals(Value.STRING)) {
                    return new Value(Value.STRING, left.asString() + right.asString());
                }
                requireNum(left, "+", line); requireNum(right, "+", line);
                return new Value(Value.NUM, left.asNum() + right.asNum());
            }
            case "arith_sub": requireNum(left,"-",line); requireNum(right,"-",line); return new Value(Value.NUM, left.asNum() - right.asNum());
            case "arith_mul": requireNum(left,"*",line); requireNum(right,"*",line); return new Value(Value.NUM, left.asNum() * right.asNum());
            case "arith_div": {
                requireNum(left,"/",line); requireNum(right,"/",line);
                if (right.asNum() == 0) semanticError("Division by zero", line);
                return new Value(Value.NUM, left.asNum() / right.asNum());
            }
            case "arith_mod": {
                requireNum(left,"%",line); requireNum(right,"%",line);
                if (right.asNum() == 0) semanticError("Modulo by zero", line);
                return new Value(Value.NUM, left.asNum() % right.asNum());
            }
            // Relational
            case "rel_lt": case "rel_ls":  requireNum(left,"<",line);  requireNum(right,"<",line);  return new Value(Value.BOOL, left.asNum() <  right.asNum());
            case "rel_lse":                requireNum(left,"<=",line); requireNum(right,"<=",line); return new Value(Value.BOOL, left.asNum() <= right.asNum());
            case "rel_gt":                 requireNum(left,">",line);  requireNum(right,">",line);  return new Value(Value.BOOL, left.asNum() >  right.asNum());
            case "rel_gte":                requireNum(left,">=",line); requireNum(right,">=",line); return new Value(Value.BOOL, left.asNum() >= right.asNum());
            // Equality — polymorphic, but both sides must match
            case "rel_eq":  {
                checkSameType(left, right, "==", line);
                return new Value(Value.BOOL, valuesEqual(left, right));
            }
            case "rel_neq": {
                checkSameType(left, right, "!=", line);
                return new Value(Value.BOOL, !valuesEqual(left, right));
            }
            // Logical
            case "log_and": requireBool(left,"&&",line); requireBool(right,"&&",line); return new Value(Value.BOOL, left.asBool() && right.asBool());
            case "log_or":  requireBool(left,"||",line); requireBool(right,"||",line); return new Value(Value.BOOL, left.asBool() || right.asBool());

            default: return left;
        }
    }

    private Value evalLiteral(Tree node) {
        if (node == null) return new Value(Value.VOID, null);
        Tree child = firstChild(node);
        if (child == null) return new Value(Value.VOID, null);
        return evalExpression(child);
    }

    // -----------------------------------------------------------------------
    // FUNCTION CALL: id PRIMARY' -> id l_paren ARG_LIST r_paren
    // -----------------------------------------------------------------------
    private Value evalFunctionCall(Tree node) {
        String funcName = extractId(node);
        int lineNum = extractLine(node);

        SymbolTable.FunctionAttributes fa = symTable.getFunctionAttributes(funcName);
        if (fa == null) {
            semanticError("Function '" + funcName + "' is not declared", lineNum);
            return new Value(Value.VOID, null);
        }

        // Collect argument values
        List<Value> args = new ArrayList<>();
        Tree argList = findChildByLabel(node, "ARG_LIST");
        collectArgs(argList, args);

        // Type-check argument count and types
        if (args.size() != fa.paramTypes.size()) {
            semanticError("Function '" + funcName + "' expects " + fa.paramTypes.size() +
                " arguments but got " + args.size(), lineNum);
        }
        for (int i = 0; i < args.size(); i++) {
            if (!args.get(i).type.equals(fa.paramTypes.get(i))) {
                semanticError("Argument " + (i+1) + " of '" + funcName + "': expected " +
                    fa.paramTypes.get(i) + " but got " + args.get(i).type, lineNum);
            }
        }

        // Execute the function body in its own scope
        symTable.pushScope();
        for (int i = 0; i < fa.paramNames.size(); i++) {
            symTable.declareVariable(fa.paramNames.get(i), fa.paramTypes.get(i));
            symTable.assignValue(fa.paramNames.get(i), args.get(i).raw);
        }

        Value returnVal = new Value(Value.VOID, null);
        try {
            execBlock(fa.body);
        } catch (ReturnSignal r) {
            returnVal = r.value;
        } finally {
            symTable.popScope();
        }

        // Type-check return value
        if (!fa.returnType.equals("EMPTY") && !returnVal.type.equals(fa.returnType)) {
            semanticError("Function '" + funcName + "' must return " + fa.returnType +
                " but returned " + returnVal.type, lineNum);
        }
        return returnVal;
    }

    private void collectArgs(Tree node, List<Value> args) {
        if (node == null || node.children.isEmpty()) return;
        // ARG_LIST -> EXPRESSION MORE_ARGS
        Tree exprNode = findDeepExpression(node);
        if (exprNode != null) args.add(evalExpression(exprNode));
        Tree moreArgs = findChildByLabel(node, "MORE_ARGS");
        collectArgs(moreArgs, args);
    }

    // -----------------------------------------------------------------------
    // TYPE-CHECKING HELPERS
    // -----------------------------------------------------------------------
    private void checkTypeMatch(String declaredType, Value val, int line) {
        String vt = val.type;
        if (declaredType.equals(vt)) return;
        // QUALITY can accept a boolean result from a comparison
        if (declaredType.equals("QUALITY") && vt.equals(Value.BOOL)) return;
        semanticError("Type mismatch: variable is " + declaredType + " but got " + vt, line);
    }

    private void checkSameType(Value a, Value b, String op, int line) {
        if (!a.type.equals(b.type)) {
            semanticError("Operator " + op + " requires both operands to be the same type, got " +
                a.type + " and " + b.type, line);
        }
    }

    private void requireNum(Value v, String op, int line) {
        if (!v.type.equals(Value.NUM))
            semanticError("Operator " + op + " requires PRICE operands, got " + v.type, line);
    }

    private void requireBool(Value v, String op, int line) {
        if (!v.type.equals(Value.BOOL))
            semanticError("Operator " + op + " requires QUALITY operands, got " + v.type, line);
    }

    private boolean valuesEqual(Value a, Value b) {
        if (a.raw == null && b.raw == null) return true;
        if (a.raw == null || b.raw == null) return false;
        if (a.type.equals(Value.NUM)) return a.asNum() == b.asNum();
        return a.raw.equals(b.raw);
    }

    private double getNumVar(String name, int line) {
        SymbolTable.VariableAttributes attr = symTable.getAttributes(name);
        if (attr == null) semanticError("Variable '" + name + "' not declared", line);
        if (attr.value == null) semanticError("Variable '" + name + "' has no value", line);
        return ((Double) attr.value);
    }

    private double toNum(Value v, int line) {
        if (!v.type.equals(Value.NUM)) semanticError("Expected PRICE value", line);
        return v.asNum();
    }

    private void semanticError(String msg, int line) {
        throw new SemanticException(msg, line);
    }

    // -----------------------------------------------------------------------
    // AST NAVIGATION HELPERS
    // -----------------------------------------------------------------------
    private String label(Tree node) {
        return node.data;
    }

    private List<Tree> getChildren(Tree node) {
        List<Tree> list = new ArrayList<>();
        if (node == null) return list;
        for (Object o : node.children) list.add((Tree) o);
        return list;
    }

    private Tree firstChild(Tree node) {
        if (node == null || node.children.isEmpty()) return null;
        return (Tree) node.children.get(0);
    }

    private Tree findChildByLabel(Tree node, String targetLabel) {
        if (node == null) return null;
        for (Object o : node.children) {
            Tree c = (Tree) o;
            if (c.data.equals(targetLabel)) return c;
        }
        return null;
    }

    private boolean hasChild(Tree node, String lbl) {
        return findChildByLabel(node, lbl) != null;
    }

    private boolean hasChildLabel(Tree node, String prefix) {
        if (node == null) return false;
        for (Object o : node.children) {
            Tree c = (Tree) o;
            if (c.data.startsWith(prefix)) return true;
        }
        return false;
    }

    // Finds the first EXPRESSION node by walking depth-first
    private Tree findDeepExpression(Tree node) {
        if (node == null) return null;
        for (Object o : node.children) {
            Tree c = (Tree) o;
            if (c.data.equals("EXPRESSION") || c.data.startsWith("UNARY") ||
                c.data.startsWith("PRIMARY") || c.data.startsWith("LITERAL") ||
                c.data.startsWith("numlit") || c.data.startsWith("stringlit") ||
                c.data.startsWith("true") || c.data.startsWith("false") ||
                c.data.startsWith("id (")) {
                return c;
            }
        }
        // Go deeper
        for (Object o : node.children) {
            Tree found = findDeepExpression((Tree) o);
            if (found != null) return found;
        }
        return null;
    }

    // Finds an EXPRESSION that's a direct child (not nested inside FOR_INIT/FOR_UPDATE)
    private Tree findDirectExpression(Tree node) {
        if (node == null) return null;
        for (Object o : node.children) {
            Tree c = (Tree) o;
            if (c.data.equals("EXPRESSION")) return c;
        }
        return null;
    }

    private Tree findDeepExpressionFrom(List<Tree> children, int startIdx) {
        for (int i = startIdx; i < children.size(); i++) {
            Tree c = children.get(i);
            if (c.data.equals("EXPRESSION")) return c;
            Tree found = findDeepExpression(c);
            if (found != null) return found;
        }
        return null;
    }

    // Extracts the McLang type string from a TYPE terminal in a subtree
    private String extractType(Tree node) {
        for (Object o : node.children) {
            Tree c = (Tree) o;
            String lbl = c.data;
            if (lbl.startsWith("num_type"))    return "PRICE";
            if (lbl.startsWith("string_type")) return "RECIPE";
            if (lbl.startsWith("bool_type"))   return "QUALITY";
            if (lbl.startsWith("void"))        return "EMPTY";
            if (lbl.equals("TYPE")) return extractType(c);
        }
        return "PRICE";
    }

    private String tokenTypeToMcLang(String lbl) {
        if (lbl.startsWith("num_type"))    return "PRICE";
        if (lbl.startsWith("string_type")) return "RECIPE";
        if (lbl.startsWith("bool_type"))   return "QUALITY";
        if (lbl.startsWith("void"))        return "EMPTY";
        return "PRICE";
    }

    // Extracts the first id lexeme from a subtree
    private String extractId(Tree node) {
        if (node == null) return null;
        for (Object o : node.children) {
            Tree c = (Tree) o;
            if (c.data.startsWith("id (")) return extractLexeme(c);
            String found = extractId(c);
            if (found != null) return found;
        }
        return null;
    }

    // Pulls the lexeme out of a terminal node label like  id (score)  -> score
    private String extractLexeme(Tree node) {
        String d = node.data;
        int start = d.indexOf("(");
        int end   = d.lastIndexOf(")");
        if (start >= 0 && end > start) return d.substring(start + 1, end).trim();
        return d;
    }

    // Gets the line number stored on the first terminal in a subtree (best effort)
    private int extractLine(Tree node) {
        // We don't store line numbers in Tree nodes directly, so return -1.
        // The ErrorReporter already has line info from the scanner phase.
        return -1;
    }

    private String stripQuotes(String s) {
        if (s.startsWith("\"") && s.endsWith("\"")) return s.substring(1, s.length() - 1);
        // Handle escape sequences
        return s.replace("\\n", "\n").replace("\\t", "\t").replace("\\\"", "\"").replace("\\\\", "\\");
    }
}