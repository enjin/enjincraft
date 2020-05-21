package com.enjin.enjincraft.spigot.player;

import lombok.SneakyThrows;
import org.bukkit.permissions.Permissible;
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Map;

public class EnjPermissionAttachment {

    private static final Field permissionsField;

    static {
        Class<?> clazz = PermissionAttachment.class;
        permissionsField = getDeclaredField(clazz,"permissions");
        permissionsField.setAccessible(true);
    }

    private Permissible permissible;
    private Plugin plugin;
    private PermissionAttachment attachment;
    private Map<String, Boolean> permissions;

    public EnjPermissionAttachment(Permissible permissible, Plugin plugin) {
        this.permissible = permissible;
        this.plugin = plugin;
        clear();
    }

    public boolean hasPermission(String permission) {
        return permissions.containsKey(permission);
    }

    public void addPermissions(Collection<String> permissions) {
        if (permissions == null)
            return;

        permissions.forEach(this::setPermission);
    }

    public void setPermission(String permission) {
        attachment.setPermission(permission, true);
    }

    public void unsetPermission(String permission) {
        attachment.unsetPermission(permission);
    }

    @SneakyThrows(IllegalAccessException.class)
    public void clear() {
        if (attachment != null)
            attachment.remove();

        attachment = permissible.addAttachment(plugin);
        permissions = (Map<String, Boolean>) permissionsField.get(attachment);
    }

    @SneakyThrows(NoSuchFieldException.class)
    private static Field getDeclaredField(Class clazz, String name) {
        return clazz.getDeclaredField(name);
    }

}
