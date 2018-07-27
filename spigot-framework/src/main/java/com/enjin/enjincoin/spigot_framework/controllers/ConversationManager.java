package com.enjin.enjincoin.spigot_framework.controllers;

import com.enjin.enjincoin.spigot_framework.BasePlugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.conversations.*;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class ConversationManager implements ConversationAbandonedListener {

    /**
     * <p>The spigot plugin.</p>
     */
    private ConversationFactory conversationFactory;

    /**
     * <p>The spigot plugin.</p>
     */
    private BasePlugin main;

    public ConversationManager(BasePlugin main) {
        this.main = main;
        conversationFactory = new ConversationFactory(this.main)
                .withModality(false)
                .withFirstPrompt(new WhichMobPrompt())
                .withPrefix(new SummoningConversationPrefix())
                .withEscapeSequence("/quit")
                .withTimeout(10)
                .thatExcludesNonPlayersWithMessage("Go away evil console!")
                .addConversationAbandonedListener(this);
    }

    public ConversationFactory getConversationFactory() { return this.conversationFactory; }

    @Override
    public void conversationAbandoned(ConversationAbandonedEvent conversationAbandonedEvent) {
        if (conversationAbandonedEvent.gracefulExit()) {
            conversationAbandonedEvent.getContext().getForWhom().sendRawMessage("Conversation exited gracefully.");
        } else {
            conversationAbandonedEvent.getContext().getForWhom().sendRawMessage("Conversation abandoned by" + conversationAbandonedEvent.getCanceller().getClass().getName());
        }
    }

    // Below are all the prompt implementations for making a conversation tree
    private class WhichMobPrompt extends FixedSetPrompt {
        public WhichMobPrompt() {
            super(EntityType.COW.name(),
                    EntityType.CHICKEN.name(),
                    EntityType.CREEPER.name(),
                    "None");
        }

        public String getPromptText(ConversationContext context) {
            return "What would you like to summon? " + formatFixedSet();
        }

        @Override
        protected Prompt acceptValidatedInput(ConversationContext context, String s) {
            if (s.equals("None")) {
                return Prompt.END_OF_CONVERSATION;
            }
            context.setSessionData("type", s);
            return new HowManyPrompt();
        }
    }

    private class HowManyPrompt extends NumericPrompt {
        public String getPromptText(ConversationContext context) {
            return "How many " + context.getSessionData("type") + "s would you like to summon?";
        }

        @Override
        protected boolean isNumberValid(ConversationContext context, Number input) {
            return input.intValue() > 0 && input.intValue() <= 20;
        }

        @Override
        protected String getFailedValidationText(ConversationContext context, Number invalidInput) {
            return "Input must be between 1 and 20.";
        }

        @Override
        protected Prompt acceptValidatedInput(ConversationContext context, Number number) {
            context.setSessionData("count", number.intValue());
            return new ForWhomPrompt(context.getPlugin());
        }
    }

    private class ForWhomPrompt extends PlayerNamePrompt {
        public ForWhomPrompt(Plugin plugin) {
            super(plugin);
        }

        public String getPromptText(ConversationContext context) {
            return "Who should receive your " + context.getSessionData("type") + "s?";
        }

        @Override
        protected Prompt acceptValidatedInput(ConversationContext context, Player player) {
            context.setSessionData("who", player);
            return new SummonedPrompt();
        }

        @Override
        protected String getFailedValidationText(ConversationContext context, String invalidInput) {
            return invalidInput + " is not online!";
        }
    }

    private class SummonedPrompt extends MessagePrompt {
        public String getPromptText(ConversationContext context) {
            Player who = (Player)context.getSessionData("who");
            String what = (String)context.getSessionData("type");
            Integer count = (Integer)context.getSessionData("count");

            for (int i = 0; i < count; i++) {
                World world = who.getWorld();
                world.spawnEntity(who.getLocation(), EntityType.valueOf(what));
            }

            return "Engage!";
        }

        @Override
        protected Prompt getNextPrompt(ConversationContext context) {
            return Prompt.END_OF_CONVERSATION;
        }
    }

    private class SummoningConversationPrefix implements ConversationPrefix {

        public String getPrefix(ConversationContext context) {
            String what = (String)context.getSessionData("type");
            Integer count = (Integer)context.getSessionData("count");
            Player who = (Player)context.getSessionData("who");

            if (what != null && count == null && who == null) {
                return ChatColor.GREEN + "Summon " + what + ": " + ChatColor.WHITE;
            }
            if (what != null && count != null && who == null) {
                return ChatColor.GREEN + "Summon " + count + " " + what + ": " + ChatColor.WHITE;
            }
            if (what != null && count != null && who != null) {
                return ChatColor.GREEN + "Summon " + count + " " + what + " to " + who.getName() + ": " + ChatColor.WHITE;
            }
            return ChatColor.GREEN + "Summon: " + ChatColor.WHITE;
        }
    }

    public class BaseConversationPrefix implements ConversationPrefix {
        public String getPrefix(ConversationContext context) {
            String what = (String)context.getSessionData("type");
            Integer count = (Integer)context.getSessionData("count");
            Player who = (Player)context.getSessionData("who");

            if (what != null && count == null && who == null) {
                return ChatColor.GREEN + "Summon " + what + ": " + ChatColor.WHITE;
            }
            if (what != null && count != null && who == null) {
                return ChatColor.GREEN + "Summon " + count + " " + what + ": " + ChatColor.WHITE;
            }
            if (what != null && count != null && who != null) {
                return ChatColor.GREEN + "Summon " + count + " " + what + " to " + who.getName() + ": " + ChatColor.WHITE;
            }
            return ChatColor.GREEN + "Summon: " + ChatColor.WHITE;
        }
    }
}
