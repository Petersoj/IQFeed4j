package net.jacobpeterson.iqfeed4j.feed.lookup.news.xml.headline;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

import java.util.ArrayList;

/**
 * {@link NewsHeadlines} contains {@link NewsHeadline}s.
 */
@JacksonXmlRootElement(localName = "news_headlines")
public class NewsHeadlines {

    @JacksonXmlProperty(localName = "news_headline")
    @JacksonXmlElementWrapper(useWrapping = false)
    private ArrayList<NewsHeadline> headlines;

    /**
     * Gets {@link #headlines}.
     *
     * @return a {@link ArrayList} of {@link NewsHeadline}s
     */
    public ArrayList<NewsHeadline> getHeadlines() {
        return headlines;
    }

    /**
     * Sets {@link #headlines}.
     *
     * @param headlines a {@link ArrayList} of {@link NewsHeadline}s
     */
    public void setHeadlines(ArrayList<NewsHeadline> headlines) {
        this.headlines = headlines;
    }

    @Override
    public String toString() {
        return "NewsHeadlines{" +
                "headlines=" + headlines +
                '}';
    }
}
