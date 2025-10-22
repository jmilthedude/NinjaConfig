package net.ninjadev.ninjaconfig;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NinjaConfig implements ModInitializer {

    public static final Logger LOG = LoggerFactory.getLogger("ninjaconfig");

    @Override
    public void onInitialize() {
        /*
        ConfigManager manager = new ConfigManager.Builder("ninjaconfig").logger(LOG).build();
        manager.register("example", new ExampleConfig());
        */
    }
}
