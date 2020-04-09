package com.enjin.enjincraft.spigot.configuration;

import com.google.gson.annotations.SerializedName;
import de.tr7zw.changeme.nbtapi.NBTContainer;
import de.tr7zw.changeme.nbtapi.NBTItem;
import de.tr7zw.changeme.nbtapi.NBTReflectionUtil;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

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
    private List<String> assignablePermissions;

    @Builder
    public TokenModel(@NonNull String id, @NonNull String nbt, List<String> assignablePermissions) {
        this.id = id;
        this.nbt = nbt;
        this.assignablePermissions = assignablePermissions == null ? new ArrayList<>() : assignablePermissions;
    }

    protected void load() {
        nbtContainer = new NBTContainer(nbt);
        nbtItem =  new NBTItem(NBTItem.convertNBTtoItem(nbtContainer));
        nbtItem.setString(NBT_ID, id);
        displayName = nbtItem.getItem().getItemMeta().getDisplayName();
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

}
