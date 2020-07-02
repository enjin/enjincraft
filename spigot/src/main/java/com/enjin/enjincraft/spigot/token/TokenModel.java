package com.enjin.enjincraft.spigot.token;

import com.enjin.enjincraft.spigot.Bootstrap;
import com.enjin.enjincraft.spigot.EnjinCraft;
import com.enjin.enjincraft.spigot.SpigotBootstrap;
import com.enjin.enjincraft.spigot.util.StringUtils;
import com.enjin.enjincraft.spigot.util.TokenUtils;
import com.enjin.enjincraft.spigot.wallet.TokenViewState;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;
import de.tr7zw.changeme.nbtapi.NBTContainer;
import de.tr7zw.changeme.nbtapi.NBTItem;
import lombok.*;
import org.bukkit.ChatColor;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.net.URL;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

@NoArgsConstructor
@ToString
public class TokenModel {

    public static final String NBT_ID          = "enjinTokenID";
    public static final String NBT_INDEX       = "enjinTokenIndex";
    public static final String NBT_NONFUNGIBLE = "enjinTokenNF";

    private transient NBTContainer nbtContainer;
    private transient NBTItem nbtItem;
    @Getter
    private transient String displayName;
    @Getter(onMethod_ = {@Synchronized("uriLock")})
    @Setter(value = AccessLevel.PROTECTED, onMethod_ = {@Synchronized("uriLock")})
    private transient String nameFromURI;
    @Getter
    @Setter(AccessLevel.PACKAGE)
    private transient boolean markedForDeletion = false;

    // Mutexes
    private final transient Object uriLock = new Object();

    @Getter
    private String id;
    @Getter
    private String index = TokenUtils.BASE_INDEX;
    @Getter
    @Setter(AccessLevel.PACKAGE)
    private boolean nonfungible = false;
    @Getter
    @Setter(AccessLevel.PROTECTED)
    @SerializedName("alternate-id")
    private String alternateId;
    @Getter
    @Setter(AccessLevel.PROTECTED)
    private String nbt;
    @SerializedName("assignable-permissions")
    private List<TokenPermission> assignablePermissions = new ArrayList<>();
    @Getter(onMethod_ = {@Synchronized("uriLock")})
    @Setter(value = AccessLevel.PROTECTED, onMethod_ = {@Synchronized("uriLock")})
    @SerializedName("metadata-uri")
    private String metadataURI;
    @Getter
    @Setter(AccessLevel.PROTECTED)
    private TokenViewState walletViewState = TokenViewState.NORMAL;

    /**
     * Secondary constructor used in earlier versions of the plugin.
     *
     * @since 1.0
     * @param id the token id
     * @param alternateId the alternate id to be used by the token manager
     * @param nbt the NBT of the item stack
     * @param assignablePermissions the list of permissions
     */
    public TokenModel(@NonNull String id,
                      String alternateId,
                      @NonNull String nbt,
                      List<TokenPermission> assignablePermissions) {
        this(id, null, null, alternateId, nbt, assignablePermissions, null, null);
    }

    /**
     * Primary constructor for token models.
     *
     * @since 1.1
     * @param id the token id
     * @param index the token index
     * @param nonfungible the fungible state
     *                    <p>
     *                        True for non-fungible, false for fungible.
     *                    </p>
     * @param alternateId the alternate id to be used by the token manager
     * @param nbt the NBT of the item stack
     * @param assignablePermissions the list of permissions
     * @param metadataURI the metadata uri from the platform
     * @param walletViewState the view state in the token wallet
     * @throws IllegalArgumentException if the id or index are invalid
     * @throws IllegalStateException if is fungible with a non-null, non-base index
     */
    @Builder
    public TokenModel(@NonNull String id,
                      String index,
                      Boolean nonfungible,
                      String alternateId,
                      @NonNull String nbt,
                      List<TokenPermission> assignablePermissions,
                      String metadataURI,
                      TokenViewState walletViewState) throws IllegalArgumentException, IllegalStateException {
        if (!TokenUtils.isValidId(id))
            throw new IllegalArgumentException(String.format("Invalid id: %s", id));
        if (index != null && !TokenUtils.isValidIndex(index))
            throw new IllegalArgumentException(String.format("Invalid index: %s", index));
        if (index != null && !nonfungible && !index.equals(TokenUtils.BASE_INDEX))
            throw new IllegalStateException(String.format("Token %s is fungible but was given a invalid index", id));

        this.id = id;
        this.index = index == null
                ? TokenUtils.BASE_INDEX
                : index;
        this.nonfungible = nonfungible == null
                ? false
                : nonfungible;
        this.alternateId = alternateId;
        this.nbt = nbt;
        this.assignablePermissions = assignablePermissions == null
                ? new ArrayList<>()
                : assignablePermissions;
        this.metadataURI = metadataURI;
        this.walletViewState = walletViewState == null
                ? TokenViewState.NORMAL
                : walletViewState;
    }

