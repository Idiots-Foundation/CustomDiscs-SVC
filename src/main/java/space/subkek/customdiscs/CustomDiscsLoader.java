package space.subkek.customdiscs;

import io.papermc.paper.plugin.loader.PluginClasspathBuilder;
import io.papermc.paper.plugin.loader.PluginLoader;
import io.papermc.paper.plugin.loader.library.impl.MavenLibraryResolver;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.repository.RemoteRepository;
import org.jetbrains.annotations.NotNull;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import space.subkek.customdiscs.util.HTTPRequestUtils;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashSet;
import java.util.Set;

@SuppressWarnings("UnstableApiUsage")
public class CustomDiscsLoader implements PluginLoader {
  private static final String LIBS_DATA_URL = "https://raw.githubusercontent.com/Idiots-Foundation/CustomDiscs-SVC/refs/heads/master/libs.json";

  @Override
  public void classloader(@NotNull PluginClasspathBuilder classpathBuilder) {
    System.setProperty("customdiscs.loader.success", "false");

    JSONObject data = HTTPRequestUtils.getJSONResponse(LIBS_DATA_URL);
    JSONObject remoteDeps = null;
    if (data != null) remoteDeps = (JSONObject) data.get("deps");

    JSONObject cache = loadCache(classpathBuilder);
    JSONObject cachedDeps = null;
    if (cache != null) cachedDeps = (JSONObject) cache.get("deps");

    JSONObject deps;
    if (remoteDeps != null) {
      deps = remoteDeps;
      if (!remoteDeps.equals(cachedDeps)) {
        saveCache(classpathBuilder, data);
      }
    } else if (cachedDeps != null) {
      deps = cachedDeps;
      classpathBuilder.getContext().getLogger().warn("Failed to fetch remote deps. Using cached deps.");
    } else {
      classpathBuilder.getContext().getLogger().error("Failed to load deps: no remote and no cached deps provided.");
      return;
    }

    Set<String> repositories = new HashSet<>();
    Set<String> dependencies = new HashSet<>();
    for (Object key : deps.keySet()) {
      repositories.add((String) key);
      JSONArray repoDeps = (JSONArray) deps.get(key);
      for (Object dep : repoDeps) {
        dependencies.add((String) dep);
      }
    }

    MavenLibraryResolver resolver = new MavenLibraryResolver();
    repositories.forEach(repository -> resolver.addRepository(new RemoteRepository.Builder(null, "default", repository).build()));
    dependencies.forEach(dependency -> resolver.addDependency(new Dependency(new DefaultArtifact(dependency), null)));
    classpathBuilder.addLibrary(resolver);

    System.setProperty("customdiscs.loader.success", "true");
  }

  private File getCacheFile(PluginClasspathBuilder classpathBuilder) {
    File dataDirectory = classpathBuilder.getContext().getDataDirectory().toFile();
    //noinspection ResultOfMethodCallIgnored
    dataDirectory.mkdirs();
    File cache = new File(dataDirectory, "cache.json");
    if (!cache.exists()) {
      try (PrintWriter pw = new PrintWriter(new OutputStreamWriter(Files.newOutputStream(cache.toPath()), StandardCharsets.UTF_8))) {
        pw.println("{}");
        pw.flush();
      } catch (Throwable e) {
        classpathBuilder.getContext().getLogger().error("Failed to create caches.json.", e);
        return null;
      }
    }
    return cache;
  }

  private JSONObject loadCache(PluginClasspathBuilder classpathBuilder) {
    File cache = getCacheFile(classpathBuilder);
    if (cache == null) return null;

    try (FileInputStream fis = new FileInputStream(cache);
         InputStreamReader isr = new InputStreamReader(fis, StandardCharsets.UTF_8)) {
      return (JSONObject) new JSONParser().parse(isr);
    } catch (Throwable e) {
      classpathBuilder.getContext().getLogger().error("Invalid caches.json. It will be reset.", e);
      return new JSONObject();
    }
  }

  private void saveCache(PluginClasspathBuilder classpathBuilder, JSONObject cacheJson) {
    File cache = getCacheFile(classpathBuilder);
    if (cache == null) return;

    try (FileOutputStream fos = new FileOutputStream(cache);
         OutputStreamWriter osw = new OutputStreamWriter(fos, StandardCharsets.UTF_8);
         PrintWriter pw = new PrintWriter(osw)) {
      pw.println(cacheJson.toJSONString());
      pw.flush();
    } catch (Throwable e) {
      classpathBuilder.getContext().getLogger().error("Failed to save caches.json.", e);
    }
  }
}
