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
                if (stream.currentChar == '=') {
                    stream.advance();
                    return new Token("<assign_mul>", "*=", null, line, col);
                }
                return new Token("<arith_mul>", "*", null, line, col);
            }

            // /   ->  <arith_div>
            // /=  ->  <assign_div>
            case '/': {
                if (stream.currentChar == '=') {
                    stream.advance();
                    return new Token("<assign_div>", "/=", null, line, col);
                }
                return new Token("<arith_div>", "/", null, line, col);
            }

            // %   ->  <arith_mod>
            // %=  ->  <assign_mod>
            case '%': {
                if (stream.currentChar == '=') {
                    stream.advance();
                    return new Token("<assign_mod>", "%=", null, line, col);
                }
                return new Token("<arith_mod>", "%", null, line, col);
            }

            // =   ->  <assign_as>
            // ==  ->  <rel_eq>
            case '=': {
                if (stream.currentChar == '=') {
                    stream.advance();
                    return new Token("<rel_eq>", "==", null, line, col);
                }
                return new Token("<assign_as>", "=", null, line, col);
            }

            // !   ->  <log_not>
            // !=  ->  <rel_neq>
            case '!': {
                if (stream.currentChar == '=') {
                    stream.advance();
                    return new Token("<rel_neq>", "!=", null, line, col);
                }
                return new Token("<log_not>", "!", null, line, col);
            }

            // <   ->  <rel_ls>
            // <=  ->  <rel_lse>
            case '<': {
                if (stream.currentChar == '=') {
                    stream.advance();
                    return new Token("<rel_lse>", "<=", null, line, col);
                }
                return new Token("<rel_ls>", "<", null, line, col);
            }

             // >   ->  <rel_gt>
            // >=  ->  <rel_gte>
            case '>': {
                if (stream.currentChar == '=') {
                    stream.advance();
                    return new Token("<rel_gte>", ">=", null, line, col);
                }
                return new Token("<rel_gt>", ">", null, line, col);
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