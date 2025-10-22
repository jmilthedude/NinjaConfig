package net.ninjadev.ninjaconfig.codec;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Codec responsible for reading and writing configuration instances to disk.
 *
 * <p>Implementations must merge file contents into an existing in-memory instance
 * and be able to write an instance back to a file. Codecs should return the
 * preferred file extension (including the leading dot) from {@link #defaultExtension()}.</p>
 */
public interface ConfigCodec {
    /**
     * Merge the data from the given file into the provided target instance.
     *
     * @param file path to the file to read
     * @param target instance to populate
     * @param <T> concrete type of the target
     * @return a {@link MergeResult} describing whether the file existed and if any errors/keys were missing
     * @throws IOException if an I/O error occurs while reading the file
     */
    <T> MergeResult mergeInto(Path file, T target) throws IOException;

    /**
     * Write the supplied instance to the target file path. Implementations should
     * create or overwrite the file atomically when possible.
     *
     * @param file path to write to
     * @param instance configuration instance to serialize
     * @throws IOException if an I/O error occurs while writing
     */
    void write(Path file, Object instance) throws IOException;

    /**
     * @return the default file extension used by this codec (including the leading dot), e.g. ".json"
     */
    String defaultExtension();
}
