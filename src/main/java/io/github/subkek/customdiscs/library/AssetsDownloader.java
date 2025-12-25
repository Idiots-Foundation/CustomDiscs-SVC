package io.github.subkek.customdiscs.library;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParser;
import io.github.subkek.customdiscs.CustomDiscs;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.File;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

@SuppressWarnings("all")
public class AssetsDownloader {
  private static final CustomDiscs plugin = CustomDiscs.getPlugin();

  public static void loadLibraries(File rootFolder) {
    try {
      File hashes = new File(rootFolder, "hashes.json");
      if (!hashes.exists()) {
        try (PrintWriter pw = new PrintWriter(new OutputStreamWriter(Files.newOutputStream(hashes.toPath()), StandardCharsets.UTF_8))) {
          pw.println("{}");
          pw.flush();
        } catch (Exception e) {
          e.printStackTrace();
        }
      }

      JSONObject json;
      try (InputStreamReader hashReader = new InputStreamReader(Files.newInputStream(hashes.toPath()), StandardCharsets.UTF_8)) {
        json = (JSONObject) new JSONParser().parse(hashReader);
      } catch (Throwable e) {
        new RuntimeException("Invalid hashes.json! It will be reset.", e).printStackTrace();
        json = new JSONObject();
      }
      String oldHash =  json.containsKey("libs") ? json.get("libs").toString() : "EMPTY";
      String oldVersion = json.containsKey("version") ? json.get("version").toString() : "EMPTY";

      File libsFolder = new File(rootFolder, ".libs");
      libsFolder.mkdirs();

      LibraryDownloadManager downloadManager = new LibraryDownloadManager(libsFolder);

      String hash = "N/A";
      try {
        hash = downloadManager.getHash();

        if (!hash.equals(oldHash) || !plugin.getDescription().getVersion().equals(oldVersion)) {
          downloadManager.downloadLibraries((result, jarName, percentage) -> {
            if (result) {
              CustomDiscs.info("Downloaded library \"{0}\"", jarName);
            } else {
              CustomDiscs.error("Unable to download library \"{0}\"", jarName);
            }
          });
        }
      } catch (Throwable e) {
        plugin.getSLF4JLogger().error("Error while downloading libraries");
        e.printStackTrace();
      }

      LibraryLoader.loadLibraries(libsFolder, (file, e) -> {
        String jarName = file.getName();
        if (e == null) {
          CustomDiscs.info("Loaded library \"{0}\"", jarName);
        } else {
          CustomDiscs.error("Unable to load library \"{0}\"", jarName);
          e.printStackTrace();
        }
      });

      json.put("libs", hash);
      json.put("version", plugin.getDescription().getVersion());

      try (PrintWriter pw = new PrintWriter(new OutputStreamWriter(Files.newOutputStream(hashes.toPath()), StandardCharsets.UTF_8))) {
        Gson g = new GsonBuilder().setPrettyPrinting().create();
        pw.println(g.toJson(new JsonParser().parse(json.toString())));
        pw.flush();
      } catch (Exception e) {
        e.printStackTrace();
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}