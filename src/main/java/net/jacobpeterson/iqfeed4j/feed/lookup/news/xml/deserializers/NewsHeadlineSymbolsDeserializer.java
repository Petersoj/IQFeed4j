package net.jacobpeterson.iqfeed4j.feed.lookup.news.xml.deserializers;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.google.common.base.Splitter;
import net.jacobpeterson.iqfeed4j.feed.lookup.news.xml.headline.NewsHeadline;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * {@link NewsHeadlineSymbolsDeserializer} is for {@link NewsHeadline#getSymbols()} XML deserialization.
 */
public class NewsHeadlineSymbolsDeserializer extends JsonDeserializer<List<String>> {

    @Override
    @SuppressWarnings("UnstableApiUsage")
    public List<String> deserialize(JsonParser parser, DeserializationContext context) throws IOException {
        return Splitter.on(':').splitToStream(parser.getValueAsString())
                .filter(Objects::nonNull)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }
}
