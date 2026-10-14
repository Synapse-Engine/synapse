package io.synapseproject.engine.registry;

import io.synapseproject.api.model.Identifiers;
import io.synapseproject.api.model.execution.CustomActionHandler;
import io.synapseproject.api.model.execution.CustomConditionHandler;
import io.synapseproject.api.model.registry.RegistryManager;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import org.jspecify.annotations.NullMarked;

@NullMarked
public final class DefaultRegistryManager implements RegistryManager {

  private final ConcurrentHashMap<String, CustomActionHandler> actions = new ConcurrentHashMap<>();
  private final ConcurrentHashMap<String, CustomConditionHandler> conditions =
      new ConcurrentHashMap<>();

  @Override
  public CompletableFuture<Void> registerAction(String key, CustomActionHandler handler) {
    return register(actions, Identifiers.require(key, "key"), handler, "action");
  }

  @Override
  public CompletableFuture<Void> unregisterAction(String key) {
    return unregister(actions, Identifiers.require(key, "key"), "action");
  }

  @Override
  public CompletableFuture<Void> registerCondition(String key, CustomConditionHandler handler) {
    return register(conditions, Identifiers.require(key, "key"), handler, "condition");
  }

  @Override
  public CompletableFuture<Void> unregisterCondition(String key) {
    return unregister(conditions, Identifiers.require(key, "key"), "condition");
  }

  public Optional<CustomActionHandler> findAction(String key) {
    return Optional.ofNullable(actions.get(key));
  }

  public Optional<CustomConditionHandler> findCondition(String key) {
    return Optional.ofNullable(conditions.get(key));
  }

  private static <T> CompletableFuture<Void> register(
      ConcurrentHashMap<String, T> handlers, String key, T handler, String kind) {
    Objects.requireNonNull(handler, "handler");
    if (handlers.putIfAbsent(key, handler) != null) {
      return CompletableFuture.failedFuture(
          new IllegalStateException(kind + " already registered: " + key));
    }
    return CompletableFuture.completedFuture(null);
  }

  private static CompletableFuture<Void> unregister(
      ConcurrentHashMap<String, ?> handlers, String key, String kind) {
    if (handlers.remove(key) == null) {
      return CompletableFuture.failedFuture(
          new IllegalArgumentException(kind + " is not registered: " + key));
    }
    return CompletableFuture.completedFuture(null);
  }
}
