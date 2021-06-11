package net.jacobpeterson.iqfeed4j.feed.lookup.news.xml.configuration;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

import java.util.ArrayList;

/**
 * {@link NewsConfiguration} contains {@link NewsCategory}s.
 */
@JacksonXmlRootElement(localName = "DynamicNewsConf")
public class NewsConfiguration {

    @JacksonXmlProperty(localName = "category")
    @JacksonXmlElementWrapper(useWrapping = false)
    private ArrayList<NewsCategory> categories;

    /**
     * Gets {@link #categories}.
     *
     * @return a {@link ArrayList} of {@link NewsCategory}s
     */
    public ArrayList<NewsCategory> getCategories() {
        return categories;
    }

    /**
     * Sets {@link #categories}.
     *
     * @param categories a {@link ArrayList} of {@link NewsCategory}s
     */
    public void setCategories(ArrayList<NewsCategory> categories) {
        this.categories = categories;
    }

    @Override
    public String toString() {
        return "NewsConfiguration{" +
                "categories=" + categories +
                '}';
    }
}
