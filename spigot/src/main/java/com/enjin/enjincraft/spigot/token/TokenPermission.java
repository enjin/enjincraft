package com.enjin.enjincraft.spigot.token;

import com.google.gson.*;
import lombok.Getter;

import java.lang.reflect.Type;
import java.util.*;

public class TokenPermission {

    public static final String PERMISSION_KEY = "perm";
    public static final String WORLDS_KEY = "worlds";

    @Getter
    private final String permission;
    @Getter
    private final Set<String> worlds = new HashSet<>();

    public TokenPermission(String permission) {
        this.permission = permission;
    }

    public TokenPermission(String permission, String world) {
        this.permission = permission;
        this.worlds.add(world);
    }

    public TokenPermission(String permission, Collection<String> worlds) {
        this.permission = permission;

        addWorlds(worlds);
    }

    @Override
    public boolean equals(Object object) {
        if (object instanceof TokenPermission)
            return permission.equals(((TokenPermission) object).getPermission());

        return false;
    }

    public boolean addWorld(String world) {
        if (world.equals(TokenManager.GLOBAL)) {
            if (worlds.contains(TokenManager.GLOBAL)) {
                return false;
            } else {
                worlds.clear();
                worlds.add(world);
                return true;
            }
        }

        return worlds.add(world);
    }

    public boolean addWorlds(Collection<String> worlds) {
        if (worlds.contains(TokenManager.GLOBAL))
            return addWorld(TokenManager.GLOBAL);

        return this.worlds.addAll(worlds);
    }

    public boolean removeWorld(String world) {
        if (world.equals(TokenManager.GLOBAL)) {
            if (worlds.size() > 0) {
                worlds.clear();
                return true;
            } else {
                return false;
            }
        }

        return worlds.remove(world);
    }

    public boolean removeWorlds(Collection<String> worlds) {
        if (worlds.contains(TokenManager.GLOBAL)) {
            if (this.worlds.size() > 0) {
                this.worlds.clear();
                return true;
            } else {
                return false;
            }
        }

        return this.worlds.removeAll(worlds);
    }

    public boolean isGlobal() {
        return worlds.isEmpty() || worlds.contains(TokenManager.GLOBAL);
    }

    public static class TokenPermissionSerializer implements JsonSerializer<TokenPermission> {

        @Override
        public JsonElement serialize(TokenPermission src, Type typeOfSrc, JsonSerializationContext context) {
            JsonElement permJson = new JsonPrimitive(src.getPermission());

            // Serializes just the permission if global
            if (src.getWorlds().isEmpty() || src.getWorlds().contains(TokenManager.GLOBAL))
                return permJson;

            JsonArray worldsJson = new JsonArray();
            src.getWorlds().forEach(worldsJson::add);

            JsonObject object = new JsonObject();
            object.add(PERMISSION_KEY, permJson);
            object.add(WORLDS_KEY, worldsJson);

            return object;
        }

    }

    public static class TokenPermissionDeserializer implements JsonDeserializer<TokenPermission> {

        @Override
        public TokenPermission deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            JsonPrimitive permJson;
            JsonArray worldsJson;

            if (json instanceof JsonObject) {
                JsonObject object = json.getAsJsonObject();

                if (!object.has(PERMISSION_KEY))
                    throw new JsonParseException("Missing token permission key \"" + PERMISSION_KEY + "\"!");

                permJson = object.get(PERMISSION_KEY).getAsJsonPrimitive();

                // Treats permission as global if key is absent
                if (!object.has(WORLDS_KEY))
                    return new TokenPermission(permJson.getAsString());

                worldsJson = object.get(WORLDS_KEY).getAsJsonArray();

                List<String> worlds = new ArrayList<>();
                worldsJson.forEach(worldElement -> {
                    if (worldElement instanceof JsonPrimitive) {
                        JsonPrimitive world = worldElement.getAsJsonPrimitive();
                        worlds.add(world.getAsString());
                    }
                });

                // Treats permission as global if list is empty
                if (worlds.isEmpty())
                    return new TokenPermission(permJson.getAsString());

                return new TokenPermission(permJson.getAsString(), worlds);
            } else if (json instanceof JsonPrimitive) {
                permJson = json.getAsJsonPrimitive();

                return new TokenPermission(permJson.getAsString());
            }

            throw new JsonParseException("Token permission not of type JsonPrimitive or JsonObject!");
        }

    }

}
