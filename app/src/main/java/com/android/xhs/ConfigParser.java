package com.android.xhs;

import java.util.ArrayList;
import java.util.List;

/**
 * Parses hook configuration from various sources.
 * Supports programmatic registration of hook configs.
 */
public class ConfigParser {

    private final List<HookConfig> configs = new ArrayList<>();

    /**
     * Adds a hook configuration programmatically.
     */
    public ConfigParser addHook(HookConfig config) {
        if (config != null) {
            configs.add(config);
        }
        return this;
    }

    /**
     * Parses a list of config strings and adds them.
     */
    public ConfigParser parseLines(List<String> lines) {
        for (String line : lines) {
            try {
                HookConfig config = HookConfig.parse(line);
                if (config != null) {
                    configs.add(config);
                }
            } catch (Exception e) {
                // Skip invalid lines
            }
        }
        return this;
    }

    /**
     * Returns all registered configurations.
     */
    public List<HookConfig> getConfigs() {
        return new ArrayList<>(configs);
    }

    /**
     * Clears all configurations.
     */
    public void clear() {
        configs.clear();
    }

    // === Predefined hook configurations for 小红书 (com.xingin.xhs) ===

    /**
     * Creates default configurations targeting common scenarios in 小红书.
     */
    public static ConfigParser createDefault() {
        ConfigParser parser = new ConfigParser();

        // Example hooks - these would need to be adjusted based on actual app analysis
        // parser.addHook(new HookConfig(
        //     "com.xingin.xhs.MainActivity",
        //     "onCreate",
        //     new Class<?>[]{android.os.Bundle.class},
        //     HookConfig.HookType.METHOD,
        //     HookConfig.HookBehavior.INTERCEPT,
        //     null,
        //     "main_activity_on_create"
        // ));

        return parser;
    }
}
