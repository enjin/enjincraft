package com.enjin.ecmp.spigot.cmd;

import com.enjin.ecmp.spigot.Messages;
import com.enjin.ecmp.spigot.SpigotBootstrap;
import com.enjin.ecmp.spigot.enums.Permission;
import com.enjin.ecmp.spigot.player.EnjPlayer;
import com.enjin.ecmp.spigot.util.MessageUtils;
import com.enjin.ecmp.spigot.wallet.TokenWalletView;
import net.kyori.text.TextComponent;
import net.kyori.text.format.TextColor;
import org.bukkit.command.CommandSender;

public class CmdWallet extends EnjCommand {

    public CmdWallet(SpigotBootstrap bootstrap) {
        super(bootstrap);
        this.aliases.add("wallet");
        this.aliases.add("wal");
        this.requirements = CommandRequirements.builder()
                .withAllowedSenderTypes(SenderType.PLAYER)
                .withPermission(Permission.CMD_WALLET)
                .build();
    }

    @Override
    public void execute(CommandContext context) {
        EnjPlayer enjPlayer = context.enjPlayer;

        if (!enjPlayer.isLinked()) {
            Messages.identityNotLinked(context.sender);
            return;
        }

        TokenWalletView view = new TokenWalletView(bootstrap, enjPlayer);
        view.open(context.player);
    }

}
