package com.enjin.enjincraft.spigot.conversations.factories;

import com.enjin.enjincraft.spigot.conversations.prompts.TokenTypePrompt;
import org.bukkit.conversations.ConversationFactory;
import org.bukkit.conversations.NullConversationPrefix;
import org.bukkit.conversations.Prompt;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

public class TokenCreationConversationFactory extends ConversationFactory {

    public TokenCreationConversationFactory(@NotNull Plugin plugin, Prompt prompt) {
        super(plugin);
        init(prompt);
    }

    private void init(Prompt prompt) {
        // Configure Factory
        withPrefix(new NullConversationPrefix());
        withFirstPrompt(prompt);
        withLocalEcho(false);
        withEscapeSequence("quit");
    }

}
