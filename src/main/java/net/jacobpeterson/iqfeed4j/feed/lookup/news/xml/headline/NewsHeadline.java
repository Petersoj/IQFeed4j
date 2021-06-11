package net.jacobpeterson.iqfeed4j.feed.lookup.news.xml.headline;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import net.jacobpeterson.iqfeed4j.feed.lookup.news.xml.deserializers.NewsHeadlineSymbolsDeserializer;

import java.time.LocalDateTime;
import java.util.List;

/**
 * {@link NewsHeadline} represents a news headline from a news feed.
 */
public class NewsHeadline {

    @JacksonXmlProperty(localName = "id")
    protected String id;
    @JacksonXmlProperty(localName = "source")
    protected String source;
    @JacksonXmlProperty(localName = "timestamp")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyyMMddHHmmss")
    protected LocalDateTime timestamp;
    @JacksonXmlProperty(localName = "symbols")
    @JsonDeserialize(using = NewsHeadlineSymbolsDeserializer.class)
    protected List<String> symbols;
    @JacksonXmlProperty(localName = "text")
    protected String text;

    /**
     * Instantiates a new {@link NewsHeadline}.
     */
    public NewsHeadline() {}

    /**
     * Gets {@link #id}.
     *
     * @return the ID
     */
    public String getID() {
        return id;
    }

    /**
     * Sets {@link #id}.
     *
     * @param id the ID
     */
    public void setID(String id) {
        this.id = id;
    }

    /**
     * Gets {@link #source}.
     *
     * @return the source
     */
    public String getSource() {
        return source;
    }

    /**
     * Sets {@link #source}.
     *
     * @param source the source
     */
    public void setSource(String source) {
        this.source = source;
    }

    /**
     * Gets {@link #timestamp}.
     *
     * @return the timestamp
     */
    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    /**
     * Sets {@link #timestamp}.
     *
     * @param timestamp the timestamp
     */
    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
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

    @Override
    public String toString() {
        return "NewsHeadline{" +
                "id='" + id + '\'' +
                ", source='" + source + '\'' +
                ", timestamp=" + timestamp +
                ", symbols=" + symbols +
                ", text='" + text + '\'' +
                '}';
    }
}
