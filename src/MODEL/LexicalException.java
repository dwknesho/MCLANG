package MODEL;

public class LexicalException extends RuntimeException{
    public int line;
    public int col;
    public String lexeme;

    public LexicalException(String message, String lexeme, int line, int col) {
        super(message);
        this.lexeme = lexeme;
        this.line = line;
        this.col = col;
    }
}
