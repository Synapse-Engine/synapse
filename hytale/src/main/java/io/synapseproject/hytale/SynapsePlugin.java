package io.synapseproject.hytale;

import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import io.synapseproject.api.SynapseBindings;
import io.synapseproject.engine.SynapseEngine;
import java.util.logging.Level;
import org.jspecify.annotations.NullMarked;

@NullMarked
public final class SynapsePlugin extends JavaPlugin {

  private final SynapseEngine engine = new SynapseEngine();

  public SynapsePlugin(JavaPluginInit init) {
    super(init);
  }

  @Override
  protected void setup() {
    getCommandRegistry().registerCommand(new SynapseCommand());
  }

  @Override
  protected void start() {
    SynapseBindings.register(engine);
    getLogger().at(Level.INFO).log("Synapse started");
  }

  @Override
  protected void shutdown() {
    SynapseBindings.unregister();
    getLogger().at(Level.INFO).log("Synapse stopped");
  }
}
