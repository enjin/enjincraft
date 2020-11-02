package com.enjin.enjincraft.spigot.cmd.conf;

import com.enjin.enjincraft.spigot.SpigotBootstrap;
import com.enjin.enjincraft.spigot.cmd.CommandContext;
import com.enjin.enjincraft.spigot.cmd.CommandRequirements;
import com.enjin.enjincraft.spigot.cmd.EnjCommand;
import com.enjin.enjincraft.spigot.enums.Permission;
import com.enjin.enjincraft.spigot.i18n.Translation;

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

}
