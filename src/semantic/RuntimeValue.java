package semantic;

public class RuntimeValue {
    public String type; // "PRICE", "RECIPE", "QUALITY"
    public Object value; 

    public RuntimeValue(String type, Object value) {
        this.type = type;
        this.value = value;
    }

    public double asDouble() {
        if (type.equals("PRICE")) return (Double) value;
        throw new RuntimeException("Semantic Error: Type Mismatch! Expected PRICE but got " + type);
    }

    public boolean asBoolean() {
        if (type.equals("QUALITY")) return (Boolean) value;
        throw new RuntimeException("Semantic Error: Type Mismatch! Expected QUALITY but got " + type);
    }

    public String asString() {
        return value.toString();
    }

    @Override
    public String toString() {
        // Formats numbers cleanly (e.g., 5.0 -> 5) so output looks like whole numbers when appropriate
        if (type.equals("PRICE")) {
            double d = (Double) value;
            if (d == (long) d) return String.format("%d", (long) d);
            return String.format("%s", d);
        }
        return value != null ? value.toString() : "null";
    }
}