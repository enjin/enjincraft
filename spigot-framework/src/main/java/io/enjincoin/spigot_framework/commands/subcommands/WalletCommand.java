package io.enjincoin.spigot_framework.commands.subcommands;

import io.enjincoin.sdk.client.service.identities.vo.Identity;
import io.enjincoin.spigot_framework.BasePlugin;
import io.enjincoin.spigot_framework.inventory.WalletInventory;
import io.enjincoin.spigot_framework.util.MessageUtils;
import net.kyori.text.TextComponent;
import net.kyori.text.format.TextColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

/**
 * <p>Wallet command handler.</p>
 */
public class WalletCommand {

    /**
     * <p>The spigot plugin.</p>
     */
    private BasePlugin main;

    /**
     * <p>Wallet command handler constructor.</p>
     *
     * @param main the Spigot plugin
     */
    public WalletCommand(BasePlugin main) {
        this.main = main;
    }

    /**
     * <p>Executes and performs operations defined for the command.</p>
     *
     * @param sender the command sender
     * @param args the command arguments
     *
     * @since 1.0
     */
    public void execute(CommandSender sender, String[] args) {
        if (sender instanceof Player) {
            Player player = (Player) sender;
            Identity identity = this.main.getBootstrap().getIdentities().get(player.getUniqueId());
            if (identity != null) {
                Inventory inventory = WalletInventory.create(main, player, identity);
                player.openInventory(inventory);
            } else {
                TextComponent text = TextComponent.of("You have not linked a wallet to your account.")
                        .color(TextColor.RED);
                MessageUtils.sendMessage(sender, text);
            }
        } else {
            TextComponent text = TextComponent.of("Only players can use this command.")
                    .color(TextColor.RED);
            MessageUtils.sendMessage(sender, text);
        }
    }

}
