package net.jacobpeterson.iqfeed4j.util.chars;

/**
 * {@link CharUtil} defines utility methods for {@link Character} or the primitive 'char' type.
 */
public final class CharUtil {

    /**
     * Finds the last index of a non-number character in a {@link String}, or <code>-1</code> if none was found.
     *
     * @param string the string to search
     *
     * @return the index
     */
    public static int lastIndexOfNonNumber(String string) {
        if (string.isEmpty()) {
            return -1;
        }

        // Start at end of string
        for (int index = string.length() - 1; index >= 0; index--) {
            char ch = string.charAt(index);
            if (!(ch == '0' || ch == '1' || ch == '2' || ch == '3' || ch == '4' || ch == '5' || ch == '6' ||
                    ch == '7' || ch == '8' || ch == '9')) {
                return index;
            }
        }

        return -1;
    }
}
