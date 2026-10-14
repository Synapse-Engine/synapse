package io.synapseproject.engine;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.synapseproject.api.SynapseAPI;
import io.synapseproject.api.SynapseBindings;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class SynapseEngineTest {

  @AfterEach
  void reset() {
    SynapseBindings.unregister();
  }

  @Test
  void registerExposesTheEngine() {
    try (SynapseEngine engine = new SynapseEngine()) {
      assertThrows(IllegalStateException.class, SynapseAPI::get);

      SynapseBindings.register(engine);

      assertSame(engine, SynapseAPI.get());
      assertSame(engine.getGraphManager(), SynapseAPI.get().getGraphManager());
      assertSame(engine.getPlayerStateManager(), SynapseAPI.get().getPlayerStateManager());
      assertSame(engine.getRegistryManager(), SynapseAPI.get().getRegistryManager());
      assertSame(engine.getEventManager(), SynapseAPI.get().getEventManager());

      SynapseBindings.unregister();
      assertThrows(IllegalStateException.class, SynapseAPI::get);
    }
  }

  @Test
  void secondRegisterFails() {
    try (SynapseEngine first = new SynapseEngine();
        SynapseEngine second = new SynapseEngine()) {
      SynapseBindings.register(first);
      assertThrows(IllegalStateException.class, () -> SynapseBindings.register(second));
    }
  }

  @Test
  void unregisterClearsTheEngine() {
    try (SynapseEngine engine = new SynapseEngine()) {
      SynapseBindings.register(engine);
    }
    SynapseBindings.unregister();
    assertThrows(IllegalStateException.class, SynapseAPI::get);
  }
}
