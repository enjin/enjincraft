package com.enjin.enjincoin.spigot_framework.commands.subcommands;

import com.enjin.enjincoin.spigot_framework.BasePlugin;
import com.enjin.enjincoin.spigot_framework.player.MinecraftPlayer;
import com.enjin.enjincoin.spigot_framework.util.MessageUtils;
import net.kyori.text.TextComponent;
import net.kyori.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * <p>Balance command handler.</p>
 */
public class SidebarCommand {

    /**
     * <p>The spigot plugin.</p>
     */
    private BasePlugin main;

    /**
     * <p>Balance command handler constructor.</p>
     *
     * @param main the Spigot plugin
     */
    public SidebarCommand(BasePlugin main) {
        this.main = main;
    }

    /**
     * <p>Executes and performs operations defined for the command.</p>
     *
     * @param sender the command sender
     * @param args   the command arguments
     * @since 1.0
     */
    public void execute(CommandSender sender, String[] args) {
        if (sender instanceof Player) {
            Player player = (Player) sender;

            MinecraftPlayer mcPlayer = this.main.getBootstrap().getPlayerManager().getPlayer(player.getUniqueId());
            // reload/refresh user info
            Bukkit.getScheduler().runTaskAsynchronously(main, () -> {
                mcPlayer.reloadUser();

                Bukkit.getScheduler().runTask(main, () -> {
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
