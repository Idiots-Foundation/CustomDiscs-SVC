package space.subkek.customdiscs.language;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.simpleyaml.configuration.file.YamlFile;
import space.subkek.customdiscs.CustomDiscs;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Objects;

public final class YamlLanguage {
  private static final MiniMessage MINIMESSAGE = MiniMessage.miniMessage();
  private final YamlFile language = new YamlFile();

  public void load() {
    final var plugin = CustomDiscs.getPlugin();
    final var locale = plugin.getCDConfig().getLocale();

    try {
      final var langDir = plugin.getDataFolder().toPath().resolve("language");
      Files.createDirectories(langDir);
      final var langFile = langDir.resolve("%s.yml".formatted(locale)).toFile();
      final var isNew = !langFile.exists();

      if (isNew) {
        final var resourcePath = "language/%s.yml".formatted(this.languageExists(locale) ? locale : Language.ENGLISH.getLabel());
        this.saveResourceSafely(resourcePath, langFile);
      }

      this.language.load(langFile);

      final var currentVersion = plugin.getPluginMeta().getVersion();
      final var fileVersion = this.language.getString("version", "unknown");

      if (isNew) {
        this.language.set("version", currentVersion);
        this.language.save(langFile);
      } else if (!fileVersion.equals(currentVersion)) {
        this.handleUpdate(langDir, langFile, locale, currentVersion);
      }
    } catch (final Throwable e) {
      CustomDiscs.error("Error while loading language: ", e);
    }
  }

  private void handleUpdate(final Path directory, final File file, final String locale, final String version) throws IOException {
    final var resourcePath = "language/%s.yml".formatted(locale);

    final var nextLang = new YamlFile();
    nextLang.load(() -> this.getClass().getClassLoader().getResourceAsStream(resourcePath));

    final var oldContent = this.language.get("language");
    final var newContent = nextLang.get("language");

    if (!Objects.equals(oldContent, newContent)) {
      final var timestamp = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date());
      final var backupPath = directory.resolve("%s-%s.backup".formatted(file.getName(), timestamp));
      Files.copy(file.toPath(), backupPath, StandardCopyOption.REPLACE_EXISTING);

      this.saveResourceSafely(resourcePath, file);
      this.language.load(file);
    }

    this.language.set("version", version);
    this.language.save(file);
  }

  private void saveResourceSafely(final String resourcePath, final File outFile) throws IOException {
    try (final var in = this.getClass().getClassLoader().getResourceAsStream(resourcePath)) {
      if (in == null) throw new IOException("Resource not found: %s".formatted(resourcePath));
      Files.copy(in, outFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
    }
  }

  private String getFormattedString(final String key, final Object... replace) {
    var result = this.language.getString("language.%s".formatted(key), "<%s>".formatted(key));
    for (var i = 0; i < replace.length; i++) {
      result = result.replace("{%d}".formatted(i), (String) replace[i]);
    }
    return result;
  }

  public Component component(final String key, final Object... replace) {
    return MINIMESSAGE.deserialize(this.getFormattedString(key, replace));
  }

  public Component component(final String key, final Component replacement) {
    return MINIMESSAGE.deserialize(this.getFormattedString(key))
      .append(Component.space())
      .append(replacement);
  }

  public Component PComponent(final String key, final Object... replace) {
    return MINIMESSAGE.deserialize(this.string("prefix") + this.getFormattedString(key, replace));
  }

  public String string(final String key, final Object... replace) {
    return this.getFormattedString(key, replace);
  }

  public boolean languageExists(final String label) {
    final var inputStream = this.getClass().getClassLoader().getResourceAsStream("language/%s.yml".formatted(label));
    return !Objects.isNull(inputStream);
  }
}
