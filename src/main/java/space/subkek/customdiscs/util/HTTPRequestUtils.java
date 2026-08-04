package space.subkek.customdiscs.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URLConnection;
import java.util.stream.Collectors;

public final class HTTPRequestUtils {
  private static final String USER_AGENT = "Mozilla/5.0 (X11; Linux x86_64; rv:146.0) Gecko/20100101 Firefox/146.0";

  public static String getTextResponse(final String link) {
    return getTextResponse(link, false);
  }

  public static String getTextResponse(final String link, final boolean joinLines) {
    try {
      final var connection = createConnection(link);

      final var delimiter = joinLines ? "" : "\n";
      try (final var reader = new BufferedReader(
        new InputStreamReader(connection.getInputStream()))) {
        return reader.lines().collect(Collectors.joining(delimiter));
      }
    } catch (final IOException e) {
      return null;
    }
  }

  private static URLConnection createConnection(final String link) throws IOException {
    final var url = URI.create(link).toURL();
    final var protocol = url.getProtocol();
    if (!protocol.equals("http") && !protocol.equals("https")) {
      throw new IOException("Only http/https schemes are allowed");
    }
    final var connection = url.openConnection();

    connection.setUseCaches(false);
    connection.setDefaultUseCaches(false);

    connection.addRequestProperty("User-Agent", USER_AGENT);
    connection.addRequestProperty("Cache-Control", "no-cache, no-store, must-revalidate");
    connection.addRequestProperty("Pragma", "no-cache");

    return connection;
  }
}
