package com.enjin.enjincraft.spigot.cmd;

import com.enjin.enjincraft.spigot.SpigotBootstrap;
import com.enjin.enjincraft.spigot.enums.Permission;
import com.enjin.enjincraft.spigot.i18n.Translation;
import com.enjin.enjincraft.spigot.token.TokenManager;
import com.enjin.enjincraft.spigot.token.TokenModel;
import de.tr7zw.changeme.nbtapi.NBTContainer;
import de.tr7zw.changeme.nbtapi.NBTItem;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.List;

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
            if (context.args.size() != 1)
                return;

            Player sender = context.player;
            PlayerInventory inventory = sender.getInventory();
            ItemStack held = inventory.getItemInMainHand();
            if (!held.getType().isItem())
                return;

            NBTContainer nbt = NBTItem.convertItemtoNBT(held);
            String id = context.args.get(0);
            TokenModel model = TokenModel.builder()
                    .id(id)
                    .nbt(nbt.toString())
                    .build();

            if (bootstrap.getTokenManager().saveToken(id, model))
                Translation.COMMAND_TOKEN_CREATE_SUCCESS.send(sender);
            else
                Translation.COMMAND_TOKEN_CREATE_FAILED.send(sender);
        }

        @Override
        public Translation getUsageTranslation() {
            return Translation.COMMAND_TOKEN_CREATE_DESCRIPTION;
        }

    }

    public class CmdAddPerm extends EnjCommand {

        public CmdAddPerm(SpigotBootstrap bootstrap, EnjCommand parent) {
            super(bootstrap, parent);
            this.aliases.add("addperm");
            this.requiredArgs.add("token-id");
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

            String tokenId = context.args.get(0);
            String perm = context.args.get(1);
            List<String> worlds = context.args.size() >= 3 ? context.args.subList(2, context.args.size()) : null;
            Player sender = context.player;
            int status;

            // Checks if permission is world based
            if (worlds != null && !worlds.contains(TokenManager.GLOBAL))
                status = bootstrap.getTokenManager().addPermissionToToken(perm, tokenId, worlds);
            else
                status = bootstrap.getTokenManager().addPermissionToToken(perm, tokenId, TokenManager.GLOBAL);

            switch (status) {
                case TokenManager.PERM_ADDED_SUCCESS:
                    Translation.COMMAND_TOKEN_ADDPERM_PERMADDED.send(sender);
                    break;
                case TokenManager.PERM_NOSUCHTOKEN:
                    Translation.COMMAND_TOKEN_ADDREVOKEPERM_NOSUCHTOKEN.send(sender);
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
            this.requiredArgs.add("token-id");
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

            String tokenId = context.args.get(0);
            String perm = context.args.get(1);
            List<String> worlds = context.args.size() >= 3 ? context.args.subList(2, context.args.size()) : null;
            Player sender = context.player;
            int status;

            // Checks if permission is world based
            if (worlds != null && !worlds.contains(TokenManager.GLOBAL))
                status = bootstrap.getTokenManager().removePermissionFromToken(perm, tokenId, worlds);
            else
                status = bootstrap.getTokenManager().removePermissionFromToken(perm, tokenId, TokenManager.GLOBAL);

            switch (status) {
                case TokenManager.PERM_REMOVED_SUCCESS:
                    Translation.COMMAND_TOKEN_REVOKEPERM_PERMREVOKED.send(sender);
                    break;
                case TokenManager.PERM_NOSUCHTOKEN:
                    Translation.COMMAND_TOKEN_ADDREVOKEPERM_NOSUCHTOKEN.send(sender);
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