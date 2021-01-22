package com.enjin.enjincraft.spigot.conversations.factories;

import com.enjin.enjincraft.spigot.conversations.prompts.TokenTypePrompt;
import org.bukkit.conversations.ConversationFactory;
import org.bukkit.conversations.NullConversationPrefix;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

public class TokenCreationConversationFactory extends ConversationFactory {

    public TokenCreationConversationFactory(@NotNull Plugin plugin) {
        super(plugin);
        init();
    }

    private void init() {
        // Configure Factory
        withPrefix(new NullConversationPrefix());
        withFirstPrompt(new TokenTypePrompt());
        withLocalEcho(false);
        withEscapeSequence("quit");
    }

}
