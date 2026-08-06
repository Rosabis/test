package com.android.xhs;

import io.github.libxposed.api.XposedInterface;
import io.github.libxposed.api.XposedModule;
import io.github.libxposed.api.XposedModuleInterface;

/**
 * Static utility class for module logging and common operations.
 * Wraps the XposedModule instance for safe access from non-module classes.
 */
public final class XhsModule {

    private static MainModule moduleInstance;
    private static final String LOG_TAG = "XhsModule";
    private static final int LOG_PRIORITY_INFO = 0;
    private static final int LOG_PRIORITY_ERROR = 1;

    private XhsModule() {
    }

    /**
     * Sets the module instance for logging.
     */
    public static void setInstance(MainModule module) {
        moduleInstance = module;
    }

    /**
     * Logs a message to the Xposed framework log.
     */
    public static void log(String message) {
        if (moduleInstance != null) {
            try {
                moduleInstance.log(LOG_PRIORITY_INFO, LOG_TAG, message);
            } catch (Exception ignored) {
            }
        }
    }

    /**
     * Logs a message with a throwable.
     */
    public static void log(String message, Throwable throwable) {
        if (moduleInstance != null) {
            try {
                moduleInstance.log(LOG_PRIORITY_ERROR, LOG_TAG, message, throwable);
            } catch (Exception ignored) {
            }
        }
    }

    /**
     * Logs an info message.
     */
    public static void logInfo(String message) {
        log(message);
    }

    /**
     * Logs an error message.
     */
    public static void logError(String message) {
        if (moduleInstance != null) {
            try {
                moduleInstance.log(LOG_PRIORITY_ERROR, LOG_TAG, message);
            } catch (Exception ignored) {
            }
        }
    }

    /**
     * Gets the framework name.
     */
    public static String getFrameworkName() {
        if (moduleInstance != null) {
            return moduleInstance.getFrameworkName();
        }
        return "Unknown";
    }

    /**
     * Gets the framework version code.
     */
    public static long getFrameworkVersionCode() {
        if (moduleInstance != null) {
            return moduleInstance.getFrameworkVersionCode();
        }
        return 0;
    }

    /**
     * Gets the framework properties.
     */
    public static long getFrameworkProperties() {
        if (moduleInstance != null) {
            return moduleInstance.getFrameworkProperties();
        }
        return 0;
    }

    /**
     * Checks if the framework supports remote preferences.
     */
    public static boolean hasRemoteCapabilities() {
        if (moduleInstance != null) {
            long props = moduleInstance.getFrameworkProperties();
            return (props & XposedInterface.PROP_CAP_REMOTE) != 0;
        }
        return false;
    }

    /**
     * Checks if the framework supports system server hooking.
     */
    public static boolean hasSystemCapabilities() {
        if (moduleInstance != null) {
            long props = moduleInstance.getFrameworkProperties();
            return (props & XposedInterface.PROP_CAP_SYSTEM) != 0;
        }
        return false;
    }

    /**
     * Gets the module's application info.
     */
    public static android.content.pm.ApplicationInfo getApplicationInfo() {
        if (moduleInstance != null) {
            return moduleInstance.getModuleApplicationInfo();
        }
        return null;
    }

    /**
     * Gets the current module instance.
     */
    public static MainModule getInstance() {
        return moduleInstance;
    }
}
