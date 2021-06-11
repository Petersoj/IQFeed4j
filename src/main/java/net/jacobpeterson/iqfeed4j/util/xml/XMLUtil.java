package net.jacobpeterson.iqfeed4j.util.xml;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/**
 * {@link XMLUtil} contains utility methods for XML.
 */
public class XMLUtil {

    /**
     * This is a standard {@link XmlMapper}.
     */
    public static final XmlMapper STANDARD_XML_MAPPER = new XmlMapper();

    static {
        STANDARD_XML_MAPPER.registerModule(new JavaTimeModule());
    }
}
