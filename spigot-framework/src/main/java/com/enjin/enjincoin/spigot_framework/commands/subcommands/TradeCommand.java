package com.enjin.enjincoin.spigot_framework.commands.subcommands;

import com.enjin.enjincoin.sdk.client.service.identities.vo.Identity;
import com.enjin.enjincoin.spigot_framework.BasePlugin;
import com.enjin.enjincoin.spigot_framework.player.MinecraftPlayer;
import com.enjin.enjincoin.spigot_framework.player.PlayerManager;
import com.enjin.enjincoin.spigot_framework.trade.TradeManager;
import com.enjin.enjincoin.spigot_framework.util.MessageUtils;
import net.kyori.text.TextComponent;
import net.kyori.text.event.ClickEvent;
import net.kyori.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;


import java.util.Arrays;

import static org.bukkit.Bukkit.getServer;

/**
 * <p>Trade command handler.</p>
 */
public class TradeCommand {

    /**
     * <p>The spigot plugin.</p>
     */
    private BasePlugin plugin;

    /**
     * <p>Empty line helper for MessageUtils.sendMessage</p>
     */
    private final TextComponent newline = TextComponent.of("");

    /**
     * <p>Link command handler constructor.</p>
     *
     * @param plugin the Spigot plugin
     */
    public TradeCommand(BasePlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * <p>Executes and performs operations defined for the command.</p>
     *
     * @param sender the command sender
     * @param args   the command arguments
     *
     * @since 1.0
     */
    public void execute(Player sender, String[] args) {
        if (args.length > 0) {
            String sub = args[0];
            String[] subArgs = args.length == 1 ? new String[0] : Arrays.copyOfRange(args, 1, args.length);
            if (sub.equalsIgnoreCase("legacy")) {
                legacy(sender, subArgs);
            } else if (sub.equalsIgnoreCase("invite")) {
                invite(sender, subArgs);
            } else if (sub.equalsIgnoreCase("accept")) {
                inviteAccept(sender, subArgs);
            }
        }
    }

    private void invite(Player sender, String[] args) {
        if (args.length > 0) {
            Player target = Bukkit.getPlayer(args[0]);
            if (target != null) {
                if (target != sender) {
                    PlayerManager playerManager = this.plugin.getBootstrap().getPlayerManager();
                    TradeManager tradeManager = this.plugin.getBootstrap().getTradeManager();
                    MinecraftPlayer senderMP = playerManager.getPlayer(sender.getUniqueId());
                    MinecraftPlayer targetMP = playerManager.getPlayer(target.getUniqueId());
                    boolean result = tradeManager.addInvite(senderMP, targetMP);

                    if (result) {
                        final TextComponent.Builder inviteMessageBuilder = TextComponent.builder("")
                                .color(TextColor.GRAY)
                                .append(TextComponent.builder(String.format("%s", sender.getName()))
                                        .color(TextColor.GOLD)
                                        .build())
                                .append(TextComponent.of(" has invited you to trade. "))
                                .append(TextComponent.builder("Accept")
                                        .color(TextColor.GREEN)
                                        .clickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,
                                                String.format("/enj trade accept %s", sender.getName())))
                                        .build())
                                .append(TextComponent.of(" | "))
                                .append(TextComponent.builder("Decline")
                                        .color(TextColor.RED)
                                        .clickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,
                                                String.format("/enj trade decline %s", sender.getName())))
                                        .build());
                        MessageUtils.sendMessage(target, inviteMessageBuilder.build());
                    } else {
                        // TODO: Info: a trade invite with that player is already open!
                    }
                } else {
                    // TODO: Error: cannot invite yourself to trade!
                }
            } else {
                // TODO: Error: could not find the player you specified!
            }
        } else {
            // TODO: Display some form of player selection ui.
        }
    }

    private void inviteAccept(Player sender, String[] args) {
        if (args.length > 0) {
            Player target = Bukkit.getPlayer(args[0]);
            if (target != null) {
                PlayerManager playerManager = this.plugin.getBootstrap().getPlayerManager();
                TradeManager tradeManager = this.plugin.getBootstrap().getTradeManager();
                MinecraftPlayer senderMP = playerManager.getPlayer(target.getUniqueId());
                MinecraftPlayer targetMP = playerManager.getPlayer(sender.getUniqueId());

                boolean result = tradeManager.acceptInvite(senderMP, targetMP);

                if (!result) {
                    // TODO: No open invite or player is already in a trade
                }
            }
        } else {
            // TODO: Error: no player name was provided!
        }
    }

    private void inviteDecline(Player sender, String[] args) {
        if (args.length > 0) {
            Player target = Bukkit.getPlayer(args[0]);

            // TODO: Decline player invite in trade manager.
        } else {
            // TODO: Error: no player name was provided!
        }
    }

    private void legacy(Player sender, String[] args) {
        MinecraftPlayer mcplayer = this.plugin.getBootstrap().getPlayerManager().getPlayer(sender.getUniqueId());

        // not a minecraft player...
        if (mcplayer == null) return;

        Identity identity = mcplayer.getIdentity();

        // no tokens to trade
        if (identity == null || identity.getTokens().isEmpty()) return;

        sender.sendMessage(ChatColor.GOLD + "Currently Online Players: " + "" + Bukkit.getServer().getOnlinePlayers().size() + "/" + Bukkit.getServer().getMaxPlayers());
        for(Player p : Bukkit.getOnlinePlayers()) {
            sender.sendMessage(ChatColor.GREEN + "" + p.getDisplayName() );
        }

        final TextComponent.Builder component = TextComponent.builder().content("").color(TextColor.GRAY);

        if (args.length <= 0) {
            component.append(TextComponent.builder().content("ENJ TRADING").color(TextColor.DARK_PURPLE).build());
            component.append(TextComponent.builder().content(" ").build());
            component.append(TextComponent.builder().content(" Click to: ").build());
            component.append(TextComponent.builder().content("Start trade").color(TextColor.DARK_AQUA).clickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/enj trade start_trade")).build());
            component.append(TextComponent.builder().content(" | ").build());
            component.append(TextComponent.builder().content("Resume trade").color(TextColor.DARK_AQUA).clickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/enj trade resume_trade")).build());
        }

        // NOTE some elements may require lists with next/previous options such as player lists, items lists and review trades
        if (args.length > 0) {
            switch(args[0]) {
                case "start_trade":
                    // build new trade object and set sender
                    // set Prompt to [TRADE: ID]
                    // save argument to status
                    // run command /enj trade with
                    component.append(TextComponent.builder().content("Registering new trade... ").build());
                    component.append(TextComponent.builder().content("Lets get started!").color(TextColor.DARK_AQUA).clickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/enj trade with")).build());
                    break;
                case "with":
                    // if args[1] == null
                    // show list
                    // show help /enj trade with playername
                    // and suggest command /enj trade with
                    // else
                    // validate receiver (args[1]) and save receiver to trade
                    // save argument to status
                    // run command /enj trade item

                    if (args[1] == null) {
                        component.append(TextComponent.builder().content("show online user list").build());
                        component.append(TextComponent.builder().content("Start a new trade").color(TextColor.DARK_AQUA).clickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/enj trade with")).build());
                    } else {
                        component.append(TextComponent.builder().content(args[1] + " selected...").build());
                        component.append(TextComponent.builder().content("Start a new trade").color(TextColor.DARK_AQUA).clickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/enj trade item")).build());
                    }
                    break;
                case "":
                    // if args[1] == null
                    // show list of valid items from senders wallet + inventory
                    // show help /enj trade item itemName
                    // suggest command /enj trade item
                    // else
                    // validate item (args[1])
                    // construct a new senderOffer with item
                    // register senderOffer to trade
                    // save argument to status
                    // run command /enj trade amount
                    if (args[1] == null) {
                        component.append(TextComponent.builder().content("show item list").build());
                        component.append(TextComponent.builder().content("ITEM #1").color(TextColor.DARK_AQUA).clickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/enj trade item")).build());
                    } else {
                        component.append(TextComponent.builder().content(args[1] + " selected...").build());
                        component.append(TextComponent.builder().content("Yes").color(TextColor.DARK_AQUA).clickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/enj trade amount")).build());
                        component.append(TextComponent.builder().content(" or ").build());
                        component.append(TextComponent.builder().content("No").color(TextColor.DARK_AQUA).clickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/enj trade item")).build());
                    }
                    break;
                case "amount":
                    // if args[1] == null
                    // show number available for trade.senderOffer.item
                    // show prompt for quantity (minimum 1
                    // suggest command /enj trade amount
                    // else
                    // validate quantity to send
                    // register quantity to trade.senderOffer
                    // save argument to status
                    // run command /enj trade validate
                    if (args[1] == null) {
                        component.append(TextComponent.builder().content("how many?").build());
                        component.append(TextComponent.builder().content("Specify quantity").color(TextColor.DARK_AQUA).clickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/enj trade amount")).build());
                    } else {
                        component.append(TextComponent.builder().content(args[1] + " of Item, is this correct?").build());
                        component.append(TextComponent.builder().content("Yes").color(TextColor.DARK_AQUA).clickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/enj trade item")).build());
                        component.append(TextComponent.builder().content(" or ").build());
                        component.append(TextComponent.builder().content("No").color(TextColor.DARK_AQUA).clickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/enj trade amount")).build());
                    }
                    break;
                case "verify":
                    // validate the current trade programmatically
                    // Show trade summary to sender
                    // Show send trade or cancel click options
                    // save argument to status
                    // either
                    // run command /enj trade send
                    // run command /enj trade discard
                    break;
                case "send":
                    // send trade offer to receiver
                    // if receiver does not have a trade active, set this trade as active for receiver
                    // else
                    // notify receiver of a pending trade for review/resume
                    // register trade with receiver as resumable
                    // show trade offer summary from sender to receiver
                    // save argument to status
                    // prompt receiver to accept (as gift) or make an offer
                    break;
                case "discard":
                    // cancel current transaction and remove from trade manager
                    // inform sender that the transaction was discards
                    // if the trade was pending receiver, notify receiver that the trade has been canceled
                    break;
                case "resume_trade":
                    // if args[1] == null
                    // fetch a list of available pending trades and their current status
                    // build clickable list of pending trades
                    // click results in
                    // run command /enj trade resume_trade trade_id
                    // else (trade_id)
                    // validate trade_id and trade status
                    // set selected trade as active trade for sender
                    // run command /enj trade <status>
                    break;
                default:
                    break;
            }
        }

        MessageUtils.sendMessage(sender, component.build());
    }

    private void errorInvalidUuid(CommandSender sender) {
        final TextComponent text = TextComponent.of("The UUID provided is invalid.")
                .color(TextColor.RED);

        MessageUtils.sendMessage(sender, newline);
        MessageUtils.sendMessage(sender, text);
    }

    /**
     * method should just provide user feedback that the player's identity is already unlinked then provide
     * the linking code for them to use to link a wallet.
     *
     * @param sender
     * @param address
     */
    private void handleError(CommandSender sender, String address) {
        TextComponent text = TextComponent.of("You have not linked a wallet to your account.").color(TextColor.RED);
        MessageUtils.sendMessage(sender, text);
        text = TextComponent.of("Please type '/enj link' to link your account to your Enjin Wallet.").color(TextColor.RED);
        MessageUtils.sendMessage(sender, text);
    }
}
