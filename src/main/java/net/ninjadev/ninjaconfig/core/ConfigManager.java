package net.ninjadev.ninjaconfig.core;

import net.fabricmc.loader.api.FabricLoader;
import net.ninjadev.ninjaconfig.api.ConfigBase;
import net.ninjadev.ninjaconfig.codec.ConfigCodec;
import net.ninjadev.ninjaconfig.codec.GsonNestedCommentCodec;
import net.ninjadev.ninjaconfig.codec.MergeResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manager that registers, loads and saves configuration objects for a mod.
 *
 * <p>Each registered config is associated with a filename and is persisted
 * using the configured {@link ConfigCodec}.</p>
 */
public final class ConfigManager {

    /**
     * Entry describing a registered configuration.
     *
     * @param fileName base filename for the config
     * @param config the in-memory config instance
     * @param extension file extension used by the codec (including the dot)
     * @param <T> concrete config type
     */
    public record Entry<T extends ConfigBase<T>>(String fileName, T config, String extension) {
        public Entry(String fileName, T config, String extension) {
            this.fileName = Objects.requireNonNull(fileName);
            this.config = Objects.requireNonNull(config);
            this.extension = Objects.requireNonNull(extension);
        }
    }

    /**
     * Builder for creating a {@link ConfigManager} with custom settings.
     */
    public static final class Builder {
        private final String modId;
        private Path rootDir;
        private ConfigCodec codec;
        private Logger logger;
        private AutoLoadPolicy policy = AutoLoadPolicy.EAGER;

        /**
         * Create a builder for the supplied mod id. By default the manager will use
         * the Fabric config directory under the mod id as its root.
         *
         * @param modId mod identifier used for logging and the config directory
         */
        public Builder(String modId) {
            this.modId = Objects.requireNonNull(modId);
            this.rootDir = FabricLoader.getInstance().getConfigDir().resolve(modId);
        }

        /** Set a custom root directory for config files. */
        public Builder rootDir(Path rootDir) { this.rootDir = rootDir; return this; }
        /** Set a codec to use for reading and writing configs. */
        public Builder codec(ConfigCodec codec) { this.codec = codec; return this; }
        /** Set the logger used by the manager. */
        public Builder logger(Logger logger) { this.logger = logger; return this; }
        /** Set whether configs are loaded eagerly or manually. */
        public Builder policy(AutoLoadPolicy policy) { this.policy = policy; return this; }

        /**
         * Build the {@link ConfigManager} instance.
         *
         * @return configured ConfigManager
         */
        public ConfigManager build() {
            ConfigCodec codec = (this.codec != null) ? this.codec : new GsonNestedCommentCodec();
            Logger log = (logger != null) ? logger : LoggerFactory.getLogger(modId);
            return new ConfigManager(rootDir, codec, log, policy);
        }
    }

    private final Path rootDir;
    private final AutoLoadPolicy policy;
    private final Map<String, Entry<?>> entries = new ConcurrentHashMap<>();

    private final ConfigCodec codec;
    private final Logger log;

    private ConfigManager(Path rootDir, ConfigCodec codec, Logger log, AutoLoadPolicy policy) {
        this.rootDir = rootDir; this.codec = codec; this.log = log; this.policy = policy;
    }

    /**
     * Register a configuration instance with a filename and return the instance.
     * If the manager is configured with {@link AutoLoadPolicy#EAGER} the
     * configuration will be loaded from disk immediately.
     *
     * @param fileName base filename to use for this configuration
     * @param cfg config instance
     * @param <T> concrete config type
     * @return the passed config instance
     * @throws IllegalStateException when a config with the same filename is already registered
     */
    public <T extends ConfigBase<T>> T register(String fileName, T cfg) {
        Entry<T> e = new Entry<>(fileName, cfg, codec.defaultExtension());
        if (entries.putIfAbsent(fileName, e) != null)
            throw new IllegalStateException("Duplicate config: " + fileName);
        if (policy == AutoLoadPolicy.EAGER) load(e);
        return cfg;
    }

    /** Load all registered configurations. */
    public void loadAll()  { this.getSnapshot().forEach(this::load); }
    /** Save all registered configurations. */
    public void saveAll()  { this.getSnapshot().forEach(this::save); }
    /** Save only configurations marked as dirty. */
    public void saveDirty(){ this.getSnapshot().stream().filter(en -> en.config.isDirty()).forEach(this::save); }


    private List<Entry<?>> getSnapshot() {
        return List.copyOf(entries.values());
    }

    private <T extends ConfigBase<T>> void load(Entry<T> e) {
        Path path = filePath(e.fileName, e.extension);
        T config = e.config;
        try {
            config.resetDefaults();

            MergeResult mergeResult = codec.mergeInto(path, config);

            T validated = config.validate(config);
            if (validated != config) config.copyFrom(validated);

            config.afterLoad();
            config.markClean();

            if (!mergeResult.fileExists() || mergeResult.missingKeys() || mergeResult.parseError()) {
                save(e);
            }
        } catch (Exception ex) {
            log.warn("Failed to read {}. Regenerating defaults.", path, ex);
            config.resetDefaults();
            save(e);
        }
    }

    private <T extends ConfigBase<T>> void save(Entry<T> e) {
        Path dir = rootDir;
        Path path = filePath(e.fileName, e.extension);
        T config = e.config;
        try {
            Files.createDirectories(dir);
            config.beforeSave();
            Path tmp = Files.createTempFile(dir, e.fileName + "_", e.extension + ".tmp");
            codec.write(tmp, config);
            try {
                Files.move(tmp, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException ex) {
                Files.move(tmp, path, StandardCopyOption.REPLACE_EXISTING);
            }
            config.markClean();
            log.info("Saved {}", path);
        } catch (IOException ex) {
            log.error("Failed to save {}", path, ex);
        }
    }

    private Path filePath(String fileName, String extension) {
        String fn = fileName.endsWith(extension) ? fileName : fileName + extension;
        return rootDir.resolve(fn);
    }
}
