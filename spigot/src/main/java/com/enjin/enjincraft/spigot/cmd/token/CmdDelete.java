package com.enjin.enjincraft.spigot.cmd.token;

import com.enjin.enjincraft.spigot.SpigotBootstrap;
import com.enjin.enjincraft.spigot.cmd.CommandContext;
import com.enjin.enjincraft.spigot.cmd.CommandRequirements;
import com.enjin.enjincraft.spigot.cmd.EnjCommand;
import com.enjin.enjincraft.spigot.cmd.SenderType;
import com.enjin.enjincraft.spigot.enums.Permission;
import com.enjin.enjincraft.spigot.i18n.Translation;
import com.enjin.enjincraft.spigot.token.TokenManager;
import com.enjin.enjincraft.spigot.token.TokenModel;
import com.enjin.enjincraft.spigot.util.TokenUtils;
import org.bukkit.command.CommandSender;

import java.util.Objects;

public class CmdDelete extends EnjCommand {

    public CmdDelete(EnjCommand parent) {
        super(parent);
        this.aliases.add("delete");
        this.requiredArgs.add("id");
        this.optionalArgs.add("index");
        this.requirements = CommandRequirements.builder()
                .withPermission(Permission.CMD_TOKEN_CREATE)
                .withAllowedSenderTypes(SenderType.PLAYER)
                .build();
    }

    @Override
    public void execute(CommandContext context) {
        String id = context.args().get(0);
        String index = context.args().size() > requiredArgs.size()
                ? context.args().get(1)
                : null;
        CommandSender sender = context.sender();

        TokenManager tokenManager = bootstrap.getTokenManager();

        TokenModel baseModel = tokenManager.getToken(id);
        if (baseModel == null) {
            Translation.COMMAND_TOKEN_NOSUCHTOKEN.send(sender);
            return;
        }

        String fullId;
        try {
            fullId = baseModel.isNonfungible()
                    ? TokenUtils.createFullId(baseModel.getId(), TokenUtils.parseIndex(Objects.requireNonNull(index)))
                    : TokenUtils.createFullId(baseModel.getId());
        } catch (NullPointerException e) {
            Translation.COMMAND_TOKEN_MUSTPASSINDEX.send(sender);
            return;
        } catch (IllegalArgumentException e) {
            Translation.COMMAND_TOKEN_INVALIDFULLID.send(sender);
            return;
        } catch (Exception e) {
            bootstrap.log(e);
            return;
        }

        int result = tokenManager.deleteTokenConf(fullId);
        switch (result) {
            case TokenManager.TOKEN_DELETE_SUCCESS:
                Translation.COMMAND_TOKEN_DELETE_SUCCESS.send(sender);
                return;
            case TokenManager.TOKEN_DELETE_FAILED:
                Translation.COMMAND_TOKEN_DELETE_FAILED.send(sender);
                return;
            case TokenManager.TOKEN_DELETE_FAILEDNFTBASE:
                Translation.COMMAND_TOKEN_DELETE_BASENFT_1.send(sender);
                Translation.COMMAND_TOKEN_DELETE_BASENFT_2.send(sender);
                return;
            default:
                bootstrap.debug(String.format("Unhandled result when deleting token (status: %d)", result));
                break;
        }
    }

    @Override
    public Translation getUsageTranslation() {
        return Translation.COMMAND_TOKEN_DELETE_DESCRIPTION;
    }

}
