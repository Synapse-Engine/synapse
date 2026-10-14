package io.synapseproject.engine.graph;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.synapseproject.api.event.NodeExecuteEvent;
import io.synapseproject.api.event.QuestStateChangeEvent;
import io.synapseproject.api.model.PropertyValue;
import io.synapseproject.api.model.execution.NodeResult;
import io.synapseproject.api.model.graph.Edge;
import io.synapseproject.api.model.graph.Graph;
import io.synapseproject.api.model.graph.Node;
import io.synapseproject.api.model.graph.NodeType;
import io.synapseproject.api.model.graph.Port;
import io.synapseproject.api.model.graph.PortDirection;
import io.synapseproject.engine.SynapseEngine;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import org.junit.jupiter.api.Test;

class GraphWalkerTest {

  @Test
  void linearGraphStopsOnTheTerminalNode() throws Exception {
    try (SynapseEngine engine = new SynapseEngine()) {
      UUID playerId = UUID.randomUUID();
      engine.putGraph(
          graph(
              "quest",
              List.of(dialogue("a"), dialogue("b"), dialogue("c")),
              List.of(edge("ab", "a", "b"), edge("bc", "b", "c"))));
      List<QuestStateChangeEvent> changes = new ArrayList<>();
      try (var _ = engine.getEventManager().subscribe(QuestStateChangeEvent.class, changes::add)) {
        assertFalse(engine.getPlayerStateManager().isNodeCompleted(playerId, "quest", "c").join());
        assertEquals("c", engine.start(playerId, "quest").join());
        assertTrue(engine.getPlayerStateManager().isNodeCompleted(playerId, "quest", "c").join());
        assertFalse(engine.getPlayerStateManager().isNodeCompleted(playerId, "quest", "a").join());
        assertEquals(List.of(new QuestStateChangeEvent(playerId, "quest", "c", true)), changes);

        assertEquals("c", engine.start(playerId, "quest").join());
        assertEquals(1, changes.size());
      }
    }
  }

  @Test
  void branchFollowsTheNamedPort() {
    try (SynapseEngine engine = new SynapseEngine()) {
      engine
          .getRegistryManager()
          .registerCondition(
              "gate", _ -> CompletableFuture.completedFuture(new NodeResult.Branch("yes")))
          .join();
      Node check =
          new Node(
              "check",
              NodeType.CONDITION,
              Map.of("condition", new PropertyValue.Text("gate")),
              List.of(
                  new Port("in", PortDirection.IN),
                  new Port("yes", PortDirection.OUT),
                  new Port("no", PortDirection.OUT)));
      engine.putGraph(
          graph(
              "quest",
              List.of(check, dialogue("yes"), dialogue("no")),
              List.of(
                  edge("yes-edge", "check", "yes", "yes"), edge("no-edge", "check", "no", "no"))));

      assertEquals("yes", engine.start(UUID.randomUUID(), "quest").join());
    }
  }

  @Test
  void cycleStopsAtTheStepBudget() throws Exception {
    try (SynapseEngine engine = new SynapseEngine()) {
      engine.putGraph(
          graph(
              "loop",
              List.of(dialogue("start"), dialogue("a"), dialogue("b")),
              List.of(edge("in", "start", "a"), edge("ab", "a", "b"), edge("ba", "b", "a"))));

      List<String> executed = new ArrayList<>();
      String nodeId;
      try (var _ =
          engine
              .getEventManager()
              .subscribe(NodeExecuteEvent.class, event -> executed.add(event.nodeId()))) {
        nodeId =
            assertTimeoutPreemptively(
                Duration.ofSeconds(2), () -> engine.start(UUID.randomUUID(), "loop").join());
      }
      assertEquals(expectedCycle(), executed);
      assertEquals(executed.getLast(), nodeId);
    }
  }

  @Test
  void pendingStaysOnTheNode() {
    try (SynapseEngine engine = new SynapseEngine()) {
      engine
          .getRegistryManager()
          .registerAction("wait", _ -> CompletableFuture.completedFuture(new NodeResult.Pending()))
          .join();
      engine.putGraph(
          graph(
              "quest",
              List.of(
                  node(
                      "wait-here",
                      NodeType.ACTION,
                      Map.of("action", new PropertyValue.Text("wait"))),
                  dialogue("after")),
              List.of(edge("next", "wait-here", "after"))));

      UUID playerId = UUID.randomUUID();
      assertEquals("wait-here", engine.start(playerId, "quest").join());
      assertFalse(
          engine.getPlayerStateManager().isNodeCompleted(playerId, "quest", "wait-here").join());
    }
  }

  @Test
  void missingGraphFails() {
    try (SynapseEngine engine = new SynapseEngine()) {
      CompletionException exception =
          assertThrows(
              CompletionException.class, () -> engine.start(UUID.randomUUID(), "missing").join());
      assertInstanceOf(IllegalArgumentException.class, exception.getCause());
    }
  }

  private static List<String> expectedCycle() {
    List<String> nodes = new ArrayList<>();
    for (int executed = 0; executed < GraphWalker.STEP_BUDGET; executed++) {
      if (executed == 0) {
        nodes.add("start");
      } else if (executed % 2 == 1) {
        nodes.add("a");
      } else {
        nodes.add("b");
      }
    }
    return nodes;
  }

  private static Graph graph(String id, List<Node> nodes, List<Edge> edges) {
    Map<String, Node> indexed = new java.util.LinkedHashMap<>();
    for (Node node : nodes) {
      indexed.put(node.id(), node);
    }
    return new Graph(id, 1, indexed, edges, Map.of());
  }

  private static Node dialogue(String id) {
    return node(id, NodeType.DIALOGUE, Map.of());
  }

  private static Node node(String id, NodeType type, Map<String, PropertyValue> properties) {
    return new Node(
        id,
        type,
        properties,
        List.of(new Port("in", PortDirection.IN), new Port("out", PortDirection.OUT)));
  }

  private static Edge edge(String id, String from, String to) {
    return edge(id, from, "out", to);
  }

  private static Edge edge(String id, String from, String fromPort, String to) {
    return new Edge(id, from, fromPort, to, "in");
  }
}
