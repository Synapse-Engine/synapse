package io.synapseproject.engine.player;

import io.synapseproject.api.model.Identifiers;
import io.synapseproject.api.model.PropertyValue;
import io.synapseproject.api.model.player.PlayerStateManager;
import io.synapseproject.api.model.player.VariableContainer;
import io.synapseproject.api.model.player.VariableScope;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import org.jspecify.annotations.NullMarked;

@NullMarked
public final class DefaultPlayerStateManager implements PlayerStateManager {

  private final Map<UUID, Map<String, PropertyValue>> player = new ConcurrentHashMap<>();
  private final Map<UUID, Map<String, PropertyValue>> session = new ConcurrentHashMap<>();
  private final Map<String, PropertyValue> global = new ConcurrentHashMap<>();

  @Override
  public CompletableFuture<VariableContainer> getVariables(UUID playerId) {
    Objects.requireNonNull(playerId, "playerId");
    return CompletableFuture.completedFuture(
        new VariableContainer(
            playerId,
            Map.copyOf(player.getOrDefault(playerId, Map.of())),
            Map.copyOf(session.getOrDefault(playerId, Map.of())),
            Map.copyOf(global)));
  }

  @Override
  public CompletableFuture<Void> setVariable(
      UUID playerId, VariableScope scope, String key, PropertyValue value) {
    Objects.requireNonNull(playerId, "playerId");
    Objects.requireNonNull(scope, "scope");
    Identifiers.require(key, "key");
    Objects.requireNonNull(value, "value");
    scopeOf(playerId, scope).put(key, value);
    return CompletableFuture.completedFuture(null);
  }

  @Override
  public CompletableFuture<Boolean> isNodeCompleted(UUID playerId, String graphId, String nodeId) {
    Objects.requireNonNull(playerId, "playerId");
    Identifiers.require(graphId, "graphId");
    Identifiers.require(nodeId, "nodeId");
    return CompletableFuture.completedFuture(false);
  }

  private Map<String, PropertyValue> scopeOf(UUID playerId, VariableScope scope) {
    return switch (scope) {
      case PLAYER -> player.computeIfAbsent(playerId, ignored -> new ConcurrentHashMap<>());
      case SESSION -> session.computeIfAbsent(playerId, ignored -> new ConcurrentHashMap<>());
      case GLOBAL -> global;
    };
  }
}
