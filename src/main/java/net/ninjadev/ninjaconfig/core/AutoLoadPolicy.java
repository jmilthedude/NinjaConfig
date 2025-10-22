package net.ninjadev.ninjaconfig.core;

/**
 * Policy controlling whether registered configurations are loaded automatically.
 */
public enum AutoLoadPolicy {
    /** Load configurations immediately when registered. */
    EAGER,

    /** Require an explicit call to load configurations. */
    MANUAL
}
