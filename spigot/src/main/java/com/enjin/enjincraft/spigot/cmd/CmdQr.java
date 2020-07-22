package com.enjin.enjincraft.spigot.cmd;

import com.enjin.enjincraft.spigot.SpigotBootstrap;
import com.enjin.enjincraft.spigot.enums.Permission;
import com.enjin.enjincraft.spigot.i18n.Translation;
import com.enjin.enjincraft.spigot.player.EnjPlayer;
import com.enjin.enjincraft.spigot.util.QrUtils;
import com.enjin.minecraft_commons.spigot.map.ImageRenderer;
import de.tr7zw.changeme.nbtapi.NBTItem;
import lombok.NonNull;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.map.MapView;

import java.awt.*;
import java.util.Map;
import java.util.Objects;

public class CmdQr extends EnjCommand {

    public CmdQr(SpigotBootstrap bootstrap, EnjCommand parent) {
        super(bootstrap, parent);
        this.aliases.add("qr");
        this.requirements = CommandRequirements.builder()
                .withAllowedSenderTypes(SenderType.PLAYER)
                .withPermission(Permission.CMD_LINK)
                .build();
    }

    @Override
    public void execute(CommandContext context) {
        Player sender = Objects.requireNonNull(context.player);

        EnjPlayer senderEnjPlayer = getValidSenderEnjPlayer(context);
        if (senderEnjPlayer == null)
            return;

        Image qr = senderEnjPlayer.getLinkingCodeQr();
        if (qr == null) {
            Translation.COMMAND_QR_CODENOTLOADED.send(sender);
            return;
        }

        NBTItem   nbtItem;
        ItemStack is;
        try {
            nbtItem       = new NBTItem(new ItemStack(Material.FILLED_MAP));
            ItemMeta meta = nbtItem.getItem().getItemMeta();
            if (!(meta instanceof MapMeta))
                throw new Exception("Map does not contain map metadata");

            MapView map = Bukkit.createMap(sender.getWorld());
            ImageRenderer.apply(map, qr);

            meta.setDisplayName(ChatColor.DARK_PURPLE + Translation.QR_DISPLAYNAME.translation());
            ((MapMeta) meta).setMapView(map);
            nbtItem.getItem().setItemMeta(meta);
            nbtItem.setBoolean(QrUtils.QR_TAG, true);

            is = nbtItem.getItem().clone();
        } catch (Exception e) {
            Translation.COMMAND_QR_ERROR.send(sender);
            bootstrap.log(e);
            return;
        }

        senderEnjPlayer.removeQrMap();
        if (!placeQrInInventory(sender, is))
            Translation.COMMAND_QR_INVENTORYFULL.send(sender);
    }

    @Override
    protected EnjPlayer getValidSenderEnjPlayer(@NonNull CommandContext context) throws NullPointerException {
        Player sender = Objects.requireNonNull(context.player, "Expected context to have non-null player as sender");

        EnjPlayer senderEnjPlayer = context.enjPlayer;
        if (senderEnjPlayer == null) {
            Translation.ERRORS_PLAYERNOTREGISTERED.send(sender, sender.getName());
            return null;
        } else if (!senderEnjPlayer.isLoaded()) {
            Translation.IDENTITY_NOTLOADED.send(sender);
            return null;
        } else if (senderEnjPlayer.isLinked()) {
            Translation.COMMAND_QR_ALREADYLINKED.send(sender);
            return null;
        }

        return senderEnjPlayer;
    }

    @Override
    public Translation getUsageTranslation() {
        return Translation.COMMAND_QR_DESCRIPTION;
    }

    private boolean placeQrInInventory(Player player, ItemStack is) {
        PlayerInventory inventory = player.getInventory();

        if (inventory.getItemInOffHand().getType() == Material.AIR) {
            inventory.setItemInOffHand(is);
            return true;
        }

        Map<Integer, ItemStack> leftOver = inventory.addItem(is);

        return leftOver.size() == 0;
    }
}
