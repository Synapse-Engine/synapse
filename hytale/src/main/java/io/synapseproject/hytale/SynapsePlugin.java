package io.synapseproject.hytale;

import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import io.synapseproject.api.SynapseBindings;
import io.synapseproject.engine.SynapseEngine;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.util.logging.Level;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public final class SynapsePlugin extends JavaPlugin {

  private @Nullable SynapseEngine engine;

  public SynapsePlugin(JavaPluginInit init) {
    super(init);
  }

  @Override
  protected void setup() {
    try {
      Files.createDirectories(getDataDirectory());
      engine = new SynapseEngine(getDataDirectory().resolve("synapse.db"));
    } catch (IOException exception) {
      throw new UncheckedIOException(exception);
    }
    getCommandRegistry().registerCommand(new SynapseCommand());
  }

  @Override
  protected void start() {
    SynapseEngine running = engine;
    if (running == null) {
      throw new IllegalStateException("Synapse is not open");
    }
    SynapseBindings.register(running);
    getLogger().at(Level.INFO).log("Synapse started");
  }

  @Override
  protected void shutdown() {
    SynapseBindings.unregister();
    SynapseEngine running = engine;
    if (running != null) {
      running.close();
    }
    getLogger().at(Level.INFO).log("Synapse stopped");
  }
}
