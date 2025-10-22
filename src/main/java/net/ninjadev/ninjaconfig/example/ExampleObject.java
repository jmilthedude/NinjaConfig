package net.ninjadev.ninjaconfig.example;

import com.google.gson.annotations.Expose;
import net.ninjadev.ninjaconfig.annotation.Comment;

public class ExampleObject {
    @Expose
    @Comment("This is the name of the object")
    private String name;

    @Expose
    @Comment("This is the value of the object")
    private int value;

    public ExampleObject(String name, int value) {
        this.name = name;
        this.value = value;
    }
}
