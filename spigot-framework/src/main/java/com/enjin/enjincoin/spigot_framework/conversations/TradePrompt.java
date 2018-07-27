package com.enjin.enjincoin.spigot_framework.conversations;

import org.bukkit.conversations.*;
import org.bukkit.plugin.Plugin;

// Each prompt is a conversation thread component...
public abstract class TradePrompt extends StringPrompt {

    private String prompt = "";

    public TradePrompt() {}

    public FirstPrompt getFirstPrompt(String prompt) {
        this.prompt = prompt;
        return new FirstPrompt(prompt);
    }

    public SecondPrompt getSecondPrompt(String prompt) {
        this.prompt = prompt;
        return new SecondPrompt(" ");
    }

    @Override
    public boolean blocksForInput(ConversationContext context) {
        return true;
    }


    public static class FirstPrompt extends TradePrompt {

        private String prompt;

        public FirstPrompt(String prompt) {
            this.prompt = prompt;
        }

        @Override
        public String getPromptText(ConversationContext context) {
            context.getForWhom().sendRawMessage("[Enjin Coin Trade Prompt]");
            return this.prompt;
        }

        @Override
        public Prompt acceptInput(ConversationContext context, String answer) {
            context.getForWhom().sendRawMessage("I think a prompt is " + answer + " as well.");
            return new SecondPrompt("[End Conversation]");
        }

//        @Override
//        protected boolean isInputValid(ConversationContext conversationContext, String s) {
//            return true;
//        }
//
//        @Override
//        protected Prompt acceptValidatedInput(ConversationContext conversationContext, String s) {
//            return null;
//        }
    }

    public static class SecondPrompt extends TradePrompt  {

        private String prompt;

        public SecondPrompt(String prompt) {
            this.prompt = prompt;
        }

        @Override
        public String getPromptText(ConversationContext conversationContext) {
            return prompt;
        }

        @Override
        public boolean blocksForInput(ConversationContext conversationContext) {
            return false;
        }

        @Override
        public Prompt acceptInput(ConversationContext conversationContext, String s) {
            return null;
        }

//        @Override
//        protected boolean isInputValid(ConversationContext conversationContext, String s) {
//            return true;
//        }
//
//        @Override
//        protected Prompt acceptValidatedInput(ConversationContext conversationContext, String s) {
//            return null;
//        }
    }
}