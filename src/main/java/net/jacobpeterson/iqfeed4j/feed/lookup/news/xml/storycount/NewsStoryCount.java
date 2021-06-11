package net.jacobpeterson.iqfeed4j.feed.lookup.news.xml.storycount;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

/**
 * {@link NewsStoryCount} represents a news story from a news feed.
 */
public class NewsStoryCount {

    @JacksonXmlProperty(localName = "Name")
    protected String symbol;
    @JacksonXmlProperty(localName = "StoryCount")
    protected Integer count;

    /**
     * Instantiates a new {@link NewsStoryCount}.
     */
    public NewsStoryCount() {}

    /**
     * Gets {@link #symbol}.
     *
     * @return the symbol
     */
    public String getSymbol() {
        return symbol;
    }

    /**
     * Sets {@link #symbol}.
     *
     * @param symbol the symbol
     */
    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    /**
     * Gets {@link #count}.
     *
     * @return the count
     */
    public Integer getCount() {
        return count;
    }

    /**
     * Sets {@link #count}.
     *
     * @param count the count
     */
    public void setCount(Integer count) {
        this.count = count;
    }

    @Override
    public String toString() {
        return "NewsStoryCount{" +
                "symbol='" + symbol + '\'' +
                ", count=" + count +
                '}';
    }
}
