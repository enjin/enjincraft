package com.enjin.enjincraft.spigot.cmd;

import com.enjin.enjincraft.spigot.SpigotBootstrap;
import com.enjin.enjincraft.spigot.cmd.arg.LocaleArgumentProcessor;
import com.enjin.enjincraft.spigot.configuration.Conf;
import com.enjin.enjincraft.spigot.configuration.TokenModel;
import com.enjin.enjincraft.spigot.enums.Permission;
import com.enjin.enjincraft.spigot.i18n.Locale;
import com.enjin.enjincraft.spigot.i18n.Translation;
import de.tr7zw.changeme.nbtapi.NBTContainer;
import de.tr7zw.changeme.nbtapi.NBTItem;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class CmdConf extends EnjCommand {

    public CmdConf(SpigotBootstrap bootstrap, EnjCommand parent) {
        super(bootstrap, parent);
        this.aliases.add("conf");
        this.requiredArgs.add("operation");
        this.requirements = CommandRequirements.builder()
                .withPermission(Permission.CMD_CONF)
                .build();
        this.subCommands.add(new CmdSet(bootstrap, this));
        this.subCommands.add(new CmdToken(bootstrap, this));
    }

    @Override
    public void execute(CommandContext context) {
    }

    @Override
    public Translation getUsageTranslation() {
        return Translation.COMMAND_CONF_DESCRIPTION;
    }

    public class CmdSet extends EnjCommand {

        public CmdSet(SpigotBootstrap bootstrap, EnjCommand parent) {
            super(bootstrap, parent);
            this.aliases.add("set");
            this.requiredArgs.add("setting");
            this.requirements = CommandRequirements.builder()
                    .withPermission(Permission.CMD_CONF_SET)
                    .build();
            this.addSubCommand(new CmdLang(bootstrap, this));
        }

        @Override
        public void execute(CommandContext context) {
        }

        @Override
        public Translation getUsageTranslation() {
            return Translation.COMMAND_CONF_SET_DESCRIPTION;
        }

        public class CmdLang extends EnjCommand {

            public CmdLang(SpigotBootstrap bootstrap, EnjCommand parent) {
                super(bootstrap, parent);
                this.aliases.add("lang");
                this.requiredArgs.add("language");
                this.requirements = CommandRequirements.builder()
                        .withPermission(Permission.CMD_CONF_SET)
                        .build();
            }

            @Override
            public List<String> tab(CommandContext context) {
                if (context.args.size() == 1)
                    return LocaleArgumentProcessor.INSTANCE.tab(context.sender, context.args.get(0));
                return new ArrayList<>(0);
            }

            @Override
            public void execute(CommandContext context) {
                Optional<Locale> locale = LocaleArgumentProcessor.INSTANCE.parse(context.sender, context.args.get(0));

                if (!locale.isPresent()) {
                    return;
                }

                Conf config = bootstrap.getConfig();
                config.setLocale(locale.get());
                bootstrap.plugin().saveConfig();
                Translation.setServerLocale(locale.get());
            }

            @Override
            public Translation getUsageTranslation() {
                return Translation.COMMAND_CONF_SET_LANG_DESCRIPTION;
            }
        }

    }

    public class CmdToken extends EnjCommand {

        public CmdToken(SpigotBootstrap bootstrap, EnjCommand parent) {
            super(bootstrap, parent);
            this.aliases.add("token");
            this.requiredArgs.add("operation");
            this.requirements = CommandRequirements.builder()
                    .withPermission(Permission.CMD_CONF_TOKEN)
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
            // TODO: Update COMMAND_CONF_TOKEN_DESCRIPTION for parent
            return Translation.COMMAND_CONF_TOKEN_DESCRIPTION;
        }

        public class CmdCreate extends EnjCommand {

            public CmdCreate(SpigotBootstrap bootstrap, EnjCommand parent) {
                super(bootstrap, parent);
                this.aliases.add("create");
                this.requiredArgs.add("token-id");
                this.requirements = CommandRequirements.builder()
                        .withPermission(Permission.CMD_CONF_TOKEN_CREATE)
                        .withAllowedSenderTypes(SenderType.PLAYER)
                        .build();
            }

            @Override
            public void execute(CommandContext context) {
                if (context.args.size() != 1)
                    return;

                Player player = context.player;
                PlayerInventory inventory = player.getInventory();
                ItemStack held = inventory.getItemInMainHand();
                if (!held.getType().isItem())
                    return;

                NBTContainer nbt = NBTItem.convertItemtoNBT(held);
                String id = context.args.get(0);
                TokenModel model = TokenModel.builder()
                        .id(id)
                        .nbt(nbt.toString())
                        .build();
                bootstrap.getTokenManager().saveToken(id, model);
            }

            @Override
            public Translation getUsageTranslation() {
                // TODO: Update COMMAND_CONF_TOKEN_DESCRIPTION for create
                return Translation.COMMAND_CONF_TOKEN_DESCRIPTION;
            }

        }

        public class CmdAddPerm extends EnjCommand {

            public CmdAddPerm(SpigotBootstrap bootstrap, EnjCommand parent) {
                super(bootstrap, parent);
                this.aliases.add("addperm");
                this.requiredArgs.add("perm");
                this.requiredArgs.add("token-id");
                this.requirements = CommandRequirements.builder()
                        .withPermission(Permission.CMD_CONF_TOKEN_ADDPERM)
                        .withAllowedSenderTypes(SenderType.PLAYER)
                        .build();
            }

            @Override
            public void execute(CommandContext context) {
                if (context.args.size() != 2)
                    return;

                String tokenId = context.args.get(0);
                String perm = context.args.get(1);

                bootstrap.getTokenManager().addPermissionToToken(perm, tokenId);
            }

            @Override
            public  Translation getUsageTranslation() {
                // TODO: Create and return a proper translation for this command
                return null;
            }

        }

        public class CmdRevokePerm extends EnjCommand {

            public CmdRevokePerm(SpigotBootstrap bootstrap, EnjCommand parent) {
                super(bootstrap, parent);
                this.aliases.add("revokeperm");
                this.requiredArgs.add("perm");
                this.requiredArgs.add("token-id");
                this.requirements = CommandRequirements.builder()
                        .withPermission(Permission.CMD_CONF_TOKEN_REVOKEPERM)
                        .withAllowedSenderTypes(SenderType.PLAYER)
                        .build();
            }

            @Override
            public void execute(CommandContext context) {
                if (context.args.size() != 2)
                    return;

                String tokenId = context.args.get(0);
                String perm = context.args.get(1);

                bootstrap.getTokenManager().removePermissionFromToken(perm, tokenId);
            }

            @Override
            public  Translation getUsageTranslation() {
                // TODO: Create and return a proper translation for this command
                return null;
            }

        }

    }

}
