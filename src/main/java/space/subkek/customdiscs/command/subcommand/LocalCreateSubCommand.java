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

import java.io.File;
import java.util.Arrays;
import java.util.List;

public final class LocalCreateSubCommand extends AbstractCommand {
  private final CustomDiscs plugin = CustomDiscs.getPlugin();

  public LocalCreateSubCommand() {
    super("local");
  }

  @Override
  public String getDescription() {
    return this.plugin.getLanguage().string("command.create.local.description");
  }

  @Override
  public String getSyntax() {
    return this.plugin.getLanguage().string("command.create.local.syntax");
  }

  @Override
  public void build(final LiteralArgumentBuilder<CommandSourceStack> builder) {
    builder.requires(source -> this.hasPermission(source.getSender()));
    builder.then(Commands.argument("filename", StringArgumentType.string())
      .suggests(this.quotedArgument(() -> {
        final var musicDataFolder = new File(this.plugin.getDataFolder(), "musicdata");
        if (!musicDataFolder.isDirectory()) {
          return List.of();
        }
        final var files = musicDataFolder.listFiles();
        if (files == null) {
          return List.of();
        }
        return Arrays.stream(files)
          .filter(file -> !file.isDirectory())
          .map(File::getName)
          .toList();
      }))
      .then(Commands.argument("song_name", StringArgumentType.string())
        .suggests(this.quotedArgument(List.of()))
        .executes(this::executePlayer)));
  }

  @Override
  public boolean hasPermission(final CommandSender sender) {
    return sender.hasPermission("customdiscs.create.local");
  }

  @SuppressWarnings("UnstableApiUsage")
  public int executePlayer(final CommandContext<CommandSourceStack> context) {
    final var sender = context.getSource().getSender();

    if (!(sender instanceof final Player player)) {
      return this.execute(context);
    }

    if (!LegacyUtil.isMusicDiscInHand(player)) {
      CustomDiscs.sendMessage(player, this.plugin.getLanguage().PComponent("command.create.messages.error.not-holding-disc"));
      return 0;
    }

    final var filename = this.getArgumentValue(context, "filename", String.class);
    if (filename.contains("../")) {
      CustomDiscs.sendMessage(player, this.plugin.getLanguage().PComponent("error.command.invalid-filename"));
      return 0;
    }

    final var customName = this.getArgumentValue(context, "song_name", String.class);
    if (customName.isEmpty()) {
      CustomDiscs.sendMessage(player, this.plugin.getLanguage().PComponent("error.command.disc-name-empty"));
      return 0;
    }

    final var getDirectory = new File(CustomDiscs.getPlugin().getDataFolder(), "musicdata");
    final var songFile = new File(getDirectory.getPath(), filename);
    if (songFile.exists()) {
      if (!this.getFileExtension(filename).equals("wav") && !this.getFileExtension(filename).equals("mp3") && !this.getFileExtension(filename).equals("flac")) {
        CustomDiscs.sendMessage(player, this.plugin.getLanguage().PComponent("error.command.unknown-extension"));
        return 0;
      }
    } else {
      CustomDiscs.sendMessage(player, this.plugin.getLanguage().PComponent("error.file.not-found"));
      return 0;
    }

    final var disc = new ItemStack(player.getInventory().getItemInMainHand());
    final var meta = LegacyUtil.getItemMeta(disc);

    meta.displayName(this.plugin.getLanguage().component("disc-name.simple")
      .decoration(TextDecoration.ITALIC, false));

    final Component customLoreSong = Component.text(customName)
      .decoration(TextDecoration.ITALIC, false)
      .color(NamedTextColor.GRAY);

    meta.addItemFlags(ItemFlag.values());
    meta.lore(List.of(customLoreSong));

    final var modelData = this.plugin.getCDConfig().getLocalCustomModelData();
    if (modelData != 0)
      disc.setData(DataComponentTypes.CUSTOM_MODEL_DATA, CustomModelData.customModelData().addFloat(modelData).build());

    final var data = meta.getPersistentDataContainer();
    for (final var key : data.getKeys()) {
      data.remove(key);
    }
    data.set(Keys.LOCAL_DISC.key(), Keys.LOCAL_DISC.dataType(), filename);

    player.getInventory().getItemInMainHand().setItemMeta(meta);

    CustomDiscs.sendMessage(player, this.plugin.getLanguage().component("command.create.messages.file", filename));
    CustomDiscs.sendMessage(player, this.plugin.getLanguage().component("command.create.messages.name", customName));

    return SINGLE_SUCCESS;
  }

  @Override
  public int execute(final CommandContext<CommandSourceStack> context) {
    CustomDiscs.sendMessage(context.getSource().getSender(), this.plugin.getLanguage().PComponent("error.command.cant-perform"));
    return 0;
  }

  private String getFileExtension(final String s) {
    final var index = s.lastIndexOf(".");
    if (index > 0) {
      return s.substring(index + 1);
    } else {
      return "";
    }
  }
}
