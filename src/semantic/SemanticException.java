package semantic;

public class SemanticException extends RuntimeException {
    public SemanticException(String message) {
        super("\n[SEMANTIC ERROR] " + message);
    }
}