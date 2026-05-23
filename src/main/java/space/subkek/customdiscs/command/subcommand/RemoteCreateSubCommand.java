package space.subkek.customdiscs.command.subcommand;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.CustomModelData;
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
  }

  @Override
  public LiteralArgumentBuilder<CommandSourceStack> assemble(LiteralArgumentBuilder<CommandSourceStack> builder) {
    return builder.then(Commands.argument("url", StringArgumentType.string())
      .suggests(quotedArgument(plugin.getCDConfig().getRemoteTabComplete()))

      .then(Commands.argument("song_name", StringArgumentType.string())
        .suggests(quotedArgument(null))
        .executes(this::executePlayer)));
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
  public boolean hasPermission(CommandSender sender) {
    return sender.hasPermission("customdiscs.create.remote");
  }

  @Override
  public boolean hasPermission(CommandSender sender, RemoteServices service) {
    if (sender.isOp()) return true;
    if (service == null) return hasPermission(sender);
    return sender.hasPermission("customdiscs.create.remote.%s".formatted(service.getId()));
  }

  @SuppressWarnings("UnstableApiUsage")
  public int executePlayer(CommandContext<CommandSourceStack> context) {
    CommandSender sender = context.getSource().getSender();

    if (!(sender instanceof Player player)) {
      return execute(context);
    }

    String url = getArgumentValue(context, "url", String.class);
    RemoteServices service = RemoteServices.fromUrl(url);

    if (service == null) {
      CustomDiscs.sendMessage(player, plugin.getLanguage().PComponent("error.command.invalid-url"));
      return 0;
    }

    if (!hasPermission(player, service)) {
      CustomDiscs.sendMessage(player, plugin.getLanguage().PComponent("error.command.no-permission"));
      return 0;
    }

    if (!LegacyUtil.isMusicDiscInHand(player)) {
      CustomDiscs.sendMessage(player, plugin.getLanguage().PComponent("command.create.messages.error.not-holding-disc"));
      return 0;
    }

    String customName = getArgumentValue(context, "song_name", String.class);

    if (customName.isEmpty()) {
      CustomDiscs.sendMessage(player, plugin.getLanguage().PComponent("error.command.disc-name-empty"));
      return 0;
    }

    ItemStack disc = new ItemStack(player.getInventory().getItemInMainHand());
    ItemMeta meta = LegacyUtil.getItemMeta(disc);

    meta.displayName(plugin.getLanguage().component("disc-name.%s".formatted(service.getId()))
      .decoration(TextDecoration.ITALIC, false));

    final Component customLoreSong = Component.text(customName)
      .decoration(TextDecoration.ITALIC, false)
      .color(NamedTextColor.GRAY);

    meta.addItemFlags(ItemFlag.values());
    meta.lore(List.of(customLoreSong));

    int modelData = service.getCustomModelData();
    if (modelData != 0)
      disc.setData(DataComponentTypes.CUSTOM_MODEL_DATA, CustomModelData.customModelData().addFloat(modelData).build());

    PersistentDataContainer data = meta.getPersistentDataContainer();
    for (NamespacedKey key : data.getKeys()) {
      data.remove(key);
    }
    data.set(Keys.REMOTE_DISC.key(), Keys.REMOTE_DISC.dataType(), url);

    player.getInventory().getItemInMainHand().setItemMeta(meta);

    CustomDiscs.sendMessage(player, plugin.getLanguage().component("command.create.messages.link", url));
    CustomDiscs.sendMessage(player, plugin.getLanguage().component("command.create.messages.name", customName));

    return Command.SINGLE_SUCCESS;
  }

  @Override
  public int execute(CommandContext<CommandSourceStack> context) {
    CustomDiscs.sendMessage(context.getSource().getSender(), plugin.getLanguage().PComponent("error.command.cant-perform"));
    return 0;
  }
}
