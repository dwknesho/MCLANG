package errors;

public class SyntaxException extends RuntimeException {
    public int line;
    public int col;
    public String lexeme;

    public SyntaxException(String message, String lexeme, int line, int col) {
        super(message);
        this.lexeme = lexeme;
        this.line = line;
        this.col = col;
    }
}