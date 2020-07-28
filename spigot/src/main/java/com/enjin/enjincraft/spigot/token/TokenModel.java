package com.enjin.enjincraft.spigot.token;

import com.enjin.enjincraft.spigot.Bootstrap;
import com.enjin.enjincraft.spigot.EnjinCraft;
import com.enjin.enjincraft.spigot.SpigotBootstrap;
import com.enjin.enjincraft.spigot.util.StringUtils;
import com.enjin.enjincraft.spigot.util.TokenUtils;
import com.enjin.enjincraft.spigot.wallet.TokenWalletViewState;
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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.net.URL;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

@NoArgsConstructor
@ToString
public class TokenModel {

    public static final String NBT_ID          = "enjinTokenID";
    public static final String NBT_INDEX       = "enjinTokenIndex";
    public static final String NBT_NONFUNGIBLE = "enjinTokenNF";

    @Getter
    private transient boolean loaded;
    private transient NBTContainer nbtContainer;
    private transient NBTItem nbtItem;
    @Getter(onMethod_ = {@Nullable})
    private transient String displayName;
    @Getter(onMethod_ = {@Nullable, @Synchronized("uriLock")})
    @Setter(value = AccessLevel.PROTECTED, onMethod_ = {@Synchronized("uriLock")})
    private transient String nameFromURI;
    @Getter
    @Setter(AccessLevel.PACKAGE)
    private transient boolean markedForDeletion = false;

    // Mutexes
    private final transient Object uriLock = new Object();

