package com.enjin.enjincraft.spigot.cmd.arg;

import com.enjin.enjincraft.spigot.EnjinCraft;
import com.enjin.enjincraft.spigot.configuration.TokenManager;
import com.enjin.enjincraft.spigot.configuration.TokenModel;
import org.bukkit.command.CommandSender;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class TokenDefinitionArgumentProcessor extends AbstractArgumentProcessor<TokenModel> {

    public static final TokenDefinitionArgumentProcessor INSTANCE = new TokenDefinitionArgumentProcessor();

    @Override
    public List<String> tab(CommandSender sender, String arg) {
        String    lowerCaseArg = arg.toLowerCase();
        TokenManager tokenManager = EnjinCraft.bootstrap().get().getTokenManager();
        return tokenManager.getTokenIds().stream()
                .filter(id -> id.toLowerCase().startsWith(lowerCaseArg))
                .collect(Collectors.toList());
    }

    @Override
    public Optional<TokenModel> parse(CommandSender sender, String arg) {
        Optional<TokenModel> result = Optional.empty();
        String lowerCaseArg = arg.toLowerCase();
        TokenManager tokenManager = EnjinCraft.bootstrap().get().getTokenManager();

        for (Map.Entry<String, TokenModel> entry : tokenManager.getEntries()) {
            if (entry.getKey().toLowerCase().equals(lowerCaseArg)) {
                result = Optional.of(entry.getValue());
                break;
            }
        }

        return result;
    }

}