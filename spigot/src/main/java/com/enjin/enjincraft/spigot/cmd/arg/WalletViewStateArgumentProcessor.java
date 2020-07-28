package com.enjin.enjincraft.spigot.cmd.arg;

import com.enjin.enjincraft.spigot.EnjinCraft;
import com.enjin.enjincraft.spigot.SpigotBootstrap;
import com.enjin.enjincraft.spigot.wallet.TokenWalletViewState;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.List;

public class WalletViewStateArgumentProcessor extends AbstractArgumentProcessor<TokenWalletViewState> {

    public static final WalletViewStateArgumentProcessor INSTANCE = new WalletViewStateArgumentProcessor();

    @Override
    public List<String> tab(CommandSender sender, String arg) {
        String lowerCaseArg = arg.toLowerCase();

        List<String> results = new ArrayList<>(TokenWalletViewState.values().length);
        for (TokenWalletViewState viewState : TokenWalletViewState.values()) {
            String lowerCaseState = viewState.toString().toLowerCase();
            if (lowerCaseState.startsWith(lowerCaseArg))
                results.add(lowerCaseState);
        }

        return results;
    }

    @Override
    public TokenWalletViewState parse(CommandSender sender, String arg) {
        for (TokenWalletViewState state : TokenWalletViewState.values()) {
            if (state.name().toLowerCase().equals(arg.toLowerCase()))
                return state;
        }

        return null;
    }

}
