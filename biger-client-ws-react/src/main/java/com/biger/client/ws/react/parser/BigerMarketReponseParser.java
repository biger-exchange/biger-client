package com.biger.client.ws.react.parser;

import com.biger.client.ws.react.BigerMarketEventType;
import com.biger.client.ws.react.domain.response.ExchangeResponse;
import com.biger.client.ws.react.domain.response.SymbolEvent;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Optional;
import java.util.function.Function;

public class BigerMarketReponseParser {
    static final Logger LOG = LoggerFactory.getLogger(BigerMarketReponseParser.class);

    static final public BigerMarketReponseParser INSTANCE = defaultInstance();

    private static BigerMarketReponseParser defaultInstance() {
        return new BigerMarketReponseParser();
    }

    @Getter
    final private ObjectMapper objectMapper = new ObjectMapper();

    public BigerMarketReponseParser() {
        objectMapper.configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true);
        objectMapper.configure(DeserializationFeature.UNWRAP_SINGLE_VALUE_ARRAYS, true);
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    public ExchangeResponse text2ExchangeResponse(String text, Function<String, BigerMarketEventType> methodToEventType) {
        try {
            JsonNode jsonNode = this.objectMapper.readTree(text);

            if (jsonNode.has("method")) {
                String method = jsonNode.get("method").asText();
                return this.objectMapper.convertValue(jsonNode, Optional.ofNullable(methodToEventType.apply(method)).orElse(BigerMarketEventType.KnownTypes.OTHER).type());
            } else {
                return this.objectMapper.convertValue(jsonNode, new TypeReference<ExchangeResponse<JsonNode>>() {
                });
            }
        } catch (IOException ex) {
            LOG.warn("Failed to parse text [{}]", text, ex);
            return null;
        }

    }

    public String extractSubIdFromExchangeResponse(ExchangeResponse data, Function<String, BigerMarketEventType> methodToEventType) {
        if (data.getMethod() == null || data.getMethod().isEmpty()) {
            return String.valueOf(data.getId());
        }
        return Optional.ofNullable(methodToEventType.apply(data.getMethod())).orElse(BigerMarketEventType.KnownTypes.OTHER).extractKey((SymbolEvent) data.getParams());

    }

}
