package net.ninjadev.ninjaconfig.codec;

import com.google.gson.*;
import com.google.gson.annotations.Expose;
import net.ninjadev.ninjaconfig.annotation.Comment;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

/**
 * JSON codec that stores each exposed field as a nested object containing the
 * value and an optional comment produced from the {@link Comment} annotation.
 * Read path accepts either the nested form above or a flat value.
 * Write path is recursive, so nested objects/lists/maps also get comments.
 */
public final class GsonNestedCommentCodec implements ConfigCodec {

    private final Gson gson = new GsonBuilder()
            .excludeFieldsWithoutExposeAnnotation()
            .setPrettyPrinting()
            .create();

    /**
     * Merge the JSON file at {@code file} into {@code target}.
     *
     * @param file file to read
     * @param target target instance to populate
     * @param <T> concrete type of the target
     * @return result indicating whether the file existed and if there were missing keys or parse errors
     */
    @Override
    public <T> MergeResult mergeInto(Path file, T target) {
        if (!Files.exists(file)) {
            return new MergeResult(false, true, false);
        }

        final JsonObject source;
        try (Reader r = Files.newBufferedReader(file)) {
            source = JsonParser.parseReader(r).getAsJsonObject();
        } catch (Exception ex) {
            return new MergeResult(true, true, true);
        }

        boolean missing = false;

        for (Field f : target.getClass().getDeclaredFields()) {
            if (isNotConfigField(f)) continue;

            final String name = f.getName();
            if (!source.has(name)) {
                missing = true;
                continue;
            }

            JsonElement raw = source.get(name);
            JsonElement val = unwrapComments(raw);

            try {
                f.setAccessible(true);
                Object parsed = gson.fromJson(val, f.getGenericType());
                f.set(target, parsed);
            } catch (Exception ignore) {
                missing = true;
            }
        }

        return new MergeResult(true, missing, false);
    }

    /**
     * Write the given instance to {@code file} using the nested {value, comment}
     * wrapper for each exposed field.
     *
     * @param file destination file
     * @param instance instance to serialize
     * @throws IOException if an I/O error occurs while writing
     */
    @Override
    public void write(Path file, Object instance) throws IOException {
        Class<?> type = instance.getClass();
        JsonObject out = new JsonObject();

        for (Field f : type.getDeclaredFields()) {
            if (isNotConfigField(f)) continue;
            writeField(out, instance, f);
        }

        try (Writer w = Files.newBufferedWriter(file)) {
            gson.toJson(out, w);
        }
    }

    /** @return ".json" */
    @Override
    public String defaultExtension() { return ".json"; }

    private JsonElement unwrapComments(JsonElement el) {
        if (el == null || el.isJsonNull()) return JsonNull.INSTANCE;

        if (el.isJsonObject()) {
            JsonObject o = el.getAsJsonObject();

            if (o.has("value") && (o.size() == 1 || (o.size() == 2 && o.has("comment")))) {
                return unwrapComments(o.get("value"));
            }
            JsonObject out = new JsonObject();
            for (var e : o.entrySet()) {
                out.add(e.getKey(), unwrapComments(e.getValue()));
            }
            return out;
        }

        if (el.isJsonArray()) {
            JsonArray in = el.getAsJsonArray();
            JsonArray out = new JsonArray();
            for (JsonElement e : in) out.add(unwrapComments(e));
            return out;
        }

        return el; // primitive
    }

    private void writeField(JsonObject out, Object instance, Field f) {
        f.setAccessible(true);

        final Object value;
        try {
            value = f.get(instance);
        } catch (IllegalAccessException e) {
            return;
        }

        JsonObject wrapper = new JsonObject();
        wrapper.add("value", toJsonWithComments(value));

        Comment c = f.getAnnotation(Comment.class);
        if (c != null && !c.value().isBlank()) {
            wrapper.addProperty("comment", c.value());
        }

        out.add(f.getName(), wrapper);
    }


    private JsonElement toJsonWithComments(Object obj) {
        if (obj == null) return JsonNull.INSTANCE;

        // primitives / enums
        if (obj instanceof Number || obj instanceof String || obj instanceof Boolean || obj.getClass().isEnum()) {
            return gson.toJsonTree(obj);
        }

        // arrays
        if (obj.getClass().isArray()) {
            JsonArray arr = new JsonArray();
            int len = java.lang.reflect.Array.getLength(obj);
            for (int i = 0; i < len; i++) {
                Object el = java.lang.reflect.Array.get(obj, i);
                arr.add(toJsonWithComments(el));
            }
            return arr;
        }

        // iterables
        if (obj instanceof Iterable<?> it) {
            JsonArray arr = new JsonArray();
            for (Object el : it) arr.add(toJsonWithComments(el));
            return arr;
        }

        // maps
        if (obj instanceof Map<?, ?> map) {
            JsonObject out = new JsonObject();
            for (var e : map.entrySet()) {
                if (e.getKey() instanceof String key) {
                    out.add(key, toJsonWithComments(e.getValue()));
                }
            }
            return out;
        }

        // POJO: wrap each @Expose field recursively
        JsonObject out = new JsonObject();
        for (Field f : obj.getClass().getDeclaredFields()) {
            if (isNotConfigField(f)) continue;
            writeField(out, obj, f);
        }
        return out;
    }

    private boolean isNotConfigField(Field f) {
        int m = f.getModifiers();
        return f.isSynthetic()
                || Modifier.isStatic(m)
                || Modifier.isTransient(m)
                || f.getAnnotation(Expose.class) == null;
    }
}