    @Getter(onMethod_ = {@NotNull})
    private String id;
    @Getter(onMethod_ = {@NotNull})
    private String index = TokenUtils.BASE_INDEX;
    @Getter
    @Setter(AccessLevel.PACKAGE)
    private boolean nonfungible = false;
    @Getter(onMethod_ = {@Nullable})
    @Setter(AccessLevel.PROTECTED)
    @SerializedName("alternate-id")
    private String alternateId;
    @Getter(onMethod_ = {@Nullable})
    @Setter(AccessLevel.PROTECTED)
    private String nbt;
    @SerializedName("assignable-permissions")
    private List<TokenPermission> assignablePermissions = new ArrayList<>();
    @Getter(onMethod_ = {@Nullable, @Synchronized("uriLock")})
    @Setter(value = AccessLevel.PROTECTED, onMethod_ = {@Synchronized("uriLock")})
    @SerializedName("metadata-uri")
    private String metadataURI;
    @Getter(onMethod_ = {@NotNull})
    @Setter(AccessLevel.PROTECTED)
    @SerializedName("wallet-view-state")
    private TokenWalletViewState walletViewState = TokenWalletViewState.WITHDRAWABLE;

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
                      String nbt,
                      List<TokenPermission> assignablePermissions) throws NullPointerException {
        this(id,
                null,
                null,
                alternateId,
                nbt,
                assignablePermissions,
                null,
                null);
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
                      String nbt,
                      List<TokenPermission> assignablePermissions,
                      String metadataURI,
                      TokenWalletViewState walletViewState) throws IllegalArgumentException, IllegalStateException, NullPointerException {
        if (!TokenUtils.isValidId(id))
            throw new IllegalArgumentException(String.format("Invalid id: %s", id));
        if (index != null && !TokenUtils.isValidIndex(index)) {
            throw new IllegalArgumentException(String.format("Invalid index: %s", index));
        } else if (index != null
                && nonfungible != null
                && !nonfungible
                && !index.equals(TokenUtils.BASE_INDEX)) {
            throw new IllegalStateException(String.format("Token %s is fungible but was given a invalid index", id));
        }

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
                ? TokenWalletViewState.WITHDRAWABLE
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
                rs.getBoolean("nonfungible"),
                rs.getString("alternate_id"),
                rs.getString("nbt"),
                null,
                rs.getString("metadata_uri"),
                TokenWalletViewState.valueOf(rs.getString("wallet_view_state")));
    }

    protected void load() {
        loadNameFromURI();

        if (StringUtils.isEmpty(nbt) || nonfungible && isBaseModel())
            return;

        nbtContainer = new NBTContainer(nbt);
        nbtItem = new NBTItem(NBTItem.convertNBTtoItem(nbtContainer));
        nbtItem.setString(NBT_ID, id);
        nbtItem.setString(NBT_INDEX, index);
        nbtItem.setBoolean(NBT_NONFUNGIBLE, nonfungible);

        ItemMeta meta = nbtItem.getItem().getItemMeta();
        if (meta != null)
            displayName = meta.getDisplayName();

        loaded = true;
    }

    protected void loadNameFromURI() {
        synchronized (uriLock) {
            String metadataURI = this.metadataURI;
            if (metadataURI == null)
                return;

            SpigotBootstrap bootstrap = EnjinCraft.bootstrap();

            /* The URI is expected to conform to the ERC-1155 Metadata JSON Schema as outlined in:
             * https://github.com/ethereum/EIPs/blob/master/EIPS/eip-1155.md#erc-1155-metadata-uri-json-schema.
             */
            boolean replaceIdArg    = metadataURI.contains("{id}");
            boolean replaceIndexArg = metadataURI.contains("{index}");
            boolean isValidURI = !metadataURI.isEmpty()
                    && metadataURI.endsWith(TokenManager.JSON_EXT)
                    && !(!replaceIdArg && replaceIndexArg);
            if (!isValidURI)
                bootstrap.debug(String.format("Invalid metadata URI found on token %s", getFullId()));

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
        TokenManager tokenManager = EnjinCraft.bootstrap().getTokenManager();
        TokenModel baseModel = tokenManager == null
                ? null
                : tokenManager.getToken(id);
        nameFromURI = baseModel == null
                ? null
                : baseModel.nameFromURI;
    }

    protected ItemStack addDataToLore(List<String> data) {
        ItemStack is   = nbtItem.getItem().clone();
        ItemMeta  meta = is.getItemMeta();
        if (meta == null || data.isEmpty())
            return is;

        List<String> lore = meta.hasLore()
                ? meta.getLore()
                : new ArrayList<>();

        lore.add(0, "");

        int lineNumber = 1;
        for (String line : data) {
            if (line != null)
                lore.add(lineNumber++, line);
        }

        // Determines if a new-line should be added after the data
        if (lore.size() > lineNumber && !StringUtils.isEmpty(lore.get(lineNumber).trim()))
            lore.add(lineNumber, "");

        meta.setLore(lore);
        is.setItemMeta(meta);

        return is;
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

    @Nullable
    public ItemStack getItemStack() {
        return getItemStack(false);
    }

    @Nullable
    public ItemStack getItemStack(int amount) {
        ItemStack is = getItemStack();
        if (is != null)
            is.setAmount(Math.min(amount, is.getMaxStackSize()));

        return is;
    }

    @Nullable
    public ItemStack getItemStack(boolean raw) {
        if (!isLoaded())
            return null;

        ItemStack is;
        if (raw) {
            NBTItem nbtItem = new NBTItem(NBTItem.convertNBTtoItem(nbtContainer));
            nbtItem.removeKey(NBT_ID);
            nbtItem.removeKey(NBT_INDEX);
            nbtItem.removeKey(NBT_NONFUNGIBLE);

            is = nbtItem.getItem().clone();
        } else {
            List<String> data = new ArrayList<>();

            String name = getName();
            if (!StringUtils.isEmpty(name))
                data.add(name);

            is = addDataToLore(data);
        }

        ItemMeta meta = is.getItemMeta();
        if (is.hasItemMeta()) {
            meta.setUnbreakable(true);
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE);

            is.setItemMeta(meta);
        }

        return is;
    }

    @Nullable
    public ItemStack getWalletViewItemStack() {
        ItemStack is   = getItemStack();
        if (is == null)
            return null;

        ItemMeta  meta = is.getItemMeta();
        if (meta == null)
            return is;

        List<String> data = new ArrayList<>();

        String name  = getName();
        if (!StringUtils.isEmpty(name))
            data.add(name);
        String state = getWalletViewString();
        if (!StringUtils.isEmpty(state))
            data.add(state);

        return addDataToLore(data);
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

    public boolean isValid() {
        return TokenUtils.isValidId(id)
                && TokenUtils.isValidIndex(index)
                && (nonfungible || index.equals(TokenUtils.BASE_INDEX));
    }

    public boolean isBaseModel() {
        return index.equals(TokenUtils.BASE_INDEX);
    }

    public boolean isNonFungibleInstance() {
        return nonfungible && !isBaseModel();
    }

    @NotNull
    public String getFullId() {
        return TokenUtils.createFullId(this);
    }

    @NotNull
    public List<TokenPermission> getAssignablePermissions() {
        return assignablePermissions.stream()
                .map(TokenPermission::new)
                .collect(Collectors.toList());
    }

    @NotNull
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

    @Nullable
    public TokenPermission getPermission(String permission) {
        TokenPermission tokenPerm = new TokenPermission(permission);

        int idx = assignablePermissions.indexOf(tokenPerm);
        if (idx < 0)
            return null;

        return new TokenPermission(assignablePermissions.get(idx));
    }

    private String getName() {
        Bootstrap bootstrap = EnjinCraft.bootstrap();
        if (bootstrap != null
                && bootstrap.getConfig() != null
                && bootstrap.getConfig().isIdLoreEnabled()) {
            String id = StringUtils.isEmpty(nameFromURI)
                    ? this.id
                    : nameFromURI;
            String name = nonfungible
                    ? String.format("%s #%d", id, TokenUtils.convertIndexToLong(index))
                    : String.format("%s", id);

            return ChatColor.GRAY + name;
        }

        return null;
    }

    private String getWalletViewString() {
        try {
            switch (walletViewState) {
                case WITHDRAWABLE:
                    return ChatColor.GRAY
                            + "Withdrawable "
                            + ChatColor.GREEN
                            + "\u2714"; // Heavy check mark
                case LOCKED:
                    return ChatColor.GRAY
                            + "Withdrawable "
                            + ChatColor.RED
                            + "\u274C"; // Cross mark
                default:
                    return null;
            }
        } catch (NullPointerException e) {
            return null;
        } catch (Exception e) {
            SpigotBootstrap bootstrap = EnjinCraft.bootstrap();
            bootstrap.log(e);
            return null;
        }
    }

}
