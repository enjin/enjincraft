package com.enjin.enjincraft.spigot.conversations;

import com.enjin.enjincraft.spigot.conversations.factories.TokenCreationConversationFactory;
import org.bukkit.conversations.Conversable;
import org.bukkit.conversations.Conversation;
import org.bukkit.plugin.Plugin;

public class Conversations {

    private final TokenCreationConversationFactory tokenCreationConversationFactory;

    public Conversations(Plugin plugin) {
        tokenCreationConversationFactory = new TokenCreationConversationFactory(plugin);
    }

    public Conversation startTokenCreationConversation(Conversable withWhom) {
        return tokenCreationConversationFactory.buildConversation(withWhom);
    }

}
