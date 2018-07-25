package com.enjin.enjincoin.spigot_framework.controllers;

import com.enjin.enjincoin.spigot_framework.BasePlugin;
import org.bukkit.conversations.ConversationAbandonedEvent;
import org.bukkit.conversations.ConversationAbandonedListener;
import org.bukkit.conversations.ConversationFactory;

public class ConversationManager implements ConversationAbandonedListener {

    /**
     * <p>The spigot plugin.</p>
     */
    private ConversationFactory conversationFactory;

    /**
     * <p>The spigot plugin.</p>
     */
    private BasePlugin main;

    public ConversationManager(BasePlugin main) { this.main = main; }

    @Override
    public void conversationAbandoned(ConversationAbandonedEvent conversationAbandonedEvent) {

    }
}
