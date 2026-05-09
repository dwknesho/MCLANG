package errors;
import semantic.RuntimeValue;

public class ReturnException extends RuntimeException {
    public RuntimeValue returnValue;
    public ReturnException(RuntimeValue val) { this.returnValue = val; }
}