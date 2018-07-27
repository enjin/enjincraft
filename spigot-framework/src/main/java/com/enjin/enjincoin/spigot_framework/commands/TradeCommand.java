package com.enjin.enjincoin.spigot_framework.commands;

import com.enjin.enjincoin.spigot_framework.BasePlugin;
import com.enjin.enjincoin.spigot_framework.conversations.TradePrompt;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.conversations.Conversation;
import org.bukkit.conversations.ConversationFactory;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class TradeCommand implements CommandExecutor {

    BasePlugin main;

    public TradeCommand(BasePlugin main) { this.main = main; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        ConversationFactory cf = this.main.getBootstrap().getConversationFactory();
        Conversation conv = cf.withFirstPrompt(new TradePrompt.FirstPrompt("I'm fine. How are you?"))
                .withLocalEcho(true)
                .buildConversation((Player) sender);
        conv.begin();
        return true;
    }
}
