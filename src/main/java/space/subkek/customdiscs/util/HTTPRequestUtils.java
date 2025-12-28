package space.subkek.customdiscs.util;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.util.stream.Collectors;

public class HTTPRequestUtils {
  private static final String USER_AGENT = "Mozilla/5.0 (X11; Linux x86_64; rv:146.0) Gecko/20100101 Firefox/146.0";

  public static JSONObject getJSONResponse(String link) {
    try {
      String response = getTextResponse(link, true);
      if (response != null) {
        return (JSONObject) new JSONParser().parse(response);
      }
      return null;
    } catch (ParseException e) {
      return null;
    }
  }

  public static String getTextResponse(String link) {
    return getTextResponse(link, false);
  }

  public static String getTextResponse(String link, boolean joinLines) {
    try {
      URLConnection connection = createConnection(link);

      String delimiter = joinLines ? "" : "\n";
      try (BufferedReader reader = new BufferedReader(
          new InputStreamReader(connection.getInputStream()))) {
        return reader.lines().collect(Collectors.joining(delimiter));
      }
    } catch (IOException e) {
      return null;
    }
  }

  private static URLConnection createConnection(String link) throws IOException {
    URL url = URI.create(link).toURL();
    URLConnection connection = url.openConnection();

    connection.setUseCaches(false);
    connection.setDefaultUseCaches(false);

    connection.addRequestProperty("User-Agent", USER_AGENT);
    connection.addRequestProperty("Cache-Control", "no-cache, no-store, must-revalidate");
    connection.addRequestProperty("Pragma", "no-cache");

    return connection;
  }
}
