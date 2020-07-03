package com.enjin.enjincraft.spigot.cmd;

import com.enjin.enjincraft.spigot.GraphQLException;
import com.enjin.enjincraft.spigot.NetworkException;
import com.enjin.enjincraft.spigot.SpigotBootstrap;
import com.enjin.enjincraft.spigot.cmd.arg.WalletViewStateArgumentProcessor;
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
import lombok.NonNull;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.*;
import java.util.stream.Collectors;

public class CmdToken extends EnjCommand {

    public CmdToken(SpigotBootstrap bootstrap, EnjCommand parent) {
        super(bootstrap, parent);
        this.aliases.add("token");
        this.requiredArgs.add("operation");
        this.requirements = CommandRequirements.builder()
                .withPermission(Permission.CMD_TOKEN)
                .withAllowedSenderTypes(SenderType.PLAYER)
                .build();
        this.subCommands.add(new CmdCreate(bootstrap, this));
        this.subCommands.add(new CmdCreateNFT(bootstrap, this));
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
    }

    @Override
    public void execute(CommandContext context) {
    }

    @Override
    public Translation getUsageTranslation() {
        return Translation.COMMAND_TOKEN_DESCRIPTION;
    }

    private String parseIndex(@NonNull String index) throws IllegalArgumentException {
        if (index.length() > 1 && (index.startsWith("x") || index.startsWith("X"))) {
            index = index.substring(1);
        } else {
            long parsedLong = Long.parseLong(index);
            if (parsedLong < 1L)
                throw new IllegalArgumentException("Provided index is not positive");

            index = Long.toHexString(parsedLong);
        }

        index = TokenUtils.formatIndex(index);
        if (index.equals(TokenUtils.BASE_INDEX))
            throw new IllegalArgumentException("Index may not be the base index");

        return index;
    }

    public class CmdCreate extends EnjCommand {

        public CmdCreate(SpigotBootstrap bootstrap, EnjCommand parent) {
            super(bootstrap, parent);
            this.aliases.add("create");
            this.requiredArgs.add("token-id");
            this.optionalArgs.add("nickname");
            this.requirements = CommandRequirements.builder()
                    .withPermission(Permission.CMD_TOKEN_CREATE)
                    .withAllowedSenderTypes(SenderType.PLAYER)
                    .build();
        }

        @Override
        public void execute(CommandContext context) {
            if (context.args.size() < requiredArgs.size()
                    || context.args.size() > requiredArgs.size() + optionalArgs.size())
                return;

            String tokenId = context.args.get(0);
            String alternateId = context.args.size() == 2
                    ? context.args.get(1)
                    : null;
            Player sender = context.player;
            PlayerInventory inventory = sender.getInventory();
            ItemStack held = inventory.getItemInMainHand();

            TokenManager tokenManager = bootstrap.getTokenManager();

            if (tokenManager.hasToken(tokenId)) {
                Translation.COMMAND_TOKEN_CREATE_DUPLICATE.send(sender);
                return;
            } else if (alternateId != null && !TokenManager.isValidAlternateId(alternateId)) {
                Translation.COMMAND_TOKEN_NICKNAME_INVALID.send(sender);
                return;
            } else if (held.getType() == Material.AIR || !held.getType().isItem()) {
                Translation.COMMAND_TOKEN_NOHELDITEM.send(sender);
                return;
            }

            NBTContainer nbt = NBTItem.convertItemtoNBT(held);
            TokenModel model = TokenModel.builder()
                    .id(tokenId)
                    .alternateId(alternateId)
                    .nbt(nbt.toString())
                    .build();

            int result = tokenManager.saveToken(model);
            switch (result) {
                case TokenManager.TOKEN_CREATE_SUCCESS:
                    Translation.COMMAND_TOKEN_CREATE_SUCCESS.send(sender);
                    break;
                case TokenManager.TOKEN_DUPLICATENICKNAME:
                    Translation.COMMAND_TOKEN_NICKNAME_DUPLICATE.send(sender);
                    break;
                case TokenManager.TOKEN_INVALIDNICKNAME:
                    Translation.COMMAND_TOKEN_NICKNAME_INVALID.send(sender);
                    break;
                case TokenManager.TOKEN_CREATE_FAILED:
                    Translation.COMMAND_TOKEN_CREATE_FAILED.send(sender);
                    break;
                default:
                    bootstrap.debug(String.format("Unhandled result when creating fungible token (status: %d)", result));
                    break;
            }
        }

