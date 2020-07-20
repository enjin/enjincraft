package com.enjin.enjincraft.spigot.token;

import com.enjin.enjincraft.spigot.util.TokenUtils;
import lombok.ToString;

import java.util.*;

@ToString
public class TokenPermissionGraph {

    private final Map<String, Map<String, Set<String>>> permissionTokens; // World -> (Permission -> Tokens)
    private final Map<String, Map<String, Set<String>>> tokenPermissions; // Token -> (World -> Permissions)

    public TokenPermissionGraph() {
        this.permissionTokens = new HashMap<>();
        this.tokenPermissions = new HashMap<>();
    }

    public TokenPermissionGraph(TokenPermissionGraph graph) {
        this.permissionTokens = new HashMap<>(graph.permissionTokens);
        this.tokenPermissions = new HashMap<>(graph.tokenPermissions);
    }

    protected void addToken(Map<String, Set<String>> permsMap, String fullId) {
        if (!TokenUtils.isValidFullId(fullId))
            return;

        permsMap.forEach(((world, perms) -> perms.forEach(perm -> addTokenPerm(perm, fullId, world))));
    }

    protected void addToken(TokenModel tokenModel) {
        addToken(tokenModel.getPermissionsMap(), tokenModel.getFullId());
    }

    protected void addTokenPerm(String perm, String fullId, String world) {
        if (!TokenUtils.isValidFullId(fullId))
            return;

        Map<String, Set<String>> worldTokens = getPermissionTokens(world); // Permission -> Tokens
        Map<String, Set<String>> worldPerms = getTokenPermissions(fullId); // World -> Permissions

        if (worldTokens == null) {
            worldTokens = new HashMap<>();
            worldTokens.put(perm, new HashSet<>());
            permissionTokens.put(world, worldTokens);
        }

        if (worldPerms == null) {
            worldPerms = new HashMap<>();
            worldPerms.put(world, new HashSet<>());
            tokenPermissions.put(fullId, worldPerms);
        }

        Set<String> tokens = worldTokens.get(perm);
        Set<String> perms = worldPerms.get(world);

        if (tokens == null) {
            tokens = new HashSet<>();
            worldTokens.put(perm, tokens);
        }

        if (perms == null) {
            perms = new HashSet<>();
            worldPerms.put(world, perms);
        }

        tokens.add(fullId);
        perms.add(perm);

        // Removes permission from all other worlds if world was set to global
        if (world.equals(TokenManager.GLOBAL)) {
            Set<String> nonGlobals = new HashSet<>(worldPerms.keySet());
            nonGlobals.remove(TokenManager.GLOBAL);
            removeTokenPerm(perm, fullId, nonGlobals);
        } else {
            removeTokenPermGlobal(perm, fullId);
        }
    }

    protected void addTokenPerm(String perm, String fullId, Collection<String> worlds) {
        if (!TokenUtils.isValidFullId(fullId))
            return;

        worlds.forEach(world -> addTokenPerm(perm, fullId, world));
    }

    protected void removeToken(Map<String, Set<String>> permsMap, String fullId) {
        permsMap.forEach((world, perms) -> {
            perms.forEach(perm -> removeTokenPerm(perm, fullId, world));
        });
    }

    protected void removeToken(TokenModel tokenModel) {
        removeToken(tokenModel.getPermissionsMap(), tokenModel.getFullId());
    }

    protected void removeTokenPerm(String perm, String fullId, String world) {
        if (!TokenUtils.isValidFullId(fullId))
            return;

        Map<String, Set<String>> worldTokens = getPermissionTokens(world); // Permission -> Tokens
        Map<String, Set<String>> worldPerms = getTokenPermissions(fullId); // World -> Permissions
        Set<String> tokens = null;
        Set<String> perms = null;

        if (worldTokens != null)
            tokens = worldTokens.get(perm);
        if (worldPerms != null)
            perms = worldPerms.get(world);

        if (tokens != null)
            tokens.remove(fullId);
        if (perms != null)
            perms.remove(perm);

        // Removes permission from all other worlds if world was set to global
        if (worldPerms != null && world.equals(TokenManager.GLOBAL)) {
            Set<String> nonGlobals = new HashSet<>(worldPerms.keySet());
            nonGlobals.remove(TokenManager.GLOBAL);
            removeTokenPerm(perm, fullId, nonGlobals);
        }
    }

    protected void removeTokenPerm(String perm, String fullId, Collection<String> worlds) {
        worlds.forEach(world -> removeTokenPerm(perm, fullId, world));
    }

    private void removeTokenPermGlobal(String perm, String fullId) {
        if (!TokenUtils.isValidFullId(fullId))
            return;

        Map<String, Set<String>> worldTokens = getPermissionTokens(TokenManager.GLOBAL); // Permission -> Tokens
        Map<String, Set<String>> worldPerms = getTokenPermissions(fullId);               // World -> Permissions
        Set<String> tokens = null;
        Set<String> perms = null;

        if (worldTokens != null)
            tokens = worldTokens.get(perm);
        if (worldPerms != null)
            perms = worldPerms.get(TokenManager.GLOBAL);

        if (tokens != null)
            tokens.remove(fullId);
        if (perms != null)
            perms.remove(perm);
    }

    protected void clear() {
        permissionTokens.clear();
        tokenPermissions.clear();
    }

    public Map<String, Set<String>> getPermissionTokens(String world) {
        return permissionTokens.get(world);
    }

    public Map<String, Set<String>> getTokenPermissions(String fullId) {
        if (!TokenUtils.isValidFullId(fullId))
            return null;

        return tokenPermissions.get(fullId);
    }

}
