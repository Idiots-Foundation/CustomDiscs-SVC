package space.subkek.customdiscs;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import io.papermc.paper.plugin.loader.PluginClasspathBuilder;
import io.papermc.paper.plugin.loader.PluginLoader;
import io.papermc.paper.plugin.loader.library.impl.MavenLibraryResolver;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.repository.RemoteRepository;
import org.jetbrains.annotations.NotNull;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@SuppressWarnings("UnstableApiUsage")
public class CustomDiscsLoader implements PluginLoader {
  private static final String RESOURCE_NAME = "/deps.json";
  private static final String MAVEN_CENTRAL = "maven-central";

  // From paper's original MavenLibraryResolver
  private static String getDefaultMavenCentralMirror() {
    String central = System.getenv("PAPER_DEFAULT_CENTRAL_REPOSITORY");
    if (central == null) {
      central = System.getProperty("org.bukkit.plugin.java.LibraryLoader.centralURL");
    }
    if (central == null) {
      central = "https://maven-central.storage-download.googleapis.com/maven2";
    }
    return central;
  }

  @Override
  public void classloader(@NotNull PluginClasspathBuilder classpathBuilder) {
    System.setProperty("customdiscs.loader.success", "false");

    Gson gson = new Gson();
    try (InputStream is = getClass().getResourceAsStream(RESOURCE_NAME)) {
      if (is == null) throw new FileNotFoundException(String.format("Resource not found: %s", RESOURCE_NAME));

      try (InputStreamReader isr = new InputStreamReader(is, StandardCharsets.UTF_8)) {
        Type type = new TypeToken<Map<String, List<String>>>() {
        }.getType();
        Map<String, List<String>> data = gson.fromJson(isr, type);

        if (data == null) throw new RuntimeException(String.format("%s is empty", RESOURCE_NAME));

        List<String> repositories = data.get("repositories");
        List<String> dependencies = data.get("dependencies");

        if (repositories == null || dependencies == null) {
          throw new RuntimeException(String.format("Missing 'repositories' or 'dependencies' section in %s!", RESOURCE_NAME));
        }

        MavenLibraryResolver resolver = new MavenLibraryResolver();
        repositories.forEach(url -> {
          String finalURL = url.equals(MAVEN_CENTRAL) ? getDefaultMavenCentralMirror() : url;
          String repoID = String.format("repo-%d", Math.abs(finalURL.hashCode()));

          resolver.addRepository(new RemoteRepository.Builder(repoID, "default", finalURL).build());
        });
        dependencies.forEach(dependency -> resolver.addDependency(new Dependency(new DefaultArtifact(dependency), null)));
        classpathBuilder.addLibrary(resolver);
        System.setProperty("customdiscs.loader.success", "true");
      }
    } catch (IOException e) {
      throw new RuntimeException(String.format("Failed to process %s", RESOURCE_NAME), e);
    }
  }
}
