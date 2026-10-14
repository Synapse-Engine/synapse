package io.synapseproject.engine.graph;

import io.synapseproject.api.model.Identifiers;
import io.synapseproject.api.model.graph.Graph;
import io.synapseproject.api.model.graph.GraphManager;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import org.jspecify.annotations.NullMarked;

@NullMarked
public final class DefaultGraphManager implements GraphManager {

  private final ConcurrentHashMap<String, GraphIndex> graphs = new ConcurrentHashMap<>();

  public void put(Graph graph) {
    graphs.put(graph.id(), new GraphIndex(graph));
  }

  GraphIndex require(String id) {
    GraphIndex index = graphs.get(Identifiers.require(id, "id"));
    if (index == null) {
      throw new IllegalArgumentException("Graph not found: " + id);
    }
    return index;
  }

  @Override
  public CompletableFuture<Optional<Graph>> findGraph(String id) {
    Identifiers.require(id, "id");
    GraphIndex index = graphs.get(id);
    if (index == null) {
      return CompletableFuture.completedFuture(Optional.empty());
    }
    return CompletableFuture.completedFuture(Optional.of(index.graph()));
  }

  @Override
  public CompletableFuture<List<Graph>> loadedGraphs() {
    List<Graph> loaded = new ArrayList<>();
    for (GraphIndex index : graphs.values()) {
      loaded.add(index.graph());
    }
    return CompletableFuture.completedFuture(List.copyOf(loaded));
  }

  @Override
  public CompletableFuture<Void> reload(String id) {
    Identifiers.require(id, "id");
    if (!graphs.containsKey(id)) {
      return CompletableFuture.failedFuture(new IllegalArgumentException("Graph not found: " + id));
    }
    return CompletableFuture.completedFuture(null);
  }
}
