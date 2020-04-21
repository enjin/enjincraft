package com.enjin.enjincraft.spigot.cmd;

import com.enjin.enjincraft.spigot.SpigotBootstrap;
import com.enjin.enjincraft.spigot.enums.Permission;
import com.enjin.enjincraft.spigot.i18n.Translation;
import com.enjin.enjincraft.spigot.token.TokenManager;
import com.enjin.enjincraft.spigot.token.TokenModel;
import com.enjin.enjincraft.spigot.token.TokenPermission;
import de.tr7zw.changeme.nbtapi.NBTContainer;
import de.tr7zw.changeme.nbtapi.NBTItem;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.List;
import java.util.Map;

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
        this.subCommands.add(new CmdUpdate(bootstrap, this));
        this.subCommands.add(new CmdToInv(bootstrap, this));
        this.subCommands.add(new CmdNickname(bootstrap, this));
        this.subCommands.add(new CmdAddPerm(bootstrap, this));
        this.subCommands.add(new CmdRevokePerm(bootstrap, this));
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
            this.requiredArgs.add("token-id");
            this.requirements = CommandRequirements.builder()
                    .withPermission(Permission.CMD_TOKEN_CREATE)
                    .withAllowedSenderTypes(SenderType.PLAYER)
                    .build();
        }

        @Override
        public void execute(CommandContext context) {
            if (context.args.size() < 1 || context.args.size() > 2)
                return;

            String tokenId = context.args.get(0);
            String alternateId = context.args.size() == 2 ? context.args.get(1) : null;
            Player sender = context.player;
            PlayerInventory inventory = sender.getInventory();
            ItemStack held = inventory.getItemInMainHand();

            TokenManager tokenManager = bootstrap.getTokenManager();

            if (tokenManager.hasToken(tokenId)) {
                Translation.COMMAND_TOKEN_CREATE_DUPLICATE.send(sender);
                return;
            }

            if (!held.getType().isItem()) {
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
                case TokenManager.TOKEN_CREATE_FAILED:
                    Translation.COMMAND_TOKEN_CREATE_FAILED.send(sender);
                    break;
                case TokenManager.TOKEN_DUPLICATENICKNAME:
                    Translation.COMMAND_TOKEN_NICKNAME_DUPLICATE.send(sender);
                    break;
            }
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
            this.requirements = CommandRequirements.builder()
                    .withPermission(Permission.CMD_TOKEN_CREATE)
                    .withAllowedSenderTypes(SenderType.PLAYER)
                    .build();
        }

        @Override
        public void execute(CommandContext context) {
            if (context.args.size() != 1)
                return;

            String id = context.args.get(0);
            Player sender = context.player;

            TokenManager tokenManager = bootstrap.getTokenManager();
            TokenModel tokenModel = tokenManager.getToken(id);

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

            List<TokenPermission> permissions = tokenModel.getAssignablePermissions();

            NBTContainer nbt = NBTItem.convertItemtoNBT(held);
            TokenModel newModel = TokenModel.builder()
                    .id(tokenModel.getId())
                    .alternateId(tokenModel.getAlternateId())
                    .nbt(nbt.toString())
                    .assignablePermissions(permissions)
                    .build();

            int result = tokenManager.updateTokenConf(newModel);

            switch (result) {
                case TokenManager.TOKEN_UPDATE_SUCCESS:
                    Translation.COMMAND_TOKEN_UPDATE_SUCCESS.send(sender);
                    break;
                case TokenManager.TOKEN_UPDATE_FAILED:
                    Translation.COMMAND_TOKEN_UPDATE_FAILED.send(sender);
                    break;
                case TokenManager.TOKEN_CREATE_SUCCESS:
                    Translation.COMMAND_TOKEN_CREATE_SUCCESS.send(sender);
                    break;
                case TokenManager.TOKEN_CREATE_FAILED:
                    Translation.COMMAND_TOKEN_CREATE_FAILED.send(sender);
                    break;
            }
        }

        @Override
        public Translation getUsageTranslation() {
            return Translation.COMMAND_TOKEN_UPDATE_DESCRIPTION;
        }
    }

    public class CmdToInv extends EnjCommand {

        public CmdToInv(SpigotBootstrap bootstrap, EnjCommand parent) {
            super(bootstrap, parent);
            this.aliases.add("toinv");
            this.aliases.add("give");
            this.requiredArgs.add("token-id");
            this.requirements = CommandRequirements.builder()
                    .withPermission(Permission.CMD_TOKEN_CREATE)
                    .withAllowedSenderTypes(SenderType.PLAYER)
                    .build();
        }

        @Override
        public void execute(CommandContext context) {
            if (context.args.size() != 1)
                return;

            String id = context.args.get(0);
            Player sender = context.player;

            TokenModel tokenModel = bootstrap.getTokenManager().getToken(id);

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
            this.requiredArgs.add("token-id");
            this.requiredArgs.add("alt-id");
            this.requirements = CommandRequirements.builder()
                    .withPermission(Permission.CMD_TOKEN_CREATE)
                    .withAllowedSenderTypes(SenderType.PLAYER)
                    .build();
        }

        @Override
        public void execute(CommandContext context) {
            if (context.args.size() != 2)
                return;

            String tokenId = context.args.get(0);
            String alternateId = context.args.get(1);
            Player sender = context.player;

            int result = bootstrap.getTokenManager().updateAlternateId(tokenId, alternateId);

            switch (result) {
                case TokenManager.TOKEN_UPDATE_SUCCESS:
                    Translation.COMMAND_TOKEN_NICKNAME_SUCCESS.send(sender);
                    break;
                case TokenManager.TOKEN_UPDATE_FAILED:
                    Translation.COMMAND_TOKEN_UPDATE_FAILED.send(sender);
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
            this.requirements = CommandRequirements.builder()
                    .withPermission(Permission.CMD_TOKEN_ADDPERM)
                    .withAllowedSenderTypes(SenderType.PLAYER)
                    .build();
        }

        @Override
        public void execute(CommandContext context) {
            if (context.args.size() < 2)
                return;

            String id = context.args.get(0);
            String perm = context.args.get(1);
            List<String> worlds = context.args.size() >= 3 ? context.args.subList(2, context.args.size()) : null;
            Player sender = context.player;
            int status;

            // Checks if permission is world based
            if (worlds != null && !worlds.contains(TokenManager.GLOBAL))
                status = bootstrap.getTokenManager().addPermissionToToken(perm, id, worlds);
            else
                status = bootstrap.getTokenManager().addPermissionToToken(perm, id, TokenManager.GLOBAL);

            switch (status) {
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
            }
        }

        @Override
        public  Translation getUsageTranslation() {
            return Translation.COMMAND_TOKEN_ADDPERM_DESCRIPTION;
        }

    }

    public class CmdRevokePerm extends EnjCommand {

        public CmdRevokePerm(SpigotBootstrap bootstrap, EnjCommand parent) {
            super(bootstrap, parent);
            this.aliases.add("revokeperm");
            this.requiredArgs.add("id");
            this.requiredArgs.add("perm");
            this.requirements = CommandRequirements.builder()
                    .withPermission(Permission.CMD_TOKEN_REVOKEPERM)
                    .withAllowedSenderTypes(SenderType.PLAYER)
                    .build();
        }

        @Override
        public void execute(CommandContext context) {
            if (context.args.size() < 2)
                return;

            String id = context.args.get(0);
            String perm = context.args.get(1);
            List<String> worlds = context.args.size() >= 3 ? context.args.subList(2, context.args.size()) : null;
            Player sender = context.player;
            int status;

            // Checks if permission is world based
            if (worlds != null && !worlds.contains(TokenManager.GLOBAL))
                status = bootstrap.getTokenManager().removePermissionFromToken(perm, id, worlds);
            else
                status = bootstrap.getTokenManager().removePermissionFromToken(perm, id, TokenManager.GLOBAL);

            switch (status) {
                case TokenManager.PERM_REMOVED_SUCCESS:
                    Translation.COMMAND_TOKEN_REVOKEPERM_PERMREVOKED.send(sender);
                    break;
                case TokenManager.TOKEN_NOSUCHTOKEN:
                    Translation.COMMAND_TOKEN_NOSUCHTOKEN.send(sender);
                    break;
                case TokenManager.PERM_REMOVED_NOPERMONTOKEN:
                    Translation.COMMAND_TOKEN_REVOKEPERM_PERMNOTONTOKEN.send(sender);
                    break;
            }
        }

        @Override
        public  Translation getUsageTranslation() {
            return Translation.COMMAND_TOKEN_REVOKEPERM_DESCRIPTION;
        }

    }

}