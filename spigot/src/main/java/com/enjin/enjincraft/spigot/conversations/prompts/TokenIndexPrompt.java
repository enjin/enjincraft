package com.enjin.enjincraft.spigot.conversations.prompts;

import com.enjin.enjincraft.spigot.util.ValidationUtils;
import org.bukkit.conversations.ConversationContext;
import org.bukkit.conversations.Prompt;
import org.bukkit.conversations.RegexPrompt;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.math.BigInteger;

public class TokenIndexPrompt extends RegexPrompt {

    public static final String KEY = "token-index";
    private static final String TEXT = "Enter the token index:";

    private final Prompt next;

    public TokenIndexPrompt(Prompt next) {
        super(ValidationUtils.NUMBER_PATTERN);
        this.next = next;
    }

    public TokenIndexPrompt() {
        this(null);
    }

    @Nullable
    @Override
    protected Prompt acceptValidatedInput(@NotNull ConversationContext context, @NotNull String input) {
        BigInteger val = new BigInteger(input);
        context.setSessionData(KEY, val);

        if (next == null && !val.equals(BigInteger.ZERO)) {
            return Prompt.END_OF_CONVERSATION;
        }

        return next == null ? new TokenNicknamePrompt() : next;
    }

    @NotNull
    @Override
    public String getPromptText(@NotNull ConversationContext context) {
        return TEXT;
    }
}
