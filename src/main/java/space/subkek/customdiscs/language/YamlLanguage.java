package space.subkek.customdiscs.language;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.simpleyaml.configuration.file.YamlFile;
import space.subkek.customdiscs.CustomDiscs;
import space.subkek.customdiscs.util.Formatter;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Objects;

public class YamlLanguage {
  private final YamlFile language = new YamlFile();
  private final MiniMessage miniMessage = MiniMessage.miniMessage();

  @SuppressWarnings("all")
  public void init() {
    CustomDiscs plugin = CustomDiscs.getPlugin();
    try {
      File languageFolder = Path.of(plugin.getDataFolder().getPath(), "language").toFile();
      languageFolder.mkdir();
      File languageFile = new File(languageFolder, Formatter.format("{0}.yml", plugin.getCDConfig().getLocale()));
      boolean isNewFile = false;

      if (!languageFile.exists()) {
        InputStream inputStream = plugin.getClass().getClassLoader().getResourceAsStream(Formatter.format("language/{0}.yml",
            languageExists(plugin.getCDConfig().getLocale()) ? plugin.getCDConfig().getLocale() : Language.ENGLISH.getLabel()
        ));
        Files.copy(inputStream, languageFile.toPath());
        isNewFile = true;
      }

      language.load(languageFile);

      if (isNewFile) {
        language.set("version", plugin.getDescription().getVersion());
        language.save(languageFile);
      }

      if (!language.getString("version").equals(plugin.getDescription().getVersion()) || plugin.getCDConfig().isDebug()) {
        File oldLanguageFile = new File(languageFolder.getPath(), Formatter.format(
            "{0}-{1}.backup",
            languageFile.getName(),
            new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date())
        ));
        if (oldLanguageFile.exists()) oldLanguageFile.delete();
        Files.copy(languageFile.toPath(), oldLanguageFile.toPath());
        languageFile.delete();

        InputStream inputStream = plugin.getClass().getClassLoader().getResourceAsStream(Formatter.format("language{0}{1}.yml", File.separator, plugin.getCDConfig().getLocale()));
        Files.copy(inputStream, languageFile.toPath());

        Object oldLanguage = language.getMapValues(true).get("language");

        language.load(languageFile);
        language.set("version", plugin.getDescription().getVersion());
        language.save(languageFile);

        Object newLanguage = language.getMapValues(true).get("language");

        if (oldLanguage.equals(newLanguage)) {
          oldLanguageFile.delete();
        }
      }
    } catch (Throwable e) {
      CustomDiscs.error("Error while loading language: ", e);
    }
  }

  private String getFormattedString(String key, Object... replace) {
    return Formatter.format(language.getString(
        Formatter.format("language.{0}", key), Formatter.format("<{0}>", key)), replace);
  }

  public Component component(String key, Object... replace) {
    return miniMessage.deserialize(getFormattedString(key, replace));
  }

  public Component component(String key, Component replacement) {
    return miniMessage.deserialize(getFormattedString(key))
        .append(Component.space())
        .append(replacement);
  }

  public Component PComponent(String key, Object... replace) {
    return miniMessage.deserialize(string("prefix.normal") + getFormattedString(key, replace));
  }

  public Component deserialize(String message, Object... replace) {
    return miniMessage.deserialize(Formatter.format(message, replace));
  }

  public String string(String key, Object... replace) {
    return getFormattedString(key, replace);
  }

  public boolean languageExists(String label) {
    InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream(Formatter.format("language{0}{1}.yml", File.separator, label));
    return !Objects.isNull(inputStream);
  }
}
