package io.synapseproject.engine.graph;

import io.synapseproject.api.event.GraphTraverseEvent;
import io.synapseproject.api.event.NodeExecuteEvent;
import io.synapseproject.api.event.QuestStateChangeEvent;
import io.synapseproject.api.model.PropertyValue;
import io.synapseproject.api.model.execution.ExecutionContext;
import io.synapseproject.api.model.execution.NodeResult;
import io.synapseproject.api.model.graph.Edge;
import io.synapseproject.api.model.graph.Node;
import io.synapseproject.api.model.graph.NodeType;
import io.synapseproject.engine.event.DefaultEventManager;
import io.synapseproject.engine.player.DefaultPlayerStateManager;
import io.synapseproject.engine.registry.DefaultRegistryManager;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.jspecify.annotations.NullMarked;

@NullMarked
public final class GraphWalker {

  static final int STEP_BUDGET = 64;

  private final DefaultGraphManager graphs;
  private final DefaultPlayerStateManager players;
  private final DefaultRegistryManager registry;
  private final DefaultEventManager events;

  public GraphWalker(
      DefaultGraphManager graphs,
      DefaultPlayerStateManager players,
      DefaultRegistryManager registry,
      DefaultEventManager events) {
    this.graphs = graphs;
    this.players = players;
    this.registry = registry;
    this.events = events;
  }

  public CompletableFuture<String> start(UUID playerId, String graphId) {
    Objects.requireNonNull(playerId, "playerId");
    return graphs
        .require(graphId)
        .thenCompose(
            index ->
                players
                    .cursor(playerId, graphId)
                    .thenCompose(
                        cursor -> {
                          String nodeId = cursor.orElseGet(index::entry);
                          try {
                            index.graph().getNodeOrThrow(nodeId);
                          } catch (RuntimeException exception) {
                            return CompletableFuture.failedFuture(exception);
                          }
                          return walk(playerId, index, nodeId, 0);
                        }));
  }

  private CompletableFuture<String> walk(
      UUID playerId, GraphIndex index, String nodeId, int executed) {
    String graphId = index.graph().id();
    return players
        .place(playerId, graphId, nodeId)
        .thenCompose(
            ignored -> {
              if (executed >= STEP_BUDGET) {
                return CompletableFuture.completedFuture(nodeId);
              }
              return players
                  .completed(playerId, graphId, nodeId)
                  .thenCompose(
                      done -> {
                        if (done && index.edgesFrom(nodeId).isEmpty()) {
                          return CompletableFuture.completedFuture(nodeId);
                        }
                        events.publish(new NodeExecuteEvent(playerId, graphId, nodeId));
                        return evaluate(playerId, index, nodeId)
                            .thenCompose(
                                result -> advance(playerId, index, nodeId, result, executed));
                      });
            });
  }

  private CompletableFuture<NodeResult> evaluate(UUID playerId, GraphIndex index, String nodeId) {
    Node node = index.graph().getNodeOrThrow(nodeId);
    if (node.type() == NodeType.DIALOGUE || node.type() == NodeType.EVENT) {
      return CompletableFuture.completedFuture(new NodeResult.Success());
    }
    String property = node.type() == NodeType.ACTION ? "action" : "condition";
    PropertyValue value = node.properties().get(property);
    if (!(value instanceof PropertyValue.Text(String key))) {
      return CompletableFuture.completedFuture(new NodeResult.Failed("missing " + property));
    }
    return players
        .snapshot(playerId)
        .thenComposeAsync(
            variables -> {
              ExecutionContext context =
                  new ExecutionContext(playerId, index.graph().id(), nodeId, Map.of(), variables);
              if (node.type() == NodeType.ACTION) {
                return registry
                    .findAction(key)
                    .map(handler -> handler.handle(context))
                    .orElseGet(
                        () ->
                            CompletableFuture.completedFuture(
                                new NodeResult.Failed("action is not registered: " + key)));
              }
              return registry
                  .findCondition(key)
                  .map(handler -> handler.handle(context))
                  .orElseGet(
                      () ->
                          CompletableFuture.completedFuture(
                              new NodeResult.Failed("condition is not registered: " + key)));
            });
  }

  private CompletableFuture<String> advance(
      UUID playerId, GraphIndex index, String nodeId, NodeResult result, int executed) {
    Objects.requireNonNull(result, "result");
    return switch (result) {
      case NodeResult.Failed(String _), NodeResult.Pending() ->
          CompletableFuture.completedFuture(nodeId);
      case NodeResult.Success() ->
          follow(playerId, index, nodeId, index.edgesFrom(nodeId), true, executed);
      case NodeResult.Branch(String portId) ->
          follow(playerId, index, nodeId, index.edgesFromPort(nodeId, portId), false, executed);
    };
  }

  private CompletableFuture<String> follow(
      UUID playerId,
      GraphIndex index,
      String nodeId,
      List<Edge> edges,
      boolean terminal,
      int executed) {
    if (edges.isEmpty()) {
      if (!terminal) {
        return CompletableFuture.completedFuture(nodeId);
      }
      String graphId = index.graph().id();
      return players
          .complete(playerId, graphId, nodeId)
          .thenApply(
              added -> {
                if (added) {
                  events.publish(new QuestStateChangeEvent(playerId, graphId, nodeId, true));
                }
                return nodeId;
              });
    }
    if (edges.size() != 1 || executed + 1 >= STEP_BUDGET) {
      return CompletableFuture.completedFuture(nodeId);
    }
    Edge edge = edges.getFirst();
    events.publish(
        new GraphTraverseEvent(playerId, index.graph().id(), edge.fromNodeId(), edge.toNodeId()));
    return walk(playerId, index, edge.toNodeId(), executed + 1);
  }
}
