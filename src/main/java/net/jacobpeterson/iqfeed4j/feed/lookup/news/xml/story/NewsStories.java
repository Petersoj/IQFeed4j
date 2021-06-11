package net.jacobpeterson.iqfeed4j.feed.lookup.news.xml.story;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

import java.util.ArrayList;

/**
 * {@link NewsStories} contains {@link NewsStory}s.
 */
@JacksonXmlRootElement(localName = "news_stories")
public class NewsStories {

    @JacksonXmlProperty(localName = "news_story")
    @JacksonXmlElementWrapper(useWrapping = false)
    private ArrayList<NewsStory> newsStories;

    /**
     * Gets {@link #newsStories}.
     *
     * @return a {@link ArrayList} of {@link NewsStory}s
     */
    public ArrayList<NewsStory> getNewsStories() {
        return newsStories;
    }

    /**
     * Sets {@link #newsStories}.
     *
     * @param newsStories a {@link ArrayList} of {@link NewsStory}s
     */
    public void setNewsStories(ArrayList<NewsStory> newsStories) {
        this.newsStories = newsStories;
    }

    @Override
    public String toString() {
        return "NewsStories{" +
                "newsStories=" + newsStories +
                '}';
    }
}
