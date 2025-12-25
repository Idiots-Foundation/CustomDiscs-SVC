package io.github.subkek.customdiscs.util;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

public class HTTPRequestUtils {

  private static final AtomicLong NOP_ATOMIC_LONG = new AtomicLong();
  private static final String USER_AGENT = "Mozilla/5.0";

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

  public static boolean download(File file, String link) {
    try {
      URLConnection connection = createConnection(link);

      try (ReadableByteChannel rbc = Channels.newChannel(connection.getInputStream());
           FileChannel fos = FileChannel.open(file.toPath(),
               StandardOpenOption.CREATE,
               StandardOpenOption.TRUNCATE_EXISTING,
               StandardOpenOption.WRITE)) {
        fos.transferFrom(rbc, 0, Long.MAX_VALUE);
      }
      return true;
    } catch (IOException e) {
      return false;
    }
  }

  public static byte[] download(String link) {
    return download(link, NOP_ATOMIC_LONG);
  }

  public static byte[] download(String link, AtomicLong progressUpdate) {
    try {
      URLConnection connection = createConnection(link);

      try (InputStream is = connection.getInputStream();
           ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

        progressUpdate.set(0);
        byte[] buffer = new byte[4096];
        int bytesRead;

        while ((bytesRead = is.read(buffer)) > 0) {
          baos.write(buffer, 0, bytesRead);
          progressUpdate.set(baos.size());
        }

        return baos.toByteArray();
      }
    } catch (IOException e) {
      return null;
    }
  }

  public static long getContentSize(String link) {
    try {
      URLConnection connection = createConnection(link);

      if (connection instanceof HttpURLConnection) {
        ((HttpURLConnection) connection).setRequestMethod("HEAD");
      }

      return connection.getContentLengthLong();
    } catch (IOException e) {
      return -1;
    }
  }

  public static String getContentType(String link) {
    try {
      URLConnection connection = createConnection(link);

      if (connection instanceof HttpURLConnection) {
        ((HttpURLConnection) connection).setRequestMethod("HEAD");
      }

      return connection.getContentType();
    } catch (IOException e) {
      return "";
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