package com.enjin.enjincraft.spigot.cmd;

import com.enjin.enjincraft.spigot.GraphQLException;
import com.enjin.enjincraft.spigot.NetworkException;
import com.enjin.enjincraft.spigot.SpigotBootstrap;
import com.enjin.enjincraft.spigot.cmd.arg.WalletViewStateArgumentProcessor;
import com.enjin.enjincraft.spigot.conversations.Conversations;
import com.enjin.enjincraft.spigot.conversations.prompts.TokenIdPrompt;
import com.enjin.enjincraft.spigot.conversations.prompts.TokenIndexPrompt;
import com.enjin.enjincraft.spigot.conversations.prompts.TokenNicknamePrompt;
import com.enjin.enjincraft.spigot.conversations.prompts.TokenTypePrompt;
import com.enjin.enjincraft.spigot.enums.Permission;
import com.enjin.enjincraft.spigot.i18n.Translation;
import com.enjin.enjincraft.spigot.token.TokenManager;
import com.enjin.enjincraft.spigot.token.TokenModel;
import com.enjin.enjincraft.spigot.util.MessageUtils;
import com.enjin.enjincraft.spigot.util.StringUtils;
import com.enjin.enjincraft.spigot.util.TokenUtils;
import com.enjin.enjincraft.spigot.wallet.TokenWalletViewState;
import com.enjin.sdk.TrustedPlatformClient;
import com.enjin.sdk.graphql.GraphQLResponse;
import com.enjin.sdk.models.token.GetToken;
import com.enjin.sdk.models.token.Token;
import de.tr7zw.changeme.nbtapi.NBTContainer;
import de.tr7zw.changeme.nbtapi.NBTItem;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.conversations.Conversation;
import org.bukkit.conversations.ConversationAbandonedEvent;
import org.bukkit.conversations.ConversationContext;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.math.BigInteger;
import java.util.*;
import java.util.stream.Collectors;

public class CmdToken extends EnjCommand {

    public CmdToken(SpigotBootstrap bootstrap, EnjCommand parent) {
        super(bootstrap, parent);
        this.aliases.add("token");
        this.requiredArgs.add("operation");
        this.requirements = CommandRequirements.builder()
                .withPermission(Permission.CMD_TOKEN)
                .withAllowedSenderTypes(SenderType.PLAYER, SenderType.CONSOLE)
                .build();
        this.subCommands.add(new CmdCreate(bootstrap, this));
        this.subCommands.add(new CmdUpdate(bootstrap, this));
        this.subCommands.add(new CmdDelete(bootstrap, this));
        this.subCommands.add(new CmdToInv(bootstrap, this));
        this.subCommands.add(new CmdNickname(bootstrap, this));
        this.subCommands.add(new CmdAddPerm(bootstrap, this));
        this.subCommands.add(new CmdAddPermNFT(bootstrap, this));
        this.subCommands.add(new CmdRevokePerm(bootstrap, this));
        this.subCommands.add(new CmdRevokePermNFT(bootstrap, this));
        this.subCommands.add(new CmdGetURI(bootstrap, this));
        this.subCommands.add(new CmdRemoveURI(bootstrap, this));
        this.subCommands.add(new CmdSetWalletView(bootstrap, this));
        this.subCommands.add(new CmdList(bootstrap, this));
        this.subCommands.add(new CmdExport(bootstrap, this));
        this.subCommands.add(new CmdImport(bootstrap, this));
    }

    @Override
    public void execute(CommandContext context) {
    }

    @Override
    public Translation getUsageTranslation() {
        return Translation.COMMAND_TOKEN_DESCRIPTION;
    }

    public class CmdCreate extends EnjCommand {

