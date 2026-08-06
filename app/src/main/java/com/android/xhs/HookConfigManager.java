package com.android.xhs;

import java.util.ArrayList;
import java.util.List;

/**
 * Manages hook configurations from various sources.
 * Supports loading configs from strings, arrays, and programmatic registration.
 *
 * <p>This class replaces the original module's configuration loading mechanism
 * with a cleaner, type-safe API.</p>
 */
public class HookConfigManager {

    private static HookConfigManager instance;

    private final List<HookConfig> configs = new ArrayList<>();
    private final List<String> configStrings = new ArrayList<>();

    private HookConfigManager() {
    }

    public static synchronized HookConfigManager getInstance() {
        if (instance == null) {
            instance = new HookConfigManager();
        }
        return instance;
    }

    /**
     * Adds a configuration string in the format:
     * "className|methodName|paramTypes|hookType|behavior|returnValue|hookId"
     */
    public HookConfigManager addConfigString(String configString) {
        configStrings.add(configString);
        try {
            HookConfig config = HookConfig.parse(configString);
            if (config != null) {
                configs.add(config);
            }
        } catch (Exception e) {
            // Invalid config string, skip
        }
        return this;
    }

    /**
     * Adds a pre-built HookConfig.
     */
    public HookConfigManager addConfig(HookConfig config) {
        if (config != null) {
            configs.add(config);
        }
        return this;
    }

    /**
     * Registers a method hook configuration.
     */
    public HookConfigManager hookMethod(String className, String methodName,
                                         Class<?>[] paramTypes,
                                         HookConfig.HookBehavior behavior) {
        return addConfig(new HookConfig(className, methodName, paramTypes,
                HookConfig.HookType.METHOD, behavior, null, null));
    }

    /**
     * Registers a constructor hook configuration.
     */
    public HookConfigManager hookConstructor(String className, Class<?>[] paramTypes,
                                              HookConfig.HookBehavior behavior) {
        return addConfig(new HookConfig(className, "<init>", paramTypes,
                HookConfig.HookType.CONSTRUCTOR, behavior, null, null));
    }

    /**
     * Registers a hook that blocks a method.
     */
    public HookConfigManager blockMethod(String className, String methodName,
                                          Class<?>[] paramTypes) {
        return addConfig(new HookConfig(className, methodName, paramTypes,
                HookConfig.HookType.METHOD, HookConfig.HookBehavior.BLOCK, null, null));
    }

    /**
     * Registers a hook that replaces return value.
     */
    public HookConfigManager replaceReturn(String className, String methodName,
                                            Class<?>[] paramTypes, Object returnValue) {
        return addConfig(new HookConfig(className, methodName, paramTypes,
                HookConfig.HookType.METHOD, HookConfig.HookBehavior.REPLACE_RETURN,
                returnValue, null));
    }

    /**
     * Registers a hook that skips and returns a custom value.
     */
    public HookConfigManager skipAndReturn(String className, String methodName,
                                            Class<?>[] paramTypes, Object returnValue) {
        return addConfig(new HookConfig(className, methodName, paramTypes,
                HookConfig.HookType.METHOD, HookConfig.HookBehavior.SKIP_AND_RETURN,
                returnValue, null));
    }

    /**
     * Registers a hook with a unique ID for later replacement/removal.
     */
    public HookConfigManager hookWithId(String className, String methodName,
                                         Class<?>[] paramTypes,
                                         HookConfig.HookBehavior behavior,
                                         String hookId) {
        return addConfig(new HookConfig(className, methodName, paramTypes,
                HookConfig.HookType.METHOD, behavior, null, hookId));
    }

    /**
     * Returns all configurations.
     */
    public List<HookConfig> getConfigs() {
        return new ArrayList<>(configs);
    }

    /**
     * Returns the number of registered configurations.
     */
    public int getConfigCount() {
        return configs.size();
    }

    /**
     * Clears all configurations.
     */
    public void clear() {
        configs.clear();
        configStrings.clear();
    }

    /**
     * Gets all registered config strings.
     */
    public List<String> getConfigStrings() {
        return new ArrayList<>(configStrings);
    }

    // === Preset configurations for 小红书 ===

    /**
     * Creates a preset configuration for common 小红书 hooks.
     */
    public static HookConfigManager createXhsPreset() {
        HookConfigManager manager = new HookConfigManager();

        // Add preset configurations here
        // These are examples and need to be adjusted based on actual analysis

        return manager;
    }
}