        @Override
        public Translation getUsageTranslation() {
            return Translation.COMMAND_TOKEN_CREATE_DESCRIPTION;
        }

    }

    public class CmdCreateNFT extends EnjCommand {

        public CmdCreateNFT(SpigotBootstrap bootstrap, EnjCommand parent) {
            super(bootstrap, parent);
            this.aliases.add("createnft");
            this.requiredArgs.add("id");
            this.requiredArgs.add("index");
            this.optionalArgs.add("nickname");
            this.requirements = CommandRequirements.builder()
                    .withPermission(Permission.CMD_TOKEN_CREATENFT)
                    .withAllowedSenderTypes(SenderType.PLAYER)
                    .build();
        }

        @Override
        public void execute(CommandContext context) {
            if (context.args.size() < requiredArgs.size()
                    || context.args.size() > requiredArgs.size() + optionalArgs.size())
                return;

            String id = context.args.get(0);
            String index = context.args.get(1);
            String alternateId = context.args.size() == 3
                    ? context.args.get(2)
                    : null;
            Player sender = context.player;
            PlayerInventory inventory = sender.getInventory();
            ItemStack held = inventory.getItemInMainHand();

            TokenManager tokenManager = bootstrap.getTokenManager();

            TokenModel baseModel = tokenManager.getToken(id);
            if (baseModel != null && !baseModel.isNonfungible()) { // Must not refer to a fungible token
                Translation.COMMAND_TOKEN_ISFUNGIBLE.send(sender);
                return;
            } else if (alternateId != null
                    && baseModel != null
                    && baseModel.getAlternateId() != null
                    && !alternateId.equals(baseModel.getAlternateId())) { // Must not replace nickname on create
                Translation.COMMAND_TOKEN_CREATENFT_REPLACENICKNAME.send(sender);
                return;
            } else if (alternateId != null && !TokenManager.isValidAlternateId(alternateId)) {
                Translation.COMMAND_TOKEN_NICKNAME_INVALID.send(sender);
                return;
            } else if (held.getType() == Material.AIR || !held.getType().isItem()) {
                Translation.COMMAND_TOKEN_NOHELDITEM.send(sender);
                return;
            } else if (baseModel != null) {
                id = baseModel.getId();
            }

            String fullId;
            try {
                index  = parseIndex(index);
                fullId = TokenUtils.createFullId(id, index);
            } catch (IllegalArgumentException e) {
                Translation.COMMAND_TOKEN_INVALIDFULLID.send(sender);
                return;
            } catch (Exception e) {
                Translation.ERRORS_EXCEPTION.send(sender, e);
                bootstrap.log(e);
                return;
            }

            if (tokenManager.hasToken(fullId)) {
                Translation.COMMAND_TOKEN_CREATENFT_DUPLICATE.send(sender);
                return;
            }

            NBTContainer nbt = NBTItem.convertItemtoNBT(held);
            TokenModel tokenModel = TokenModel.builder()
                    .id(id)
                    .index(index)
                    .nonfungible(true)
                    .alternateId(alternateId)
                    .nbt(nbt.toString())
                    .walletViewState(baseModel == null
                            ? TokenWalletViewState.WITHDRAWABLE
                            : baseModel.getWalletViewState())
                    .build();

            int result = tokenManager.saveToken(tokenModel);
            switch (result) {
                case TokenManager.TOKEN_CREATE_SUCCESS:
                    Translation.COMMAND_TOKEN_CREATE_SUCCESS.send(sender);
                    break;
                case TokenManager.TOKEN_ALREADYEXISTS:
                    Translation.COMMAND_TOKEN_CREATENFT_DUPLICATE.send(sender);
                    break;
                case TokenManager.TOKEN_INVALIDDATA:
                    Translation.COMMAND_TOKEN_INVALIDFULLID.send(sender);
                    break;
                case TokenManager.TOKEN_CREATE_FAILED:
                    Translation.COMMAND_TOKEN_CREATE_FAILED.send(sender);
                    break;
                case TokenManager.TOKEN_CREATE_FAILEDNFTBASE:
                    Translation.COMMAND_TOKEN_CREATENFT_BASEFAILED.send(sender);
                    break;
                default:
                    bootstrap.debug(String.format("Unhandled result when creating non-fungible token (status: %d)", result));
                    break;
            }
        }

