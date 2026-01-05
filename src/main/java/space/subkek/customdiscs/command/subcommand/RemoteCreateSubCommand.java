package space.subkek.customdiscs.command.subcommand;

import dev.jorel.commandapi.arguments.TextArgument;
import dev.jorel.commandapi.executors.CommandArguments;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.NamespacedKey;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import space.subkek.customdiscs.CustomDiscs;
import space.subkek.customdiscs.Keys;
import space.subkek.customdiscs.command.AbstractSubCommand;
import space.subkek.customdiscs.util.LegacyUtil;
import space.subkek.customdiscs.util.RemoteServices;

import java.util.List;

public class RemoteCreateSubCommand extends AbstractSubCommand {
  private final CustomDiscs plugin = CustomDiscs.getPlugin();

  public RemoteCreateSubCommand() {
    super("remote");

    this.withFullDescription(getDescription());
    this.withUsage(getSyntax());

    this.withArguments(new TextArgument("url")
        .replaceSuggestions(quotedArgument(plugin.getCDConfig().getRemoteTabComplete())));
    this.withArguments(new TextArgument("song_name")
        .replaceSuggestions(quotedArgument(null)));
    this.executesPlayer(this::executePlayer);
    this.executes(this::execute);
  }

  @Override
  public String getDescription() {
    return plugin.getLanguage().string("command.create.remote.description");
  }

  @Override
  public String getSyntax() {
    return plugin.getLanguage().string("command.create.remote.syntax");
  }

  @Override
  public boolean hasPermission(CommandSender sender, RemoteServices service) {
    return sender.hasPermission("customdiscs.create.remote." + service.getId());
  }

  @Override
  public void executePlayer(Player player, CommandArguments arguments) {
    String url = getArgumentValue(arguments, "url", String.class);
    RemoteServices service = RemoteServices.fromUrl(url);

    if (!hasPermission(player, service)) {
      CustomDiscs.sendMessage(player, plugin.getLanguage().PComponent("error.command.no-permission"));
      return;
    }

    if (!LegacyUtil.isMusicDiscInHand(player)) {
      CustomDiscs.sendMessage(player, plugin.getLanguage().PComponent("command.create.messages.error.not-holding-disc"));
      return;
    }

    String customName = getArgumentValue(arguments, "song_name", String.class);

    if (customName.isEmpty()) {
      CustomDiscs.sendMessage(player, plugin.getLanguage().PComponent("error.command.disc-name-empty"));
      return;
    }

    ItemStack disc = new ItemStack(player.getInventory().getItemInMainHand());

    ItemMeta meta = LegacyUtil.getItemMeta(disc);

    meta.displayName(plugin.getLanguage().component("disc-name." + service.getId())
        .decoration(TextDecoration.ITALIC, false));

    final Component customLoreSong = Component.text(customName)
        .decoration(TextDecoration.ITALIC, false)
        .color(NamedTextColor.GRAY);

    meta.addItemFlags(ItemFlag.values());
    meta.lore(List.of(customLoreSong));

    int modelData = service.getCustomModelData();
    if (modelData > 0)
      meta.setCustomModelData(modelData);

    PersistentDataContainer data = meta.getPersistentDataContainer();
    for (NamespacedKey key : data.getKeys()) {
      data.remove(key);
    }
    data.set(Keys.REMOTE_DISC.key(), Keys.REMOTE_DISC.dataType(), url);

    player.getInventory().getItemInMainHand().setItemMeta(meta);

    CustomDiscs.sendMessage(player, plugin.getLanguage().component("command.create.messages.link", url));
    CustomDiscs.sendMessage(player, plugin.getLanguage().component("command.create.messages.name", customName));
  }

  @Override
  public void execute(CommandSender sender, CommandArguments arguments) {
    CustomDiscs.sendMessage(sender, plugin.getLanguage().PComponent("error.command.cant-perform"));
  }
}
