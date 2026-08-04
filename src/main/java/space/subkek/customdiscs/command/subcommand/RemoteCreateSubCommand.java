package space.subkek.customdiscs.command.subcommand;

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
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import space.subkek.customdiscs.CustomDiscs;
import space.subkek.customdiscs.Keys;
import space.subkek.customdiscs.command.AbstractCommand;
import space.subkek.customdiscs.util.LegacyUtil;
import space.subkek.customdiscs.util.RemoteServices;

import java.util.List;

public final class RemoteCreateSubCommand extends AbstractCommand {
  private final CustomDiscs plugin = CustomDiscs.getPlugin();

  public RemoteCreateSubCommand() {
    super("remote");
  }

  @Override
  public String getDescription() {
    return this.plugin.getLanguage().string("command.create.remote.description");
  }

  @Override
  public String getSyntax() {
    return this.plugin.getLanguage().string("command.create.remote.syntax");
  }

  @Override
  public void build(final LiteralArgumentBuilder<CommandSourceStack> builder) {
    builder.requires(source -> this.hasPermission(source.getSender()));
    builder.then(Commands.argument("url", StringArgumentType.string())
      .suggests(this.quotedArgument(this.plugin.getCDConfig().getRemoteTabComplete()))
      .then(Commands.argument("song_name", StringArgumentType.string())
        .suggests(this.quotedArgument(List.of()))
        .executes(this::executePlayer)));
  }

  @Override
  public boolean hasPermission(final CommandSender sender) {
    return sender.hasPermission("customdiscs.create.remote");
  }

  @SuppressWarnings("UnstableApiUsage")
  public int executePlayer(final CommandContext<CommandSourceStack> context) {
    final var sender = context.getSource().getSender();

    if (!(sender instanceof final Player player)) {
      return this.execute(context);
    }

    final var url = this.getArgumentValue(context, "url", String.class);
    final var service = RemoteServices.fromUrl(url);

    if (service == null) {
      CustomDiscs.sendMessage(player, this.plugin.getLanguage().PComponent("error.command.invalid-url"));
      return 0;
    }

    if (!service.hasPermission(player)) {
      CustomDiscs.sendMessage(player, this.plugin.getLanguage().PComponent("error.command.no-permission"));
      return 0;
    }

    if (!LegacyUtil.isMusicDiscInHand(player)) {
      CustomDiscs.sendMessage(player, this.plugin.getLanguage().PComponent("command.create.messages.error.not-holding-disc"));
      return 0;
    }

    final var customName = this.getArgumentValue(context, "song_name", String.class);

    if (customName.isEmpty()) {
      CustomDiscs.sendMessage(player, this.plugin.getLanguage().PComponent("error.command.disc-name-empty"));
      return 0;
    }

    final var disc = new ItemStack(player.getInventory().getItemInMainHand());
    final var meta = LegacyUtil.getItemMeta(disc);

    meta.displayName(this.plugin.getLanguage().component("disc-name.%s".formatted(service.getId()))
      .decoration(TextDecoration.ITALIC, false));

    final Component customLoreSong = Component.text(customName)
      .decoration(TextDecoration.ITALIC, false)
      .color(NamedTextColor.GRAY);

    meta.addItemFlags(ItemFlag.values());
    meta.lore(List.of(customLoreSong));

    final var modelData = service.getCustomModelData();
    if (modelData != 0)
      disc.setData(DataComponentTypes.CUSTOM_MODEL_DATA, CustomModelData.customModelData().addFloat(modelData).build());

    final var data = meta.getPersistentDataContainer();
    for (final var key : data.getKeys()) {
      data.remove(key);
    }
    data.set(Keys.REMOTE_DISC.key(), Keys.REMOTE_DISC.dataType(), url);

    player.getInventory().getItemInMainHand().setItemMeta(meta);

    CustomDiscs.sendMessage(player, this.plugin.getLanguage().component("command.create.messages.link", url));
    CustomDiscs.sendMessage(player, this.plugin.getLanguage().component("command.create.messages.name", customName));

    return SINGLE_SUCCESS;
  }

  @Override
  public int execute(final CommandContext<CommandSourceStack> context) {
    CustomDiscs.sendMessage(context.getSource().getSender(), this.plugin.getLanguage().PComponent("error.command.cant-perform"));
    return 0;
  }
}
