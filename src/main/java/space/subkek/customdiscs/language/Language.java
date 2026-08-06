package space.subkek.customdiscs.language;

import lombok.Getter;

import java.util.ArrayList;

@Getter
public enum Language {
  RUSSIAN("ru_RU"),
  ENGLISH("en_US"),
  TURKISH("tr_TR");

  private final String label;

  Language(final String title) {
    this.label = title;
  }

  public static String getAllSeparatedComma() {
    final var labels = new ArrayList<String>();
    for (final var language : values()) {
      labels.add(language.getLabel());
    }
    return String.join(", ", labels);
  }

  public static boolean isExists(final String label) {
    for (final var language : values()) {
      if (language.label.equals(label)) return true;
    }
    return false;
  }
}
