package io.github.subkek.customdiscs;

import lombok.Getter;
import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataType;

@Getter
public class Keys {
  public static final Key<String> CUSTOM_DISC = new Key<>("customdisc", PersistentDataType.STRING);
  public static final Key<String> REMOTE_DISC = new Key<>("remote-customdisc", PersistentDataType.STRING);
  /**
   * @deprecated All discs that rely on streaming platforms will be removed in future versions.
   * Use {@link #REMOTE_DISC} instead.
   */
  @Deprecated
  public static final Key<String> YOUTUBE_DISC = new Key<>("customdiscyt", PersistentDataType.STRING);
  /**
   * @deprecated All discs that rely on streaming platforms will be removed in future versions.
   * Use {@link #REMOTE_DISC} instead.
   */
  @Deprecated
  public static final Key<String> SOUNDCLOUD_DISC = new Key<>("customdiscsc", PersistentDataType.STRING);

  @Getter
  public static class Key<T> {
    private final NamespacedKey key;
    private final PersistentDataType<T, T> dataType;

    public Key(String key, PersistentDataType<T, T> dataType) {
      this.key = new NamespacedKey(CustomDiscs.getPlugin(), key);
      this.dataType = dataType;
    }
  }
}
