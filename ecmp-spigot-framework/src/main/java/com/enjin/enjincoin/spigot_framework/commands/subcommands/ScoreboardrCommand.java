package com.enjin.enjincoin.spigot_framework.commands.subcommands;

import com.enjin.enjincoin.spigot_framework.BasePlugin;
import com.enjin.enjincoin.spigot_framework.player.MinecraftPlayer;
import com.enjin.enjincoin.spigot_framework.util.MessageUtils;
import net.kyori.text.TextComponent;
import net.kyori.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ScoreboardrCommand {

    private BasePlugin plugin;

    public ScoreboardrCommand(BasePlugin plugin) {
        this.plugin = plugin;
    }

    public void execute(CommandSender sender, String[] args) {
        if (sender instanceof Player) {
            Player player = (Player) sender;

            MinecraftPlayer mcPlayer = this.plugin.getBootstrap().getPlayerManager().getPlayer(player.getUniqueId());
            // reload/refresh user info
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                mcPlayer.reloadUser();

                Bukkit.getScheduler().runTask(plugin, () -> {
                    if (mcPlayer.showScoreboard()) {
                        mcPlayer.showScoreboard(false);
                    } else {
                        mcPlayer.showScoreboard(true);
                    }
                });
            });
        } else {
            TextComponent text = TextComponent.of("Only players can use this command.")
                    .color(TextColor.RED);
            MessageUtils.sendMessage(sender, text);
        }
    }

    private void sendMsg(CommandSender sender, String msg) {
        TextComponent text = TextComponent.of(msg)
                .color(TextColor.GOLD);
        MessageUtils.sendMessage(sender, text);
    }

}
