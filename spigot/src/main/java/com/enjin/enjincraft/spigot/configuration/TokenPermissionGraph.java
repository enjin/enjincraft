package com.enjin.enjincraft.spigot.configuration;

import lombok.Getter;
import lombok.ToString;

import java.util.*;

@ToString
public class TokenPermissionGraph {

    @Getter
    private Map<String, Set<String>> permissionTokens = new HashMap<>(); // Permission -> Tokens
    @Getter
    private Map<String, Set<String>> tokenPermissions = new HashMap<>(); // Token -> Permissions

    protected void addToken(List<String> perms, String tokenId) {
        for (String perm : perms) {
            addTokenPerm(perm, tokenId);
        }
    }

    protected void addToken(TokenModel tokenModel) {
        addToken(tokenModel.getAssignablePermissions(), tokenModel.getId());
    }

    protected void addTokenPerm(String perm, String tokenId) {
        Set<String> tokens = permissionTokens.get(perm);
        Set<String> perms = tokenPermissions.get(tokenId);

        if (tokens == null) {
            tokens = new HashSet<>();
            permissionTokens.put(perm, tokens);
        }

        if (perms == null) {
            perms = new HashSet<>();
            tokenPermissions.put(tokenId, perms);
        }

        tokens.add(tokenId);
        perms.add(perm);
    }

    protected void removeToken(List<String> perms, String tokenId) {
        for (String perm : perms) {
            removeTokenPerm(perm, tokenId);
        }
    }

    protected void removeToken(TokenModel tokenModel) {
        removeToken(tokenModel.getAssignablePermissions(), tokenModel.getId());
    }

    protected void removeTokenPerm(String perm, String tokenId) {
        Set<String> tokens = permissionTokens.get(perm);
        Set<String> perms = tokenPermissions.get(tokenId);

        if (tokens != null)
            tokens.remove(tokenId);
        if (perms != null)
            perms.remove(perm);
    }

}
