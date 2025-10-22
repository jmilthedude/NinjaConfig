package net.ninjadev.ninjaconfig.example;

import com.google.gson.annotations.Expose;
import net.ninjadev.ninjaconfig.NinjaConfig;
import net.ninjadev.ninjaconfig.annotation.Comment;
import net.ninjadev.ninjaconfig.api.ConfigBase;

public class ExampleConfig extends ConfigBase<ExampleConfig> {

    @Expose
    @Comment("This is a String value.")
    private String someString;

    @Expose
    @Comment("This is an Integer value. (1..100)")
    public int someInt;

    @Expose private ExampleObject someObject;

    @Override
    public void resetDefaults() {
        this.someString = "yourModId";
        this.someInt = 15;
        this.someObject = new ExampleObject("defaultName", 42);
    }

    @Override
    public void copyFrom(ExampleConfig other) {
        this.someString = other.someString;
        this.someInt = other.someInt;
        this.someObject = other.someObject;
    }

    @Override
    public ExampleConfig validate(ExampleConfig cfg) {
        if (cfg.someInt < 1 || cfg.someInt > 100) {
            cfg.someInt = 15;
        }
        return super.validate(cfg);
    }

    @Override
    public void afterLoad() {
        NinjaConfig.LOG.info("ExampleConfig loaded: someString='{}, someInt={}", someString, someInt);
    }
}
