import java.util.HashMap;
import java.util.Map;

public class Keywords {
    public static final String TK_START        = "<start>";
    public static final String TK_END          = "<end>";

    // Data types
    public static final String TK_NUM_TYPE     = "<num_type>";
    public static final String TK_BOOL_TYPE    = "<bool_type>";
    public static final String TK_STRING_TYPE  = "<string_type>";
    public static final String TK_VOID         = "<void>";

    // Conditionals
    public static final String TK_IF           = "<if>";
    public static final String TK_ELSEIF       = "<elseif>";
    public static final String TK_ELSE         = "<else>";
    public static final String TK_SWITCH       = "<switch>";
    public static final String TK_CASE         = "<case>";
    public static final String TK_DEFAULT      = "<default>";
    public static final String TK_BREAK        = "<break>";
    public static final String TK_CONTINUE     = "<continue>";

    // Loops
    public static final String TK_WHILE        = "<while>";
    public static final String TK_FOR          = "<for>";
    public static final String TK_DO_WHILE     = "<do_while>";

    // Input / Output
    public static final String TK_INPUT        = "<input>";
    public static final String TK_OUTPUT       = "<output>";

    // Exception handling
    public static final String TK_TRY          = "<try>";
    public static final String TK_CATCH        = "<catch>";
    public static final String TK_FINALLY      = "<finally>";
    public static final String TK_THROW        = "<throw>";

    // Boolean constants
    public static final String TK_TRUE         = "<true>";
    public static final String TK_FALSE        = "<false>";

    // Functions
    public static final String TK_FUNCTION     = "<function>";
    public static final String TK_RETURN       = "<return>";

    // -------------------------------------------------------------------------
    // The keyword map
    // -------------------------------------------------------------------------

    private static final Map<String, String> KEYWORDS = new HashMap<>();

    static {
        // Program structure
        KEYWORDS.put("ORDER_START", TK_START);
        KEYWORDS.put("ORDER_END",   TK_END);

        // Data types
        KEYWORDS.put("PRICE",       TK_NUM_TYPE);
        KEYWORDS.put("QUALITY",     TK_BOOL_TYPE);
        KEYWORDS.put("RECIPE",      TK_STRING_TYPE);
        KEYWORDS.put("EMPTY",       TK_VOID);

        // Conditionals
        KEYWORDS.put("WANT",        TK_IF);
        KEYWORDS.put("SIDE",        TK_ELSEIF);
        KEYWORDS.put("ONLY",        TK_ELSE);
        KEYWORDS.put("MENU",        TK_SWITCH);
        KEYWORDS.put("FOOD",        TK_CASE);
        KEYWORDS.put("SOLDOUT",     TK_DEFAULT);
        KEYWORDS.put("DONE",        TK_BREAK);
        KEYWORDS.put("NEXT",        TK_CONTINUE);

        // Loops
        KEYWORDS.put("REFILL",      TK_WHILE);
        KEYWORDS.put("PREP",        TK_FOR);
        KEYWORDS.put("STIR",        TK_DO_WHILE);

        // Input / Output
        KEYWORDS.put("ORDER",       TK_INPUT);
        KEYWORDS.put("SERVE",       TK_OUTPUT);

        // Exception handling
        KEYWORDS.put("CHECK",       TK_TRY);
        KEYWORDS.put("HANDLE",      TK_CATCH);
        KEYWORDS.put("CLEAN",       TK_FINALLY);
        KEYWORDS.put("SPILL",       TK_THROW);

        // Boolean constants
        KEYWORDS.put("FRESH",       TK_TRUE);
        KEYWORDS.put("EXPIRED",     TK_FALSE);

        // Functions
        KEYWORDS.put("TASK",        TK_FUNCTION);
        KEYWORDS.put("YIELD",       TK_RETURN);
}
  

public static String lookup(String word) {
        return KEYWORDS.get(word);          // null when not found
    }

  public static boolean isKeyword(String word) {
        return KEYWORDS.containsKey(word);
    }

     public static Map<String, String> getAll() {
        return java.util.Collections.unmodifiableMap(KEYWORDS);
    }
    public static String getTokenName(String word) {
    // Check keywords first
    String keyword = KEYWORDS.get(word);
    if (keyword != null) return keyword;

    // Check if it's a valid identifier (must start with lowercase)
    if (word != null && !word.isEmpty() && Character.isLowerCase(word.charAt(0))) {
        return "<id>";
    }

    return null; // invalid — neither keyword nor valid identifier
}
}