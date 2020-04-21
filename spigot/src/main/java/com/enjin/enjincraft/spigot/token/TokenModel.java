package com.enjin.enjincraft.spigot.token;

import com.google.gson.annotations.SerializedName;
import de.tr7zw.changeme.nbtapi.NBTContainer;
import de.tr7zw.changeme.nbtapi.NBTItem;
import lombok.*;
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
    @Setter(AccessLevel.PROTECTED)
    @SerializedName("alternate-id")
    private String alternateId;

    @Getter
    private String nbt;

    @SerializedName("assignable-permissions")
    private List<TokenPermission> assignablePermissions;

    @Builder
    public TokenModel(@NonNull String id, String alternateId, @NonNull String nbt, List<TokenPermission> assignablePermissions) {
        this.id = id;
        this.alternateId = alternateId;
        this.nbt = nbt;
        this.assignablePermissions = assignablePermissions == null ? new ArrayList<>() : assignablePermissions;
    }

    protected void load() {
        nbtContainer = new NBTContainer(nbt);
        nbtItem =  new NBTItem(NBTItem.convertNBTtoItem(nbtContainer));
        nbtItem.setString(NBT_ID, id);
        displayName = nbtItem.getItem().getItemMeta().getDisplayName();
    }

    protected boolean applyBlacklist(Collection<String> blacklist) {
        AtomicBoolean result = new AtomicBoolean(true);

        blacklist.forEach(permission -> {
            TokenPermission tokenPerm = new TokenPermission(permission, TokenManager.GLOBAL);
            int idx = assignablePermissions.indexOf(tokenPerm);

            // Checks if the blacklisted permission was not removed
            if (idx >= 0 && !assignablePermissions.remove(tokenPerm))
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

    public boolean addPermission(String permission) {
        return addPermissionToWorld(permission, TokenManager.GLOBAL);
    }

    public boolean addPermissionToWorld(String permission, String world) {
        TokenPermission tokenPerm = new TokenPermission(permission, world);
        int idx = assignablePermissions.indexOf(tokenPerm);

        if (idx < 0)
            return assignablePermissions.add(tokenPerm);

        TokenPermission other = assignablePermissions.get(idx);

        // This permission is applied globally
        if (world.equals(TokenManager.GLOBAL) && other.getWorlds().contains(TokenManager.GLOBAL)) {
            return false;
        } else if (world.equals(TokenManager.GLOBAL) && !other.getWorlds().contains(TokenManager.GLOBAL)) {
            assignablePermissions.remove(idx);
            return assignablePermissions.add(tokenPerm);
        }

        other.getWorlds().remove(TokenManager.GLOBAL); // This permission is not applied globally

        return other.addWorld(world);
    }

    public boolean addPermissionToWorlds(String permission, Collection<String> worlds) {
        TokenPermission tokenPerm = new TokenPermission(permission, worlds);
        int idx = assignablePermissions.indexOf(tokenPerm);

        if (idx < 0)
            return assignablePermissions.add(tokenPerm);

        TokenPermission other = assignablePermissions.get(idx);

        // This permission is applied globally
        if (worlds.contains(TokenManager.GLOBAL) && other.getWorlds().contains(TokenManager.GLOBAL)) {
            return false;
        } else if (worlds.contains(TokenManager.GLOBAL) && !other.getWorlds().contains(TokenManager.GLOBAL)) {
            assignablePermissions.remove(idx);
            return assignablePermissions.add(tokenPerm);
        }

        other.getWorlds().remove(TokenManager.GLOBAL); // This permission is not applied globally

        return other.addWorlds(worlds);
    }

    public boolean removePermission(String permission) {
        return removePermissionFromWorld(permission, TokenManager.GLOBAL);
    }

    public boolean removePermissionFromWorld(String permission, String world) {
        TokenPermission tokenPerm = new TokenPermission(permission, world);
        int idx = assignablePermissions.indexOf(tokenPerm);

        if (idx < 0)
            return false;
        else if (world.equals(TokenManager.GLOBAL))
            return assignablePermissions.remove(tokenPerm);

        TokenPermission other = assignablePermissions.get(idx);

        // Checks if the world was not removed
        if (!other.getWorlds().remove(world))
            return false;
        else if (other.getWorlds().size() == 0)
            assignablePermissions.remove(other);

        return true;
    }

    public boolean removePermissionFromWorlds(String permission, Collection<String> worlds) {
        TokenPermission tokenPerm = new TokenPermission(permission, worlds);
        int idx = assignablePermissions.indexOf(tokenPerm);

        if (idx < 0)
            return false;
        else if (worlds.contains(TokenManager.GLOBAL))
            return assignablePermissions.remove(tokenPerm);

        TokenPermission other = assignablePermissions.get(idx);

        // Checks if any worlds were not removed
        if (!other.getWorlds().removeAll(worlds))
            return false;
        else if (other.getWorlds().size() == 0)
            assignablePermissions.remove(other);

        return true;
    }

    public Map<String, Set<String>> getAssignablePermissions() {
        Map<String, Set<String>> permissionMap = new HashMap<>();

        for (TokenPermission tokenPerm : assignablePermissions) {
            String permission = tokenPerm.getPermission();
            Set<String> worlds = tokenPerm.getWorlds();

            if (worlds.contains(TokenManager.GLOBAL)) {
                continue;
            }

            worlds.forEach(world -> {
                Set<String> worldPerms = permissionMap.computeIfAbsent(world, k -> new HashSet<>());
                worldPerms.add(permission);
            });
        }

        return permissionMap;
    }

}
