package io.synapseproject.engine.event;

import io.synapseproject.api.event.EventManager;
import io.synapseproject.api.event.SynapseEvent;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import org.jspecify.annotations.NullMarked;

@NullMarked
public final class DefaultEventManager implements EventManager {

  private final ConcurrentHashMap<Class<?>, List<Consumer<?>>> listeners =
      new ConcurrentHashMap<>();

  @Override
  public <T extends SynapseEvent> AutoCloseable subscribe(Class<T> type, Consumer<T> listener) {
    Objects.requireNonNull(type, "type");
    Objects.requireNonNull(listener, "listener");
    List<Consumer<?>> bucket =
        listeners.computeIfAbsent(type, ignored -> new CopyOnWriteArrayList<>());
    bucket.add(listener);
    return () -> bucket.remove(listener);
  }
}
