package io.synapseproject.engine.player;

import io.synapseproject.api.model.Identifiers;
import io.synapseproject.api.model.PropertyValue;
import io.synapseproject.api.model.player.PlayerStateManager;
import io.synapseproject.api.model.player.VariableContainer;
import io.synapseproject.api.model.player.VariableScope;
import io.synapseproject.engine.store.EngineStore;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import org.jspecify.annotations.NullMarked;

@NullMarked
public final class DefaultPlayerStateManager implements PlayerStateManager {

  private final EngineStore store;
  private final Map<UUID, Map<String, PropertyValue>> session = new ConcurrentHashMap<>();

  public DefaultPlayerStateManager(EngineStore store) {
    this.store = Objects.requireNonNull(store, "store");
  }

  @Override
  public CompletableFuture<VariableContainer> getVariables(UUID playerId) {
    Objects.requireNonNull(playerId, "playerId");
    return snapshot(playerId);
  }

  @Override
  public CompletableFuture<Void> setVariable(
      UUID playerId, VariableScope scope, String key, PropertyValue value) {
    Objects.requireNonNull(playerId, "playerId");
    Objects.requireNonNull(scope, "scope");
    Identifiers.require(key, "key");
    Objects.requireNonNull(value, "value");
    if (scope == VariableScope.SESSION) {
      session.computeIfAbsent(playerId, ignored -> new ConcurrentHashMap<>()).put(key, value);
      return CompletableFuture.completedFuture(null);
    }
    if (scope == VariableScope.PLAYER) {
      return store.setPlayerVariable(playerId, key, value);
    }
    return store.setGlobalVariable(key, value);
  }

  @Override
  public CompletableFuture<Boolean> isNodeCompleted(UUID playerId, String graphId, String nodeId) {
    Objects.requireNonNull(playerId, "playerId");
    Identifiers.require(graphId, "graphId");
    Identifiers.require(nodeId, "nodeId");
    return completed(playerId, graphId, nodeId);
  }

  public CompletableFuture<VariableContainer> snapshot(UUID playerId) {
    Objects.requireNonNull(playerId, "playerId");
    return store
        .playerVariables(playerId)
        .thenCombine(
            store.globalVariables(),
            (player, global) ->
                new VariableContainer(
                    playerId,
                    player,
                    Map.copyOf(session.getOrDefault(playerId, Map.of())),
                    global));
  }

  public CompletableFuture<Optional<String>> cursor(UUID playerId, String graphId) {
    return store.cursor(playerId, graphId);
  }

  public CompletableFuture<Void> place(UUID playerId, String graphId, String nodeId) {
    return store.place(playerId, graphId, nodeId);
  }

  public CompletableFuture<Boolean> completed(UUID playerId, String graphId, String nodeId) {
    return store.completed(playerId, graphId, nodeId);
  }

  public CompletableFuture<Boolean> complete(UUID playerId, String graphId, String nodeId) {
    return store.complete(playerId, graphId, nodeId);
  }
}
