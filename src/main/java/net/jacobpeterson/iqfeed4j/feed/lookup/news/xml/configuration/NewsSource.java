package net.jacobpeterson.iqfeed4j.feed.lookup.news.xml.configuration;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

/**
 * {@link NewsSource} represents a type of news source.
 */
public class NewsSource {

    @JacksonXmlProperty(localName = "type", isAttribute = true)
    protected String id;
    @JacksonXmlProperty(localName = "name", isAttribute = true)
    protected String name;
    @JacksonXmlProperty(localName = "auth_code", isAttribute = true)
    protected String authCode;
    @JacksonXmlProperty(localName = "icon_id", isAttribute = true)
    protected Integer iconID;
    @JacksonXmlProperty(localName = "fixed_page", isAttribute = true)
    protected Boolean fixedPage;

    /**
     * Instantiates a new {@link NewsSource}.
     */
    public NewsSource() {}

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

    /**
     * Gets {@link #authCode}.
     *
     * @return the auth code
     */
    public String getAuthCode() {
        return authCode;
    }

    /**
     * Sets {@link #authCode}.
     *
     * @param authCode the auth code
     */
    public void setAuthCode(String authCode) {
        this.authCode = authCode;
    }

    /**
     * Gets {@link #iconID}.
     *
     * @return the icon ID
     */
    public Integer getIconID() {
        return iconID;
    }

    /**
     * Sets {@link #iconID}.
     *
     * @param iconID the icon ID
     */
    public void setIconID(Integer iconID) {
        this.iconID = iconID;
    }

    /**
     * Gets {@link #fixedPage}.
     *
     * @return the fixed page
     */
    public Boolean isFixedPage() {
        return fixedPage;
    }

    /**
     * Sets {@link #fixedPage}.
     *
     * @param fixedPage the fixed page
     */
    public void setFixedPage(Boolean fixedPage) {
        this.fixedPage = fixedPage;
    }

    @Override
    public String toString() {
        return "NewsSource{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", authCode='" + authCode + '\'' +
                ", iconID=" + iconID +
                ", fixedPage=" + fixedPage +
                '}';
    }
}
