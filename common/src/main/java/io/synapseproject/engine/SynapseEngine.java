package io.synapseproject.engine;

import io.synapseproject.api.SynapseAPI;
import io.synapseproject.api.event.EventManager;
import io.synapseproject.api.model.Identifiers;
import io.synapseproject.api.model.graph.Graph;
import io.synapseproject.api.model.graph.GraphManager;
import io.synapseproject.api.model.player.PlayerStateManager;
import io.synapseproject.api.model.registry.RegistryManager;
import io.synapseproject.engine.event.DefaultEventManager;
import io.synapseproject.engine.graph.DefaultGraphManager;
import io.synapseproject.engine.graph.GraphWalker;
import io.synapseproject.engine.player.DefaultPlayerStateManager;
import io.synapseproject.engine.registry.DefaultRegistryManager;
import io.synapseproject.engine.store.EngineStore;
import io.synapseproject.engine.store.SqliteEngineStore;
import java.nio.file.Path;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import org.jspecify.annotations.NullMarked;

@NullMarked
public final class SynapseEngine implements SynapseAPI, AutoCloseable {

  private final EngineStore store;
  private final DefaultGraphManager graphs;
  private final DefaultPlayerStateManager players;
  private final DefaultRegistryManager registry;
  private final DefaultEventManager events;
  private final GraphWalker walker;
  private boolean closed;

  public SynapseEngine() {
    this(SqliteEngineStore.open(null));
  }

  public SynapseEngine(Path database) {
    this(SqliteEngineStore.open(Objects.requireNonNull(database, "database")));
  }

  private SynapseEngine(EngineStore store) {
    this.store = store;
    graphs = new DefaultGraphManager(store);
    players = new DefaultPlayerStateManager(store);
    registry = new DefaultRegistryManager();
    events = new DefaultEventManager();
    walker = new GraphWalker(graphs, players, registry, events);
    try {
      graphs.load().join();
    } catch (RuntimeException exception) {
      try {
        store.close();
      } catch (RuntimeException close) {
        exception.addSuppressed(close);
      }
      if (exception instanceof CompletionException completion
          && completion.getCause() instanceof RuntimeException runtime) {
        throw runtime;
      }
      throw exception;
    }
  }

  public CompletableFuture<Void> putGraph(Graph graph) {
    return graphs.put(Objects.requireNonNull(graph, "graph"));
  }

  public CompletableFuture<String> start(UUID playerId, String graphId) {
    return walker.start(playerId, Identifiers.require(graphId, "graphId"));
  }

  @Override
  public GraphManager getGraphManager() {
    return graphs;
  }

  @Override
  public PlayerStateManager getPlayerStateManager() {
    return players;
  }

  @Override
  public RegistryManager getRegistryManager() {
    return registry;
  }

  @Override
  public EventManager getEventManager() {
    return events;
  }

  @Override
  public void close() {
    if (closed) {
      return;
    }
    closed = true;
    store.close();
  }
}
