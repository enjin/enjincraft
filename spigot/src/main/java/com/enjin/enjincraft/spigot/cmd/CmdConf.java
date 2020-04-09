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
            this.requiredArgs.add("token-id");
            this.requirements = CommandRequirements.builder()
                    .withPermission(Permission.CMD_CONF_TOKEN)
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
            return Translation.COMMAND_CONF_TOKEN_DESCRIPTION;
        }

    }
}
