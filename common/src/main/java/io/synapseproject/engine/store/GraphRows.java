package io.synapseproject.engine.store;

import io.synapseproject.api.model.graph.Graph;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;
import org.jspecify.annotations.NullMarked;

@NullMarked
public final class GraphRows implements RowMapper<Graph> {

  @Override
  public Graph map(ResultSet rows, StatementContext context) throws SQLException {
    Graph graph = Documents.decodeGraph(rows.getString("document"));
    if (!graph.id().equals(rows.getString("id"))) {
      throw new IllegalStateException("graph id mismatch");
    }
    return graph;
  }
}
