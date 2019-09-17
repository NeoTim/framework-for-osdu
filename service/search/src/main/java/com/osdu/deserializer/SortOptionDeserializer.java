package com.osdu.deserializer;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.osdu.model.osdu.SortOption;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;

/**
 * Custom deserializer for OSDU search object. Needed since the structure defined in documentation describes two different styles
 * for inner objects of the sort array -
 * 1 - Object with inner field named as a fieldName and inner object of that field with K:V pair order:orderType
 * (example : {
 * "region":{
 * "order":"asc"
 * }
 * }
 * )
 * 2 - String with the name of the field.
 * (example : "fieldName")
 */
public class SortOptionDeserializer extends JsonDeserializer<SortOption> {
    @Override
    public SortOption deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        JsonNode node = p.getCodec().readTree(p);
        SortOption sortOption = new SortOption();

        if (!node.fields().hasNext()) {
            sortOption.setFieldName(node.asText());
            sortOption.setOrderType(SortOption.OrderType.asc);
        }

        for (Iterator<Map.Entry<String, JsonNode>> it = node.fields(); it.hasNext(); ) {
            Map.Entry<String, JsonNode> next = it.next();
            sortOption.setFieldName(next.getKey());
            sortOption.setOrderType(SortOption.OrderType.valueOf(next.getValue().get("order").asText()));
        }

        return sortOption;
    }
}