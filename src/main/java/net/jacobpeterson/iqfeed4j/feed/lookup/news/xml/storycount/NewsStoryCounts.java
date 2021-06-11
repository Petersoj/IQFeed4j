package net.jacobpeterson.iqfeed4j.feed.lookup.news.xml.storycount;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

import java.util.ArrayList;

/**
 * {@link NewsStoryCounts} contains {@link NewsStoryCount}s.
 */
@JacksonXmlRootElement(localName = "story_counts")
public class NewsStoryCounts {

    @JacksonXmlProperty(localName = "symbol")
    @JacksonXmlElementWrapper(useWrapping = false)
    private ArrayList<NewsStoryCount> newsStoryCounts;

    /**
     * Gets {@link #newsStoryCounts}.
     *
     * @return a {@link ArrayList} of {@link NewsStoryCount}s
     */
    public ArrayList<NewsStoryCount> getNewsStoryCounts() {
        return newsStoryCounts;
    }

    /**
     * Sets {@link #newsStoryCounts}.
     *
     * @param newsStoryCounts a {@link ArrayList} of {@link NewsStoryCount}s
     */
    public void setNewsStoryCounts(ArrayList<NewsStoryCount> newsStoryCounts) {
        this.newsStoryCounts = newsStoryCounts;
    }

    @Override
    public String toString() {
        return "NewsStoryCounts{" +
                "newsStoryCounts=" + newsStoryCounts +
                '}';
    }
}
