package com.enjin.enjincraft.spigot.conversations.prompts;

import com.enjin.enjincraft.spigot.EnjinCraft;
import com.enjin.enjincraft.spigot.token.TokenManager;
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
        String id = ValidationUtils.getValidTokenId(input);
        context.setSessionData(KEY, id);

        Prompt next = this.next;

        if (next == null) {
            boolean nft = (boolean) context.getAllSessionData().getOrDefault(TokenTypePrompt.KEY, false);

            if (nft) {
                boolean baseExists = EnjinCraft.bootstrap().get().getTokenManager().hasToken(id);
                next = baseExists ? new TokenIndexPrompt() : new TokenNicknamePrompt();
            } else {
                next = new TokenNicknamePrompt();
            }
        }

        return next;
    }

    @NotNull
    @Override
    public String getPromptText(@NotNull ConversationContext context) {
        return TEXT;
    }
}
