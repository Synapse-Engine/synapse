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
    SynapseEngine engine = new SynapseEngine();
    assertThrows(IllegalStateException.class, SynapseAPI::get);

    SynapseBindings.register(engine);

    assertSame(engine, SynapseAPI.get());
    assertSame(engine.getGraphManager(), SynapseAPI.get().getGraphManager());
    assertSame(engine.getPlayerStateManager(), SynapseAPI.get().getPlayerStateManager());
    assertSame(engine.getRegistryManager(), SynapseAPI.get().getRegistryManager());
    assertSame(engine.getEventManager(), SynapseAPI.get().getEventManager());
  }

  @Test
  void secondRegisterFails() {
    SynapseBindings.register(new SynapseEngine());
    assertThrows(IllegalStateException.class, () -> SynapseBindings.register(new SynapseEngine()));
  }

  @Test
  void unregisterClearsTheEngine() {
    SynapseBindings.register(new SynapseEngine());
    SynapseBindings.unregister();
    assertThrows(IllegalStateException.class, SynapseAPI::get);
  }
}
