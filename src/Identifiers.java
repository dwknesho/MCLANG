import java.io.IOException;

public class Identifiers {
    public static Token scan(ReadChar stream, int startCol) throws IOException {
        StringBuilder sb = new StringBuilder();
        
        // keep reading as long as the character is a letter, digit, or underscore
        while (Character.isLetterOrDigit(stream.currentChar) || stream.currentChar == '_') {
            sb.append((char) stream.currentChar);
            stream.advance();
        }
        
        String word = sb.toString();
        
        if (Character.isUpperCase(word.charAt(0))) {
            // if its uupercase, assume that its a reserved word
            String tokenName = Keywords.getTokenName(word);
            if (tokenName != null) {
                return new Token(tokenName, word, null, stream.line, startCol);
            }
            // check error 
            return new Token("<error>", word, "Identifier cannot start with uppercase", stream.line, startCol);
        }
        
        // for valid identifier
        return new Token("<id>", word, null, stream.line, startCol);
    }
}