package com.enjin.enjincraft.spigot.token;

import com.google.gson.*;
import com.google.gson.annotations.SerializedName;
import de.tr7zw.changeme.nbtapi.NBTContainer;
import de.tr7zw.changeme.nbtapi.NBTItem;
import lombok.*;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.lang.reflect.Type;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

@ToString
public class TokenModel {

    public static final String NBT_ID = "tokenID";

    private transient NBTContainer nbtContainer;
    private transient NBTItem nbtItem;
    @Getter
    private transient String displayName;
    private transient Object uriLock = new Object();

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

    @Getter(onMethod_ = {@Synchronized("uriLock")})
    @Setter(value = AccessLevel.PROTECTED, onMethod_ = {@Synchronized("uriLock")})
    @SerializedName("metadata-uri")
    private String metadataURI;

    public TokenModel(@NonNull String id,
                      String alternateId,
                      @NonNull String nbt,
                      List<TokenPermission> assignablePermissions) {
        this(id, alternateId, nbt, assignablePermissions, null);
    }

    @Builder
    public TokenModel(@NonNull String id,
                      String alternateId,
                      @NonNull String nbt,
                      List<TokenPermission> assignablePermissions,
                      String metadataURI) {
        this.id = id;
        this.alternateId = alternateId;
        this.nbt = nbt;
        this.assignablePermissions = assignablePermissions == null ? new ArrayList<>() : assignablePermissions;
        this.metadataURI = metadataURI;
    }

    protected void load() {
        nbtContainer = new NBTContainer(nbt);
        nbtItem =  new NBTItem(NBTItem.convertNBTtoItem(nbtContainer));
        nbtItem.setString(NBT_ID, id);

        ItemMeta meta = nbtItem.getItem().getItemMeta();
        if (meta != null)
            displayName = meta.getDisplayName();
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
        return getItemStack(false);
    }

    public ItemStack getItemStack(boolean raw) {
        ItemStack is;

        if (raw) {
            NBTItem item = new NBTItem(NBTItem.convertNBTtoItem(nbtContainer));

            if (item.hasKey(NBT_ID))
                item.removeKey(NBT_ID);

            is = item.getItem().clone();
        } else {
            is = nbtItem.getItem().clone();
        }

        ItemMeta meta = is.getItemMeta();

        meta.setUnbreakable(true);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE);

        is.setItemMeta(meta);

        return is;
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

    public List<TokenPermission> getAssignablePermissions() {
        List<TokenPermission> permissions = new ArrayList<>(assignablePermissions.size());

        assignablePermissions.forEach(permission -> {
            TokenPermission copy = new TokenPermission(permission.getPermission(), permission.getWorlds());
            permissions.add(copy);
        });

        return permissions;
    }

    public Map<String, Set<String>> getPermissionsMap() {
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

    public static class TokenModelDeserializer implements JsonDeserializer<TokenModel> {

        private static final Gson gson = new GsonBuilder()
                .registerTypeAdapter(TokenPermission.class, new TokenPermission.TokenPermissionDeserializer())
                .setPrettyPrinting()
                .create();

        @Override
        public TokenModel deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            TokenModel tokenModel = gson.fromJson(json, TokenModel.class);
            tokenModel.uriLock = new Object();

            return tokenModel;
        }

    }

}
