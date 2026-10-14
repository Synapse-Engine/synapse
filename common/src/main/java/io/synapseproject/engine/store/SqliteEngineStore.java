package io.synapseproject.engine.store;

import io.synapseproject.api.model.PropertyValue;
import io.synapseproject.api.model.graph.Graph;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Types;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Supplier;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.argument.AbstractArgumentFactory;
import org.jdbi.v3.core.argument.Argument;
import org.jdbi.v3.core.config.ConfigRegistry;
import org.jdbi.v3.sqlobject.SqlObjectPlugin;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public final class SqliteEngineStore implements EngineStore {

  private static final String SCHEMA =
      """
      CREATE TABLE IF NOT EXISTS graphs (
        id TEXT PRIMARY KEY,
        document TEXT NOT NULL
      );
      CREATE TABLE IF NOT EXISTS player_variables (
        player_id TEXT NOT NULL,
        variable_key TEXT NOT NULL,
        value TEXT NOT NULL,
        PRIMARY KEY (player_id, variable_key)
      );
      CREATE TABLE IF NOT EXISTS global_variables (
        variable_key TEXT PRIMARY KEY,
        value TEXT NOT NULL
      );
      CREATE TABLE IF NOT EXISTS completed_nodes (
        player_id TEXT NOT NULL,
        graph_id TEXT NOT NULL,
        node_id TEXT NOT NULL,
        PRIMARY KEY (player_id, graph_id, node_id)
      );
      CREATE TABLE IF NOT EXISTS cursors (
        player_id TEXT NOT NULL,
        graph_id TEXT NOT NULL,
        node_id TEXT NOT NULL,
        PRIMARY KEY (player_id, graph_id)
      );
      """;

  private final ExecutorService executor;
  private final Handle handle;
  private final StoreDao dao;
  private boolean closed;

  private SqliteEngineStore(@Nullable Path database) {
    executor = storeThread();
    handle = connect(database);
    try {
      dao = handle.attach(StoreDao.class);
    } catch (RuntimeException exception) {
      try {
        handle.close();
      } finally {
        executor.close();
      }
      throw exception;
    }
  }

  public static SqliteEngineStore open(@Nullable Path database) {
    return new SqliteEngineStore(database);
  }

  @Override
  public <T> CompletableFuture<T> call(Supplier<T> work) {
    return CompletableFuture.supplyAsync(work, executor);
  }

  @Override
  public CompletableFuture<Void> saveGraph(Graph graph) {
    return run(() -> dao.saveGraph(graph.id(), graph));
  }

  @Override
  public CompletableFuture<List<Graph>> loadGraphs() {
    return call(dao::loadGraphs);
  }

  @Override
  public CompletableFuture<Optional<Graph>> findGraph(String id) {
    return call(() -> dao.findGraph(id));
  }

  @Override
  public CompletableFuture<Void> setPlayerVariable(UUID playerId, String key, PropertyValue value) {
    return run(() -> dao.setPlayerVariable(playerId, key, value));
  }

  @Override
  public CompletableFuture<Map<String, PropertyValue>> playerVariables(UUID playerId) {
    return call(() -> dao.playerVariables(playerId));
  }

  @Override
  public CompletableFuture<Void> setGlobalVariable(String key, PropertyValue value) {
    return run(() -> dao.setGlobalVariable(key, value));
  }

  @Override
  public CompletableFuture<Map<String, PropertyValue>> globalVariables() {
    return call(dao::globalVariables);
  }

  @Override
  public CompletableFuture<Boolean> complete(UUID playerId, String graphId, String nodeId) {
    return call(() -> dao.complete(playerId, graphId, nodeId) == 1);
  }

  @Override
  public CompletableFuture<Boolean> completed(UUID playerId, String graphId, String nodeId) {
    return call(() -> dao.completed(playerId, graphId, nodeId).isPresent());
  }

  @Override
  public CompletableFuture<Void> place(UUID playerId, String graphId, String nodeId) {
    return run(() -> dao.place(playerId, graphId, nodeId));
  }

  @Override
  public CompletableFuture<Optional<String>> cursor(UUID playerId, String graphId) {
    return call(() -> dao.cursor(playerId, graphId));
  }

  @Override
  public void close() {
    if (closed) {
      return;
    }
    closed = true;
    try {
      await(executor.submit(handle::close));
    } finally {
      executor.close();
    }
  }

  private CompletableFuture<Void> run(Runnable action) {
    return CompletableFuture.runAsync(action, executor);
  }

  private Handle connect(@Nullable Path database) {
    try {
      return await(executor.submit(() -> openHandle(database)));
    } catch (RuntimeException exception) {
      executor.close();
      throw exception;
    }
  }

  private static ExecutorService storeThread() {
    return Executors.newSingleThreadExecutor(
        runnable -> {
          Thread thread = new Thread(runnable, "synapse-store");
          thread.setDaemon(true);
          return thread;
        });
  }

  private static Handle openHandle(@Nullable Path database) {
    Handle handle = database(url(database)).open();
    try {
      handle.execute(database == null ? "PRAGMA journal_mode=MEMORY" : "PRAGMA journal_mode=WAL");
      handle.execute("PRAGMA busy_timeout=5000");
      handle.createScript(SCHEMA).execute();
      return handle;
    } catch (RuntimeException exception) {
      handle.close();
      throw exception;
    }
  }

  private static Jdbi database(String url) {
    Jdbi jdbi = Jdbi.create(url).installPlugin(new SqlObjectPlugin());
    jdbi.registerColumnMapper(
        PropertyValue.class, (rows, column, _) -> Documents.decodeValue(rows.getString(column)));
    jdbi.registerColumnMapper(
        Graph.class, (rows, column, _) -> Documents.decodeGraph(rows.getString(column)));
    jdbi.registerArgument(
        new AbstractArgumentFactory<PropertyValue>(Types.VARCHAR) {
          @Override
          protected Argument build(PropertyValue value, ConfigRegistry config) {
            return (position, statement, _) ->
                statement.setString(position, Documents.encode(value));
          }
        });
    jdbi.registerArgument(
        new AbstractArgumentFactory<Graph>(Types.VARCHAR) {
          @Override
          protected Argument build(Graph value, ConfigRegistry config) {
            return (position, statement, _) ->
                statement.setString(position, Documents.encode(value));
          }
        });
    return jdbi;
  }

  private static String url(@Nullable Path database) {
    if (database == null) {
      return "jdbc:sqlite::memory:";
    }
    Path path = database.toAbsolutePath();
    Path parent = path.getParent();
    if (parent != null) {
      try {
        Files.createDirectories(parent);
      } catch (IOException exception) {
        throw new UncheckedIOException(exception);
      }
    }
    return "jdbc:sqlite:" + path.toString().replace('\\', '/');
  }

  private static <T> T await(Future<T> future) {
    try {
      return future.get();
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("interrupted while using the database", exception);
    } catch (ExecutionException exception) {
      Throwable cause = exception.getCause();
      if (cause instanceof RuntimeException runtime) {
        throw runtime;
      }
      throw new IllegalStateException("failed to use the database", cause);
    }
  }
}
