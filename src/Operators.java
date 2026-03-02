import java.io.IOException;

public class Operators {

    public static Token scan(ReadChar stream, int startCol) throws IOException {
        int line = stream.line;
        int col  = startCol;
        char c   = (char) stream.currentChar;

        switch (c) {

       
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

            // %   ->  <arith_mod>
            // %=  ->  <assign_mod>
            case '%': {
                stream.advance();
                if (stream.currentChar == '=') {
                    stream.advance();
                    return new Token("<assign_mod>", "%=", null, line, col);
                }
                return new Token("<arith_mod>", "%", null, line, col);
            }

            // =   ->  <assign_as>
            // ==  ->  <rel_eq>
            case '=': {
                stream.advance();
                if (stream.currentChar == '=') {
                    stream.advance();
                    return new Token("<rel_eq>", "==", null, line, col);
                }
                return new Token("<assign_as>", "=", null, line, col);
            }

            // !   ->  <log_not>
            // !=  ->  <rel_neq>
            // Note: !! and !* are comments, already skipped by ReadChar.skipSpaceandComments()
            case '!': {
                stream.advance();
                if (stream.currentChar == '=') {
                    stream.advance();
                    return new Token("<rel_neq>", "!=", null, line, col);
                }
                return new Token("<log_not>", "!", null, line, col);
            }

            // <   ->  <rel_ls>
            // <=  ->  <rel_lse>
            case '<': {
                stream.advance();
                if (stream.currentChar == '=') {
                    stream.advance();
                    return new Token("<rel_lse>", "<=", null, line, col);
                }
                return new Token("<rel_ls>", "<", null, line, col);
            }

            // >   ->  <rel_gt>
            // >=  ->  <rel_gte>
            case '>': {
                stream.advance();
                if (stream.currentChar == '=') {
                    stream.advance();
                    return new Token("<rel_gte>", ">=", null, line, col);
                }
                return new Token("<rel_gt>", ">", null, line, col);
            }

            // &&  ->  <log_and>   (single & is a scanning error)
            case '&': {
                stream.advance();
                if (stream.currentChar == '&') {
                    stream.advance();
                    return new Token("<log_and>", "&&", null, line, col);
                }
                return new Token("<error>", "&", "Single '&' is not valid. Did you mean '&&'?", line, col);
            }

            // ||  ->  <log_or>    (single | is a scanning error)
            case '|': {
                stream.advance();
                if (stream.currentChar == '|') {
                    stream.advance();
                    return new Token("<log_or>", "||", null, line, col);
                }
                return new Token("<error>", "|", "Single '|' is not valid. Did you mean '||'?", line, col);
            }

            // Grouping & separator symbols
            case '(': stream.advance(); return new Token("<l_paren>", "(", null, line, col);
            case ')': stream.advance(); return new Token("<r_paren>", ")", null, line, col);
            case '{': stream.advance(); return new Token("<l_brace>", "{", null, line, col);
            case '}': stream.advance(); return new Token("<r_brace>", "}", null, line, col);
            case '[': stream.advance(); return new Token("<l_brack>", "[", null, line, col);
            case ']': stream.advance(); return new Token("<r_brack>", "]", null, line, col);
            case ';': stream.advance(); return new Token("<semi>",    ";", null, line, col);
            case ',': stream.advance(); return new Token("<comma>",   ",", null, line, col);
            case ':': stream.advance(); return new Token("<colon>",   ":", null, line, col);

            default:
                return null;
        }
    }
}