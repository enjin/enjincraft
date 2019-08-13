package com.enjin.ecmp.spigot.cmd;

import com.enjin.ecmp.spigot.SpigotBootstrap;
import com.enjin.ecmp.spigot.player.EnjPlayer;
import com.enjin.ecmp.spigot.util.MessageUtils;
import com.enjin.ecmp.spigot.wallet.TokenWalletView;
import net.kyori.text.TextComponent;
import net.kyori.text.format.TextColor;
import org.bukkit.command.CommandSender;

public class CmdWallet extends EnjCommand {

    private SpigotBootstrap bootstrap;

    public CmdWallet(SpigotBootstrap bootstrap) {
        super();
        this.bootstrap = bootstrap;
        this.aliases.add("wallet");
        this.aliases.add("wal");
    }

    @Override
    public void execute(CommandContext context) {
        EnjPlayer enjPlayer = context.enjPlayer;

        if (enjPlayer == null) return;

        CommandSender sender = context.sender;

        if (enjPlayer.isLinked()) {
            TokenWalletView view = new TokenWalletView(bootstrap, enjPlayer);
            view.open(context.player);
        } else {
            TextComponent text = TextComponent.of("You have not linked a wallet to your account.").color(TextColor.RED);
            MessageUtils.sendComponent(sender, text);
            text = TextComponent.of("Please type '/enj link' to link your account to your Enjin Wallet.").color(TextColor.RED);
            MessageUtils.sendComponent(sender, text);
        }
    }

}
