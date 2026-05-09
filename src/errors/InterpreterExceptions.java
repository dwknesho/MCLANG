package errors;

public class InterpreterExceptions {
    // Thrown by DONE (break)
    public static class BreakException extends RuntimeException {}

    // Thrown by NEXT (continue)
    public static class ContinueException extends RuntimeException {}

    // Thrown by SPILL (throw)
    public static class SpillException extends RuntimeException {
        public Object thrownValue;
        public SpillException(Object thrownValue) {
            super(thrownValue.toString());
            this.thrownValue = thrownValue;
        }
    }

    // Thrown by YIELD (return)
    public static class ReturnException extends RuntimeException {
        public Object returnValue;
        public ReturnException(Object returnValue) {
            super("Return", null, false, false);
            this.returnValue = returnValue;
        }
    }
}