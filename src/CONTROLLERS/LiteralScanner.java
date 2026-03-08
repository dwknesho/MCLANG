package CONTROLLERS;

import MODEL.Token;
import MODEL.LexicalException;
import java.io.IOException;

public class LiteralScanner {

    // For numeric literals
    public static Token scanNumber(ReadChar stream, int startCol) throws IOException {
        int startLine = stream.line;
        StringBuilder number = new StringBuilder();

        while (Character.isDigit(stream.currentChar)) {
            number.append((char) stream.currentChar);
            stream.advance();
        }

        if (stream.currentChar == '.') {
            number.append('.');
            stream.advance();

            // Decimal must be followed by a digit ( "10." is invalid)
            if (!Character.isDigit(stream.currentChar)) {
                throw new LexicalException("Trailing decimal point is invalid", number.toString(), startLine, startCol);
            }

            while (Character.isDigit(stream.currentChar)) {
                number.append((char) stream.currentChar);
                stream.advance();
            }
        }

        String lexeme = number.toString();
        Double value = Double.parseDouble(lexeme);
        return new Token("<numlit>", lexeme, value, startLine, startCol);
    }

    // For string literals
    public static Token scanString(ReadChar stream, int startCol) throws IOException {
        int startLine = stream.line;
        StringBuilder string = new StringBuilder();
        StringBuilder lexemeBuffer = new StringBuilder();
        
        lexemeBuffer.append('"'); 
        stream.advance(); 

        while (stream.currentChar != '"' && stream.currentChar != -1) {
            if (stream.currentChar == '\\') { 
                lexemeBuffer.append('\\');
                stream.advance();
                
                char escapeChar = (char) stream.currentChar;
                lexemeBuffer.append(escapeChar);

                // Only 4 valid escape sequences allowed
                switch (escapeChar) {
                    case 'n': string.append('\n'); break;
                    case 't': string.append('\t'); break;
                    case '"': string.append('"'); break;
                    case '\\': string.append('\\'); break;
                    default:
                        stream.advance(); 
                        throw new LexicalException("Invalid escape sequence", "\\" + escapeChar, startLine, startCol);
                }
            } else {
                string.append((char) stream.currentChar);
                lexemeBuffer.append((char) stream.currentChar);
            }
            stream.advance();
        }

        if (stream.currentChar == -1) {
            throw new LexicalException("Unterminated string literal", lexemeBuffer.toString(), startLine, startCol);
        }

        lexemeBuffer.append('"'); 
        stream.advance(); 
        
        return new Token("<stringlit>", lexemeBuffer.toString(), string.toString(), startLine, startCol);
    }
}