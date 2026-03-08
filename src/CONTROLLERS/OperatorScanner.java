package CONTROLLERS;

import MODEL.Token;
import MODEL.LexicalException;
import java.io.IOException;

public class OperatorScanner {

    public static Token scan(ReadChar stream, int startCol) throws IOException {
        int line = stream.line;
        int col  = startCol;
        char c = (char) stream.currentChar;
        
        stream.advance(); // Consume the first character

        switch (c) {
            // Arithmetic & Assignment

            // +   ->  <arith_add>
            // ++  ->  <incer>
            // +=  ->  <assign_add>
            case '+': {
                stream.advance();
                if (stream.currentChar == '+') {
                    stream.advance();
                    return new Token("<incre>", "++", null, line, col);
                }
                if (stream.currentChar == '=') {
                    stream.advance();
                    return new Token("<assign_add>", "+=", null, line, col);
                }
                return new Token("<arith_add>", "+", null, line, col);
            }

            // -   ->  <arith_sub>
            // --  ->  <decre>
            // -=  ->  <assign_min>
            case '-': {
                stream.advance();
                if (stream.currentChar == '-') {
                    stream.advance();
                    return new Token("<decre>", "--", null, line, col);
                }
                if (stream.currentChar == '=') {
                    stream.advance();
                    return new Token("<assign_min>", "-=", null, line, col);
                }
                return new Token("<arith_sub>", "-", null, line, col);
            }

            // *   ->  <arith_mul>
            // *=  ->  <assign_mul>
            case '*': {
                stream.advance();
                if (stream.currentChar == '=') {
                    stream.advance();
                    return new Token("<assign_mul>", "*=", null, line, col);
                }
                return new Token("<arith_mul>", "*", null, line, col);
            }

            // /   ->  <arith_div>
            // /=  ->  <assign_div>
            case '/': {
                stream.advance();
                if (stream.currentChar == '=') {
                    stream.advance();
                    return new Token("<assign_div>", "/=", null, line, col);
                }
                return new Token("<arith_div>", "/", null, line, col);
            }

            // Logical Operators
            case '&':
                if (stream.currentChar == '&') { 
                    stream.advance(); 
                    return new Token("<log_and>", "&&", null, line, col); 
                }
                throw new LexicalException("Single '&' is not valid, must be '&&'", "&", line, col);
            case '|':
                if (stream.currentChar == '|') { 
                    stream.advance(); 
                    return new Token("<log_or>", "||", null, line, col); 
                }
                throw new LexicalException("Single '|' is not valid, must be '||'", "|", line, col);

            // Grouping and Punctuation
            case '(': return new Token("<l_paren>", "(", null, line, col);
            case ')': return new Token("<r_paren>", ")", null, line, col);
            case '{': return new Token("<l_brace>", "{", null, line, col);
            case '}': return new Token("<r_brace>", "}", null, line, col);
            case '[': return new Token("<l_brack>", "[", null, line, col);
            case ']': return new Token("<r_brack>", "]", null, line, col);
            case ';': return new Token("<semi>", ";", null, line, col);
            case ':': return new Token("<colon>", ":", null, line, col);
            case ',': return new Token("<comma>", ",", null, line, col);

            default:
                // Invalid operator
                return null;
        }
    }
}