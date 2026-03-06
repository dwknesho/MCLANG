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

    // 2. Check if we are at the End of File
        if (stream.currentChar == -1) {

            if (startLine != stream.line) {
                return new Token("<error>", "EOF", "Unclosed multi-line comment", startLine, startCol);
            }

            return new Token("[EOF]", "EOF", null, stream.line, stream.col);
        }

        int currentCol = stream.col;
        char c = (char) stream.currentChar;

        // If it starts with a letter, it must be an identifier or keyword
        if (Character.isLetter(c)) {
            return Identifiers.scan(stream, startCol);
        } 
        // if it starts with digits then its a number literal 
        else if (Character.isDigit(c) || c == '.') { // triggers scanNumber if starts with .
            return Literals.scanNumber(stream, startCol);
        } 
        // If it starts " (Double Quotation) then it is a string literal.
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
            // Fallback for unrecognized characters
            stream.advance(); // consume the character
            return new Token("<error>", String.valueOf(c), "Unrecognized character", stream.line, startCol);
        }
    }
}

