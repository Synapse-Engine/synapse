package io.synapseproject.engine.graph;

import io.synapseproject.api.model.graph.Edge;
import io.synapseproject.api.model.graph.Graph;
import io.synapseproject.api.model.graph.Node;
import io.synapseproject.api.model.graph.Port;
import io.synapseproject.api.model.graph.PortDirection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.NullMarked;

@NullMarked
final class GraphIndex {

  private final Graph graph;
  private final Map<String, List<Edge>> fromNode;
  private final Map<String, List<Edge>> fromPort;
  private final List<String> entries;

  GraphIndex(Graph graph) {
    this.graph = graph;
    Map<String, List<Edge>> nodeEdges = new HashMap<>();
    Map<String, List<Edge>> portEdges = new HashMap<>();
    for (Edge edge : graph.edges()) {
      Node from = graph.getNodeOrThrow(edge.fromNodeId());
      Node to = graph.getNodeOrThrow(edge.toNodeId());
      requirePort(from, edge.fromPortId(), PortDirection.OUT);
      requirePort(to, edge.toPortId(), PortDirection.IN);
      nodeEdges.computeIfAbsent(edge.fromNodeId(), ignored -> new ArrayList<>()).add(edge);
      portEdges
          .computeIfAbsent(
              portKey(edge.fromNodeId(), edge.fromPortId()), ignored -> new ArrayList<>())
          .add(edge);
    }
    this.fromNode = copy(nodeEdges);
    this.fromPort = copy(portEdges);
    List<String> entryIds = new ArrayList<>();
    for (String nodeId : graph.nodes().keySet()) {
      boolean incoming = false;
      for (Edge edge : graph.edges()) {
        if (edge.toNodeId().equals(nodeId)) {
          incoming = true;
          break;
        }
      }
      if (!incoming) {
        entryIds.add(nodeId);
      }
    }
    this.entries = List.copyOf(entryIds);
  }

  Graph graph() {
    return graph;
  }

  String entry() {
    if (entries.size() != 1) {
      throw new IllegalStateException(
          "graph " + graph.id() + " has " + entries.size() + " entry nodes");
    }
    return entries.getFirst();
  }

  List<Edge> edgesFrom(String nodeId) {
    return fromNode.getOrDefault(nodeId, List.of());
  }

  List<Edge> edgesFromPort(String nodeId, String portId) {
    return fromPort.getOrDefault(portKey(nodeId, portId), List.of());
  }

  private static void requirePort(Node node, String portId, PortDirection direction) {
    for (Port port : node.ports()) {
      if (port.id().equals(portId)) {
        if (port.direction() != direction) {
          throw new IllegalArgumentException(
              "port " + portId + " on " + node.id() + " is " + port.direction());
        }
        return;
      }
    }
    throw new IllegalArgumentException("port " + portId + " was not found on " + node.id());
  }

  private static String portKey(String nodeId, String portId) {
    return nodeId + "\0" + portId;
  }

  private static Map<String, List<Edge>> copy(Map<String, List<Edge>> edges) {
    Map<String, List<Edge>> copied = new HashMap<>();
    for (Map.Entry<String, List<Edge>> entry : edges.entrySet()) {
      copied.put(entry.getKey(), List.copyOf(entry.getValue()));
    }
    return Map.copyOf(copied);
  }
}
