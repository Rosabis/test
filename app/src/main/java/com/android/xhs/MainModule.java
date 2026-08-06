package com.android.xhs;

import io.github.libxposed.api.XposedInterface;
import io.github.libxposed.api.XposedModule;
import io.github.libxposed.api.XposedModuleInterface;

import java.util.List;

/**
 * Main entry point for the Xhs Xposed module.
 * Extends XposedModule (API 102) and implements all lifecycle callbacks.
 *
 * <p>This module targets 小红书 (com.xingin.xhs) and provides a configurable
 * hook framework that can intercept method calls using the latest libxposed API 102.</p>
 *
 * <p>Key API 102 features used:
 * <ul>
 *   <li>HookBuilder pattern for hook installation</li>
 *   <li>Chain-based interception model</li>
 *   <li>setId() for hook replacement management</li>
 *   <li>Hot reload support (onHotReloading/onHotReloaded)</li>
 *   <li>Priority-based hook ordering</li>
 * </ul>
 */
public class MainModule extends XposedModule {

    private static final String TAG = "XhsModule";
    private static MainModule instance;

    private HookManager hookManager;
    private ConfigParser configParser;
    /**
     * Constructor called by the framework when module is loaded.
     */
    public MainModule() {
        super();
        instance = this;
        XhsModule.setInstance(this);
        this.hookManager = HookManager.getInstance();
        this.configParser = ConfigParser.createDefault();
    }

    /**
     * Called when the module is loaded into a target process.
     * This is the first callback in the lifecycle.
     */
    @Override
    public void onModuleLoaded(XposedModuleInterface.ModuleLoadedParam param) {
        super.onModuleLoaded(param);
        logInfo("Module loaded for process: " + param.getProcessName());

        hookManager.initialize(this, param);

        logInfo("XhsModule initialized (framework=" + XhsModule.getFrameworkName()
                + ", versionCode=" + XhsModule.getFrameworkVersionCode() + ")");
    }

    /**
     * Called when a package is loaded into the process.
     * This is where hooks should be installed for the target application.
     */
    @Override
    public void onPackageLoaded(XposedModuleInterface.PackageLoadedParam param) {
        super.onPackageLoaded(param);

        String packageName = param.getPackageName();
        logInfo("Package loaded: " + packageName);

        // Only process if this is the first package in the process
        if (!param.isFirstPackage()) {
            return;
        }

        hookManager.onPackageLoaded(param);

        // Install all configured hooks
        List<HookConfig> configs = configParser.getConfigs();
        if (!configs.isEmpty()) {
            logInfo("Installing " + configs.size() + " hook(s)...");
            hookManager.registerConfigs(configs);
        }

        // Install hooks immediately for available classes
        installHooksForPackage(param);
    }

    /**
     * Called when the package's ClassLoader is ready to create Application.
     * Use this for hooks that need the Application context.
     */
    @Override
    public void onPackageReady(XposedModuleInterface.PackageReadyParam param) {
        super.onPackageReady(param);
        logInfo("Package ready: " + param.getPackageName());
    }

    /**
     * Called when the module is about to be hot-reloaded.
     * Return true to allow the reload, false to cancel.
     */
    @Override
    public boolean onHotReloading(XposedModuleInterface.HotReloadingParam param) {
        logInfo("Hot reloading requested...");
        // Unhook all existing hooks before reload
        hookManager.removeAllHooks();
        return true;
    }

    /**
     * Called after the module has been hot-reloaded.
     * Re-install hooks in the new code.
     */
    @Override
    public void onHotReloaded(XposedModuleInterface.HotReloadedParam param) {
        logInfo("Hot reloaded, re-installing hooks...");
        // Hooks were removed during reload, need to be re-installed
        XposedModuleInterface.PackageLoadedParam pkgParam =
                hookManager.getPackageLoadedParam();
        if (pkgParam != null) {
            installHooksForPackage(pkgParam);
        }
    }

    /**
     * Installs hooks for a specific package using the ClassLoader from the param.
     */
    private void installHooksForPackage(XposedModuleInterface.PackageLoadedParam param) {
        ClassLoader classLoader = param.getDefaultClassLoader();
        List<HookConfig> configs = configParser.getConfigs();

        for (HookConfig config : configs) {
            try {
                installSingleHook(config, classLoader);
            } catch (Exception e) {
                logError("Failed to hook " + config.getClassName() + "."
                        + config.getMethodName() + ": " + e.getMessage(), e);
            }
        }

        logInfo("Hooks installation complete");
    }

