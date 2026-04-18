package errors;

import java.util.ArrayList;
import java.util.List;

public class ErrorReporter {
    private List<String> lexicalErrors;
    private List<String> syntaxErrors;
    private int errorCounter;

    public ErrorReporter() {
        this.lexicalErrors = new ArrayList<>();
        this.syntaxErrors  = new ArrayList<>();
        this.errorCounter  = 0;
    }

    // Lexical Errors
    public void reportLexicalError(int line, int col, String message, String lexeme) {
        errorCounter++;
        lexicalErrors.add(
            "  [" + errorCounter + "] Line " + line + ", Col " + col +
            " | " + message + " -> '" + lexeme + "'"
        );
    }

    // Post-ORDER_END Error (treated as lexical)
    public void reportPostOrderEndError(int line, int col, String lexeme) {
        errorCounter++;
        lexicalErrors.add(
            "  [" + errorCounter + "] Line " + line + ", Col " + col +
            " | Nothing is allowed after ORDER_END -> '" + lexeme + "'"
        );
    }

    // Syntax Errors
    public void reportSyntaxError(int line, int col, String message) {
        errorCounter++;
        syntaxErrors.add(
            "  [" + errorCounter + "] Line " + line + ", Col " + col +
            " | " + message
        );
    }

    // Queries
    public boolean hasErrors() {
        return !lexicalErrors.isEmpty() || !syntaxErrors.isEmpty();
    }

    public int getErrorCount() {
        return errorCounter;
    }

    // Helper: works on Java 8+, avoids String.repeat() which needs Java 11
    private String repeatChar(char ch, int times) {
        StringBuilder sb = new StringBuilder(times);
        for (int i = 0; i < times; i++) sb.append(ch);
        return sb.toString();
    }

    // Summary
    public void printSummary() {
        if (!hasErrors()) {
            System.out.println("\n  No errors found. Compilation successful.");
            return;
        }

        String divider     = repeatChar('-', 60);
        String thickDivide = repeatChar('=', 60);

        System.out.println("\n" + thickDivide);
        System.out.println(" COMPILATION ERRORS  (" + errorCounter + " total)");
        System.out.println(thickDivide);

        // Lexical section
        if (!lexicalErrors.isEmpty()) {
            System.out.println("\n  LEXICAL ERRORS  (" + lexicalErrors.size() + ")");
            System.out.println("  " + divider);
            for (String err : lexicalErrors) {
                System.out.println(err);
            }
        }

        // Syntax section
        if (!syntaxErrors.isEmpty()) {
            System.out.println("\n  SYNTAX ERRORS  (" + syntaxErrors.size() + ")");
            System.out.println("  " + divider);
            for (String err : syntaxErrors) {
                System.out.println(err);
            }
        }

        System.out.println("\n" + thickDivide + "\n");
    }
}