    /**
     * Constructor to be used when getting a token from a database.
     *
     * @since 1.1
     * @param rs the result set
     * @throws SQLException if an exception occurs when processing the result set
     */
    public TokenModel(ResultSet rs) throws SQLException {
        this(rs.getString("token_id"),
                rs.getString("token_index"),
                null,
                null,
                rs.getString("nbt"),
                null,
                null,
                null);
    }

    protected void load() {
        loadNameFromURI();

        if (nonfungible && isBaseModel())
            return;

        nbtContainer = new NBTContainer(nbt);
        nbtItem = new NBTItem(NBTItem.convertNBTtoItem(nbtContainer));
        nbtItem.setString(NBT_ID, id);
        nbtItem.setString(NBT_INDEX, index);
        nbtItem.setBoolean(NBT_NONFUNGIBLE, nonfungible);

        ItemMeta meta = nbtItem.getItem().getItemMeta();
        if (meta != null) {
            displayName = meta.getDisplayName();

            Bootstrap bootstrap = EnjinCraft.bootstrap().orElse(null);
            if (bootstrap != null
                    && bootstrap.getConfig() != null
                    && bootstrap.getConfig().isIdLoreEnabled())
                addDataToLore();
        }
    }

    protected void loadNameFromURI() {
        synchronized (uriLock) {
            String metadataURI = this.metadataURI;
            if (metadataURI == null)
                return;

            Bootstrap bootstrap = EnjinCraft.bootstrap().orElse(null);

            /* The URI is expected to conform to the ERC-1155 Metadata JSON Schema as outlined in:
             * https://github.com/ethereum/EIPs/blob/master/EIPS/eip-1155.md#erc-1155-metadata-uri-json-schema.
             */
            boolean replaceIdArg    = metadataURI.contains("{id}");
            boolean replaceIndexArg = metadataURI.contains("{index}");
            boolean isValidURI = !metadataURI.isEmpty()
                    && metadataURI.endsWith(TokenManager.JSON_EXT)
                    && !(!replaceIdArg && replaceIndexArg);
            if (bootstrap instanceof SpigotBootstrap && !isValidURI)
                ((SpigotBootstrap) bootstrap).debug(String.format("Invalid metadata URI found on token %s", getFullId()));

            if (!isValidURI && isNonFungibleInstance()) { // Is non-fungible
                loadDefaultName();
                return;
            } else if (!isValidURI && !isNonFungibleInstance()) { // Is fungible or non-fungible base model
                nameFromURI = null;
                return;
            }

            if (replaceIdArg)
                metadataURI = metadataURI.replace("{id}", id);
            if (replaceIndexArg)
                metadataURI = metadataURI.replace("{index}", index);

            try {
                URL uri = new URL(metadataURI);
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(uri.openStream()))) {
                    Gson gson = new Gson();
                    JsonObject jsonObject = gson.fromJson(reader, JsonObject.class);
                    nameFromURI = jsonObject.get("name").getAsString();
                }
            } catch (FileNotFoundException e) {
                if (bootstrap instanceof SpigotBootstrap) {
                    String token = isNonFungibleInstance()
                            ? String.format("%s #%d", id, TokenUtils.convertIndexToLong(index))
                            : id;
                    ((SpigotBootstrap) bootstrap).debug(String.format("Could not find file \"%s\" when loading metadata for token %s",
                            metadataURI,
                            token));
                }

                loadDefaultName();
            } catch (Exception e) {
                if (bootstrap instanceof SpigotBootstrap)
                    ((SpigotBootstrap) bootstrap).log(e);
            }
        }
    }

    private void loadDefaultName() {
        Bootstrap bootstrap = EnjinCraft.bootstrap().orElse(null);
        TokenManager tokenManager = bootstrap == null
                ? null
                : bootstrap.getTokenManager();
        TokenModel baseModel = tokenManager == null
                ? null
                : tokenManager.getToken(id);
        nameFromURI = baseModel == null
                ? null
                : baseModel.nameFromURI;
    }

    protected void addDataToLore() {
        ItemMeta meta = nbtItem.getItem().getItemMeta();
        if (meta == null)
            return;

        String name = StringUtils.isEmpty(nameFromURI)
                ? id
                : nameFromURI;
        String data = nonfungible
                ? String.format("%s #%d", name, TokenUtils.convertIndexToLong(index))
                : String.format("%s", name);

        if (meta.hasLore()) {
            List<String> lore = meta.getLore();
            lore.add("");
            lore.add(ChatColor.DARK_PURPLE + data);
            meta.setLore(lore);
        } else {
            List<String> lore = new ArrayList<>();
            lore.add("");
            lore.add(ChatColor.DARK_PURPLE + data);
            meta.setLore(lore);
        }

        nbtItem.getItem().setItemMeta(meta);
    }

    protected boolean applyBlacklist(Collection<String> blacklist) {
        boolean changed = false;

        List<TokenPermission> newPermissions = new ArrayList<>(assignablePermissions.size());
        for (TokenPermission permission : assignablePermissions) {
            if (blacklist.contains(permission.getPermission()))
                changed = true;
            else
                newPermissions.add(permission);
        }

        if (changed)
            assignablePermissions = newPermissions;

        return changed;
    }

    public ItemStack getItemStack() {
        return getItemStack(false);
    }

    public ItemStack getItemStack(boolean raw) {
        ItemStack is;

        if (raw) {
            NBTItem nbtItem = new NBTItem(NBTItem.convertNBTtoItem(nbtContainer));
            nbtItem.removeKey(NBT_ID);
            nbtItem.removeKey(NBT_INDEX);
            nbtItem.removeKey(NBT_NONFUNGIBLE);

            is = nbtItem.getItem().clone();
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
        if (idx < 0) {
            assignablePermissions.add(tokenPerm);
            return true;
        }

        TokenPermission other = assignablePermissions.get(idx);
        return other.addWorld(world);
    }

    public boolean addPermissionToWorlds(String permission, Collection<String> worlds) {
        TokenPermission tokenPerm = new TokenPermission(permission, worlds);

        int idx = assignablePermissions.indexOf(tokenPerm);
        if (idx < 0) {
            assignablePermissions.add(tokenPerm);
            return true;
        }

        TokenPermission other = assignablePermissions.get(idx);
        return other.addWorlds(worlds);
    }

    public boolean removePermission(String permission) {
        return removePermissionFromWorld(permission, TokenManager.GLOBAL);
    }

    public boolean removePermissionFromWorld(String permission, String world) {
        TokenPermission tokenPerm = new TokenPermission(permission);

        int idx = assignablePermissions.indexOf(tokenPerm);
        if (idx < 0)
            return false;

        TokenPermission other = assignablePermissions.get(idx);

        if (world.equals(TokenManager.GLOBAL)) {
            assignablePermissions.remove(idx);
            return true;
        }

        return other.removeWorld(world);
    }

    public boolean removePermissionFromWorlds(String permission, Collection<String> worlds) {
        TokenPermission tokenPerm = new TokenPermission(permission);

        int idx = assignablePermissions.indexOf(tokenPerm);
        if (idx < 0)
            return false;

        TokenPermission other = assignablePermissions.get(idx);

        if (worlds.contains(TokenManager.GLOBAL)) {
            assignablePermissions.remove(idx);
            return true;
        }

        return other.removeWorlds(worlds);
    }

    public boolean isBaseModel() {
        return index.equals(TokenUtils.BASE_INDEX);
    }

    public boolean isNonFungibleInstance() {
        return nonfungible && !isBaseModel();
    }

    public String getFullId() {
        return TokenUtils.createFullId(this);
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

        for (TokenPermission permission : assignablePermissions) {
            String      perm   = permission.getPermission();
            Set<String> worlds = permission.getWorlds();

            if (permission.isGlobal())
                permissionMap.computeIfAbsent(TokenManager.GLOBAL, k -> new HashSet<>()).add(perm);
            else
                worlds.forEach(world -> permissionMap.computeIfAbsent(world, k -> new HashSet<>()).add(perm));
        }

        return permissionMap;
    }

    public TokenPermission getPermission(String permission) {
        TokenPermission tokenPerm = new TokenPermission(permission);

        int idx = assignablePermissions.indexOf(tokenPerm);
        if (idx < 0)
            return null;

        return new TokenPermission(assignablePermissions.get(idx));
    }

}