    /**
     * Installs a single hook with custom hooker.
     */
    private void installSingleHook(HookConfig config, ClassLoader classLoader)
            throws Exception {
        Class<?> targetClass = classLoader.loadClass(config.getClassName());
        java.lang.reflect.Executable executable = config.resolve(targetClass);

        // Create hooker with the config
        XhsHooker hooker = new XhsHooker(config) {
            @Override
            protected void onBefore(XposedInterface.Chain chain, java.lang.reflect.Executable executable) {
                switch (config.getBehavior()) {
                    case LOG_ONLY:
                        logInfo("[HOOK] Before: " + executable.getDeclaringClass().getSimpleName()
                                + "." + executable.getName());
                        break;
                    case INTERCEPT:
                        break;
                    default:
                        break;
                }
            }

            @Override
            protected void onAfter(XposedInterface.Chain chain, java.lang.reflect.Executable executable,
                                   Object result) {
                switch (config.getBehavior()) {
                    case LOG_ONLY:
                        logInfo("[HOOK] After: " + executable.getDeclaringClass().getSimpleName()
                                + "." + executable.getName() + " result=" + result);
                        break;
                    case REPLACE_RETURN:
                        logInfo("[HOOK] Return replaced: " + executable.getName());
                        break;
                    default:
                        break;
                }
            }
        };

        // Use HookBuilder pattern (API 102)
        XposedInterface.HookBuilder builder = hookManager.hook(executable)
                .setPriority(XposedInterface.PRIORITY_DEFAULT);

        if (config.getHookId() != null) {
            builder.setId(config.getHookId());
        }

        builder.intercept(hooker);

        logInfo("Hook installed: " + config.getClassName() + "." + config.getMethodName()
                + " [behavior=" + config.getBehavior() + "]");
    }

    // === Public API for dynamic hook registration ===

    /**
     * Programmatically registers and installs a hook.
     */
    public void hookMethod(String className, String methodName, Class<?>[] paramTypes,
                           XposedInterface.Hooker hooker, String hookId) {
        ClassLoader cl = hookManager.getCurrentClassLoader();
        if (cl == null) {
            hookManager.registerConfig(new HookConfig(className, methodName, paramTypes,
                    HookConfig.HookType.METHOD, HookConfig.HookBehavior.INTERCEPT, null, hookId));
            return;
        }

        try {
            Class<?> targetClass = cl.loadClass(className);
            java.lang.reflect.Method method = targetClass.getDeclaredMethod(methodName, paramTypes);
            hookManager.installHook(method, hooker, hookId);
        } catch (Exception e) {
            logError("Failed to hook method: " + className + "." + methodName, e);
        }
    }

    /**
     * Programmatically hooks a constructor.
     */
    public void hookConstructor(String className, Class<?>[] paramTypes,
                                 XposedInterface.Hooker hooker, String hookId) {
        ClassLoader cl = hookManager.getCurrentClassLoader();
        if (cl == null) {
            hookManager.registerConfig(new HookConfig(className, "<init>", paramTypes,
                    HookConfig.HookType.CONSTRUCTOR, HookConfig.HookBehavior.INTERCEPT, null, hookId));
            return;
        }

        try {
            Class<?> targetClass = cl.loadClass(className);
            java.lang.reflect.Constructor<?> constructor =
                    targetClass.getDeclaredConstructor(paramTypes);
            hookManager.installHook(constructor, hooker, hookId);
        } catch (Exception e) {
            logError("Failed to hook constructor: " + className, e);
        }
    }

    /**
     * Removes a hook by ID.
     */
    public boolean unhook(String hookId) {
        return hookManager.removeHook(hookId);
    }

    /**
     * Replaces a hook's behavior by ID.
     */
    public boolean replaceHook(String hookId, XposedInterface.Hooker newHooker) {
        return hookManager.replaceHook(hookId, newHooker);
    }

    // === Utility methods ===

    /**
     * Logs an info message via XposedInterface.
     */
    public static void logInfo(String message) {
        if (instance != null) {
            instance.log(0, TAG, message);
        }
    }

    /**
     * Logs an error message with throwable.
     */
    public static void logError(String message, Throwable t) {
        if (instance != null) {
            instance.log(1, TAG, message, t);
        }
    }

    /**
     * Logs a message with priority level.
     */
    public static void log(String message) {
        if (instance != null) {
            instance.log(0, TAG, message);
        }
    }

    /**
     * Returns the singleton instance.
     */
    public static MainModule getInstance() {
        return instance;
    }

    /**
     * Returns the HookManager.
     */
    public HookManager getHookManager() {
        return hookManager;
    }

    /**
     * Returns the ConfigParser.
     */
    public ConfigParser getConfigParser() {
        return configParser;
    }
}
