package io.synapseproject.engine.player;

import io.synapseproject.api.model.Identifiers;
import io.synapseproject.api.model.PropertyValue;
import io.synapseproject.api.model.player.PlayerStateManager;
import io.synapseproject.api.model.player.VariableContainer;
import io.synapseproject.api.model.player.VariableScope;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import org.jspecify.annotations.NullMarked;

@NullMarked
public final class DefaultPlayerStateManager implements PlayerStateManager {

  private final Map<UUID, Map<String, PropertyValue>> player = new ConcurrentHashMap<>();
  private final Map<UUID, Map<String, PropertyValue>> session = new ConcurrentHashMap<>();
  private final Map<String, PropertyValue> global = new ConcurrentHashMap<>();
  private final Map<String, String> cursors = new ConcurrentHashMap<>();
  private final Set<String> completed = ConcurrentHashMap.newKeySet();

  @Override
  public CompletableFuture<VariableContainer> getVariables(UUID playerId) {
    Objects.requireNonNull(playerId, "playerId");
    return CompletableFuture.completedFuture(snapshot(playerId));
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
    return CompletableFuture.completedFuture(completed(playerId, graphId, nodeId));
  }

  public VariableContainer snapshot(UUID playerId) {
    Objects.requireNonNull(playerId, "playerId");
    return new VariableContainer(
        playerId,
        Map.copyOf(player.getOrDefault(playerId, Map.of())),
        Map.copyOf(session.getOrDefault(playerId, Map.of())),
        Map.copyOf(global));
  }

  public Optional<String> cursor(UUID playerId, String graphId) {
    return Optional.ofNullable(cursors.get(key(playerId, graphId, "")));
  }

  public void place(UUID playerId, String graphId, String nodeId) {
    cursors.put(key(playerId, graphId, ""), nodeId);
  }

  public boolean completed(UUID playerId, String graphId, String nodeId) {
    return completed.contains(key(playerId, graphId, nodeId));
  }

  public boolean complete(UUID playerId, String graphId, String nodeId) {
    return completed.add(key(playerId, graphId, nodeId));
  }

  private static String key(UUID playerId, String graphId, String nodeId) {
    return playerId + "\0" + graphId + "\0" + nodeId;
  }

  private Map<String, PropertyValue> scopeOf(UUID playerId, VariableScope scope) {
    return switch (scope) {
      case PLAYER -> player.computeIfAbsent(playerId, ignored -> new ConcurrentHashMap<>());
      case SESSION -> session.computeIfAbsent(playerId, ignored -> new ConcurrentHashMap<>());
      case GLOBAL -> global;
    };
  }
}
