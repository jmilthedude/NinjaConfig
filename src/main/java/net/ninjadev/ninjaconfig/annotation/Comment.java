package net.ninjadev.ninjaconfig.annotation;

import java.lang.annotation.*;

/**
 * Annotation used to attach a human-readable comment to a configuration field.
 *
 * <p>Comments are written alongside values by codecs that support comments and
 * are intended for consumers editing the configuration files by hand.</p>
 */
@Documented
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Comment {
    /** The textual comment to write for the annotated field. */
    String value();
}
