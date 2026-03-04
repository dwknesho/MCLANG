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
        stream.advance(); // Skip the opening quote (")
        
        // keeps reading until we hit another quote, a newline, or the end of the file
        while (stream.currentChar != '"' && stream.currentChar != '\n' && stream.currentChar != -1) {
            
            // handles escape characters (like \n for newline)
            if (stream.currentChar == '\\') {
                stream.advance(); 
                if (stream.currentChar == 'n') sb.append('\n');
                else if (stream.currentChar == 't') sb.append('\t');
                else if (stream.currentChar == '"') sb.append('"');
                else if (stream.currentChar == '\\') sb.append('\\');
                else sb.append((char) stream.currentChar);
            } else {
                sb.append((char) stream.currentChar); // add normal characters
            }
            stream.advance();
        }
        
        // check if the string was properly closed with a quote
        if (stream.currentChar == '"') {
            stream.advance(); // Skip the closing quote
            return new Token("<stringlit>", "\"" + sb.toString() + "\"", sb.toString(), stream.line, startCol);
        }
        
        // If it stopped because of a newline or EOF, throw an error
        return new Token("<error>", sb.toString(), "Unclosed string", stream.line, startCol);
    }
}