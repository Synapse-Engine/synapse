package io.synapseproject.engine.store;

import io.synapseproject.api.model.PropertyValue;
import io.synapseproject.api.model.graph.Graph;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.jdbi.v3.sqlobject.config.KeyColumn;
import org.jdbi.v3.sqlobject.config.ValueColumn;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.jdbi.v3.sqlobject.statement.UseRowMapper;
import org.jspecify.annotations.NullMarked;

@NullMarked
interface StoreDao {

  @SqlUpdate(
      """
      INSERT INTO graphs(id, document) VALUES (:id, :document)
      ON CONFLICT(id) DO UPDATE SET document = excluded.document
      """)
  void saveGraph(@Bind("id") String id, @Bind("document") Graph graph);

  @SqlQuery("SELECT id, document FROM graphs")
  @UseRowMapper(GraphRows.class)
  List<Graph> loadGraphs();

  @SqlQuery("SELECT document FROM graphs WHERE id = :id")
  Optional<Graph> findGraph(@Bind("id") String id);

  @SqlUpdate(
      """
      INSERT INTO player_variables(player_id, variable_key, value)
      VALUES (:player, :key, :value)
      ON CONFLICT(player_id, variable_key) DO UPDATE SET value = excluded.value
      """)
  void setPlayerVariable(
      @Bind("player") UUID playerId, @Bind("key") String key, @Bind("value") PropertyValue value);

  @SqlQuery("SELECT variable_key, value FROM player_variables WHERE player_id = :player")
  @KeyColumn("variable_key")
  @ValueColumn("value")
  Map<String, PropertyValue> playerVariables(@Bind("player") UUID playerId);

  @SqlUpdate(
      """
      INSERT INTO global_variables(variable_key, value) VALUES (:key, :value)
      ON CONFLICT(variable_key) DO UPDATE SET value = excluded.value
      """)
  void setGlobalVariable(@Bind("key") String key, @Bind("value") PropertyValue value);

  @SqlQuery("SELECT variable_key, value FROM global_variables")
  @KeyColumn("variable_key")
  @ValueColumn("value")
  Map<String, PropertyValue> globalVariables();

  @SqlUpdate(
      """
      INSERT INTO completed_nodes(player_id, graph_id, node_id)
      VALUES (:player, :graph, :node)
      ON CONFLICT DO NOTHING
      """)
  int complete(
      @Bind("player") UUID playerId, @Bind("graph") String graphId, @Bind("node") String nodeId);

  @SqlQuery(
      """
      SELECT 1 FROM completed_nodes
      WHERE player_id = :player AND graph_id = :graph AND node_id = :node
      """)
  Optional<Integer> completed(
      @Bind("player") UUID playerId, @Bind("graph") String graphId, @Bind("node") String nodeId);

  @SqlUpdate(
      """
      INSERT INTO cursors(player_id, graph_id, node_id) VALUES (:player, :graph, :node)
      ON CONFLICT(player_id, graph_id) DO UPDATE SET node_id = excluded.node_id
      """)
  void place(
      @Bind("player") UUID playerId, @Bind("graph") String graphId, @Bind("node") String nodeId);

  @SqlQuery("SELECT node_id FROM cursors WHERE player_id = :player AND graph_id = :graph")
  Optional<String> cursor(@Bind("player") UUID playerId, @Bind("graph") String graphId);
}
