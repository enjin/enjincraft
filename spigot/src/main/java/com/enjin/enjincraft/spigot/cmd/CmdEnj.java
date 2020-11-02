package com.enjin.enjincraft.spigot.cmd;

import com.enjin.enjincraft.spigot.SpigotBootstrap;
import com.enjin.enjincraft.spigot.cmd.conf.CmdConf;
import com.enjin.enjincraft.spigot.cmd.player.CmdLink;
import com.enjin.enjincraft.spigot.cmd.player.CmdQr;
import com.enjin.enjincraft.spigot.cmd.player.CmdUnlink;
import com.enjin.enjincraft.spigot.cmd.token.CmdToken;
import com.enjin.enjincraft.spigot.cmd.wallet.CmdBalance;
import com.enjin.enjincraft.spigot.cmd.wallet.CmdWallet;
import com.enjin.enjincraft.spigot.cmd.wallet.send.CmdDevSend;
import com.enjin.enjincraft.spigot.cmd.wallet.send.CmdSend;
import com.enjin.enjincraft.spigot.cmd.wallet.trade.CmdTrade;
import com.enjin.enjincraft.spigot.enums.CommandProcess;
import com.enjin.enjincraft.spigot.enums.Usage;
import com.enjin.enjincraft.spigot.i18n.Translation;
import com.enjin.enjincraft.spigot.player.UnregisteredPlayerException;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescriptionFile;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CmdEnj extends EnjCommand implements CommandExecutor, TabCompleter {

    private final CmdHelp cmdHelp;

    public CmdEnj(SpigotBootstrap bootstrap) {
        super(bootstrap);
        this.aliases.add("enj");
        this.addSubCommand(new CmdBalance(this));
        this.addSubCommand(new CmdConf(this));
        this.addSubCommand(new CmdDevSend(this));
        this.addSubCommand(this.cmdHelp = new CmdHelp(this));
        this.addSubCommand(new CmdLink(this));
        this.addSubCommand(new CmdQr(this));
        this.addSubCommand(new CmdSend(this));
        this.addSubCommand(new CmdToken(this));
        this.addSubCommand(new CmdTrade(this));
        this.addSubCommand(new CmdUnlink(this));
        this.addSubCommand(new CmdWallet(this));
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        try {
            process(new CommandContext(bootstrap, sender, new ArrayList<>(Arrays.asList(args)), label), CommandProcess.EXECUTE);
        } catch (UnregisteredPlayerException ex) {
            bootstrap.log(ex);
        }

        return true;
    }

    @Override
    public void execute(CommandContext context) {
        Plugin plugin = bootstrap.plugin();
        PluginDescriptionFile description = plugin.getDescription();
        Translation.COMMAND_ROOT_DETAILS.send(context.sender,
                description.getName(),
                description.getVersion(),
                cmdHelp.getUsage(context.senderType, Usage.COMMAND_ONLY));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        CommandContext context = new CommandContext(bootstrap, sender, new ArrayList<>(Arrays.asList(args)), label);
        process(context, CommandProcess.TAB);
        return context.tabCompletionResult;
    }

    @Override
    public Translation getUsageTranslation() {
        return Translation.COMMAND_ROOT_DESCRIPTION;
    }

}
