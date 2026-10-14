package io.synapseproject.engine.graph;

import io.synapseproject.api.model.Identifiers;
import io.synapseproject.api.model.graph.Graph;
import io.synapseproject.api.model.graph.GraphManager;
import io.synapseproject.engine.store.EngineStore;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import org.jspecify.annotations.NullMarked;

@NullMarked
public final class DefaultGraphManager implements GraphManager {

  private final EngineStore store;
  private final ConcurrentHashMap<String, GraphIndex> graphs = new ConcurrentHashMap<>();

  public DefaultGraphManager(EngineStore store) {
    this.store = Objects.requireNonNull(store, "store");
  }

  public CompletableFuture<Void> put(Graph graph) {
    Objects.requireNonNull(graph, "graph");
    GraphIndex index = new GraphIndex(graph);
    return store.saveGraph(graph).thenRun(() -> graphs.put(graph.id(), index));
  }

  public CompletableFuture<Void> load() {
    return store
        .loadGraphs()
        .thenAccept(
            loaded -> {
              for (Graph graph : loaded) {
                graphs.put(graph.id(), new GraphIndex(graph));
              }
            });
  }

  CompletableFuture<GraphIndex> require(String id) {
    String required = Identifiers.require(id, "id");
    return store.call(
        () -> {
          GraphIndex index = graphs.get(required);
          if (index == null) {
            throw new IllegalArgumentException("Graph not found: " + required);
          }
          return index;
        });
  }

  @Override
  public CompletableFuture<Optional<Graph>> findGraph(String id) {
    String required = Identifiers.require(id, "id");
    return store.call(() -> Optional.ofNullable(graphs.get(required)).map(GraphIndex::graph));
  }

  @Override
  public CompletableFuture<List<Graph>> loadedGraphs() {
    return store.call(
        () -> {
          List<Graph> loaded = new ArrayList<>();
          for (GraphIndex index : graphs.values()) {
            loaded.add(index.graph());
          }
          return List.copyOf(loaded);
        });
  }

  @Override
  public CompletableFuture<Void> reload(String id) {
    String required = Identifiers.require(id, "id");
    return store
        .findGraph(required)
        .thenAccept(
            found -> {
              Graph graph =
                  found.orElseThrow(
                      () -> new IllegalArgumentException("Graph not found: " + required));
              graphs.put(required, new GraphIndex(graph));
            });
  }
}
