package com.enjin.enjincoin.spigot_framework.util;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.enjin.enjincoin.spigot_framework.player.MinecraftPlayer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;

/**
 * Class to set and clear a player's sidebar scoreboard.
 */
public class Scoreboards implements Listener {

	private Map<UUID, Scoreboard> scoreboards = new HashMap<>();

	/**
	 * Sets a player's scoreboard sidebar with the Enj Coin stats.
	 * 
	 * @param mcplayer The minecraft player container.
	 */
	public void setSidebar(MinecraftPlayer mcplayer) {

		Player player = mcplayer.getBukkitPlayer();

		String enjBalance = (mcplayer.getIdentityData() == null) ? "0.0" : String.valueOf(mcplayer.getIdentityData().getEnjBalance());
		String ethBalance = (mcplayer.getIdentityData() == null) ? "0.0" : String.valueOf(mcplayer.getIdentityData().getEthBalance());
		String status = (mcplayer.getIdentity().getLinkingCode() == null) ?
				ChatColor.DARK_PURPLE + "linked" : ChatColor.GOLD + mcplayer.getIdentity().getLinkingCode();

		int total = mcplayer.getIdentity().getTokens().size();

		String title = ChatColor.GOLD + ChatColor.BOLD.toString() + "Enjin".toUpperCase();
		List<String> lines = new LinkedList<>();
		// blank spacer lines of varying number of whitespaces as scoreboard sidebar does not display duplicate lines
		lines.add("    ");
		lines.add(format("ENJ ", ChatColor.GOLD + enjBalance));
		lines.add("   ");
		lines.add(format("ETH ", ChatColor.GOLD + ethBalance));
		lines.add("  ");
		lines.add(format("Status", status));
		lines.add(" ");
		lines.add(format("Tokens", total));
		lines.add("");
		lines.add(ChatColor.YELLOW + "www.enjin.com");

		setSidebar(player, title, lines);
	}

	/**
	 * Formats a key and value to be displayed on a scoreboard sidebar.
	 * 
	 * @param key The key name.
	 * @param value The value.
	 * 
	 * @return The formatted string.
	 */
	private String format(String key, Object value) {
		return key + ": " + ChatColor.GREEN + value.toString();
	}

	/**
	 * Sets a player's scoreboard sidebar with specified text.
	 * 
	 * @param player The player.
	 * @param title The title to be displayed at the top of the scoreboard side. Supports chat colors. Maximum of 32 characters.
	 * @param lines The lines of text to be displayed on the scoreboard sidebar. Starting at the top. Maximum of 15 lines. Repeated lines will be not
	 *        be shown.
	 */
	public void setSidebar(Player player, String title, List<String> lines) {

		// throw exception if lines exceed the max that a scoreboard can display
		if (lines.size() > 15) {
			throw new IllegalArgumentException("Scoreboard sidebar can only have 15 lines set");
		}

		Scoreboard scoreboard = scoreboards.get(player.getUniqueId());
		// create new sccoreboard if doesn't exist
		if (scoreboard == null) {
			scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
			scoreboards.put(player.getUniqueId(), scoreboard);
			player.setScoreboard(scoreboard);
		}

		// create sidebar objective if doesn't exist
		Objective sidebar = scoreboard.getObjective(DisplaySlot.SIDEBAR);
		if (sidebar == null) {
			sidebar = scoreboard.registerNewObjective(player.getName(), "dummy", title);
			sidebar.setDisplaySlot(DisplaySlot.SIDEBAR);
		}

		// Add lines
		int score = lines.size();
		for (String line : lines) {
			sidebar.getScore(line).setScore(score--);
		}
	}

	public void clearSidebar(Player player) {
		hideSidebar(player);
	}

	/**
	 * Removes a player's scoreboard sidebar.
	 * 
	 * @param player The player.
	 */
	public void hideSidebar(Player player) {

		Scoreboard scoreboard = player.getScoreboard();

		if (scoreboard != null) {

			Objective sidebar = scoreboard.getObjective(DisplaySlot.SIDEBAR);

			if (sidebar != null) {
				sidebar.unregister();
			}
		}

	}

	@EventHandler
	public void onPlayerQuit(PlayerQuitEvent event) {
		Player player = event.getPlayer();
		player.setScoreboard(Bukkit.getScoreboardManager().getNewScoreboard());

		this.scoreboards.remove(player.getUniqueId());
	}

}
