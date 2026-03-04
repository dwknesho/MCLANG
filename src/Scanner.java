import java.io.IOException;

public class Scanner {
    private ReadChar stream;

    // Initialize the scanner with the file to read
    public Scanner(String filePath) {
        stream = new ReadChar(filePath);
    }

    // This is the method the Tester calls over and over until EOF
    public Token getNextToken() throws IOException {
        stream.skipSpaceandComments();

        // If we hit the end of the file, return the special EOF token
        if (stream.currentChar == -1) {
            return new Token("[EOF]", "EOF", null, stream.line, stream.col);
        }

        int startCol = stream.col;
        char c = (char) stream.currentChar;

        // If it starts with a letter, it must be an identifier or keyword
        if (Character.isLetter(c)) {
            return Identifiers.scan(stream, startCol);
        } 
        // if it starts with digits then its a number literal 
        else if (Character.isDigit(c) || c == '.') { // triggers scanNumber if starts with .
            return Literals.scanNumber(stream, startCol);
        } 
        // " meaning literals
        else if (c == '"') {
            return Literals.scanString(stream, startCol);
        } 
        
    
        else {
            return Operators.scan(stream, startCol);
        }
    }
}

