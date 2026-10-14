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
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.jspecify.annotations.NullMarked;

@NullMarked
public final class SynapseEngine implements SynapseAPI {

  private final DefaultGraphManager graphs = new DefaultGraphManager();
  private final DefaultPlayerStateManager players = new DefaultPlayerStateManager();
  private final DefaultRegistryManager registry = new DefaultRegistryManager();
  private final DefaultEventManager events = new DefaultEventManager();
  private final GraphWalker walker = new GraphWalker(graphs, players, registry, events);

  public void putGraph(Graph graph) {
    graphs.put(Objects.requireNonNull(graph, "graph"));
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
}