        @Override
        public Translation getUsageTranslation() {
            return Translation.COMMAND_TOKEN_CREATENFT_DESCRIPTION;
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
            if (context.args.size() < requiredArgs.size()
                    || context.args.size() > requiredArgs.size() + optionalArgs.size())
                return;

            String id = context.args.get(0);
            String index = context.args.size() > requiredArgs.size()
                    ? context.args.get(1)
                    : null;
            Player sender = context.player;

            TokenManager tokenManager = bootstrap.getTokenManager();
            TokenModel   baseModel    = tokenManager.getToken(id);
            if (baseModel == null) {
                Translation.COMMAND_TOKEN_NOSUCHTOKEN.send(sender);
                return;
            }

            String fullId;
            try {
                fullId = baseModel.isNonfungible()
                        ? TokenUtils.createFullId(baseModel.getId(), parseIndex(Objects.requireNonNull(index)))
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

            PlayerInventory inventory = sender.getInventory();
            ItemStack held = inventory.getItemInMainHand();

            if (!held.getType().isItem()) {
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
            if (context.args.size() < requiredArgs.size()
                    || context.args.size() > requiredArgs.size() + optionalArgs.size())
                return;

            String id = context.args.get(0);
            String index = context.args.size() > requiredArgs.size()
                    ? context.args.get(1)
                    : null;
            Player sender = context.player;

            TokenManager tokenManager = bootstrap.getTokenManager();
            TokenModel   baseModel    = tokenManager.getToken(id);
            if (baseModel == null) {
                Translation.COMMAND_TOKEN_NOSUCHTOKEN.send(sender);
                return;
            }

            String fullId;
            try {
                fullId = baseModel.isNonfungible() && index != null
                        ? TokenUtils.createFullId(baseModel.getId(), parseIndex(index))
                        : TokenUtils.createFullId(baseModel.getId());
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
            if (context.args.size() < requiredArgs.size()
                    || context.args.size() > requiredArgs.size() + optionalArgs.size())
                return;

            String id = context.args.get(0);
            String index = context.args.size() > requiredArgs.size()
                    ? context.args.get(1)
                    : null;
            Player sender = context.player;

            TokenManager tokenManager = bootstrap.getTokenManager();
            TokenModel   baseModel    = tokenManager.getToken(id);
            if (baseModel == null) {
                Translation.COMMAND_TOKEN_NOSUCHTOKEN.send(sender);
                return;
            }

            String fullId;
            try {
                fullId = baseModel.isNonfungible()
                        ? TokenUtils.createFullId(baseModel.getId(), parseIndex(Objects.requireNonNull(index)))
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

            PlayerInventory inventory = sender.getInventory();
            Map<Integer, ItemStack> leftOver = inventory.addItem(tokenModel.getItemStack(true));

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
            if (context.args.size() != 2)
                return;

            String id = context.args.get(0);
            String alternateId = context.args.get(1);
            Player sender = context.player;

            TokenManager tokenManager = bootstrap.getTokenManager();

            TokenModel tokenModel = tokenManager.getToken(id);
            if (tokenModel == null) {
                Translation.COMMAND_TOKEN_NOSUCHTOKEN.send(sender);
                return;
            } else if (tokenModel.isNonfungible()) {
                id = TokenUtils.normalizeFullId(tokenModel.getFullId());
            } else {
                id = tokenModel.getFullId();
            }

            int result = tokenManager.updateAlternateId(id, alternateId);
            switch (result) {
                case TokenManager.TOKEN_UPDATE_SUCCESS:
                    Translation.COMMAND_TOKEN_NICKNAME_SUCCESS.send(sender);
                    break;
                case TokenManager.TOKEN_NOSUCHTOKEN:
                    Translation.COMMAND_TOKEN_NOSUCHTOKEN.send(sender);
                    break;
                case TokenManager.TOKEN_DUPLICATENICKNAME:
                    Translation.COMMAND_TOKEN_NICKNAME_DUPLICATE.send(sender);
                    break;
                case TokenManager.TOKEN_HASNICKNAME:
                    Translation.COMMAND_TOKEN_NICKNAME_HAS.send(sender);
                    break;
                case TokenManager.TOKEN_INVALIDNICKNAME:
                    Translation.COMMAND_TOKEN_NICKNAME_INVALID.send(sender);
                    break;
                case TokenManager.TOKEN_ISNOTBASE:
                    Translation.COMMAND_TOKEN_ISNONFUNGIBLEINSTANCE.send(sender);
                    break;
                case TokenManager.TOKEN_UPDATE_FAILED:
                    Translation.COMMAND_TOKEN_UPDATE_FAILED.send(sender);
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
            if (context.args.size() < requiredArgs.size())
                return;

            String id = context.args.get(0);
            String perm = context.args.get(1);
            List<String> worlds = context.args.size() > requiredArgs.size()
                    ? context.args.subList(requiredArgs.size(), context.args.size())
                    : null;
            Player sender = context.player;

            boolean isGlobal = worlds == null || worlds.contains(TokenManager.GLOBAL);
            int     result   = isGlobal
                    ? bootstrap.getTokenManager().addPermissionToToken(perm, id, TokenManager.GLOBAL)
                    : bootstrap.getTokenManager().addPermissionToToken(perm, id, worlds);
            switch (result) {
                case TokenManager.PERM_ADDED_SUCCESS:
                    Translation.COMMAND_TOKEN_ADDPERM_PERMADDED.send(sender);
                    break;
                case TokenManager.TOKEN_NOSUCHTOKEN:
                    Translation.COMMAND_TOKEN_NOSUCHTOKEN.send(sender);
                    break;
                case TokenManager.PERM_ADDED_DUPLICATEPERM:
                    Translation.COMMAND_TOKEN_ADDPERM_DUPLICATEPERM.send(sender);
                    break;
                case TokenManager.PERM_ADDED_BLACKLISTED:
                    Translation.COMMAND_TOKEN_ADDPERM_PERMREJECTED.send(sender);
                    break;
                case TokenManager.PERM_ISGLOBAL:
                    Translation.COMMAND_TOKEN_PERM_ISGLOBAL.send(sender);
                    break;
                case TokenManager.TOKEN_UPDATE_FAILED:
                    Translation.COMMAND_TOKEN_UPDATE_FAILED.send(sender);
                    break;
                default:
                    bootstrap.debug(String.format("Unhandled result when adding base permission (status: %d)", result));
                    break;
            }
        }

        @Override
        public  Translation getUsageTranslation() {
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
            if (context.args.size() < requiredArgs.size())
                return;

            String id = context.args.get(0);
            String index = context.args.get(1);
            String perm = context.args.get(2);
            List<String> worlds = context.args.size() > requiredArgs.size()
                    ? context.args.subList(requiredArgs.size(), context.args.size())
                    : null;
            Player sender = context.player;

            TokenManager tokenManager = bootstrap.getTokenManager();
            TokenModel   baseModel    = tokenManager.getToken(id);
            if (baseModel != null && !baseModel.isNonfungible()) {
                Translation.COMMAND_TOKEN_ISFUNGIBLE.send(sender);
                return;
            } else if (baseModel != null) {
                id = baseModel.getId();
            }

            String fullId;
            try {
                index  = parseIndex(index);
                fullId = TokenUtils.createFullId(id, index);
            } catch (IllegalArgumentException e) {
                Translation.COMMAND_TOKEN_INVALIDFULLID.send(sender);
                return;
            } catch (Exception e) {
                bootstrap.log(e);
                return;
            }

            boolean isGlobal = worlds == null || worlds.contains(TokenManager.GLOBAL);
            int     result   = isGlobal
                    ? tokenManager.addPermissionToToken(perm, fullId, TokenManager.GLOBAL)
                    : tokenManager.addPermissionToToken(perm, fullId, worlds);
            switch (result) {
                case TokenManager.PERM_ADDED_SUCCESS:
                    Translation.COMMAND_TOKEN_ADDPERM_PERMADDED.send(sender);
                    break;
                case TokenManager.TOKEN_NOSUCHTOKEN:
                    Translation.COMMAND_TOKEN_NOSUCHTOKEN.send(sender);
                    break;
                case TokenManager.PERM_ADDED_DUPLICATEPERM:
                    Translation.COMMAND_TOKEN_ADDPERM_DUPLICATEPERM.send(sender);
                    break;
                case TokenManager.PERM_ADDED_BLACKLISTED:
                    Translation.COMMAND_TOKEN_ADDPERM_PERMREJECTED.send(sender);
                    break;
                case TokenManager.PERM_ISGLOBAL:
                    Translation.COMMAND_TOKEN_PERM_ISGLOBAL.send(sender);
                    break;
                case TokenManager.TOKEN_UPDATE_FAILED:
                    Translation.COMMAND_TOKEN_UPDATE_FAILED.send(sender);
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
            if (context.args.size() < requiredArgs.size())
                return;

            String id = context.args.get(0);
            String perm = context.args.get(1);
            List<String> worlds = context.args.size() > requiredArgs.size()
                    ? context.args.subList(requiredArgs.size(), context.args.size())
                    : null;
            Player sender = context.player;

            boolean isGlobal = worlds == null || worlds.contains(TokenManager.GLOBAL);
            int     result   = isGlobal
                    ? bootstrap.getTokenManager().removePermissionFromToken(perm, id, TokenManager.GLOBAL)
                    : bootstrap.getTokenManager().removePermissionFromToken(perm, id, worlds);
            switch (result) {
                case TokenManager.PERM_REMOVED_SUCCESS:
                    Translation.COMMAND_TOKEN_REVOKEPERM_PERMREVOKED.send(sender);
                    break;
                case TokenManager.TOKEN_NOSUCHTOKEN:
                    Translation.COMMAND_TOKEN_NOSUCHTOKEN.send(sender);
                    break;
                case TokenManager.PERM_REMOVED_NOPERMONTOKEN:
                    Translation.COMMAND_TOKEN_REVOKEPERM_PERMNOTONTOKEN.send(sender);
                    break;
                case TokenManager.PERM_ISGLOBAL:
                    Translation.COMMAND_TOKEN_PERM_ISGLOBAL.send(sender);
                    break;
                case TokenManager.TOKEN_UPDATE_FAILED:
                    Translation.COMMAND_TOKEN_UPDATE_FAILED.send(sender);
                    break;
                default:
                    bootstrap.debug(String.format("Unhandled result when removing base permission (status: %d)", result));
                    break;
            }
        }

        @Override
        public  Translation getUsageTranslation() {
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
            if (context.args.size() < requiredArgs.size())
                return;

            String id = context.args.get(0);
            String index = context.args.get(1);
            String perm = context.args.get(2);
            List<String> worlds = context.args.size() > requiredArgs.size()
                    ? context.args.subList(requiredArgs.size(), context.args.size())
                    : null;
            Player sender = context.player;

            TokenManager tokenManager = bootstrap.getTokenManager();
            TokenModel   baseModel    = tokenManager.getToken(id);
            if (baseModel != null && !baseModel.isNonfungible()) {
                Translation.COMMAND_TOKEN_ISFUNGIBLE.send(sender);
                return;
            } else if (baseModel != null) {
                id = baseModel.getId();
            }

            String fullId;
            try {
                index  = parseIndex(index);
                fullId = TokenUtils.createFullId(id, index);
            } catch (IllegalArgumentException e) {
                Translation.COMMAND_TOKEN_INVALIDFULLID.send(sender);
                return;
            } catch (Exception e) {
                bootstrap.log(e);
                return;
            }

            boolean isGlobal = worlds == null || worlds.contains(TokenManager.GLOBAL);
            int     result   = isGlobal
                    ? tokenManager.removePermissionFromToken(perm, fullId, TokenManager.GLOBAL)
                    : tokenManager.removePermissionFromToken(perm, fullId, worlds);
            switch (result) {
                case TokenManager.PERM_REMOVED_SUCCESS:
                    Translation.COMMAND_TOKEN_REVOKEPERM_PERMREVOKED.send(sender);
                    break;
                case TokenManager.TOKEN_NOSUCHTOKEN:
                    Translation.COMMAND_TOKEN_NOSUCHTOKEN.send(sender);
                    break;
                case TokenManager.PERM_REMOVED_NOPERMONTOKEN:
                    Translation.COMMAND_TOKEN_REVOKEPERM_PERMNOTONTOKEN.send(sender);
                    break;
                case TokenManager.PERM_ISGLOBAL:
                    Translation.COMMAND_TOKEN_PERM_ISGLOBAL.send(sender);
                    break;
                case TokenManager.TOKEN_UPDATE_FAILED:
                    Translation.COMMAND_TOKEN_UPDATE_FAILED.send(sender);
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
            if (context.args.size() != requiredArgs.size())
                return;

            String id = context.args.get(0);
            Player sender = context.player;

            TokenModel tokenModel = bootstrap.getTokenManager().getToken(id);
            if (tokenModel == null) {
                Translation.COMMAND_TOKEN_NOSUCHTOKEN.send(sender);
                return;
            }

            getURI(sender, tokenModel.getId());
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
            if (context.args.size() != requiredArgs.size())
                return;

            String id = context.args.get(0);
            Player sender = context.player;

            TokenManager tokenManager = bootstrap.getTokenManager();
            TokenModel   tokenModel   = tokenManager.getToken(id);
            if (tokenModel == null) {
                Translation.COMMAND_TOKEN_NOSUCHTOKEN.send(sender);
                return;
            } else if (StringUtils.isEmpty(tokenModel.getMetadataURI())) {
                Translation.COMMAND_TOKEN_REMOVEURI_EMPTY.send(sender);
                return;
            }

            int result = tokenManager.updateMetadataURI(tokenModel.getId(), null);
            switch (result) {
                case TokenManager.TOKEN_NOSUCHTOKEN:
                    Translation.COMMAND_TOKEN_NOSUCHTOKEN.send(sender);
                    break;
                case TokenManager.TOKEN_UPDATE_SUCCESS:
                    Translation.COMMAND_TOKEN_REMOVEURI_SUCCESS.send(sender);
                    break;
                case TokenManager.TOKEN_ISNOTBASE:
                    Translation.COMMAND_TOKEN_ISNONFUNGIBLEINSTANCE.send(sender);
                    break;
                case TokenManager.TOKEN_UPDATE_FAILED:
                    Translation.COMMAND_TOKEN_REMOVEURI_FAILED.send(sender);
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
            if (context.args.size() != requiredArgs.size())
                return;

            String id = context.args.get(0);
            String view = context.args.get(1);
            Player sender = context.player;

            TokenWalletViewState viewState;
            try {
                viewState = TokenWalletViewState.valueOf(view.toUpperCase());
            } catch (IllegalArgumentException e) {
                Translation.COMMAND_TOKEN_SETWALLETVIEW_INVALIDVIEW.send(sender);
                return;
            } catch (Exception e) {
                bootstrap.log(e);
                return;
            }

            int result = bootstrap.getTokenManager().updateWalletViewState(id, viewState);
            switch (result) {
                case TokenManager.TOKEN_UPDATE_SUCCESS:
                    Translation.COMMAND_TOKEN_UPDATE_SUCCESS.send(sender);
                    break;
                case TokenManager.TOKEN_UPDATE_FAILED:
                    Translation.COMMAND_TOKEN_UPDATE_FAILED.send(sender);
                    break;
                case TokenManager.TOKEN_NOSUCHTOKEN:
                    Translation.COMMAND_TOKEN_NOSUCHTOKEN.send(sender);
                    break;
                case TokenManager.TOKEN_ISNOTBASE:
                    Translation.COMMAND_TOKEN_ISNONFUNGIBLEINSTANCE.send(sender);
                    break;
                case TokenManager.TOKEN_HASWALLETVIEWSTATE:
                    Translation.COMMAND_TOKEN_SETWALLETVIEW_HAS.send(sender);
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
            Player sender = context.player;

            if (id == null)
                listBaseTokens(sender);
            else
                listNonfungibleInstances(sender, id);
        }

        private void listBaseTokens(Player sender) {
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

        private void listNonfungibleInstances(Player sender, String id) {
            TokenManager tokenManager = bootstrap.getTokenManager();
            TokenModel   baseModel    = tokenManager.getToken(id);
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

}