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
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@SuppressWarnings("UnstableApiUsage")
public class CustomDiscsLoader implements PluginLoader {
  private static final String RESOURCE_NAME = "/deps.json";
  private static final String MAVEN_CENTRAL = "maven-central";

  private static String getDefaultMavenCentralMirror() {
    try {
      final var field = MavenLibraryResolver.class.getDeclaredField("MAVEN_CENTRAL_DEFAULT_MIRROR");
      field.setAccessible(true);
      return (String) field.get(null);
    } catch (final NoSuchFieldException | IllegalAccessException ignored) {
    }

    // Official Google mirror by default
    return "https://maven-central.storage-download.googleapis.com/maven2";
  }

  @Override
  public void classloader(@NotNull final PluginClasspathBuilder classpathBuilder) {
    System.setProperty("customdiscs.loader.success", "false");

    final var gson = new Gson();
    try (final var is = this.getClass().getResourceAsStream(RESOURCE_NAME)) {
      if (is == null) throw new FileNotFoundException("Resource not found: %s".formatted(RESOURCE_NAME));

      try (final var isr = new InputStreamReader(is, StandardCharsets.UTF_8)) {
        final var type = new TypeToken<Map<String, List<String>>>() {
        }.getType();
        final Map<String, List<String>> data = gson.fromJson(isr, type);

        if (data == null) throw new RuntimeException("%s is empty".formatted(RESOURCE_NAME));

        final var repositories = data.get("repositories");
        final var dependencies = data.get("dependencies");

        if (repositories == null || dependencies == null) {
          throw new RuntimeException("Missing 'repositories' or 'dependencies' section in %s!".formatted(RESOURCE_NAME));
        }

        final var resolver = new MavenLibraryResolver();
        repositories.forEach(url -> {
          final var finalURL = url.equals(MAVEN_CENTRAL) ? getDefaultMavenCentralMirror() : url;
          final var repoID = "repo-%d".formatted(Math.abs(finalURL.hashCode()));

          resolver.addRepository(new RemoteRepository.Builder(repoID, "default", finalURL).build());
        });
        dependencies.forEach(dependency -> resolver.addDependency(new Dependency(new DefaultArtifact(dependency), null)));
        classpathBuilder.addLibrary(resolver);
        System.setProperty("customdiscs.loader.success", "true");
      }
    } catch (final IOException e) {
      throw new RuntimeException("Failed to process %s".formatted(RESOURCE_NAME), e);
    }
  }
}
