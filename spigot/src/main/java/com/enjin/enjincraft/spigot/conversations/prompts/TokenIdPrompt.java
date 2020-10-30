package com.enjin.enjincraft.spigot.conversations.prompts;

import com.enjin.enjincraft.spigot.util.ValidationUtils;
import org.bukkit.conversations.ConversationContext;
import org.bukkit.conversations.Prompt;
import org.bukkit.conversations.RegexPrompt;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class TokenIdPrompt extends RegexPrompt {

    public static final String KEY = "token-id";
    private static final String TEXT = "Enter the token id:";

    private final Prompt next;

    public TokenIdPrompt(Prompt next) {
        super(ValidationUtils.TOKEN_ID_PATTERN);
        this.next = next;
    }

    public TokenIdPrompt() {
        this(null);
    }

    @Nullable
    @Override
    protected Prompt acceptValidatedInput(@NotNull ConversationContext context, @NotNull String input) {
        context.setSessionData(KEY, ValidationUtils.getValidTokenId(input));

        Prompt next = this.next;

        if (next == null) {
            next = ((boolean) context.getAllSessionData().getOrDefault(TokenTypePrompt.KEY, false))
                    ? new TokenIndexPrompt()
                    : new TokenNicknamePrompt();
        }

        return next;
    }

    @NotNull
    @Override
    public String getPromptText(@NotNull ConversationContext context) {
        return TEXT;
    }
}
