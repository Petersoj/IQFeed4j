package net.jacobpeterson.iqfeed4j.feed.lookup.news.xml.story;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import net.jacobpeterson.iqfeed4j.feed.lookup.news.xml.deserializers.NewsHeadlineSymbolsDeserializer;
import net.jacobpeterson.iqfeed4j.feed.lookup.news.xml.deserializers.NewsStoryIsLinkDeserializer;

import java.util.List;

/**
 * {@link NewsStory} represents a news story from a news feed.
 */
public class NewsStory {

    @JacksonXmlProperty(localName = "is_link")
    @JsonDeserialize(using = NewsStoryIsLinkDeserializer.class)
    protected Boolean isLink;
    @JacksonXmlProperty(localName = "story_text")
    protected String text;
    @JacksonXmlProperty(localName = "symbols")
    @JsonDeserialize(using = NewsHeadlineSymbolsDeserializer.class)
    protected List<String> symbols;

    /**
     * Instantiates a new {@link NewsStory}.
     */
    public NewsStory() {}

    /**
     * Gets {@link #isLink}.
     *
     * @return the link
     */
    public Boolean isLink() {
        return isLink;
    }

    /**
     * Sets {@link #isLink}.
     *
     * @param link the link
     */
    public void setLink(Boolean link) {
        isLink = link;
    }

    /**
     * Gets {@link #text}.
     *
     * @return the text
     */
    public String getText() {
        return text;
    }

    /**
     * Sets {@link #text}.
     *
     * @param text the text
     */
    public void setText(String text) {
        this.text = text;
    }

    /**
     * Gets {@link #symbols}.
     *
     * @return the symbols
     */
    public List<String> getSymbols() {
        return symbols;
    }

    /**
     * Sets {@link #symbols}.
     *
     * @param symbols the symbols
     */
    public void setSymbols(List<String> symbols) {
        this.symbols = symbols;
    }

    @Override
    public String toString() {
        return "NewsStory{" +
                "isLink=" + isLink +
                ", text='" + text + '\'' +
                ", symbols=" + symbols +
                '}';
    }
}
