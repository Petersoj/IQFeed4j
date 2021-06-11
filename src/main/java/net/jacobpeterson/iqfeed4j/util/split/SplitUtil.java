package net.jacobpeterson.iqfeed4j.util.split;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * {@link SplitUtil} contains utility functions for splitting {@link String}s.
 */
public class SplitUtil {

    private static final Pattern QUOTE_ESCAPED_SPACE_PATTERN = Pattern.compile("[^\\s\"']+|\"([^\"]*)\"|'([^']*)'");

    /**
     * Splits a {@link String} using the space key as a delimiter, but ignores spaces surrounded in quotes.
     *
     * @param toSplit the {@link String} to split
     *
     * @return a {@link List} of {@link String}s
     *
     * @see <a href="https://stackoverflow.com/a/366532">"Regex for splitting a string using space" reference</a>
     */
    public static List<String> splitQuoteEscapedSpaces(String toSplit) {
        List<String> splitList = new ArrayList<>();
        Matcher regexMatcher = QUOTE_ESCAPED_SPACE_PATTERN.matcher(toSplit);

        while (regexMatcher.find()) {
            if (regexMatcher.group(1) != null) {
                // Add double-quoted string without the quotes
                splitList.add(regexMatcher.group(1));
            } else if (regexMatcher.group(2) != null) {
                // Add single-quoted string without the quotes
                splitList.add(regexMatcher.group(2));
            } else {
                // Add unquoted word
                splitList.add(regexMatcher.group());
            }
        }

        return splitList;
    }
}
