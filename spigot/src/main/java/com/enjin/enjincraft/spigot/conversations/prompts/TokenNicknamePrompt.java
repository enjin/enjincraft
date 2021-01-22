package com.enjin.enjincraft.spigot.conversations.prompts;

import com.enjin.enjincraft.spigot.util.ValidationUtils;
import org.bukkit.conversations.ConversationContext;
import org.bukkit.conversations.Prompt;
import org.bukkit.conversations.RegexPrompt;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class TokenNicknamePrompt extends RegexPrompt {

    public static final String KEY = "token-nickname";
    private static final String TEXT = "Enter a unique nickname for the token (must be alphanumeric with a minimum length of 3):";

    private final Prompt next;

    public TokenNicknamePrompt(Prompt next) {
        super(ValidationUtils.getAlphaNumericPattern(3));
        this.next = next;
    }

    public TokenNicknamePrompt() {
        this(Prompt.END_OF_CONVERSATION);
    }

    @Nullable
    @Override
    protected Prompt acceptValidatedInput(@NotNull ConversationContext context, @NotNull String input) {
        context.setSessionData(KEY, input);
        return next;
    }

    @NotNull
    @Override
    public String getPromptText(@NotNull ConversationContext context) {
        return TEXT;
    }
}
