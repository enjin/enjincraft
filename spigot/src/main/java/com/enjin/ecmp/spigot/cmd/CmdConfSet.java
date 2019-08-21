package com.enjin.ecmp.spigot.cmd;

import com.enjin.ecmp.spigot.SpigotBootstrap;
import com.enjin.ecmp.spigot.cmd.arg.LocaleArgumentProcessor;
import com.enjin.ecmp.spigot.configuration.EnjConfig;
import com.enjin.ecmp.spigot.enums.Permission;
import com.enjin.ecmp.spigot.i18n.Locale;
import com.enjin.ecmp.spigot.i18n.Translation;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class CmdConfSet extends EnjCommand {

    public CmdConfSet(SpigotBootstrap bootstrap, EnjCommand parent) {
        super(bootstrap, parent);
        this.aliases.add("set");
        this.requiredArgs.add("setting");
        this.requirements = CommandRequirements.builder()
                .withPermission(Permission.CMD_SET)
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
                    .withPermission(Permission.CMD_SET)
                    .build();
        }

        @Override
        public List<String> tab(CommandContext context) {
            if (context.args.size() == 1)
                return LocaleArgumentProcessor.INSTANCE.tab();
            return new ArrayList<>(0);
        }

        @Override
        public void execute(CommandContext context) {
            Optional<Locale> locale = LocaleArgumentProcessor.INSTANCE.parse(context.args.get(0));

            if (!locale.isPresent()) {
                // TODO invalid lang message
                return;
            }

            EnjConfig config = bootstrap.getConfig();
            config.setLocale(locale.get().locale());
            config.save();
            bootstrap.loadLocale();
            // TODO: success message
        }

        @Override
        public Translation getUsageTranslation() {
            return Translation.COMMAND_CONF_SET_LANG_DESCRIPTION;
        }
    }

}
