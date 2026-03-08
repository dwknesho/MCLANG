import java.io.IOException;

public class Scanner {
    private ReadChar stream;

    // Initialize the scanner with the file to read
    public Scanner(String filePath) {
        stream = new ReadChar(filePath);
    }

    // This is the method the Tester calls over and over until EOF
    public Token getNextToken() throws IOException {
        
        int startLine = stream.line;
        int startCol = stream.col;
    
        stream.skipSpaceandComments();

        // Check if we are at eof
        if (stream.currentChar == -1) {

            if (stream.unclosedComment) {
                stream.unclosedComment = false;
                return new Token("<error>", "EOF", "Unclosed multi-line comment", startLine, startCol);
            }

            return new Token("[EOF]", "EOF", null, stream.line, stream.col);
        }

        char c = (char) stream.currentChar;

        // If it starts with a letter, it must be an identifier or keyword
        if (Character.isLetter(c) || c == '_') {
            return Identifiers.scan(stream, startCol);
        } 
        // if it starts with digits then its a number literal 
        else if (Character.isDigit(c) || c == '.') {
            Token numToken = Literals.scanNumber(stream, startCol);
            if (Character.isLetter(stream.currentChar) || stream.currentChar == '_') {
                return Identifiers.scanInvalidDigitStart(stream, numToken.lexeme, startCol);
            }
            return numToken;
        }
        // If it starts " then it is a string literal.
        else if (c == '"') {
            return Literals.scanString(stream, startCol);
        } 
        // Else then it assumes it is a symbol.
        else {
            // Check if Operators.scan returns null before returning it
            Token opToken = Operators.scan(stream, startCol);
            if (opToken != null) {
                return opToken;
            }
            // for unrecognized characters
            stream.advance(); // consume the character
            return new Token("<error>", String.valueOf(c), "Unrecognized character", stream.line, startCol);
        }
    }
}

