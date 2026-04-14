package lexer;

import errors.LexicalException;
import java.io.IOException;

public class Scanner {
    private ReadChar stream;

    public Scanner(String filePath) {
        stream = new ReadChar(filePath);
    }

    public Token getNextToken() throws IOException {
        int startLine = stream.line;
        int startCol = stream.col;
    
        stream.skipSpaceAndComments();

        // 1. Check EOF and Unclosed Comments
        if (stream.currentChar == -1) {
            if (stream.unclosedComment) {
                stream.unclosedComment = false;
                throw new LexicalException("Unclosed multi-line comment", "EOF", startLine, startCol);
            }
            return new Token("[EOF]", "EOF", null, stream.line, stream.col);
        }

        char c = (char) stream.currentChar;

        // 2. Letters AND underscores go to IdentifierScanner to build the full word
        if (Character.isLetter(c) || c == '_') {
            return IdentifierScanner.scan(stream, startCol);
        } 
        // 3. Digits go to LiteralScanner to scan for Numbers
        else if (Character.isDigit(c)) { 
            Token numToken = LiteralScanner.scanNumber(stream, startCol);
            // Check invalid identifiers that start with a number (like 123abc)
            if (Character.isLetter(stream.currentChar) || stream.currentChar == '_') {
                return IdentifierScanner.scanInvalidDigitStart(stream, numToken.lexeme, startCol);
            }
            return numToken;
        }
        // 4. Reject leading decimal points (.5 is invalid)
        else if (c == '.') {
            stream.advance(); 
            throw new LexicalException("Numbers cannot start with a decimal point (use 0.x instead)", ".", startLine, startCol);
        }
        // 5. Double quotes go to String Scanner to scan the string
        else if (c == '"') {
            return LiteralScanner.scanString(stream, startCol);
        } 
        // 6. Symbols go to OperatorScanner
        else {
            Token opToken = OperatorScanner.scan(stream, startCol);
            
            if (opToken != null) {  // Valid Operator
                return opToken;
            }
            
            // 7. Completely Unrecognized characters
            String errorChar = String.valueOf(c);
            throw new LexicalException("Unrecognized character", errorChar, startLine, startCol);
        }
    }
}