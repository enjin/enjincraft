package com.enjin.enjincraft.spigot.token;

import com.google.gson.annotations.SerializedName;
import de.tr7zw.changeme.nbtapi.NBTContainer;
import de.tr7zw.changeme.nbtapi.NBTItem;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;
import org.bukkit.World;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

@ToString
public class TokenModel {

    public static final String NBT_ID = "tokenID";

    private transient NBTContainer nbtContainer;
    private transient NBTItem nbtItem;
    @Getter
    private transient String displayName;

    @Getter
    private String id;

    @Getter
    private String nbt;

    @SerializedName("assignable-permissions")
    @Getter
    private Map<String, List<String>> assignablePermissions;

    @Builder
    public TokenModel(@NonNull String id, @NonNull String nbt, HashMap<String, List<String>> assignablePermissions) {
        this.id = id;
        this.nbt = nbt;
        this.assignablePermissions = assignablePermissions == null ? new HashMap<>() : assignablePermissions;
    }

    protected void load() {
        nbtContainer = new NBTContainer(nbt);
        nbtItem =  new NBTItem(NBTItem.convertNBTtoItem(nbtContainer));
        nbtItem.setString(NBT_ID, id);
        displayName = nbtItem.getItem().getItemMeta().getDisplayName();
    }

    protected boolean applyBlacklist(Collection<String> blacklist) {
        AtomicBoolean result = new AtomicBoolean(true);

        assignablePermissions.forEach((world, strings) -> {
            if (!strings.removeAll(blacklist))
                result.set(false);
        });

        return result.get();
    }

    public ItemStack getItemStack() {
        ItemStack stack = nbtItem.getItem().clone();
        ItemMeta meta = stack.getItemMeta();

        meta.setUnbreakable(true);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE);

        stack.setItemMeta(meta);

        return stack;
    }

    public boolean addPermission(String permission, String world) {
        List<String> worldPerms = assignablePermissions.computeIfAbsent(world, k -> new ArrayList<>());

        // Prevents duplicate permissions from being added
        if (!worldPerms.contains(permission)) {
            worldPerms.add(permission);
            return true;
        }

        return false;
    }

    public boolean addPermissionToWorlds(String permission, Collection<String> worlds) {
        AtomicBoolean result = new AtomicBoolean(true);

        worlds.forEach(world -> {
            if (addPermission(permission, world))
                result.set(false);
        });

        return result.get();
    }

    public boolean removePermission(String permission, String world) {
        List<String> worldPerms = assignablePermissions.get(world);

        if (worldPerms != null)
            return worldPerms.remove(permission);

        return false;
    }

    public boolean removePermissionFromWorlds(String permission, Collection<String> worlds) {
        AtomicBoolean result = new AtomicBoolean(true);

        worlds.forEach(world -> {
            if (!removePermission(permission, world))
                result.set(false);
        });

        return result.get();
    }

}
