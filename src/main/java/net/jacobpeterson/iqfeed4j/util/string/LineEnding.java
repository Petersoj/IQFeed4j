package net.jacobpeterson.iqfeed4j.util.string;

/**
 * {@link LineEnding} defines enums for the various types of ASCII line endings.
 */
public enum LineEnding {

    /**
     * &lt;CR&gt;&lt;LF&gt; line ending. Note that there is no ASCII char for this, only an ASCII String.
     */
    CR_LF((char) 0, "\r\n"),

    /**
     * &lt;CR&gt; line ending.
     */
    CR('\r', "\r"),

    /**
     * &lt;LF&gt; line ending.
     */
    LF('\n', "\n");

    private final String asciiString;
    private final char asciiChar;

    /**
     * Instantiates a new {@link LineEnding}.
     *
     * @param asciiChar   the ASCII char
     * @param asciiString the ASCII string
     */
    LineEnding(char asciiChar, String asciiString) {
        this.asciiChar = asciiChar;
        this.asciiString = asciiString;
    }

    /**
     * Gets {@link #asciiString}.
     *
     * @return a {@link String}
     */
    public String getASCIIString() {
        return asciiString;
    }

    /**
     * Gets {@link #asciiChar}.
     *
     * @return a char
     */
    public char getASCIIChar() {
        return asciiChar;
    }
}
