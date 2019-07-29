package com.enjin.ecmp.spigot_framework.player;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class PlayerScoreboard {

    private static String title = ChatColor.GOLD.toString() + ChatColor.BOLD.toString() + "ENJIN COIN";

    private static String enjKey = "enj";
    private static String enjEntry = "ENJ: " + ChatColor.GOLD.toString();

    private static String ethKey = "eth";
    private static String ethEntry = "ETH: " + ChatColor.GOLD.toString();

    private static String statusKey = "status";
    private static String statusEntry = "Status: ";

    private static String urlKey = "url";
    private static String urlEntry = ChatColor.YELLOW.toString() + "www.enjincoin.io";

    private MinecraftPlayer owner;

    private boolean enabled;

    private Scoreboard scoreboard;

    private Team enj;
    private Team eth;
    private Team status;
    private Team url;

    public PlayerScoreboard(MinecraftPlayer owner) {
        this.owner = owner;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;

        Player player = this.owner.getBukkitPlayer();
        if (enabled) {
            if (this.scoreboard == null) {
                init();
            } else {
                update();
            }

            player.setScoreboard(this.scoreboard);
        } else {
            player.setScoreboard(Bukkit.getScoreboardManager().getNewScoreboard());
        }
    }

    public void update() {
        String enjBalance = (!owner.isIdentityLoaded()) ? "N/A" : formatDouble(owner.getEnjBalance());
        String ethBalance = (!owner.isIdentityLoaded()) ? "N/A" : formatDouble(owner.getEnjBalance());
        String linkStatus = (!owner.isIdentityLoaded()) ? "N/A"
                : (owner.getLinkingCode() == null ? ChatColor.DARK_PURPLE + "linked"
                : ChatColor.GOLD + owner.getLinkingCode());

        enj.setSuffix(enjBalance);
        eth.setSuffix(ethBalance);
        status.setSuffix(linkStatus);
    }

    private void init() {
        scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();

        Objective objective = scoreboard.registerNewObjective("Test", "Dummy", title);
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);

        this.enj = scoreboard.registerNewTeam(enjKey);
        this.enj.addEntry(enjEntry);

        this.eth = scoreboard.registerNewTeam(ethKey);
        this.eth.addEntry(ethEntry);

        this.status = scoreboard.registerNewTeam(statusKey);
        this.status.addEntry(statusEntry);

        this.url = scoreboard.registerNewTeam(urlKey);
        this.url.addEntry(urlEntry);

        objective.getScore(getSpacer(3)).setScore(7);
        objective.getScore(enjEntry).setScore(6);
        objective.getScore(getSpacer(2)).setScore(5);
        objective.getScore(ethEntry).setScore(4);
        objective.getScore(getSpacer(1)).setScore(3);
        objective.getScore(statusEntry).setScore(2);
        objective.getScore(getSpacer(0)).setScore(1);
        objective.getScore(urlEntry).setScore(0);

        update();
    }

    /**
     * Formats a number to display no more than two decimals.
     *
     * @param value The value to format.
     * @return The formatted string.
     */
    private String formatDouble(BigDecimal value) {
        String retv = "";

        if (value == null) {
            retv = "N/A";
        } else {
            BigDecimal bd = value;
            bd = bd.setScale(2, RoundingMode.DOWN);
            retv = String.valueOf(bd);
        }

        return retv;
    }

    private String getSpacer(int spaces) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < spaces; i++) {
            builder.append(' ');
        }
        return builder.toString();
    }

}
