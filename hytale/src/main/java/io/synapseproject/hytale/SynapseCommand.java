package io.synapseproject.hytale;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import io.synapseproject.api.SynapseAPI;
import org.jspecify.annotations.NullMarked;

@NullMarked
public final class SynapseCommand extends CommandBase {

  public SynapseCommand() {
    super("synapse", "Show whether the Synapse engine is loaded");
  }

  @Override
  protected void executeSync(CommandContext context) {
    String message;
    try {
      SynapseAPI.get();
      message = "Synapse is loaded.";
    } catch (IllegalStateException exception) {
      message = "Synapse is not loaded.";
    }
    context.sendMessage(Message.raw(message));
  }
}
