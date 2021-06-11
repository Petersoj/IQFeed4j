package net.jacobpeterson.iqfeed4j.feed.lookup.news.xml.configuration;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

import java.util.ArrayList;

/**
 * {@link NewsCategory} contains {@link NewsMajorSource}s.
 */
public class NewsCategory {

    @JacksonXmlProperty(localName = "major_type")
    @JacksonXmlElementWrapper(useWrapping = false)
    private ArrayList<NewsMajorSource> majorSources;
    @JacksonXmlProperty(localName = "name")
    private String name;

    /**
     * Gets {@link #majorSources}.
     *
     * @return a {@link ArrayList} of {@link NewsMajorSource}s
     */
    public ArrayList<NewsMajorSource> getMajorSources() {
        return majorSources;
    }

    /**
     * Sets {@link #majorSources}.
     *
     * @param majorSources a {@link ArrayList} of {@link NewsMajorSource}s
     */
    public void setMajorSources(ArrayList<NewsMajorSource> majorSources) {
        this.majorSources = majorSources;
    }

    /**
     * Gets {@link #name}.
     *
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * Sets {@link #name}.
     *
     * @param name the name
     */
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return "NewsCategory{" +
                "majorSources=" + majorSources +
                ", name='" + name + '\'' +
                '}';
    }
}
