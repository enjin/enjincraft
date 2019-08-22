package com.enjin.ecmp.spigot.cmd;

import com.enjin.ecmp.spigot.SpigotBootstrap;
import com.enjin.ecmp.spigot.enums.Permission;
import com.enjin.ecmp.spigot.i18n.Translation;
import com.enjin.ecmp.spigot.player.EnjPlayer;
import com.enjin.ecmp.spigot.wallet.TokenWalletView;

public class CmdWallet extends EnjCommand {

    public CmdWallet(SpigotBootstrap bootstrap, EnjCommand parent) {
        super(bootstrap, parent);
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
            Translation.WALLET_NOTLINKED_SELF.send(context.sender);
            return;
        }

        TokenWalletView view = new TokenWalletView(bootstrap, enjPlayer);
        view.open(context.player);
    }

    @Override
    public Translation getUsageTranslation() {
        return Translation.COMMAND_WALLET_DESCRIPTION;
    }

}
