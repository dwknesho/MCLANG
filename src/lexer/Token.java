package lexer;

public class Token {
    public String tokenName;                //name of token [ORDER_START]
    public String lexeme;                   //text of the .txt file
    public Object value;                    //value for constants
    public int line;                        //line error
    public int col;                         //column error

    public Token(String tokenName, String lexeme, Object value, int line, int col) {
        this.tokenName = tokenName;
        this.lexeme = lexeme;
        this.value = value;
        this.line = line;
        this.col = col;
    }
}