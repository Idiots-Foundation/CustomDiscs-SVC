package space.subkek.customdiscs;

import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataType;

public class Keys {
  public static final Key<String> LOCAL_DISC = Key.create("local", PersistentDataType.STRING);
  public static final Key<String> REMOTE_DISC = Key.create("remote", PersistentDataType.STRING);

  @Deprecated(forRemoval = true)
  public static final Key<String> LEGACY_LOCAL_DISC = Key.create("customdisc", PersistentDataType.STRING);
  @Deprecated(forRemoval = true)
  public static final Key<String> LEGACY_REMOTE_DISC = Key.create("remote-customdisc", PersistentDataType.STRING);
  @Deprecated(forRemoval = true)
  public static final Key<String> LEGACY_YOUTUBE_DISC = Key.create("customdiscyt", PersistentDataType.STRING);
  @Deprecated(forRemoval = true)
  public static final Key<String> LEGACY_SOUNDCLOUD_DISC = Key.create("customdiscsc", PersistentDataType.STRING);

  public record Key<T>(NamespacedKey key, PersistentDataType<T, T> dataType) {
    public static <Z> Key<Z> create(String key, PersistentDataType<Z, Z> dataType) {
      return new Key<>(new NamespacedKey(CustomDiscs.getPlugin(), key), dataType);
    }
  }
}
