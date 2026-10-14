package io.synapseproject.engine.store;

import io.synapseproject.api.model.PropertyValue;
import io.synapseproject.api.model.graph.Graph;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import org.jspecify.annotations.NullMarked;

@NullMarked
public interface EngineStore extends AutoCloseable {

  <T> CompletableFuture<T> call(Supplier<T> work);

  CompletableFuture<Void> saveGraph(Graph graph);

  CompletableFuture<List<Graph>> loadGraphs();

  CompletableFuture<Optional<Graph>> findGraph(String id);

  CompletableFuture<Void> setPlayerVariable(UUID playerId, String key, PropertyValue value);

  CompletableFuture<Map<String, PropertyValue>> playerVariables(UUID playerId);

  CompletableFuture<Void> setGlobalVariable(String key, PropertyValue value);

  CompletableFuture<Map<String, PropertyValue>> globalVariables();

  CompletableFuture<Boolean> complete(UUID playerId, String graphId, String nodeId);

  CompletableFuture<Boolean> completed(UUID playerId, String graphId, String nodeId);

  CompletableFuture<Void> place(UUID playerId, String graphId, String nodeId);

  CompletableFuture<Optional<String>> cursor(UUID playerId, String graphId);

  @Override
  void close();
}
