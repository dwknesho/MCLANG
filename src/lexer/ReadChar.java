package lexer;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class ReadChar {
    private BufferedReader reader;
    public int line = 1;
    public int col = 0;
    public int currentChar = -1;
    public boolean unclosedComment = false;

    public ReadChar(String filePath) {
        try {
            reader = new BufferedReader(new FileReader(filePath));
            advance(); // read the first character
        } catch (IOException e) {
            System.err.println("File error: " + e.getMessage());
        }
    }

    public void advance() throws IOException {
        if (currentChar == '\n') {
            line++;
            col = 0;
        } else {
            col++;
        }
        currentChar = reader.read();
    }

    public void skipSpaceAndComments() throws IOException {
        while (currentChar != -1) {
            if (Character.isWhitespace(currentChar)) { //skip spaces, tab, newline using java method
                advance();
            } 
            //check if ! is comment or logical not
            else if (currentChar == '!') {
                reader.mark(1);
                int nextChar = reader.read(); // peak next character

                if (nextChar == '!') { // single line comment
                    col++;
                    while (currentChar != '\n' && currentChar != -1) {
                        advance(); 
                    }
                } 
                else if (nextChar == '*') { // multi line comment
                    col++;
                    advance();
                    boolean closed = false;

                    while (currentChar != -1) {
                        if (currentChar == '*') {
                            advance();
                            if (currentChar == '!') {
                                advance();
                                closed = true;
                                break;
                            }
                        } else {
                            advance();
                        }
                    }
                    if (!closed) {
                        unclosedComment = true;
                        return;
                    }
                } 
                else {                  // ! is a logical NOT operator
                    reader.reset();     // Go back to the mark spot which is !
                    break;
                }
            } 
            else {
                break;
            }
        }
    }
}