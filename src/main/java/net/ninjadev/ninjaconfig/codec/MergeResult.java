package net.ninjadev.ninjaconfig.codec;

/**
 * Result of attempting to merge a configuration file into an in-memory instance.
 *
 * @param fileExists whether the file existed on disk
 * @param missingKeys whether required or expected keys were missing during merge
 * @param parseError whether a parse error occurred while reading the file
 */
public record MergeResult(boolean fileExists, boolean missingKeys, boolean parseError) {
}
