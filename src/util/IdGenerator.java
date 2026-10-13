package util;

/** IDs fit VARCHAR(20) and do not race on SELECT MAX across application clients. */
public final class IdGenerator {
    private IdGenerator() {}
    public static String next(String prefix) {
        if (prefix.length()>2) throw new IllegalArgumentException("ID prefix must have at most two characters");
        return prefix+java.util.UUID.randomUUID().toString().replace("-", "").substring(0,18);
    }
}
