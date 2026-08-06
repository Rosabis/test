package com.android.xhs;

import io.github.libxposed.api.XposedInterface;
import io.github.libxposed.api.XposedInterfaceWrapper;
import io.github.libxposed.api.XposedModuleInterface;

import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Manages hook registration, lookup, and lifecycle.
 * Uses API 102's HookBuilder pattern for hook installation.
 */
public class HookManager {

    private static HookManager instance;

    private final Map<String, XposedInterface.HookHandle> hookHandles = new HashMap<>();
    private final List<HookConfig> pendingConfigs = new ArrayList<>();

    private XposedModuleInterface.ModuleLoadedParam moduleLoadedParam;
    private XposedModuleInterface.PackageLoadedParam packageLoadedParam;
    private XposedInterfaceWrapper xposedInterface;

    private HookManager() {
    }

    public static synchronized HookManager getInstance() {
        if (instance == null) {
            instance = new HookManager();
        }
        return instance;
    }

    /**
     * Initializes the manager with the module interface and parameters.
     */
    public void initialize(XposedInterfaceWrapper xposedInterface,
                           XposedModuleInterface.ModuleLoadedParam param) {
        this.xposedInterface = xposedInterface;
        this.moduleLoadedParam = param;
    }

    /**
     * Called when a package is loaded.
     */
    public void onPackageLoaded(XposedModuleInterface.PackageLoadedParam param) {
        this.packageLoadedParam = param;
        // Process pending configs that match this package
        processPendingConfigs(param);
    }

    /**
     * Stores configurations for deferred installation.
     */
    public void registerConfigs(List<HookConfig> configs) {
        pendingConfigs.addAll(configs);
    }

    /**
     * Registers a single hook configuration.
     */
    public void registerConfig(HookConfig config) {
        pendingConfigs.add(config);
    }

    /**
     * Processes pending configurations when the classloader is available.
     */
    private void processPendingConfigs(XposedModuleInterface.PackageLoadedParam param) {
        ClassLoader classLoader = param.getClassLoader();
        List<HookConfig> installed = new ArrayList<>();

        for (HookConfig config : pendingConfigs) {
            try {
                installHook(config, classLoader);
                installed.add(config);
            } catch (Exception e) {
                XhsModule.log("Failed to install hook for " + config.getClassName()
                        + "." + config.getMethodName() + ": " + e.getMessage());
            }
        }

        pendingConfigs.removeAll(installed);
    }

    /**
     * Installs a single hook using the API 102 HookBuilder pattern.
     */
    public XposedInterface.HookHandle installHook(HookConfig config, ClassLoader classLoader)
            throws Exception {
        Class<?> targetClass = classLoader.loadClass(config.getClassName());
        Executable executable = config.resolve(targetClass);

        return installHook(executable, config);
    }

    /**
     * Installs a hook on a specific Executable.
     */
    public XposedInterface.HookHandle installHook(Executable executable, HookConfig config) {
        XhsHooker hooker = new XhsHooker(config);
        return installHook(executable, hooker, config.getHookId());
    }

    /**
     * Core hook installation using the new API 102 HookBuilder pattern.
     */
    public XposedInterface.HookHandle installHook(Executable executable,
                                                   XposedInterface.Hooker hooker,
                                                   String hookId) {
        if (xposedInterface == null) {
            throw new IllegalStateException("XposedInterface not initialized");
        }

        // API 102: Use hook(Executable) → HookBuilder → intercept(Hooker)
        XposedInterface.HookBuilder builder = xposedInterface.hook(executable);

        if (hookId != null) {
            // API 102: setId allows replacing hooks by id
            builder.setId(hookId);
        }

        // Set priority (default is PRIORITY_DEFAULT)
        builder.setPriority(XposedInterface.PRIORITY_DEFAULT);

        // Set exception mode
        builder.setExceptionMode(XposedInterface.ExceptionMode.DEFAULT);

        // Build and install the hook
        XposedInterface.HookHandle handle = builder.intercept(hooker);

        // Store the handle
        String key = hookId != null ? hookId : executable.toString();
        hookHandles.put(key, handle);

        XhsModule.log("Hook installed: " + executable.getDeclaringClass().getSimpleName()
                + "." + executable.getName() + " [id=" + key + "]");

        return handle;
    }

    /**
     * Installs a hook with a custom Hooker on a specific method.
     */
    public XposedInterface.HookHandle hookMethod(Method method,
                                                 XposedInterface.Hooker hooker,
                                                 String hookId) {
        return installHook(method, hooker, hookId);
    }

    /**
     * Installs a hook with a custom Hooker on a specific constructor.
     */
    public XposedInterface.HookHandle hookConstructor(Constructor<?> constructor,
                                                      XposedInterface.Hooker hooker,
                                                      String hookId) {
        return installHook(constructor, hooker, hookId);
    }

    /**
     * Replaces an existing hook by its ID (API 102 feature).
     */
    public boolean replaceHook(String hookId, XposedInterface.Hooker newHooker) {
        XposedInterface.HookHandle handle = hookHandles.get(hookId);
        if (handle != null) {
            handle.replaceHook(newHooker);
            XhsModule.log("Hook replaced: id=" + hookId);
            return true;
        }
        return false;
    }

    /**
     * Removes a hook by its ID.
     */
    public boolean removeHook(String hookId) {
        XposedInterface.HookHandle handle = hookHandles.remove(hookId);
        if (handle != null) {
            handle.unhook();
            XhsModule.log("Hook removed: id=" + hookId);
            return true;
        }
        return false;
    }

    /**
     * Removes all installed hooks.
     */
    public void removeAllHooks() {
        for (XposedInterface.HookHandle handle : hookHandles.values()) {
            try {
                handle.unhook();
            } catch (Exception ignored) {
            }
        }
        hookHandles.clear();
        XhsModule.log("All hooks removed");
    }

    /**
     * Returns the number of currently installed hooks.
     */
    public int getHookCount() {
        return hookHandles.size();
    }

    /**
     * Returns all registered hook configurations.
     */
    public List<HookConfig> getPendingConfigs() {
        return new ArrayList<>(pendingConfigs);
    }

    /**
     * Checks if a class has been hooked.
     */
    public boolean isClassHooked(String className) {
        for (Map.Entry<String, XposedInterface.HookHandle> entry : hookHandles.entrySet()) {
            if (entry.getKey().startsWith(className)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Gets the ModuleLoadedParam.
     */
    public XposedModuleInterface.ModuleLoadedParam getModuleLoadedParam() {
        return moduleLoadedParam;
    }

    /**
     * Gets the PackageLoadedParam.
     */
    public XposedModuleInterface.PackageLoadedParam getPackageLoadedParam() {
        return packageLoadedParam;
    }

    /**
     * Gets the ClassLoader for the current package.
     */
    public ClassLoader getCurrentClassLoader() {
        if (packageLoadedParam != null) {
            return packageLoadedParam.getClassLoader();
        }
        return null;
    }

    /**
     * Loads a class using the current package's ClassLoader.
     */
    public Class<?> loadClass(String className) throws ClassNotFoundException {
        ClassLoader cl = getCurrentClassLoader();
        if (cl != null) {
            return cl.loadClass(className);
        }
        return Class.forName(className);
    }
}
