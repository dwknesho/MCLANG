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
        char firstChar = word.charAt(0);

        if (firstChar == '_') {
            return new Token("<error>", word, "Invalid identifier '" + word + "': cannot start with underscore", stream.line, startCol);
        }
        
        if (Character.isUpperCase(firstChar)) {
            String tokenName = Keywords.getTokenName(word); //check if its valid keyword
            if (tokenName != null) {
                return new Token(tokenName, word, null, stream.line, startCol);
            }

            //this if else runs when its not a valid keyword
            //all uppercase = assume its keyword 
            
            if (word.equals(word.toUpperCase())) {
                return new Token("<error>", word, "Invalid keyword '" + word + "': not a recognized keyword", stream.line, startCol);
            } 
            //if its not all uppercase then assume its identifier
            else {
                return new Token("<error>", word, "Invalid identifier '" + word + "': must start with a lowercase letter", stream.line, startCol);
            }
        }
        
        // for valid identifier
        return new Token("<id>", word, null, stream.line, startCol);
    }

    public static Token scanInvalidDigitStart(ReadChar stream, String prefix, int startCol) throws IOException {
        StringBuilder sb = new StringBuilder(prefix); // start with the numbers already scanned
        
        // Keep reading the rest of the letters, digits, or underscores
        while (Character.isLetterOrDigit(stream.currentChar) || stream.currentChar == '_') {
            sb.append((char) stream.currentChar);
            stream.advance();
        }
        
        String word = sb.toString();
        return new Token("<error>", word, "Invalid identifier '" + word + "': cannot start with a digit", stream.line, startCol);
    }
}