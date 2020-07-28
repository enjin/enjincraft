package com.enjin.enjincraft.spigot.cmd.arg;

import com.enjin.enjincraft.spigot.EnjinCraft;
import com.enjin.enjincraft.spigot.token.TokenManager;
import com.enjin.enjincraft.spigot.token.TokenModel;
import org.bukkit.command.CommandSender;

import java.util.List;
import java.util.stream.Collectors;

public class TokenDefinitionArgumentProcessor extends AbstractArgumentProcessor<TokenModel> {

    public static final TokenDefinitionArgumentProcessor INSTANCE = new TokenDefinitionArgumentProcessor();

    @Override
    public List<String> tab(CommandSender sender, String arg) {
        String       lowerCaseArg = arg.toLowerCase();
        TokenManager tokenManager = EnjinCraft.bootstrap().getTokenManager();
        return tokenManager.getTokenIds().stream()
                .filter(id -> id.toLowerCase().startsWith(lowerCaseArg))
                .collect(Collectors.toList());
    }

    @Override
    public TokenModel parse(CommandSender sender, String arg) {
        TokenManager tokenManager = EnjinCraft.bootstrap().getTokenManager();

        return tokenManager.getToken(arg);
    }

}
