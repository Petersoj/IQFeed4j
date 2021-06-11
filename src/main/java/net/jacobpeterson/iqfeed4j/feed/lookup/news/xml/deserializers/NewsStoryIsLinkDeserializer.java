package net.jacobpeterson.iqfeed4j.feed.lookup.news.xml.deserializers;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import net.jacobpeterson.iqfeed4j.feed.lookup.news.xml.story.NewsStory;

import java.io.IOException;

/**
 * {@link NewsStoryIsLinkDeserializer} is for {@link NewsStory#isLink()} XML deserialization.
 */
public class NewsStoryIsLinkDeserializer extends JsonDeserializer<Boolean> {

    @Override
    public Boolean deserialize(JsonParser parser, DeserializationContext context) throws IOException {
        String value = parser.getValueAsString();
        if (value.equals("Y")) {
            return true;
        } else if (value.equals("N")) {
            return false;
        } else {
            return null;
        }
    }
}
