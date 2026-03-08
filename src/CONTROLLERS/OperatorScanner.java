package CONTROLLERS;

import MODEL.Token;
import MODEL.LexicalException;
import java.io.IOException;

public class OperatorScanner {

    public static Token scan(ReadChar stream, int startCol) throws IOException {
        int startLine = stream.line;
        char c = (char) stream.currentChar;
        
        stream.advance(); // Consume the first character

        switch (c) {
            // Arithmetic & Assignment
            case '+':
                if (stream.currentChar == '+') { stream.advance(); return new Token("<incre>", "++", null, startLine, startCol); }          // ++ <incre>
                if (stream.currentChar == '=') { stream.advance(); return new Token("<assign_add>", "+=", null, startLine, startCol); }     // = <assign_add>
                return new Token("<arith_add>", "+", null, startLine, startCol);                                                            // + <arith_add>
            case '-':
                if (stream.currentChar == '-') { stream.advance(); return new Token("<decre>", "--", null, startLine, startCol); }          // -- <decre>
                if (stream.currentChar == '=') { stream.advance(); return new Token("<assign_min>", "-=", null, startLine, startCol); }     // -= <assing_min>
                return new Token("<arith_sub>", "-", null, startLine, startCol);                                                            // - <arith_sub>
            case '*':
                if (stream.currentChar == '=') { stream.advance(); return new Token("<assign_mul>", "*=", null, startLine, startCol); }     // *= <assign_mul>
                return new Token("<arith_mul>", "*", null, startLine, startCol);                                                            // *  <arith_mul>
            case '/':
                if (stream.currentChar == '=') { stream.advance(); return new Token("<assign_div>", "/=", null, startLine, startCol); }     // /= <assign_div>
                return new Token("<arith_div>", "/", null, startLine, startCol);                                                            // / <arith_div>
            case '%':
                if (stream.currentChar == '=') { stream.advance(); return new Token("<assign_mod>", "%=", null, startLine, startCol); }     // %= <assign_mod>
                return new Token("<arith_mod>", "%", null, startLine, startCol);                                                            // % <arith_mod>

            // Relational & Assignment
            case '=':
                if (stream.currentChar == '=') { stream.advance(); return new Token("<rel_eq>", "==", null, startLine, startCol); }
                return new Token("<assign_as>", "=", null, startLine, startCol);

            // Note: !! and !* are comments, already skipped by ReadChar.skipSpaceAndComments()     
            case '!':
                if (stream.currentChar == '=') { stream.advance(); return new Token("<rel_neq>", "!=", null, startLine, startCol); }
                return new Token("<log_not>", "!", null, startLine, startCol);
            case '<':
                if (stream.currentChar == '=') { stream.advance(); return new Token("<rel_lse>", "<=", null, startLine, startCol); }
                return new Token("<rel_ls>", "<", null, startLine, startCol);
            case '>':
                if (stream.currentChar == '=') { stream.advance(); return new Token("<rel_gte>", ">=", null, startLine, startCol); }
                return new Token("<rel_gt>", ">", null, startLine, startCol);

            // Logical Operators
            case '&':
                if (stream.currentChar == '&') { 
                    stream.advance(); 
                    return new Token("<log_and>", "&&", null, startLine, startCol); 
                }
                throw new LexicalException("Single '&' is not valid, must be '&&'", "&", startLine, startCol);
            case '|':
                if (stream.currentChar == '|') { 
                    stream.advance(); 
                    return new Token("<log_or>", "||", null, startLine, startCol); 
                }
                throw new LexicalException("Single '|' is not valid, must be '||'", "|", startLine, startCol);

            // Grouping and Punctuation
            case '(': return new Token("<l_paren>", "(", null, startLine, startCol);
            case ')': return new Token("<r_paren>", ")", null, startLine, startCol);
            case '{': return new Token("<l_brace>", "{", null, startLine, startCol);
            case '}': return new Token("<r_brace>", "}", null, startLine, startCol);
            case '[': return new Token("<l_brack>", "[", null, startLine, startCol);
            case ']': return new Token("<r_brack>", "]", null, startLine, startCol);
            case ';': return new Token("<semi>", ";", null, startLine, startCol);
            case ':': return new Token("<colon>", ":", null, startLine, startCol);
            case ',': return new Token("<comma>", ",", null, startLine, startCol);

            default:
                // Return null so Scanner.java can throw the "Unrecognized character" exception
                return null;
        }
    }
}