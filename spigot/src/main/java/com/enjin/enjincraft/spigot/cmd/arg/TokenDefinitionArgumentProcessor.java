package com.enjin.enjincraft.spigot.cmd.arg;

import com.enjin.enjincraft.spigot.EnjinCraft;
import com.enjin.enjincraft.spigot.configuration.TokenConf;
import com.enjin.enjincraft.spigot.configuration.TokenDefinition;
import org.bukkit.command.CommandSender;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class TokenDefinitionArgumentProcessor extends AbstractArgumentProcessor<TokenDefinition> {

    public static final TokenDefinitionArgumentProcessor INSTANCE = new TokenDefinitionArgumentProcessor();

    @Override
    public List<String> tab(CommandSender sender, String arg) {
        String    lowerCaseArg = arg.toLowerCase();
        TokenConf conf = EnjinCraft.bootstrap().get().getTokenConf();
        return conf.getTokens().keySet().stream()
                .filter(id -> id.toLowerCase().startsWith(lowerCaseArg))
                .collect(Collectors.toList());
    }

    @Override
    public Optional<TokenDefinition> parse(CommandSender sender, String arg) {
        Optional<TokenDefinition> result = Optional.empty();
        String lowerCaseArg = arg.toLowerCase();
        TokenConf conf = EnjinCraft.bootstrap().get().getTokenConf();

        for (Map.Entry<String, TokenDefinition> entry : conf.getTokens().entrySet()) {
            if (entry.getKey().toLowerCase().equals(lowerCaseArg)) {
                result = Optional.of(entry.getValue());
                break;
            }
        }

        return result;
    }

}
