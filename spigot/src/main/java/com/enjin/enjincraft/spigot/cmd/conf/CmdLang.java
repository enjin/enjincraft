package com.enjin.enjincraft.spigot.cmd.conf;

import com.enjin.enjincraft.spigot.SpigotBootstrap;
import com.enjin.enjincraft.spigot.cmd.CommandContext;
import com.enjin.enjincraft.spigot.cmd.CommandRequirements;
import com.enjin.enjincraft.spigot.cmd.EnjCommand;
import com.enjin.enjincraft.spigot.cmd.arg.LocaleArgumentProcessor;
import com.enjin.enjincraft.spigot.configuration.Conf;
import com.enjin.enjincraft.spigot.enums.Permission;
import com.enjin.enjincraft.spigot.i18n.Locale;
import com.enjin.enjincraft.spigot.i18n.Translation;

import java.util.ArrayList;
import java.util.List;

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
        if (context.args().size() == 1)
            return LocaleArgumentProcessor.INSTANCE.tab(context.sender(), context.args().get(0));

        return new ArrayList<>(0);
    }

    @Override
    public void execute(CommandContext context) {
        Locale locale = LocaleArgumentProcessor.INSTANCE
                .parse(context.sender(), context.args().get(0))
                .orElse(null);
        if (locale == null) {
            // TODO: Add Translation.
            return;
        }

        Conf config = bootstrap.getConfig();
        config.setLocale(locale);
        bootstrap.plugin().saveConfig();
        Translation.setServerLocale(locale);
    }

    @Override
    public Translation getUsageTranslation() {
        return Translation.COMMAND_CONF_SET_LANG_DESCRIPTION;
    }
}
