package MODEL;

import java.util.ArrayList;
import java.util.List;

public class ErrorReporter {
    private List<String> errorList;
    private int errorCounter;

    public ErrorReporter() {
        this.errorList = new ArrayList<>();
        this.errorCounter = 0;
    }

    // Reports a Lexical Error
    public void reportLexicalError(int line, int col, String message, String lexeme) {
        errorCounter++;
        String detail = "Error " + errorCounter + " [Line " + line + ", Col " + col + "]: " + 
                        message + " ('" + lexeme + "')";
        errorList.add(detail);
    }

    // Reports an error after ORDER_END
    public void reportPostOrderEndError(int line, int col, String lexeme) {
        errorCounter++;
        String detail = "Error " + errorCounter + " [Line " + line + ", Col " + col + "]: " +
                        "Nothing is allowed after ORDER_END ('" + lexeme + "')";
        errorList.add(detail);
    }

    public boolean hasErrors() {
        return !errorList.isEmpty();
    }

    public int getErrorCount() {
        return errorCounter;
    }

    public void printSummary() {
        if (hasErrors()) {
            System.out.println("\nLEXICAL ERRORS");
            for (String error : errorList) {
                System.out.println(error);
            }
        }
    }
}