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
import com.enjin.enjincraft.spigot.util.MessageUtils;
import com.enjin.enjincraft.spigot.util.TokenUtils;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import java.util.Set;
import java.util.stream.Collectors;

public class CmdList extends EnjCommand {

    public CmdList(SpigotBootstrap bootstrap, EnjCommand parent) {
        super(bootstrap, parent);
        this.aliases.add("list");
        this.optionalArgs.add("id");
        this.requirements = CommandRequirements.builder()
                .withPermission(Permission.CMD_TOKEN_CREATE)
                .withAllowedSenderTypes(SenderType.PLAYER)
                .build();
    }

    @Override
    public void execute(CommandContext context) {
        String id = context.args().size() > 0
                ? context.args().get(0)
                : null;
        if (id == null)
            listBaseTokens(context.sender());
        else
            listNonfungibleInstances(context.sender(), id);
    }

    private void listBaseTokens(CommandSender sender) {
        Set<String> ids = bootstrap.getTokenManager().getTokenIds();
        if (ids.isEmpty()) {
            Translation.COMMAND_TOKEN_LIST_EMPTY.send(sender);
            return;
        }

        MessageUtils.sendString(sender,
                ChatColor.GREEN + Translation.COMMAND_TOKEN_LIST_HEADER_TOKENS.translation());
        int count = 0;
        for (String id : ids) {
            MessageUtils.sendString(sender, String.format("&a%d: &6%s",
                    count++,
                    id));
        }
    }

    private void listNonfungibleInstances(CommandSender sender, String id) {
        TokenManager tokenManager = bootstrap.getTokenManager();
        TokenModel baseModel = tokenManager.getToken(id);
        if (baseModel == null) {
            Translation.COMMAND_TOKEN_NOSUCHTOKEN.send(sender);
            return;
        }

        Set<String> instances = tokenManager.getFullIds()
                .stream()
                .filter(fullId -> {
                    String tokenId = TokenUtils.getTokenID(fullId);
                    String tokenIndex = TokenUtils.getTokenIndex(fullId);
                    return tokenId.equals(baseModel.getId()) && !tokenIndex.equals(TokenUtils.BASE_INDEX);
                })
                .collect(Collectors.toSet());
        if (instances.isEmpty()) {
            Translation.COMMAND_TOKEN_LIST_EMPTY.send(sender);
            return;
        }

        MessageUtils.sendString(sender,
                ChatColor.GREEN + Translation.COMMAND_TOKEN_LIST_HEADER_NONFUNGIBLE.translation());
        int count = 0;
        for (String fullId : instances) {
            TokenModel instance = tokenManager.getToken(fullId);
            MessageUtils.sendString(sender, String.format("&a%d: &6%s #%d",
                    count++,
                    instance.getId(),
                    TokenUtils.convertIndexToLong(instance.getIndex())));
        }
    }

    @Override
    public Translation getUsageTranslation() {
        return Translation.COMMAND_TOKEN_LIST_DESCRIPTION;
    }

}
