package net.ninjadev.ninjaconfig.api;

/**
 * Base class for configuration objects managed by {@code ConfigManager}.
 *
 * <p>Implementations should expose fields (typically annotated for codecs) and
 * implement lifecycle methods such as {@link #resetDefaults()} and
 * {@link #copyFrom(ConfigBase)}. The type parameter allows concrete configs to
 * return themselves from fluent validation methods.</p>
 *
 * @param <T> concrete config type
 */
public abstract class ConfigBase<T extends ConfigBase<T>> {
    private volatile boolean dirty = true;

    /**
     * Reset this instance to its default values. Called before loading from disk
     * so missing files or keys will be populated with defaults.
     */
    public abstract void resetDefaults();

    /**
     * Validate or normalize the provided configuration object after merge.
     *
     * <p>Default implementation returns the provided instance unchanged. A
     * concrete implementation may return a different instance (for example,
     * when using immutable types) in which case the caller will copy values
     * from the returned instance via {@link #copyFrom(ConfigBase)}.</p>
     *
     * @param cfg instance to validate
     * @return the validated/normalized instance (may be the same object)
     */
    public T validate(T cfg) { return cfg; }

    /**
     * Copy values from another instance of the same concrete type into this
     * instance. Used when validation returns a replacement instance.
     *
     * @param other source instance to copy from
     */
    public abstract void copyFrom(T other);

    /** Called after a successful load to allow post-processing. */
    public void afterLoad() {}

    /** Called before writing this config to disk to allow last-minute updates. */
    public void beforeSave() {}

    /** Mark this config as modified and needing to be saved. */
    public final void markDirty() { dirty = true; }

    /** Mark this config as clean (no pending changes). */
    public final void markClean() { dirty = false; }

    /**
     * @return true when the config has unsaved changes and should be persisted
     */
    public final boolean isDirty() { return dirty; }
}
