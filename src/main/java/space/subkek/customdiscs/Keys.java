package space.subkek.customdiscs;

import lombok.Getter;
import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataType;

@Getter
public class Keys {
  public static final Key<String> LOCAL_DISC = Key.create("customdisc", PersistentDataType.STRING);
  public static final Key<String> REMOTE_DISC = Key.create("remote-customdisc", PersistentDataType.STRING);
  /**
   * @deprecated All discs that rely on streaming platforms will be removed in future versions.
   * Use {@link #REMOTE_DISC} instead.
   */
  @Deprecated
  public static final Key<String> YOUTUBE_DISC = Key.create("customdiscyt", PersistentDataType.STRING);
  /**
   * @deprecated All discs that rely on streaming platforms will be removed in future versions.
   * Use {@link #REMOTE_DISC} instead.
   */
  @Deprecated
  public static final Key<String> SOUNDCLOUD_DISC = Key.create("customdiscsc", PersistentDataType.STRING);

  public record Key<T>(NamespacedKey key, PersistentDataType<T, T> dataType) {
    public static <Z> Key<Z> create(String key, PersistentDataType<Z, Z> dataType) {
      return new Key<>(new NamespacedKey(CustomDiscs.getPlugin(), key), dataType);
    }
  }
}
