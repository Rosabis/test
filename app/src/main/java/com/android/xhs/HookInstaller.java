package com.android.xhs;

import io.github.libxposed.api.XposedInterface;
import io.github.libxposed.api.XposedModuleInterface;

import java.lang.reflect.Executable;
import java.util.ArrayList;
import java.util.List;

/**
 * Handles the actual hook installation process.
 * Separates hook resolution from installation logic.
 */
public class HookInstaller {

    private final HookManager hookManager;
    private final List<Executable> installedExecutables = new ArrayList<>();

    public HookInstaller(HookManager hookManager) {
        this.hookManager = hookManager;
    }

    /**
     * Installs all hooks from configurations using the provided ClassLoader.
     */
    public List<InstalledHook> installAll(List<HookConfig> configs, ClassLoader classLoader) {
        List<InstalledHook> results = new ArrayList<>();

        for (HookConfig config : configs) {
            try {
                InstalledHook hook = installSingle(config, classLoader);
                if (hook != null) {
                    results.add(hook);
                }
            } catch (Exception e) {
                XhsModule.log("Hook install failed: " + config.getClassName()
                        + "." + config.getMethodName() + " - " + e.getMessage());
            }
        }

        return results;
    }

    /**
     * Installs a single hook configuration.
     */
    public InstalledHook installSingle(HookConfig config, ClassLoader classLoader)
            throws Exception {
        Class<?> targetClass = classLoader.loadClass(config.getClassName());
        Executable executable = config.resolve(targetClass);

        XhsHooker hooker = new XhsHooker(config);
        XposedInterface.HookHandle handle = hookManager.installHook(executable, hooker,
                config.getHookId());

        installedExecutables.add(executable);

        return new InstalledHook(config, executable, handle);
    }

    /**
     * Installs a hook with a custom hooker.
     */
    public InstalledHook installCustom(String className, String methodName,
                                        Class<?>[] paramTypes,
                                        XposedInterface.Hooker customHooker,
                                        ClassLoader classLoader,
                                        String hookId) throws Exception {
        Class<?> targetClass = classLoader.loadClass(className);

        Executable executable;
        if (methodName.equals("<init>")) {
            executable = targetClass.getDeclaredConstructor(paramTypes);
        } else {
            executable = targetClass.getDeclaredMethod(methodName, paramTypes);
        }

        XposedInterface.HookHandle handle = hookManager.installHook(executable, customHooker,
                hookId);

        installedExecutables.add(executable);

        HookConfig config = new HookConfig(className, methodName, paramTypes,
                methodName.equals("<init>") ? HookConfig.HookType.CONSTRUCTOR : HookConfig.HookType.METHOD,
                HookConfig.HookBehavior.INTERCEPT, null, hookId);

        return new InstalledHook(config, executable, handle);
    }

    /**
     * Gets all installed executables.
     */
    public List<Executable> getInstalledExecutables() {
        return new ArrayList<>(installedExecutables);
    }

    /**
     * Checks if a class has been hooked.
     */
    public boolean isInstalled(String className) {
        for (Executable exec : installedExecutables) {
            if (exec.getDeclaringClass().getName().equals(className)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Represents an installed hook with its metadata.
     */
    public static class InstalledHook {
        private final HookConfig config;
        private final Executable executable;
        private final XposedInterface.HookHandle handle;

        public InstalledHook(HookConfig config, Executable executable,
                             XposedInterface.HookHandle handle) {
            this.config = config;
            this.executable = executable;
            this.handle = handle;
        }

        public HookConfig getConfig() {
            return config;
        }

        public Executable getExecutable() {
            return executable;
        }

        public XposedInterface.HookHandle getHandle() {
            return handle;
        }

        /**
         * Unhooks this hook.
         */
        public void unhook() {
            try {
                handle.unhook();
            } catch (Exception ignored) {
            }
        }

        /**
         * Replaces the hooker for this hook.
         */
        public void replace(XposedInterface.Hooker newHooker) {
            handle.replaceHook(newHooker);
        }
    }
}
