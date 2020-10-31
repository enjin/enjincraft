package com.enjin.enjincraft.spigot.cmd.token;

import com.enjin.enjincraft.spigot.SpigotBootstrap;
import com.enjin.enjincraft.spigot.cmd.CommandContext;
import com.enjin.enjincraft.spigot.cmd.CommandRequirements;
import com.enjin.enjincraft.spigot.cmd.EnjCommand;
import com.enjin.enjincraft.spigot.cmd.SenderType;
import com.enjin.enjincraft.spigot.conversations.Conversations;
import com.enjin.enjincraft.spigot.conversations.prompts.TokenIdPrompt;
import com.enjin.enjincraft.spigot.conversations.prompts.TokenIndexPrompt;
import com.enjin.enjincraft.spigot.conversations.prompts.TokenNicknamePrompt;
import com.enjin.enjincraft.spigot.conversations.prompts.TokenTypePrompt;
import com.enjin.enjincraft.spigot.enums.Permission;
import com.enjin.enjincraft.spigot.i18n.Translation;
import com.enjin.enjincraft.spigot.token.TokenManager;
import com.enjin.enjincraft.spigot.token.TokenModel;
import com.enjin.enjincraft.spigot.util.TokenUtils;
import de.tr7zw.changeme.nbtapi.NBTContainer;
import de.tr7zw.changeme.nbtapi.NBTItem;
import org.bukkit.Material;
import org.bukkit.conversations.Conversation;
import org.bukkit.conversations.ConversationAbandonedEvent;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.math.BigInteger;
import java.util.Map;

public class CmdCreate extends EnjCommand {

    public CmdCreate(SpigotBootstrap bootstrap, EnjCommand parent) {
        super(bootstrap, parent);
        this.aliases.add("create");
        this.requirements = CommandRequirements.builder()
                .withPermission(Permission.CMD_TOKEN_CREATE)
                .withAllowedSenderTypes(SenderType.PLAYER)
                .build();
    }

    @Override
    public void execute(CommandContext context) {
        Player sender = context.player();
        ItemStack held = sender.getInventory().getItemInMainHand();

        // Ensure player is holding a valid item
        if (held.getType() == Material.AIR || !held.getType().isItem()) {
            Translation.COMMAND_TOKEN_NOHELDITEM.send(sender);
            return;
        }

        // Setup Conversation
        Conversations conversations = new Conversations(bootstrap.plugin());
        Conversation conversation = conversations.startTokenCreationConversation(sender);
        conversation.addConversationAbandonedListener(this::execute);
        conversation.getContext().setSessionData("sender", sender);
        conversation.getContext().setSessionData("nbt-item", NBTItem.convertItemtoNBT(sender.getInventory().getItemInMainHand()));
        conversation.begin();
    }

    @Override
    public Translation getUsageTranslation() {
        return Translation.COMMAND_TOKEN_CREATE_DESCRIPTION;
    }

    public void execute(ConversationAbandonedEvent event) {
        // Check if the conversation completed gracefully.
        if (!event.gracefulExit())
            return;

        // Load managers and data store
        Map<Object, Object> data = event.getContext().getAllSessionData();
        TokenManager tokenManager = bootstrap.getTokenManager();
        // Load data from conversation context
        Player sender = (Player) data.get("sender");
        boolean nft = (boolean) data.get(TokenTypePrompt.KEY);
        String id = (String) data.get(TokenIdPrompt.KEY);
        BigInteger index = (BigInteger) data.getOrDefault(TokenIndexPrompt.KEY, null);
        // Convert index from decimal to hexadecimal representation
        String indexHex = index == null ? null : TokenUtils.bigIntToIndex(index);

        // Check whether the token can be created if another already exists.
        // This will only ever pass if the token is an nft, the index is non-zero
        // and doesn't exist in the database.
        if (tokenManager.hasToken(id)) {
            TokenModel base = tokenManager.getToken(id);

            if (base.isNonfungible() && !nft) {
                Translation.COMMAND_TOKEN_ISFUNGIBLE.send(sender);
                return;
            } else if (!base.isNonfungible()) {
                Translation.COMMAND_TOKEN_CREATE_DUPLICATE.send(sender);
                return;
            } else if (tokenManager.hasToken(TokenUtils.createFullId(id, indexHex))) {
                Translation.COMMAND_TOKEN_CREATENFT_DUPLICATE.send(sender);
                return;
            }
        } else if (nft && !index.equals(BigInteger.ZERO)) {
            Translation.COMMAND_TOKEN_CREATENFT_MISSINGBASE.send(sender);
            return;
        }

        // Start token model creation process
        NBTContainer nbt = (NBTContainer) data.get("nbt-item");
        TokenModel.TokenModelBuilder modelBuilder = TokenModel.builder()
                .id(id)
                .nonfungible(nft)
                .nbt(nbt.toString());

        // Add index if creating an nft
        if (nft) {
            modelBuilder.index(indexHex);
        }

        // Validate and add nickname if present
        if (data.containsKey(TokenNicknamePrompt.KEY)) {
            String nickname = (String) data.get(TokenNicknamePrompt.KEY);

            if (!TokenManager.isValidAlternateId(nickname)) {
                Translation.COMMAND_TOKEN_NICKNAME_INVALID.send(sender);
                return;
            }

            modelBuilder.alternateId(nickname);
        }

        // Create token model and save to database
        TokenModel model = modelBuilder.build();
        int result = tokenManager.saveToken(model);

        // Inform sender of result or log to console if unknown
        switch (result) {
            case TokenManager.TOKEN_CREATE_SUCCESS:
                Translation.COMMAND_TOKEN_CREATE_SUCCESS.send(sender);
                break;
            case TokenManager.TOKEN_CREATE_FAILED:
                Translation.COMMAND_TOKEN_CREATE_FAILED.send(sender);
                break;
            case TokenManager.TOKEN_ALREADYEXISTS:
                Translation translation = nft
                        ? Translation.COMMAND_TOKEN_CREATENFT_DUPLICATE
                        : Translation.COMMAND_TOKEN_CREATE_DUPLICATE;
                translation.send(sender);
                break;
            case TokenManager.TOKEN_INVALIDDATA:
                Translation.COMMAND_TOKEN_INVALIDDATA.send(sender);
                break;
            case TokenManager.TOKEN_CREATE_FAILEDNFTBASE:
                Translation.COMMAND_TOKEN_CREATENFT_BASEFAILED.send(sender);
                break;
            case TokenManager.TOKEN_DUPLICATENICKNAME:
                Translation.COMMAND_TOKEN_NICKNAME_DUPLICATE.send(sender);
                break;
            case TokenManager.TOKEN_INVALIDNICKNAME:
                Translation.COMMAND_TOKEN_NICKNAME_INVALID.send(sender);
                break;
            default:
                bootstrap.debug(String.format("Unhandled result when creating token (status: %d)", result));
                break;
        }
    }

}
