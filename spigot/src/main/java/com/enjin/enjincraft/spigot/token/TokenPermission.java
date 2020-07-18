package com.enjin.enjincraft.spigot.token;

import com.google.gson.*;
import lombok.Getter;
import lombok.NonNull;

import java.lang.reflect.Type;
import java.util.*;

public class TokenPermission {

    public static final String PERMISSION_KEY = "perm";
    public static final String WORLDS_KEY = "worlds";

    @Getter
    private final String permission;
    private final Set<String> worlds = new HashSet<>();

    public TokenPermission(@NonNull TokenPermission permission) throws NullPointerException {
        this(permission.permission, permission.worlds);
    }

    public TokenPermission(@NonNull String permission) throws NullPointerException {
        this(permission, TokenManager.GLOBAL);
    }

    public TokenPermission(@NonNull String permission,
                           @NonNull String world) throws NullPointerException {
        this(permission, Collections.singleton(world));
    }

    public TokenPermission(@NonNull String permission,
                           @NonNull Collection<String> worlds) throws NullPointerException {
        this.permission = permission;

        if (worlds.isEmpty())
            addWorld(TokenManager.GLOBAL);
        else
            addWorlds(worlds);
    }

    @Override
    public boolean equals(Object object) {
        return object instanceof TokenPermission && permission.equals(((TokenPermission) object).getPermission());
    }

    @Override
    public int hashCode() {
        return Objects.hash(permission);
    }

    public boolean addWorld(@NonNull String world) throws NullPointerException {
        if (isGlobal()) {
            return false;
        } else if (world.equals(TokenManager.GLOBAL)) {
            worlds.clear();
            worlds.add(TokenManager.GLOBAL);
            return true;
        }

        return worlds.add(world);
    }

    public boolean addWorlds(@NonNull Collection<String> worlds) throws NullPointerException {
        if (worlds.isEmpty() || isGlobal())
            return false;
        else if (worlds.contains(TokenManager.GLOBAL))
            return addWorld(TokenManager.GLOBAL);

        return this.worlds.addAll(worlds);
    }

    public boolean removeWorld(@NonNull String world) throws NullPointerException {
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

    public boolean removeWorlds(@NonNull Collection<String> worlds) throws NullPointerException {
        if (worlds.isEmpty()) {
            return false;
        } else if (worlds.contains(TokenManager.GLOBAL)) {
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
        return worlds.contains(TokenManager.GLOBAL);
    }

    public Set<String> getWorlds() {
        return new HashSet<>(worlds);
    }

    public static class TokenPermissionSerializer implements JsonSerializer<TokenPermission> {

        @Override
        public JsonElement serialize(TokenPermission src, Type typeOfSrc, JsonSerializationContext context) {
            JsonElement permJson = new JsonPrimitive(src.getPermission());

            // Serializes just the permission if global
            if (src.isGlobal())
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
                    throw new JsonParseException("Missing token permission key \"" + PERMISSION_KEY + "\"");

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

            throw new JsonParseException("Token permission not of type JsonPrimitive or JsonObject");
        }

    }

}
