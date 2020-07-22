package com.enjin.enjincraft.spigot.cmd;

import com.enjin.enjincraft.spigot.SpigotBootstrap;
import com.enjin.enjincraft.spigot.enums.Permission;
import com.enjin.enjincraft.spigot.i18n.Translation;
import com.enjin.enjincraft.spigot.player.EnjPlayer;
import com.enjin.enjincraft.spigot.wallet.TokenWalletView;
import org.bukkit.entity.Player;

import java.util.Objects;

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
        Player sender = Objects.requireNonNull(context.player);

        EnjPlayer senderEnjPlayer = getValidSenderEnjPlayer(context);
        if (senderEnjPlayer == null)
            return;

        new TokenWalletView(bootstrap, senderEnjPlayer).open(sender);
    }

    @Override
    public Translation getUsageTranslation() {
        return Translation.COMMAND_WALLET_DESCRIPTION;
    }

}
