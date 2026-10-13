package io.synapseproject.api;

import org.jspecify.annotations.NullMarked;

@NullMarked
public final class SynapseBindings {

  private SynapseBindings() {}

  public static void register(SynapseAPI api) {
    SynapseProvider.register(api);
  }

  public static void unregister() {
    SynapseProvider.unregister();
  }
}
