package space.subkek.customdiscs.util;

public class Formatter {
  public static String format(String str, Object... replace) {
    for (int i = 0; i < replace.length; i++) {
      str = str.replace("{%d}".formatted(i), replace[i].toString());
    }
    return str;
  }
}
