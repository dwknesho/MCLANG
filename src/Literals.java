import java.io.IOException;

public class Literals {
    
    // scan PRICE constants 
    public static Token scanNumber(ReadChar stream, int startCol) throws IOException {
        StringBuilder sb = new StringBuilder();
        boolean hasDecimal = false; // checks if we see a decimal point yet
        
        // test rules (decimal cant start with a dot)
        if (stream.currentChar == '.') {
        stream.advance(); // consume the '.'
        return new Token("<error>", ".", "Invalid decimal", stream.line, startCol);
    }
        // keeps reading digits and decimal points
        while (Character.isDigit(stream.currentChar) || stream.currentChar == '.') {
            if (stream.currentChar == '.') {
                if (hasDecimal) break; // If we already have a decimal, stop. (10.5.2 is invalid)
                hasDecimal = true;
            }
            sb.append((char) stream.currentChar);
            stream.advance();
        }
        
        String numStr = sb.toString();
        
        // test rules (decimal should have numbers infront and behind)
        if (numStr.endsWith(".")) {
            return new Token("<error>", numStr, "Invalid decimal", stream.line, startCol);
        }
        
        // convert the string to a double value and return it as a <numlit> token
        return new Token("<numlit>", numStr, Double.parseDouble(numStr), stream.line, startCol);
    }

    // Scans RECIPE constants in double quotes
    public static Token scanString(ReadChar stream, int startCol) throws IOException {
        StringBuilder sb = new StringBuilder();
        int startLine = stream.line;
        stream.advance(); // Skip opening "

        while (stream.currentChar != '"' && stream.currentChar != '\n' && stream.currentChar != -1) {
            if (stream.currentChar == '\\') {
                stream.advance(); // peek at character after \
                
                if (stream.currentChar == 'n') sb.append('\n');
                else if (stream.currentChar == 't') sb.append('\t');
                else if (stream.currentChar == '"') sb.append('"');
                else if (stream.currentChar == '\\') sb.append('\\');
                else {
                    // --- RECOVERY LOGIC ---
                    String badEscape = "\\" + (char)stream.currentChar;
                    // Keep skipping until we find the end of this "bad string"
                    while (stream.currentChar != '"' && stream.currentChar != '\n' && stream.currentChar != -1) {
                        stream.advance();
                    }
                    if (stream.currentChar == '"') stream.advance(); // consume the closing quote
                    
                    return new Token("<error>", badEscape, "Invalid escape sequence", startLine, startCol);
                }
            } else {
                sb.append((char) stream.currentChar);
            }
            stream.advance();
        }

        if (stream.currentChar == '"') {
            stream.advance(); 
            String content = sb.toString();
            return new Token("<stringlit>", "\"" + content + "\"", content, startLine, startCol);
        }

        return new Token("<error>", sb.toString(), "Unclosed string", startLine, startCol);
    }
}