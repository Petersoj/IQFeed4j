package net.jacobpeterson.iqfeed4j.feed.lookup.news.xml.configuration;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

import java.util.ArrayList;

/**
 * {@link NewsMajorSource} is a major {@link NewsSource}.
 */
public class NewsMajorSource extends NewsSource {

    @JacksonXmlProperty(localName = "minor_type")
    @JacksonXmlElementWrapper(useWrapping = false)
    private ArrayList<NewsMinorSource> minorSources;

    /**
     * Gets {@link #minorSources}.
     *
     * @return a {@link ArrayList} of {@link NewsMinorSource}s
     */
    public ArrayList<NewsMinorSource> getMinorSources() {
        return minorSources;
    }

    /**
     * Sets {@link #minorSources}.
     *
     * @param minorSources a {@link ArrayList} of {@link NewsMinorSource}s
     */
    public void setMinorSources(ArrayList<NewsMinorSource> minorSources) {
        this.minorSources = minorSources;
    }

    @Override
    public String toString() {
        return "NewsMajorSource{" +
                "minorSources=" + minorSources +
                "} " + super.toString();
    }
}
