package net.jacobpeterson.iqfeed4j.feed;

import net.jacobpeterson.iqfeed4j.model.feedenums.FeedMessageType;

import static net.jacobpeterson.iqfeed4j.util.csv.CSVUtil.valueEquals;

public abstract class AbstractRequestIDFeed {

    /**
     * Checks for error message format.
     * <br>
     * e.g. <code>[Request ID], E, [Error Text]</code> or <code>E, [Error Text]</code>
     * <br>
     * If the 'Request ID' is the char literal 'E', then this will always return true unfortunately (this is a flaw with
     * the IQFeed API)
     *
     * @param csv the CSV
     *
     * @return true if the 'csv' represents an error message
     */
    protected boolean isErrorMessage(String[] csv) {
        return valueEquals(csv, 0, FeedMessageType.ERROR.value()) ||
                valueEquals(csv, 1, FeedMessageType.ERROR.value());
    }
}
