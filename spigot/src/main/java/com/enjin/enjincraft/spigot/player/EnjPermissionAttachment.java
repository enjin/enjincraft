package com.enjin.enjincraft.spigot.player;

import org.bukkit.permissions.Permissible;
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.plugin.Plugin;

import java.util.Collection;

public class EnjPermissionAttachment {

    private Permissible permissible;
    private Plugin plugin;
    private PermissionAttachment attachment;

    public EnjPermissionAttachment(Permissible permissible, Plugin plugin) {
        this.permissible = permissible;
        this.plugin = plugin;
        clear();
    }

    public boolean hasPermission(String permission) {
        return attachment.getPermissions().containsKey(permission);
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

    public void clear() {
        if (attachment != null)
            attachment.remove();

        attachment = permissible.addAttachment(plugin);
    }

}
