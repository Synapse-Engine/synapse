package io.synapseproject.engine.store;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.synapseproject.api.event.GraphTraverseEvent;
import io.synapseproject.api.event.QuestStateChangeEvent;
import io.synapseproject.api.model.PropertyValue;
import io.synapseproject.api.model.execution.NodeResult;
import io.synapseproject.api.model.graph.Edge;
import io.synapseproject.api.model.graph.Graph;
import io.synapseproject.api.model.graph.Node;
import io.synapseproject.api.model.graph.NodeType;
import io.synapseproject.api.model.graph.Port;
import io.synapseproject.api.model.graph.PortDirection;
import io.synapseproject.api.model.player.VariableContainer;
import io.synapseproject.api.model.player.VariableScope;
import io.synapseproject.engine.SynapseEngine;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SqliteStateTest {

  @TempDir Path directory;

  @Test
  void documentsRoundTripNestedValues() {
    PropertyValue value =
        new PropertyValue.MapValue(
            Map.of(
                "line",
                new PropertyValue.Text("a\nb"),
                "flag",
                new PropertyValue.Bool(false),
                "amount",
                new PropertyValue.Number(new BigDecimal("1.50")),
                "items",
                new PropertyValue.ListValue(List.of(new PropertyValue.Text("wool")))));
    assertEquals(value, Documents.decodeValue(Documents.encode(value)));

    Graph graph =
        new Graph(
            "quest",
            3,
            Map.of("a", dialogue("a")),
            List.of(),
            Map.of("note", new PropertyValue.Text("a\nb")));
    assertEquals(graph, Documents.decodeGraph(Documents.encode(graph)));
  }

  @Test
  void playerAndGlobalStateSurviveRestart() throws Exception {
    Path database = directory.resolve("synapse.db");
    UUID playerId = UUID.randomUUID();
    UUID otherId = UUID.randomUUID();
    Graph quest =
        graph(
            List.of(dialogue("a"), dialogue("b"), dialogue("c")),
            List.of(edge("ab", "a", "b"), edge("bc", "b", "c")));
    try (SynapseEngine engine = new SynapseEngine(database)) {
      engine.putGraph(quest).join();
      engine
          .getPlayerStateManager()
          .setVariable(playerId, VariableScope.PLAYER, "rank", new PropertyValue.Text("a\nb"))
          .join();
      engine
          .getPlayerStateManager()
          .setVariable(
              playerId,
              VariableScope.GLOBAL,
              "season",
              new PropertyValue.Number(new BigDecimal("1.50")))
          .join();
      engine
          .getPlayerStateManager()
          .setVariable(playerId, VariableScope.SESSION, "match", new PropertyValue.Bool(true))
          .join();
      engine.start(playerId, "quest").join();
      assertEquals(
          new PropertyValue.Bool(true),
          engine.getPlayerStateManager().getVariables(playerId).join().session().get("match"));
    }

    try (SynapseEngine engine = new SynapseEngine(database)) {
      VariableContainer variables = engine.getPlayerStateManager().getVariables(playerId).join();
      assertEquals(new PropertyValue.Text("a\nb"), variables.player().get("rank"));
      assertEquals(
          new PropertyValue.Number(new BigDecimal("1.50")), variables.global().get("season"));
      assertTrue(variables.session().isEmpty());
      assertEquals(
          new PropertyValue.Number(new BigDecimal("1.50")),
          engine.getPlayerStateManager().getVariables(otherId).join().global().get("season"));
      assertTrue(engine.getPlayerStateManager().isNodeCompleted(playerId, "quest", "c").join());
      assertFalse(engine.getPlayerStateManager().isNodeCompleted(otherId, "quest", "c").join());
      assertEquals(quest, engine.getGraphManager().findGraph("quest").join().orElseThrow());

      AtomicInteger changes = new AtomicInteger();
      try (var _ =
          engine
              .getEventManager()
              .subscribe(QuestStateChangeEvent.class, _ -> changes.incrementAndGet())) {
        assertEquals("c", engine.start(playerId, "quest").join());
        assertEquals(0, changes.get());
      }
    }
  }

  @Test
  void cursorSurvivesRestart() throws Exception {
    Path database = directory.resolve("cursor.db");
    UUID playerId = UUID.randomUUID();
    try (SynapseEngine engine = new SynapseEngine(database)) {
      engine
          .getRegistryManager()
          .registerAction("wait", _ -> CompletableFuture.completedFuture(new NodeResult.Pending()))
          .join();
      engine
          .putGraph(
              graph(
                  List.of(
                      dialogue("start"),
                      node(
                          "wait-here",
                          NodeType.ACTION,
                          Map.of("action", new PropertyValue.Text("wait")))),
                  List.of(edge("next", "start", "wait-here"))))
          .join();
      AtomicInteger traverses = new AtomicInteger();
      try (var _ =
          engine
              .getEventManager()
              .subscribe(GraphTraverseEvent.class, _ -> traverses.incrementAndGet())) {
        assertEquals("wait-here", engine.start(playerId, "quest").join());
        assertEquals(1, traverses.get());
      }
    }

    try (SynapseEngine engine = new SynapseEngine(database)) {
      engine
          .getRegistryManager()
          .registerAction("wait", _ -> CompletableFuture.completedFuture(new NodeResult.Pending()))
          .join();
      AtomicInteger traverses = new AtomicInteger();
      try (var _ =
          engine
              .getEventManager()
              .subscribe(GraphTraverseEvent.class, _ -> traverses.incrementAndGet())) {
        assertEquals("wait-here", engine.start(playerId, "quest").join());
        assertEquals(0, traverses.get());
      }
    }
  }

  private static Graph graph(List<Node> nodes, List<Edge> edges) {
    Map<String, Node> indexed = new LinkedHashMap<>();
    for (Node node : nodes) {
      indexed.put(node.id(), node);
    }
    return new Graph("quest", 1, indexed, edges, Map.of());
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
    return new Edge(id, from, "out", to, "in");
  }
}
