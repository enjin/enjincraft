package com.enjin.enjincraft.spigot.conversations;

import com.enjin.enjincraft.spigot.conversations.factories.TokenCreationConversationFactory;
import com.enjin.enjincraft.spigot.conversations.prompts.TokenIndexPrompt;
import com.enjin.enjincraft.spigot.conversations.prompts.TokenNicknamePrompt;
import org.bukkit.conversations.Conversable;
import org.bukkit.conversations.Conversation;
import org.bukkit.conversations.Prompt;
import org.bukkit.plugin.Plugin;

public class Conversations {

    private final TokenCreationConversationFactory tokenCreationConversationFactory;

    public Conversations(Plugin plugin, boolean nft, boolean baseExists) {
        this.tokenCreationConversationFactory = new TokenCreationConversationFactory(plugin, getStartPrompt(nft, baseExists));
    }

    public Prompt getStartPrompt(boolean nft, boolean baseExists) {
        return nft ? baseExists ? new TokenIndexPrompt() : new TokenNicknamePrompt() : new TokenNicknamePrompt();
    }

    public Conversation startTokenCreationConversation(Conversable withWhom) {
        return tokenCreationConversationFactory.buildConversation(withWhom);
    }

}
