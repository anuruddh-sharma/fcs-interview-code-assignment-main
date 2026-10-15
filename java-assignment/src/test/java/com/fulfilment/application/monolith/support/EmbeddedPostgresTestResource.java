package com.fulfilment.application.monolith.support;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Map;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

/**
 * Starts PostgreSQL for the test suite. Registered through JUnit so the database is up before
 * Quarkus decides whether to launch a Dev Services container.
 */
public class EmbeddedPostgresTestResource
    implements QuarkusTestResourceLifecycleManager, BeforeAllCallback {

  static final int PORT = 55432;

  private static EmbeddedPostgres postgres;

  static {
    startDatabase();
  }

  @Override
  public Map<String, String> start() {
    startDatabase();
    return datasourceConfig();
  }

  @Override
  public void stop() {
    // Closed by the shutdown hook when the test JVM exits.
  }

  @Override
  public void beforeAll(ExtensionContext context) {
    startDatabase();
  }

  static Map<String, String> datasourceConfig() {
    return Map.of(
        "quarkus.datasource.db-kind", "postgresql",
        "quarkus.datasource.username", "postgres",
        "quarkus.datasource.password", "postgres",
        "quarkus.datasource.jdbc.url",
            "jdbc:postgresql://localhost:" + PORT + "/postgres",
        "quarkus.datasource.devservices.enabled", "false");
  }

  private static synchronized void startDatabase() {
    if (postgres != null || isPortOpen()) {
      return;
    }
    try {
      postgres = EmbeddedPostgres.builder().setPort(PORT).start();
    } catch (IOException exception) {
      throw new UncheckedIOException("Could not start embedded PostgreSQL", exception);
    }
    Runtime.getRuntime()
        .addShutdownHook(
            new Thread(
                () -> {
                  try {
                    if (postgres != null) {
                      postgres.close();
                    }
                  } catch (IOException ignored) {
                    // The test JVM is exiting.
                  }
                }));
  }

  private static boolean isPortOpen() {
    try (Socket socket = new Socket()) {
      socket.connect(new InetSocketAddress("127.0.0.1", PORT), 300);
      return true;
    } catch (IOException exception) {
      return false;
    }
  }
}
