package com.enjin.enjincraft.spigot.conversations.prompts;

import org.bukkit.conversations.ConversationContext;
import org.bukkit.conversations.FixedSetPrompt;
import org.bukkit.conversations.Prompt;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class TokenTypePrompt extends FixedSetPrompt {

    public static final String KEY = "token-nft";
    private static final String TEXT = "What type of token do you want to create? (ft/nft)";

    private final Prompt next;

    public TokenTypePrompt(Prompt next) {
        super("ft", "nft");
        this.next = next;
    }

    public TokenTypePrompt() {
        this(null);
    }

    @Nullable
    @Override
    protected Prompt acceptValidatedInput(@NotNull ConversationContext context, @NotNull String input) {
        context.setSessionData(KEY, input.equalsIgnoreCase("nft"));
        return next == null ? new TokenIdPrompt() : next;
    }

    @NotNull
    @Override
    public String getPromptText(@NotNull ConversationContext context) {
        return TEXT;
    }
}
