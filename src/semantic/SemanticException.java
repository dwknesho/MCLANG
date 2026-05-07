package semantic;
 
// Thrown by the interpreter when a semantic rule is violated.
// Carries line info so we can print "Semantic Error [Line X]: ..."
public class SemanticException extends RuntimeException {
    public int line;
 
    public SemanticException(String message) {
        super(message);
        this.line = -1;
    }
 
    public SemanticException(String message, int line) {
        super(message);
        this.line = line;
    }
}
 