        public CmdCreate(SpigotBootstrap bootstrap, EnjCommand parent) {
            super(bootstrap, parent);
            this.aliases.add("create");
            this.requirements = CommandRequirements.builder()
                    .withPermission(Permission.CMD_TOKEN_CREATE)
                    .withAllowedSenderTypes(SenderType.PLAYER)
                    .build();
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

        @Override
        public void execute(CommandContext context) {
            Player sender = context.player;
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

    }

    public class CmdUpdate extends EnjCommand {

        public CmdUpdate(SpigotBootstrap bootstrap, EnjCommand parent) {
            super(bootstrap, parent);
            this.aliases.add("update");
            this.requiredArgs.add("id");
            this.optionalArgs.add("index");
            this.requirements = CommandRequirements.builder()
                    .withPermission(Permission.CMD_TOKEN_CREATE)
                    .withAllowedSenderTypes(SenderType.PLAYER)
                    .build();
        }

        @Override
        public void execute(CommandContext context) {
            String id = context.args.get(0);
            String index = context.args.size() > requiredArgs.size()
                    ? context.args.get(1)
                    : null;
            Player sender = Objects.requireNonNull(context.player);

            TokenManager tokenManager = bootstrap.getTokenManager();

            TokenModel baseModel = tokenManager.getToken(id);
            if (baseModel == null) {
                Translation.COMMAND_TOKEN_NOSUCHTOKEN.send(sender);
                return;
            }

            String fullId;
            try {
                fullId = baseModel.isNonfungible()
                        ? TokenUtils.createFullId(baseModel.getId(), TokenUtils.parseIndex(Objects.requireNonNull(index)))
                        : TokenUtils.createFullId(baseModel.getId());
            } catch (NullPointerException e) {
                Translation.COMMAND_TOKEN_MUSTPASSINDEX.send(sender);
                return;
            } catch (IllegalArgumentException e) {
                Translation.COMMAND_TOKEN_INVALIDFULLID.send(sender);
                return;
            } catch (Exception e) {
                bootstrap.log(e);
                return;
            }

            TokenModel tokenModel = tokenManager.getToken(fullId);
            if (tokenModel == null) {
                Translation.COMMAND_TOKEN_NOSUCHTOKEN.send(sender);
                return;
            }

            ItemStack held = sender.getInventory().getItemInMainHand();
            if (held.getType() == Material.AIR || !held.getType().isItem()) {
                Translation.COMMAND_TOKEN_NOHELDITEM.send(sender);
                return;
            }

            NBTContainer nbt = NBTItem.convertItemtoNBT(held);
            TokenModel newModel = TokenModel.builder()
                    .id(tokenModel.getId())
                    .index(tokenModel.getIndex())
                    .nonfungible(tokenModel.isNonfungible())
                    .alternateId(tokenModel.getAlternateId())
                    .nbt(nbt.toString())
                    .assignablePermissions(tokenModel.getAssignablePermissions())
                    .metadataURI(tokenModel.getMetadataURI())
                    .build();

            int result = tokenManager.updateTokenConf(newModel);
            switch (result) {
                case TokenManager.TOKEN_UPDATE_SUCCESS:
                    Translation.COMMAND_TOKEN_UPDATE_SUCCESS.send(sender);
                    break;
                case TokenManager.TOKEN_CREATE_SUCCESS:
                    Translation.COMMAND_TOKEN_CREATE_SUCCESS.send(sender);
                    break;
                case TokenManager.TOKEN_CREATE_FAILED:
                    Translation.COMMAND_TOKEN_CREATE_FAILED.send(sender);
                    break;
                case TokenManager.TOKEN_UPDATE_FAILED:
                    Translation.COMMAND_TOKEN_UPDATE_FAILED.send(sender);
                    break;
                default:
                    bootstrap.debug(String.format("Unhandled result when updating token (status: %d)", result));
                    break;
            }
        }

        @Override
        public Translation getUsageTranslation() {
            return Translation.COMMAND_TOKEN_UPDATE_DESCRIPTION;
        }

    }

    public class CmdDelete extends EnjCommand {

        public CmdDelete(SpigotBootstrap bootstrap, EnjCommand parent) {
            super(bootstrap, parent);
            this.aliases.add("delete");
            this.requiredArgs.add("id");
            this.optionalArgs.add("index");
            this.requirements = CommandRequirements.builder()
                    .withPermission(Permission.CMD_TOKEN_CREATE)
                    .withAllowedSenderTypes(SenderType.PLAYER)
                    .build();
        }

        @Override
        public void execute(CommandContext context) {
            String id = context.args.get(0);
            String index = context.args.size() > requiredArgs.size()
                    ? context.args.get(1)
                    : null;
            CommandSender sender = context.sender;

            TokenManager tokenManager = bootstrap.getTokenManager();

            TokenModel baseModel = tokenManager.getToken(id);
            if (baseModel == null) {
                Translation.COMMAND_TOKEN_NOSUCHTOKEN.send(sender);
                return;
            }

            String fullId;
            try {
                fullId = baseModel.isNonfungible()
                        ? TokenUtils.createFullId(baseModel.getId(), TokenUtils.parseIndex(Objects.requireNonNull(index)))
                        : TokenUtils.createFullId(baseModel.getId());
            } catch (NullPointerException e) {
                Translation.COMMAND_TOKEN_MUSTPASSINDEX.send(sender);
                return;
            } catch (IllegalArgumentException e) {
                Translation.COMMAND_TOKEN_INVALIDFULLID.send(sender);
                return;
            } catch (Exception e) {
                bootstrap.log(e);
                return;
            }

            int result = tokenManager.deleteTokenConf(fullId);
            switch (result) {
                case TokenManager.TOKEN_DELETE_SUCCESS:
                    Translation.COMMAND_TOKEN_DELETE_SUCCESS.send(sender);
                    return;
                case TokenManager.TOKEN_DELETE_FAILED:
                    Translation.COMMAND_TOKEN_DELETE_FAILED.send(sender);
                    return;
                case TokenManager.TOKEN_DELETE_FAILEDNFTBASE:
                    Translation.COMMAND_TOKEN_DELETE_BASENFT_1.send(sender);
                    Translation.COMMAND_TOKEN_DELETE_BASENFT_2.send(sender);
                    return;
                default:
                    bootstrap.debug(String.format("Unhandled result when deleting token (status: %d)", result));
                    break;
            }
        }

        @Override
        public Translation getUsageTranslation() {
            return Translation.COMMAND_TOKEN_DELETE_DESCRIPTION;
        }

    }

    public class CmdToInv extends EnjCommand {

        public CmdToInv(SpigotBootstrap bootstrap, EnjCommand parent) {
            super(bootstrap, parent);
            this.aliases.add("toinv");
            this.aliases.add("give");
            this.requiredArgs.add("id");
            this.optionalArgs.add("index");
            this.requirements = CommandRequirements.builder()
                    .withPermission(Permission.CMD_TOKEN_CREATE)
                    .withAllowedSenderTypes(SenderType.PLAYER)
                    .build();
        }

        @Override
        public void execute(CommandContext context) {
            String id = context.args.get(0);
            String index = context.args.size() > requiredArgs.size()
                    ? context.args.get(1)
                    : null;
            Player sender = Objects.requireNonNull(context.player);

            TokenManager tokenManager = bootstrap.getTokenManager();

            TokenModel baseModel = tokenManager.getToken(id);
            if (baseModel == null) {
                Translation.COMMAND_TOKEN_NOSUCHTOKEN.send(sender);
                return;
            }

            String fullId;
            try {
                fullId = baseModel.isNonfungible()
                        ? TokenUtils.createFullId(baseModel.getId(), TokenUtils.parseIndex(Objects.requireNonNull(index)))
                        : TokenUtils.createFullId(baseModel.getId());
            } catch (NullPointerException e) {
                Translation.COMMAND_TOKEN_MUSTPASSINDEX.send(sender);
                return;
            } catch (IllegalArgumentException e) {
                Translation.COMMAND_TOKEN_INVALIDFULLID.send(sender);
                return;
            } catch (Exception e) {
                bootstrap.log(e);
                return;
            }

            TokenModel tokenModel = tokenManager.getToken(fullId);
            if (tokenModel == null) {
                Translation.COMMAND_TOKEN_NOSUCHTOKEN.send(sender);
                return;
            }

            Map<Integer, ItemStack> leftOver = sender.getInventory().addItem(tokenModel.getItemStack(true));
            if (leftOver.isEmpty())
                Translation.COMMAND_TOKEN_TOINV_SUCCESS.send(sender);
            else
                Translation.COMMAND_TOKEN_TOINV_FAILED.send(sender);
        }

        @Override
        public Translation getUsageTranslation() {
            return Translation.COMMAND_TOKEN_TOINV_DESCRIPTION;
        }
    }

    public class CmdNickname extends EnjCommand {

        public CmdNickname(SpigotBootstrap bootstrap, EnjCommand parent) {
            super(bootstrap, parent);
            this.aliases.add("nickname");
            this.requiredArgs.add("token-id|alt-id");
            this.requiredArgs.add("new-alt-id");
            this.requirements = CommandRequirements.builder()
                    .withPermission(Permission.CMD_TOKEN_CREATE)
                    .withAllowedSenderTypes(SenderType.PLAYER)
                    .build();
        }

        @Override
        public void execute(CommandContext context) {
            String id = context.args.get(0);
            String alternateId = context.args.get(1);

            TokenManager tokenManager = bootstrap.getTokenManager();

            TokenModel tokenModel = tokenManager.getToken(id);
            if (tokenModel == null) {
                Translation.COMMAND_TOKEN_NOSUCHTOKEN.send(context.sender);
                return;
            } else if (tokenModel.isNonfungible()) {
                id = TokenUtils.normalizeFullId(tokenModel.getFullId());
            } else {
                id = tokenModel.getFullId();
            }

            int result = tokenManager.updateAlternateId(id, alternateId);
            switch (result) {
                case TokenManager.TOKEN_UPDATE_SUCCESS:
                    Translation.COMMAND_TOKEN_NICKNAME_SUCCESS.send(context.sender);
                    break;
                case TokenManager.TOKEN_NOSUCHTOKEN:
                    Translation.COMMAND_TOKEN_NOSUCHTOKEN.send(context.sender);
                    break;
                case TokenManager.TOKEN_DUPLICATENICKNAME:
                    Translation.COMMAND_TOKEN_NICKNAME_DUPLICATE.send(context.sender);
                    break;
                case TokenManager.TOKEN_HASNICKNAME:
                    Translation.COMMAND_TOKEN_NICKNAME_HAS.send(context.sender);
                    break;
                case TokenManager.TOKEN_INVALIDNICKNAME:
                    Translation.COMMAND_TOKEN_NICKNAME_INVALID.send(context.sender);
                    break;
                case TokenManager.TOKEN_ISNOTBASE:
                    Translation.COMMAND_TOKEN_ISNONFUNGIBLEINSTANCE.send(context.sender);
                    break;
                case TokenManager.TOKEN_UPDATE_FAILED:
                    Translation.COMMAND_TOKEN_UPDATE_FAILED.send(context.sender);
                    break;
                default:
                    bootstrap.debug(String.format("Unhandled result when setting nickname (status: %d)", result));
                    break;
            }
        }

        @Override
        public Translation getUsageTranslation() {
            return Translation.COMMAND_TOKEN_NICKNAME_DESCRIPTION;
        }
    }

    public class CmdAddPerm extends EnjCommand {

        public CmdAddPerm(SpigotBootstrap bootstrap, EnjCommand parent) {
            super(bootstrap, parent);
            this.aliases.add("addperm");
            this.requiredArgs.add("id");
            this.requiredArgs.add("perm");
            this.optionalArgs.add("worlds...");
            this.requirements = CommandRequirements.builder()
                    .withPermission(Permission.CMD_TOKEN_ADDPERM)
                    .withAllowedSenderTypes(SenderType.PLAYER)
                    .build();
        }

        @Override
        public void execute(CommandContext context) {
            String id = context.args.get(0);
            String perm = context.args.get(1);
            List<String> worlds = context.args.size() > requiredArgs.size()
                    ? context.args.subList(requiredArgs.size(), context.args.size())
                    : null;

            boolean isGlobal = worlds == null || worlds.contains(TokenManager.GLOBAL);
            int result = isGlobal
                    ? bootstrap.getTokenManager().addPermissionToToken(perm, id, TokenManager.GLOBAL)
                    : bootstrap.getTokenManager().addPermissionToToken(perm, id, worlds);
            switch (result) {
                case TokenManager.PERM_ADDED_SUCCESS:
                    Translation.COMMAND_TOKEN_ADDPERM_PERMADDED.send(context.sender);
                    break;
                case TokenManager.TOKEN_NOSUCHTOKEN:
                    Translation.COMMAND_TOKEN_NOSUCHTOKEN.send(context.sender);
                    break;
                case TokenManager.PERM_ADDED_DUPLICATEPERM:
                    Translation.COMMAND_TOKEN_ADDPERM_DUPLICATEPERM.send(context.sender);
                    break;
                case TokenManager.PERM_ADDED_BLACKLISTED:
                    Translation.COMMAND_TOKEN_ADDPERM_PERMREJECTED.send(context.sender);
                    break;
                case TokenManager.PERM_ISGLOBAL:
                    Translation.COMMAND_TOKEN_PERM_ISGLOBAL.send(context.sender);
                    break;
                case TokenManager.TOKEN_UPDATE_FAILED:
                    Translation.COMMAND_TOKEN_UPDATE_FAILED.send(context.sender);
                    break;
                default:
                    bootstrap.debug(String.format("Unhandled result when adding base permission (status: %d)", result));
                    break;
            }
        }

        @Override
        public Translation getUsageTranslation() {
            return Translation.COMMAND_TOKEN_ADDPERM_DESCRIPTION;
        }

    }

    public class CmdAddPermNFT extends EnjCommand {

        public CmdAddPermNFT(SpigotBootstrap bootstrap, EnjCommand parent) {
            super(bootstrap, parent);
            this.aliases.add("addpermnft");
            this.requiredArgs.add("id");
            this.requiredArgs.add("index");
            this.requiredArgs.add("perm");
            this.optionalArgs.add("worlds...");
            this.requirements = CommandRequirements.builder()
                    .withPermission(Permission.CMD_TOKEN_ADDPERM)
                    .withAllowedSenderTypes(SenderType.PLAYER)
                    .build();
        }

        @Override
        public void execute(CommandContext context) {
            String id = context.args.get(0);
            String index = context.args.get(1);
            String perm = context.args.get(2);
            List<String> worlds = context.args.size() > requiredArgs.size()
                    ? context.args.subList(requiredArgs.size(), context.args.size())
                    : null;

            TokenManager tokenManager = bootstrap.getTokenManager();

            TokenModel baseModel = tokenManager.getToken(id);
            if (baseModel != null && !baseModel.isNonfungible()) {
                Translation.COMMAND_TOKEN_ISFUNGIBLE.send(context.sender);
                return;
            } else if (baseModel != null) {
                id = baseModel.getId();
            }

            String fullId;
            try {
                index = TokenUtils.parseIndex(index);
                fullId = TokenUtils.createFullId(id, index);
            } catch (IllegalArgumentException e) {
                Translation.COMMAND_TOKEN_INVALIDFULLID.send(context.sender);
                return;
            } catch (Exception e) {
                bootstrap.log(e);
                return;
            }

            boolean isGlobal = worlds == null || worlds.contains(TokenManager.GLOBAL);
            int result = isGlobal
                    ? tokenManager.addPermissionToToken(perm, fullId, TokenManager.GLOBAL)
                    : tokenManager.addPermissionToToken(perm, fullId, worlds);
            switch (result) {
                case TokenManager.PERM_ADDED_SUCCESS:
                    Translation.COMMAND_TOKEN_ADDPERM_PERMADDED.send(context.sender);
                    break;
                case TokenManager.TOKEN_NOSUCHTOKEN:
                    Translation.COMMAND_TOKEN_NOSUCHTOKEN.send(context.sender);
                    break;
                case TokenManager.PERM_ADDED_DUPLICATEPERM:
                    Translation.COMMAND_TOKEN_ADDPERM_DUPLICATEPERM.send(context.sender);
                    break;
                case TokenManager.PERM_ADDED_BLACKLISTED:
                    Translation.COMMAND_TOKEN_ADDPERM_PERMREJECTED.send(context.sender);
                    break;
                case TokenManager.PERM_ISGLOBAL:
                    Translation.COMMAND_TOKEN_PERM_ISGLOBAL.send(context.sender);
                    break;
                case TokenManager.TOKEN_UPDATE_FAILED:
                    Translation.COMMAND_TOKEN_UPDATE_FAILED.send(context.sender);
                    break;
                default:
                    bootstrap.debug(String.format("Unhandled result when adding non-fungible permission (status: %d)", result));
                    break;
            }
        }

        @Override
        public Translation getUsageTranslation() {
            return Translation.COMMAND_TOKEN_ADDPERMNFT_DESCRIPTION;
        }

    }

    public class CmdRevokePerm extends EnjCommand {

        public CmdRevokePerm(SpigotBootstrap bootstrap, EnjCommand parent) {
            super(bootstrap, parent);
            this.aliases.add("revokeperm");
            this.requiredArgs.add("id");
            this.requiredArgs.add("perm");
            this.optionalArgs.add("worlds...");
            this.requirements = CommandRequirements.builder()
                    .withPermission(Permission.CMD_TOKEN_REVOKEPERM)
                    .withAllowedSenderTypes(SenderType.PLAYER)
                    .build();
        }

        @Override
        public void execute(CommandContext context) {
            String id = context.args.get(0);
            String perm = context.args.get(1);
            List<String> worlds = context.args.size() > requiredArgs.size()
                    ? context.args.subList(requiredArgs.size(), context.args.size())
                    : null;

            boolean isGlobal = worlds == null || worlds.contains(TokenManager.GLOBAL);
            int result = isGlobal
                    ? bootstrap.getTokenManager().removePermissionFromToken(perm, id, TokenManager.GLOBAL)
                    : bootstrap.getTokenManager().removePermissionFromToken(perm, id, worlds);
            switch (result) {
                case TokenManager.PERM_REMOVED_SUCCESS:
                    Translation.COMMAND_TOKEN_REVOKEPERM_PERMREVOKED.send(context.sender);
                    break;
                case TokenManager.TOKEN_NOSUCHTOKEN:
                    Translation.COMMAND_TOKEN_NOSUCHTOKEN.send(context.sender);
                    break;
                case TokenManager.PERM_REMOVED_NOPERMONTOKEN:
                    Translation.COMMAND_TOKEN_REVOKEPERM_PERMNOTONTOKEN.send(context.sender);
                    break;
                case TokenManager.PERM_ISGLOBAL:
                    Translation.COMMAND_TOKEN_PERM_ISGLOBAL.send(context.sender);
                    break;
                case TokenManager.TOKEN_UPDATE_FAILED:
                    Translation.COMMAND_TOKEN_UPDATE_FAILED.send(context.sender);
                    break;
                default:
                    bootstrap.debug(String.format("Unhandled result when removing base permission (status: %d)", result));
                    break;
            }
        }

        @Override
        public Translation getUsageTranslation() {
            return Translation.COMMAND_TOKEN_REVOKEPERM_DESCRIPTION;
        }

    }

    public class CmdRevokePermNFT extends EnjCommand {

        public CmdRevokePermNFT(SpigotBootstrap bootstrap, EnjCommand parent) {
            super(bootstrap, parent);
            this.aliases.add("revokepermnft");
            this.requiredArgs.add("id");
            this.requiredArgs.add("index");
            this.requiredArgs.add("perm");
            this.optionalArgs.add("worlds...");
            this.requirements = CommandRequirements.builder()
                    .withPermission(Permission.CMD_TOKEN_REVOKEPERM)
                    .withAllowedSenderTypes(SenderType.PLAYER)
                    .build();
        }

        @Override
        public void execute(CommandContext context) {
            String id = context.args.get(0);
            String index = context.args.get(1);
            String perm = context.args.get(2);
            List<String> worlds = context.args.size() > requiredArgs.size()
                    ? context.args.subList(requiredArgs.size(), context.args.size())
                    : null;

            TokenManager tokenManager = bootstrap.getTokenManager();
            TokenModel baseModel = tokenManager.getToken(id);
            if (baseModel != null && !baseModel.isNonfungible()) {
                Translation.COMMAND_TOKEN_ISFUNGIBLE.send(context.sender);
                return;
            } else if (baseModel != null) {
                id = baseModel.getId();
            }

            String fullId;
            try {
                index = TokenUtils.parseIndex(index);
                fullId = TokenUtils.createFullId(id, index);
            } catch (IllegalArgumentException e) {
                Translation.COMMAND_TOKEN_INVALIDFULLID.send(context.sender);
                return;
            } catch (Exception e) {
                bootstrap.log(e);
                return;
            }

            boolean isGlobal = worlds == null || worlds.contains(TokenManager.GLOBAL);
            int result = isGlobal
                    ? tokenManager.removePermissionFromToken(perm, fullId, TokenManager.GLOBAL)
                    : tokenManager.removePermissionFromToken(perm, fullId, worlds);
            switch (result) {
                case TokenManager.PERM_REMOVED_SUCCESS:
                    Translation.COMMAND_TOKEN_REVOKEPERM_PERMREVOKED.send(context.sender);
                    break;
                case TokenManager.TOKEN_NOSUCHTOKEN:
                    Translation.COMMAND_TOKEN_NOSUCHTOKEN.send(context.sender);
                    break;
                case TokenManager.PERM_REMOVED_NOPERMONTOKEN:
                    Translation.COMMAND_TOKEN_REVOKEPERM_PERMNOTONTOKEN.send(context.sender);
                    break;
                case TokenManager.PERM_ISGLOBAL:
                    Translation.COMMAND_TOKEN_PERM_ISGLOBAL.send(context.sender);
                    break;
                case TokenManager.TOKEN_UPDATE_FAILED:
                    Translation.COMMAND_TOKEN_UPDATE_FAILED.send(context.sender);
                    break;
                default:
                    bootstrap.debug(String.format("Unhandled result when removing non-fungible permission (status: %d)", result));
                    break;
            }
        }

        @Override
        public Translation getUsageTranslation() {
            return Translation.COMMAND_TOKEN_REVOKEPERMNFT_DESCRIPTION;
        }

    }

    public class CmdGetURI extends EnjCommand {

        public CmdGetURI(SpigotBootstrap bootstrap, EnjCommand parent) {
            super(bootstrap, parent);
            this.aliases.add("geturi");
            this.requiredArgs.add("id");
            this.requirements = CommandRequirements.builder()
                    .withPermission(Permission.CMD_TOKEN_CREATE)
                    .withAllowedSenderTypes(SenderType.PLAYER)
                    .build();
        }

        @Override
        public void execute(CommandContext context) {
            String id = context.args.get(0);

            TokenModel tokenModel = bootstrap.getTokenManager().getToken(id);
            if (tokenModel == null) {
                Translation.COMMAND_TOKEN_NOSUCHTOKEN.send(context.sender);
                return;
            }

            getURI(context.sender, tokenModel.getId());
        }

        private void getURI(CommandSender sender, String tokenId) {
            TrustedPlatformClient client = bootstrap.getTrustedPlatformClient();
            client.getTokenService().getTokenAsync(new GetToken()
                            .tokenId(tokenId)
                            .withItemUri(),
                    networkResponse -> {
                        if (!networkResponse.isSuccess()) {
                            NetworkException exception = new NetworkException(networkResponse.code());
                            Translation.ERRORS_EXCEPTION.send(sender, exception.getMessage());
                            throw exception;
                        }

                        GraphQLResponse<Token> graphQLResponse = networkResponse.body();
                        if (!graphQLResponse.isSuccess()) {
                            GraphQLException exception = new GraphQLException(graphQLResponse.getErrors());
                            Translation.ERRORS_EXCEPTION.send(sender, exception);
                            throw exception;
                        }

                        String metadataURI = graphQLResponse.getData().getItemURI();
                        if (StringUtils.isEmpty(metadataURI)) {
                            Translation.COMMAND_TOKEN_GETURI_EMPTY_1.send(sender);
                            Translation.COMMAND_TOKEN_GETURI_EMPTY_2.send(sender);
                            return;
                        }

                        int result = bootstrap.getTokenManager().updateMetadataURI(tokenId, metadataURI);
                        switch (result) {
                            case TokenManager.TOKEN_NOSUCHTOKEN:
                                Translation.COMMAND_TOKEN_NOSUCHTOKEN.send(sender);
                                break;
                            case TokenManager.TOKEN_UPDATE_SUCCESS:
                                Translation.COMMAND_TOKEN_GETURI_SUCCESS.send(sender);
                                break;
                            case TokenManager.TOKEN_ISNOTBASE:
                                Translation.COMMAND_TOKEN_ISNONFUNGIBLEINSTANCE.send(sender);
                                break;
                            case TokenManager.TOKEN_UPDATE_FAILED:
                                Translation.COMMAND_TOKEN_GETURI_FAILED.send(sender);
                                break;
                            default:
                                bootstrap.debug(String.format("Unhandled result when getting the URI (status: %d)", result));
                                break;
                        }
                    });
        }

        @Override
        public Translation getUsageTranslation() {
            return Translation.COMMAND_TOKEN_GETURI_DESCRIPTION;
        }

    }

    public class CmdRemoveURI extends EnjCommand {

        public CmdRemoveURI(SpigotBootstrap bootstrap, EnjCommand parent) {
            super(bootstrap, parent);
            this.aliases.add("removeuri");
            this.requiredArgs.add("id");
            this.requirements = CommandRequirements.builder()
                    .withPermission(Permission.CMD_TOKEN_CREATE)
                    .withAllowedSenderTypes(SenderType.PLAYER)
                    .build();
        }

        @Override
        public void execute(CommandContext context) {
            String id = context.args.get(0);

            TokenManager tokenManager = bootstrap.getTokenManager();
            TokenModel tokenModel = tokenManager.getToken(id);
            if (tokenModel == null) {
                Translation.COMMAND_TOKEN_NOSUCHTOKEN.send(context.sender);
                return;
            } else if (StringUtils.isEmpty(tokenModel.getMetadataURI())) {
                Translation.COMMAND_TOKEN_REMOVEURI_EMPTY.send(context.sender);
                return;
            }

            int result = tokenManager.updateMetadataURI(tokenModel.getId(), null);
            switch (result) {
                case TokenManager.TOKEN_NOSUCHTOKEN:
                    Translation.COMMAND_TOKEN_NOSUCHTOKEN.send(context.sender);
                    break;
                case TokenManager.TOKEN_UPDATE_SUCCESS:
                    Translation.COMMAND_TOKEN_REMOVEURI_SUCCESS.send(context.sender);
                    break;
                case TokenManager.TOKEN_ISNOTBASE:
                    Translation.COMMAND_TOKEN_ISNONFUNGIBLEINSTANCE.send(context.sender);
                    break;
                case TokenManager.TOKEN_UPDATE_FAILED:
                    Translation.COMMAND_TOKEN_REMOVEURI_FAILED.send(context.sender);
                    break;
                default:
                    bootstrap.debug(String.format("Unhandled result when removing the URI (status: %d)", result));
                    break;
            }
        }

        @Override
        public Translation getUsageTranslation() {
            return Translation.COMMAND_TOKEN_REMOVEURI_DESCRIPTION;
        }

    }

    public class CmdSetWalletView extends EnjCommand {

        public CmdSetWalletView(SpigotBootstrap bootstrap, EnjCommand parent) {
            super(bootstrap, parent);
            this.aliases.add("setwalview");
            this.aliases.add("setwalletview");
            this.requiredArgs.add("id");
            this.requiredArgs.add("view");
            this.requirements = CommandRequirements.builder()
                    .withPermission(Permission.CMD_TOKEN_CREATE)
                    .withAllowedSenderTypes(SenderType.PLAYER)
                    .build();
        }

        @Override
        public void execute(CommandContext context) {
            String id = context.args.get(0);
            String view = context.args.get(1);

            TokenWalletViewState viewState;
            try {
                viewState = TokenWalletViewState.valueOf(view.toUpperCase());
            } catch (IllegalArgumentException e) {
                Translation.COMMAND_TOKEN_SETWALLETVIEW_INVALIDVIEW.send(context.sender);
                return;
            } catch (Exception e) {
                bootstrap.log(e);
                return;
            }

            int result = bootstrap.getTokenManager().updateWalletViewState(id, viewState);
            switch (result) {
                case TokenManager.TOKEN_UPDATE_SUCCESS:
                    Translation.COMMAND_TOKEN_UPDATE_SUCCESS.send(context.sender);
                    break;
                case TokenManager.TOKEN_UPDATE_FAILED:
                    Translation.COMMAND_TOKEN_UPDATE_FAILED.send(context.sender);
                    break;
                case TokenManager.TOKEN_NOSUCHTOKEN:
                    Translation.COMMAND_TOKEN_NOSUCHTOKEN.send(context.sender);
                    break;
                case TokenManager.TOKEN_ISNOTBASE:
                    Translation.COMMAND_TOKEN_ISNONFUNGIBLEINSTANCE.send(context.sender);
                    break;
                case TokenManager.TOKEN_HASWALLETVIEWSTATE:
                    Translation.COMMAND_TOKEN_SETWALLETVIEW_HAS.send(context.sender);
                    break;
                default:
                    bootstrap.debug(String.format("Unhandled result when setting the wallet view state (status: %d)", result));
                    break;
            }
        }

        @Override
        public List<String> tab(CommandContext context) {
            if (context.args.size() == 2)
                return WalletViewStateArgumentProcessor.INSTANCE.tab(context.sender, context.args.get(1));

            return new ArrayList<>(0);
        }

        @Override
        public Translation getUsageTranslation() {
            return Translation.COMMAND_TOKEN_SETWALLETVIEW_DESCRIPTION;
        }

    }

    public class CmdList extends EnjCommand {

        public CmdList(SpigotBootstrap bootstrap, EnjCommand parent) {
            super(bootstrap, parent);
            this.aliases.add("list");
            this.optionalArgs.add("id");
            this.requirements = CommandRequirements.builder()
                    .withPermission(Permission.CMD_TOKEN_CREATE)
                    .withAllowedSenderTypes(SenderType.PLAYER)
                    .build();
        }

        @Override
        public void execute(CommandContext context) {
            String id = context.args.size() > 0
                    ? context.args.get(0)
                    : null;
            if (id == null)
                listBaseTokens(context.sender);
            else
                listNonfungibleInstances(context.sender, id);
        }

        private void listBaseTokens(CommandSender sender) {
            Set<String> ids = bootstrap.getTokenManager().getTokenIds();
            if (ids.isEmpty()) {
                Translation.COMMAND_TOKEN_LIST_EMPTY.send(sender);
                return;
            }

            MessageUtils.sendString(sender,
                    ChatColor.GREEN + Translation.COMMAND_TOKEN_LIST_HEADER_TOKENS.translation());
            int count = 0;
            for (String id : ids) {
                MessageUtils.sendString(sender, String.format("&a%d: &6%s",
                        count++,
                        id));
            }
        }

        private void listNonfungibleInstances(CommandSender sender, String id) {
            TokenManager tokenManager = bootstrap.getTokenManager();
            TokenModel baseModel = tokenManager.getToken(id);
            if (baseModel == null) {
                Translation.COMMAND_TOKEN_NOSUCHTOKEN.send(sender);
                return;
            }

            Set<String> instances = tokenManager.getFullIds()
                    .stream()
                    .filter(fullId -> {
                        String tokenId = TokenUtils.getTokenID(fullId);
                        String tokenIndex = TokenUtils.getTokenIndex(fullId);
                        return tokenId.equals(baseModel.getId()) && !tokenIndex.equals(TokenUtils.BASE_INDEX);
                    })
                    .collect(Collectors.toSet());
            if (instances.isEmpty()) {
                Translation.COMMAND_TOKEN_LIST_EMPTY.send(sender);
                return;
            }

            MessageUtils.sendString(sender,
                    ChatColor.GREEN + Translation.COMMAND_TOKEN_LIST_HEADER_NONFUNGIBLE.translation());
            int count = 0;
            for (String fullId : instances) {
                TokenModel instance = tokenManager.getToken(fullId);
                MessageUtils.sendString(sender, String.format("&a%d: &6%s #%d",
                        count++,
                        instance.getId(),
                        TokenUtils.convertIndexToLong(instance.getIndex())));
            }
        }

        @Override
        public Translation getUsageTranslation() {
            return Translation.COMMAND_TOKEN_LIST_DESCRIPTION;
        }

    }

    public class CmdExport extends EnjCommand {

        public CmdExport(SpigotBootstrap bootstrap, EnjCommand parent) {
            super(bootstrap, parent);
            this.aliases.add("export");
            this.optionalArgs.add("id");
            this.optionalArgs.add("index");
            this.requirements = CommandRequirements.builder()
                    .withAllowedSenderTypes(SenderType.CONSOLE)
                    .build();
        }

        @Override
        public void execute(CommandContext context) {
            String id = context.args.size() > 0
                    ? context.args.get(0)
                    : null;
            String index = context.args.size() > 1
                    ? context.args.get(1)
                    : null;
            CommandSender sender = context.sender;

            TokenManager tokenManager = bootstrap.getTokenManager();

            int result;
            if (id != null && index != null) {
                TokenModel baseModel = tokenManager.getToken(id);
                if (baseModel != null)
                    id = baseModel.getId();

                String fullId;
                try {
                    fullId = TokenUtils.createFullId(id, TokenUtils.parseIndex(index));
                } catch (IllegalArgumentException e) {
                    Translation.COMMAND_TOKEN_INVALIDFULLID.send(sender);
                    return;
                } catch (Exception e) {
                    Translation.ERRORS_EXCEPTION.send(sender, e);
                    bootstrap.log(e);
                    return;
                }

                result = tokenManager.exportToken(fullId);
            } else if (id != null) {
                result = tokenManager.exportToken(id);
            } else {
                result = tokenManager.exportTokens();
            }

            switch (result) {
                case TokenManager.TOKEN_EXPORT_SUCCESS:
                    Translation.COMMAND_TOKEN_EXPORT_COMPLETE.send(sender);
                    Translation.COMMAND_TOKEN_EXPORT_SUCCESS.send(sender);
                    break;
                case TokenManager.TOKEN_EXPORT_PARTIAL:
                    Translation.COMMAND_TOKEN_EXPORT_COMPLETE.send(sender);
                    Translation.COMMAND_TOKEN_EXPORT_PARTIAL.send(sender);
                    break;
                case TokenManager.TOKEN_NOSUCHTOKEN:
                    Translation.COMMAND_TOKEN_NOSUCHTOKEN.send(sender);
                    break;
                case TokenManager.TOKEN_EXPORT_EMPTY:
                    Translation.COMMAND_TOKEN_EXPORT_EMPTY.send(sender);
                    break;
                case TokenManager.TOKEN_EXPORT_FAILED:
                    Translation.COMMAND_TOKEN_EXPORT_FAILED.send(sender);
                    break;
                default:
                    bootstrap.debug(String.format("Unhandled result when exporting token(s) (status: %d)", result));
                    break;
            }
        }

        @Override
        public Translation getUsageTranslation() {
            return Translation.COMMAND_TOKEN_EXPORT_DESCRIPTION;
        }

    }

    public class CmdImport extends EnjCommand {

        public CmdImport(SpigotBootstrap bootstrap, EnjCommand parent) {
            super(bootstrap, parent);
            this.aliases.add("import");
            this.requirements = CommandRequirements.builder()
                    .withAllowedSenderTypes(SenderType.CONSOLE)
                    .build();
        }

        @Override
        public void execute(CommandContext context) {
            CommandSender sender = context.sender;

            int result = bootstrap.getTokenManager().importTokens();
            switch (result) {
                case TokenManager.TOKEN_IMPORT_SUCCESS:
                    Translation.COMMAND_TOKEN_IMPORT_COMPLETE.send(sender);
                    Translation.COMMAND_TOKEN_IMPORT_SUCCESS.send(sender);
                    break;
                case TokenManager.TOKEN_IMPORT_PARTIAL:
                    Translation.COMMAND_TOKEN_IMPORT_COMPLETE.send(sender);
                    Translation.COMMAND_TOKEN_IMPORT_PARTIAL.send(sender);
                    break;
                case TokenManager.TOKEN_IMPORT_EMPTY:
                    Translation.COMMAND_TOKEN_IMPORT_EMPTY.send(sender);
                    break;
                case TokenManager.TOKEN_IMPORT_FAILED:
                    Translation.COMMAND_TOKEN_IMPORT_FAILED.send(sender);
                    break;
                default:
                    bootstrap.debug(String.format("Unhandled result when importing token(s) (status: %d)", result));
                    break;
            }
        }

        @Override
        public Translation getUsageTranslation() {
            return Translation.COMMAND_TOKEN_IMPORT_DESCRIPTION;
        }

    }

}
