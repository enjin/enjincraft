package com.enjin.enjincraft.spigot.cmd.token;

import com.enjin.enjincraft.spigot.cmd.CommandContext;
import com.enjin.enjincraft.spigot.cmd.CommandRequirements;
import com.enjin.enjincraft.spigot.cmd.EnjCommand;
import com.enjin.enjincraft.spigot.cmd.SenderType;
import com.enjin.enjincraft.spigot.cmd.arg.WalletViewStateArgumentProcessor;
import com.enjin.enjincraft.spigot.enums.Permission;
import com.enjin.enjincraft.spigot.i18n.Translation;
import com.enjin.enjincraft.spigot.token.TokenManager;
import com.enjin.enjincraft.spigot.wallet.TokenWalletViewState;

import java.util.ArrayList;
import java.util.List;

public class CmdSetWalletView extends EnjCommand {

    public CmdSetWalletView(EnjCommand parent) {
        super(parent);
        this.aliases.add("setwalview");
        this.aliases.add("setwalletview");
        this.requiredArgs.add("id");
        this.requiredArgs.add("view");
        this.requirements = CommandRequirements.builder()
                .withPermission(Permission.CMD_TOKEN_CREATE)
                .withAllowedSenderTypes(SenderType.PLAYER)
                .build();
    }

    @Override
    public void execute(CommandContext context) {
        String id = context.args().get(0);
        String view = context.args().get(1);

        TokenWalletViewState viewState;
        try {
            viewState = TokenWalletViewState.valueOf(view.toUpperCase());
        } catch (IllegalArgumentException e) {
            Translation.COMMAND_TOKEN_SETWALLETVIEW_INVALIDVIEW.send(context.sender());
            return;
        } catch (Exception e) {
            bootstrap.log(e);
            return;
        }

        int result = bootstrap.getTokenManager().updateWalletViewState(id, viewState);
        switch (result) {
            case TokenManager.TOKEN_UPDATE_SUCCESS:
                Translation.COMMAND_TOKEN_UPDATE_SUCCESS.send(context.sender());
                break;
            case TokenManager.TOKEN_UPDATE_FAILED:
                Translation.COMMAND_TOKEN_UPDATE_FAILED.send(context.sender());
                break;
            case TokenManager.TOKEN_NOSUCHTOKEN:
                Translation.COMMAND_TOKEN_NOSUCHTOKEN.send(context.sender());
                break;
            case TokenManager.TOKEN_ISNOTBASE:
                Translation.COMMAND_TOKEN_ISNONFUNGIBLEINSTANCE.send(context.sender());
                break;
            case TokenManager.TOKEN_HASWALLETVIEWSTATE:
                Translation.COMMAND_TOKEN_SETWALLETVIEW_HAS.send(context.sender());
                break;
            default:
                bootstrap.debug(String.format("Unhandled result when setting the wallet view state (status: %d)", result));
                break;
        }
    }

    @Override
    public List<String> tab(CommandContext context) {
        if (context.args().size() == 2)
            return WalletViewStateArgumentProcessor.INSTANCE.tab(context.sender(), context.args().get(1));

        return new ArrayList<>(0);
    }

    @Override
    public Translation getUsageTranslation() {
        return Translation.COMMAND_TOKEN_SETWALLETVIEW_DESCRIPTION;
    }

}
