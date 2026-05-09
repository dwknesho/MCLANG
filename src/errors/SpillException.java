package errors;
import semantic.RuntimeValue;

public class SpillException extends RuntimeException {
    public RuntimeValue spilledValue;
    public SpillException(RuntimeValue val) { this.spilledValue = val; }
}