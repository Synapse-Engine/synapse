package io.synapseproject.engine.graph;

import io.synapseproject.api.model.Identifiers;
import io.synapseproject.api.model.graph.Graph;
import io.synapseproject.api.model.graph.GraphManager;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import org.jspecify.annotations.NullMarked;

@NullMarked
public final class DefaultGraphManager implements GraphManager {

  @Override
  public CompletableFuture<Optional<Graph>> findGraph(String id) {
    Identifiers.require(id, "id");
    return CompletableFuture.completedFuture(Optional.empty());
  }

  @Override
  public CompletableFuture<List<Graph>> loadedGraphs() {
    return CompletableFuture.completedFuture(List.of());
  }

  @Override
  public CompletableFuture<Void> reload(String id) {
    Identifiers.require(id, "id");
    return CompletableFuture.completedFuture(null);
  }
}
