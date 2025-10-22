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

/**
 * JSON codec that stores each exposed field as a nested object containing the
 * value and an optional comment produced from the {@link Comment} annotation.
 *
 * <p>On read this codec accepts either the nested form
 * {"value": ... , "comment": "..."} or a flat primitive/object value.</p>
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
     * @throws IOException if an I/O error occurs while reading
     */
    @Override
    public <T> MergeResult mergeInto(Path file, T target) throws IOException {
        if (!Files.exists(file)) {
            return new MergeResult(false, true, false);
        }

        JsonObject source;
        try (Reader r = Files.newBufferedReader(file)) {
            source = JsonParser.parseReader(r).getAsJsonObject();
        } catch (Exception ex) {
            return new MergeResult(true, true, true); // force rewrite
        }

        boolean missing = false;

        for (Field f : target.getClass().getDeclaredFields()) {
            if (isNotConfigField(f)) continue;

            String name = f.getName();
            if (!source.has(name)) {
                missing = true;
                continue;
            }

            JsonElement raw = source.get(name);
            JsonElement val = (raw.isJsonObject() && raw.getAsJsonObject().has("value"))
                    ? raw.getAsJsonObject().get("value")
                    : raw;

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
            if (this.isNotConfigField(f)) continue;
            f.setAccessible(true);

            Object value;
            try { value = f.get(instance); } catch (IllegalAccessException e) { continue; }

            JsonObject wrapper = new JsonObject();
            wrapper.add("value", gson.toJsonTree(value));

            Comment c = f.getAnnotation(Comment.class);
            if (c != null && !c.value().isBlank()) {
                wrapper.addProperty("comment", c.value());
            }
            out.add(f.getName(), wrapper);
        }

        try (Writer w = Files.newBufferedWriter(file)) {
            gson.toJson(out, w);
        }
    }

    /** @return ".json" */
    @Override public String defaultExtension() { return ".json"; }

    private boolean isNotConfigField(Field f) {
        int m = f.getModifiers();
        return f.isSynthetic()
                || Modifier.isStatic(m)
                || Modifier.isTransient(m)
                || f.getAnnotation(Expose.class) == null;
    }
}
