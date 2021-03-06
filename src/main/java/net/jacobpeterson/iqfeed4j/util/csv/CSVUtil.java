package net.jacobpeterson.iqfeed4j.util.csv;

/**
 * {@link CSVUtil} defines utility methods for CSVs.
 */
public final class CSVUtil {

    /**
     * Tests if the <code>csv</code> array has <code>index</code>.
     *
     * @param csv   the CSV
     * @param index the index
     *
     * @return a boolean
     */
    public static boolean valueExists(String[] csv, int index) {
        return index < csv.length;
    }

    /**
     * Tests if the <code>csv</code> array has <code>index</code> and if the value at <code>index</code> is not empty.
     *
     * @param csv   the CSV
     * @param index the index
     *
     * @return a boolean
     */
    public static boolean valuePresent(String[] csv, int index) {
        return index < csv.length && !csv[index].isEmpty();
    }

    /**
     * Tests if the <code>csv</code> array has <code>index</code> and if the value at <code>index</code> is not empty
     * and not whitespace.
     *
     * @param csv   the CSV
     * @param index the index
     *
     * @return a boolean
     */
    public static boolean valueNotWhitespace(String[] csv, int index) {
        return index < csv.length && !csv[index].isEmpty() && !csv[index].trim().isEmpty();
    }

    /**
     * Tests if the <code>csv</code> array has <code>match</code> at <code>index</code>.
     *
     * @param csv   the CSV
     * @param index the index
     * @param match the string to check
     *
     * @return the boolean
     */
    public static boolean valueEquals(String[] csv, int index, String match) {
        return valueExists(csv, index) && csv[index].equals(match);
    }
}
