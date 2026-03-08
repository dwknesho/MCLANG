package CONTROLLERS;

import MODEL.Token;
import MODEL.LexicalException;
import MODEL.Keywords;
import java.io.IOException;

public class IdentifierScanner {

    public static Token scan(ReadChar stream, int startCol) throws IOException {
        int startLine = stream.line;
        StringBuilder word = new StringBuilder();
        
        // Build the full word
        while (Character.isLetterOrDigit(stream.currentChar) || stream.currentChar == '_') {
            word.append((char) stream.currentChar);
            stream.advance();
        }
        
        String lexeme = word.toString();
        char firstChar = lexeme.charAt(0);

        // CASE 1: Starts with an Underscore then already an invalid identifier
        if (firstChar == '_') {
            throw new LexicalException("Identifier cannot start with an underscore", lexeme, startLine, startCol);
        }
        // CASE 2: Starts with Uppercase then must be a Keyword
        else if (Character.isUpperCase(firstChar)) {
            // Must be strictly uppercase
            if (!lexeme.equals(lexeme.toUpperCase())) {
                throw new LexicalException("Invalid keyword or identifier", lexeme, startLine, startCol);
            }
            
            // Look up in the keywords map
            String tokenName = Keywords.getTokenName(lexeme); 
            
            // Invalid keyword throws error
            if (tokenName == null) {
                throw new LexicalException("Invalid keyword or identifier", lexeme, startLine, startCol);
            }
            return new Token(tokenName, lexeme, null, startLine, startCol);
        } 
        // CASE 3: Starts with Lowercase then it is a valid Identifier
        else {
            return new Token("<id>", lexeme, null, startLine, startCol);
        }
    }

    // Assumes the programmer used a number as a starting identifier name
    public static Token scanInvalidDigitStart(ReadChar stream, String numberPrefix, int startCol) throws IOException {
        int startLine = stream.line;
        StringBuilder wordError = new StringBuilder(numberPrefix);

        while (Character.isLetterOrDigit(stream.currentChar) || stream.currentChar == '_') {
            wordError.append((char) stream.currentChar);
            stream.advance();
        }

        String errorLexeme = wordError.toString();
        throw new LexicalException("Identifiers cannot start with a number", errorLexeme, startLine, startCol);
    }
}