package net.jacobpeterson.iqfeed4j.util.sort;

import net.jacobpeterson.iqfeed4j.util.enums.APICode;

/**
 * {@link SortDirection} defines enums for sorting directions.
 */
public enum SortDirection implements APICode {

    /**
     * Ascending {@link SortDirection}.
     */
    ASCENDING(0),

    /**
     * Descending {@link SortDirection}.
     */
    DESCENDING(1);

    int apiCode;

    /**
     * Instantiates a new {@link SortDirection}.
     *
     * @param apiCode the API code
     */
    SortDirection(int apiCode) {
        this.apiCode = apiCode;
    }

    @Override
    public int getAPICode() {
        return apiCode;
    }
}
