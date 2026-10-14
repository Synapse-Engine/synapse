package io.synapseproject.engine.store;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.synapseproject.api.model.PropertyValue;
import io.synapseproject.api.model.graph.Graph;
import java.math.BigDecimal;
import org.jspecify.annotations.NullMarked;

@NullMarked
final class Documents {

  private static final ObjectMapper MAPPER =
      new ObjectMapper()
          .addMixIn(PropertyValue.class, PropertyValueTypes.class)
          .addMixIn(PropertyValue.Number.class, NumberText.class);

  private Documents() {}

  static String encode(Graph graph) {
    return write(graph);
  }

  static Graph decodeGraph(String json) {
    return read(json, Graph.class);
  }

  static String encode(PropertyValue value) {
    try {
      return MAPPER.writerFor(PropertyValue.class).writeValueAsString(value);
    } catch (JsonProcessingException exception) {
      throw new IllegalArgumentException("could not write value", exception);
    }
  }

  static PropertyValue decodeValue(String json) {
    return read(json, PropertyValue.class);
  }

  private static String write(Object value) {
    try {
      return MAPPER.writeValueAsString(value);
    } catch (JsonProcessingException exception) {
      throw new IllegalArgumentException("could not write document", exception);
    }
  }

  private static <T> T read(String json, Class<T> type) {
    try {
      return MAPPER.readValue(json, type);
    } catch (JsonProcessingException exception) {
      throw new IllegalArgumentException("could not read document", exception);
    }
  }

  @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
  @JsonSubTypes({
    @JsonSubTypes.Type(value = PropertyValue.Text.class, name = "text"),
    @JsonSubTypes.Type(value = PropertyValue.Bool.class, name = "bool"),
    @JsonSubTypes.Type(value = PropertyValue.Number.class, name = "number"),
    @JsonSubTypes.Type(value = PropertyValue.ListValue.class, name = "list"),
    @JsonSubTypes.Type(value = PropertyValue.MapValue.class, name = "map")
  })
  private abstract static class PropertyValueTypes {}

  private abstract static class NumberText {
    @SuppressWarnings("unused")
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    abstract BigDecimal value();
  }
}
