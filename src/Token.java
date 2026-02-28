public class Token {
    public String type; //basically what kind of token is it
    public String lexeme; 
    public int lineNumber;

public Token (String type, String lexeme, int lineNumber){
    this.type=type;
    this.lexeme=lexeme;
    this.lineNumber=lineNumber;
}
 @Override
 public String toString(){
    return "Token(" + type +", " +  lexeme + ", line =" + lineNumber + ")"; 
 }
}