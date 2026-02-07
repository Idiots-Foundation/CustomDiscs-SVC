package space.subkek.customdiscs.language;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.simpleyaml.configuration.file.YamlFile;
import space.subkek.customdiscs.CustomDiscs;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Objects;

public class YamlLanguage {
  private static final MiniMessage MINIMESSAGE = MiniMessage.miniMessage();
  private final YamlFile language = new YamlFile();

  public void load() {
    var plugin = CustomDiscs.getPlugin();
    var locale = plugin.getCDConfig().getLocale();

    try {
      var langDir = plugin.getDataFolder().toPath().resolve("language");
      Files.createDirectories(langDir);
      var langFile = langDir.resolve("%s.yml".formatted(locale)).toFile();
      boolean isNew = !langFile.exists();

      if (isNew) {
        var resourcePath = "language/%s.yml".formatted(languageExists(locale) ? locale : Language.ENGLISH.getLabel());
        saveResourceSafely(resourcePath, langFile);
      }

      language.load(langFile);

      var currentVersion = plugin.getPluginMeta().getVersion();
      var fileVersion = language.getString("version", "unknown");

      if (isNew) {
        language.set("version", currentVersion);
        language.save(langFile);
      } else if (!fileVersion.equals(currentVersion)) {
        handleUpdate(langDir, langFile, locale, currentVersion);
      }
    } catch (Throwable e) {
      CustomDiscs.error("Error while loading language: ", e);
    }
  }

  private void handleUpdate(Path directory, File file, String locale, String version) throws IOException {
    var resourcePath = "language/%s.yml".formatted(locale);

    var nextLang = new YamlFile();
    nextLang.load(() -> getClass().getClassLoader().getResourceAsStream(resourcePath));

    var oldContent = language.get("language");
    var newContent = nextLang.get("language");

    if (!Objects.equals(oldContent, newContent)) {
      var timestamp = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date());
      var backupPath = directory.resolve("%s-%s.backup".formatted(file.getName(), timestamp));
      Files.copy(file.toPath(), backupPath, StandardCopyOption.REPLACE_EXISTING);

      saveResourceSafely(resourcePath, file);
      language.load(file);
    }

    language.set("version", version);
    language.save(file);
  }

  private void saveResourceSafely(String resourcePath, File outFile) throws IOException {
    try (InputStream in = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
      if (in == null) throw new IOException("Resource not found: %s".formatted(resourcePath));
      Files.copy(in, outFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
    }
  }

  private String getFormattedString(String key, Object... replace) {
    var result = language.getString("language.%s".formatted(key), "<%s>".formatted(key));
    for (int i = 0; i < replace.length; i++) {
      result = result.replace("{%d}".formatted(i), (String) replace[i]);
    }
    return result;
  }

  public Component component(String key, Object... replace) {
    return MINIMESSAGE.deserialize(getFormattedString(key, replace));
  }

  public Component component(String key, Component replacement) {
    return MINIMESSAGE.deserialize(getFormattedString(key))
      .append(Component.space())
      .append(replacement);
  }

  public Component PComponent(String key, Object... replace) {
    return MINIMESSAGE.deserialize(string("prefix") + getFormattedString(key, replace));
  }

  public String string(String key, Object... replace) {
    return getFormattedString(key, replace);
  }

  public boolean languageExists(String label) {
    var inputStream = this.getClass().getClassLoader().getResourceAsStream("language/%s.yml".formatted(label));
    return !Objects.isNull(inputStream);
  }
}
