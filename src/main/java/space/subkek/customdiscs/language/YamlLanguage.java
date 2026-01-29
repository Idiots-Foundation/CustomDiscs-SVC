package space.subkek.customdiscs.language;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.simpleyaml.configuration.file.YamlFile;
import space.subkek.customdiscs.CustomDiscs;
import space.subkek.customdiscs.util.Formatter;

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
    CustomDiscs plugin = CustomDiscs.getPlugin();
    String locale = plugin.getCDConfig().getLocale();

    try {
      Path langDir = plugin.getDataFolder().toPath().resolve("language");
      Files.createDirectories(langDir);
      File langFile = langDir.resolve(String.format("%s.yml", locale)).toFile();
      boolean isNew = !langFile.exists();

      if (isNew) {
        String resourcePath = String.format("language/%s.yml", languageExists(locale) ? locale : Language.ENGLISH.getLabel());
        saveResourceSafely(resourcePath, langFile);
      }

      language.load(langFile);

      String currentVersion = plugin.getPluginMeta().getVersion();
      String fileVersion = language.getString("version", "unknown");

      if (isNew) {
        language.set("version", currentVersion);
        language.save();
      } else if (!fileVersion.equals(currentVersion)) {
        handleUpdate(langDir, langFile, locale, currentVersion);
      }
    } catch (Throwable e) {
      CustomDiscs.error("Error while loading language: ", e);
    }
  }

  private void handleUpdate(Path directory, File file, String locale, String version) throws IOException {
    String resourcePath = String.format("language/%s.yml", locale);

    YamlFile nextLang = new YamlFile();
    nextLang.load(() -> getClass().getClassLoader().getResourceAsStream(resourcePath));

    Object oldContent = language.get("language");
    Object newContent = nextLang.get("language");

    if (!Objects.equals(oldContent, newContent)) {
      String timestamp = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date());
      Path backupPath = directory.resolve(String.format("%s-%s.backup", file.getName(), timestamp));
      Files.copy(file.toPath(), backupPath, StandardCopyOption.REPLACE_EXISTING);

      saveResourceSafely(resourcePath, file);
      language.load(file);
    }

    language.set("version", version);
    language.save();
  }

  private void saveResourceSafely(String resourcePath, File outFile) throws IOException {
    try (InputStream in = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
      if (in == null) throw new IOException("Resource not found: " + resourcePath);
      Files.copy(in, outFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
    }
  }

  private String getFormattedString(String key, Object... replace) {
    return Formatter.format(language.getString(
      Formatter.format("language.{0}", key), Formatter.format("<{0}>", key)), replace);
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
    return MINIMESSAGE.deserialize(string("prefix.normal") + getFormattedString(key, replace));
  }

  public Component deserialize(String message, Object... replace) {
    return MINIMESSAGE.deserialize(Formatter.format(message, replace));
  }

  public String string(String key, Object... replace) {
    return getFormattedString(key, replace);
  }

  public boolean languageExists(String label) {
    InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream(Formatter.format("language{0}{1}.yml", File.separator, label));
    return !Objects.isNull(inputStream);
  }
}
