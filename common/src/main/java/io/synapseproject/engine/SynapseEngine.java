package io.synapseproject.engine;

import io.synapseproject.api.SynapseAPI;
import io.synapseproject.api.event.EventManager;
import io.synapseproject.api.model.graph.GraphManager;
import io.synapseproject.api.model.player.PlayerStateManager;
import io.synapseproject.api.model.registry.RegistryManager;
import io.synapseproject.engine.event.DefaultEventManager;
import io.synapseproject.engine.graph.DefaultGraphManager;
import io.synapseproject.engine.player.DefaultPlayerStateManager;
import io.synapseproject.engine.registry.DefaultRegistryManager;
import org.jspecify.annotations.NullMarked;

@NullMarked
public final class SynapseEngine implements SynapseAPI {

  private final GraphManager graphs = new DefaultGraphManager();
  private final PlayerStateManager players = new DefaultPlayerStateManager();
  private final RegistryManager registry = new DefaultRegistryManager();
  private final EventManager events = new DefaultEventManager();

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
