package dev.edge.stableunlock;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * 尝试让 Edge Stable 在 Java 层通过「扩展/Android 扩展/CRX」相关 feature 判定时等价于更宽松的结果。
 * 说明：Canary 与 Stable 若 major 不一致或 Stable 未编入对应 native 逻辑，则仅能部分生效或无效。
 */
public final class EdgeStableDevUnlockHook implements IXposedHookLoadPackage {

    private static final String EDGE_STABLE = "com.microsoft.emmx";

    /** 与扩展、开发者安装相关的 feature 名子串（大小写不敏感）。可按 jadx 搜索结果追加。 */
    private static final String[] FEATURE_SUBSTRINGS = {
            "EXTENSION",
            "ANDROID_EXTENSION",
            "ANDROID_EXTENSIONS",
            "CRX",
            "SIDeload",
            "SIDELOAD",
            "DEV_EXTENSION",
            "EDGE_EXTENSION",
            "MOBILE_EXTENSION",
    };

    private static final String[] FEATURE_LIST_CLASSES = {
            "org.chromium.chrome.browser.flags.ChromeFeatureList",
            "org.chromium.chrome.browser.ChromeFeatureList",
            "com.microsoft.edge.flags.EdgeFeatureList",
            "org.chromium.chrome.browser.edge.EdgeFeatureList",
    };

    private static final String[] BUILD_INFO_CLASSES = {
            "org.chromium.base.BuildInfo",
    };

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        if (!EDGE_STABLE.equals(lpparam.packageName)) {
            return;
        }

        hookBuildInfoDebug(lpparam);
        hookFeatureLists(lpparam);
    }

    private static void hookBuildInfoDebug(XC_LoadPackage.LoadPackageParam lpparam) {
        for (String name : BUILD_INFO_CLASSES) {
            Class<?> clazz = XposedHelpers.findClassIfExists(name, lpparam.classLoader);
            if (clazz == null) {
                continue;
            }
            try {
                XposedBridge.hookAllConstructors(clazz, new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        forceDebugFields(param.thisObject);
                    }
                });
                XposedBridge.log("[EdgeStableDevUnlock] hooked BuildInfo: " + name);
            } catch (Throwable t) {
                XposedBridge.log("[EdgeStableDevUnlock] BuildInfo hook failed: " + name + " — " + t);
            }
        }
    }

    private static void forceDebugFields(Object buildInfo) {
        if (buildInfo == null) {
            return;
        }
        Class<?> c = buildInfo.getClass();
        while (c != null) {
            for (java.lang.reflect.Field f : c.getDeclaredFields()) {
                if (f.getType() != boolean.class || Modifier.isStatic(f.getModifiers())) {
                    continue;
                }
                String n = f.getName().toLowerCase(Locale.ROOT);
                if (n.equals("debug") || n.contains("isdebug") || n.endsWith("debugbuild")) {
                    try {
                        f.setAccessible(true);
                        f.setBoolean(buildInfo, true);
                    } catch (Throwable ignored) {
                    }
                }
            }
            c = c.getSuperclass();
        }
    }

    private static void hookFeatureLists(XC_LoadPackage.LoadPackageParam lpparam) {
        Set<String> hooked = new HashSet<>();
        for (String className : FEATURE_LIST_CLASSES) {
            Class<?> clazz = XposedHelpers.findClassIfExists(className, lpparam.classLoader);
            if (clazz == null) {
                continue;
            }
            for (Method m : clazz.getDeclaredMethods()) {
                if (!Modifier.isStatic(m.getModifiers())) {
                    continue;
                }
                if (m.getReturnType() != Boolean.TYPE && m.getReturnType() != Boolean.class) {
                    continue;
                }
                if (!"isEnabled".equals(m.getName())) {
                    continue;
                }
                Class<?>[] params = m.getParameterTypes();
                if (params.length < 1 || params[0] != String.class) {
                    continue;
                }
                String key = className + "#" + m.getName() + Arrays.toString(params);
                if (!hooked.add(key)) {
                    continue;
                }
                try {
                    XposedBridge.hookMethod(m, new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                            Object arg0 = param.args[0];
                            if (!(arg0 instanceof String)) {
                                return;
                            }
                            String feature = (String) arg0;
                            if (shouldForceFeatureOn(feature)) {
                                param.setResult(Boolean.TRUE);
                            }
                        }
                    });
                    XposedBridge.log("[EdgeStableDevUnlock] hooked " + key);
                } catch (Throwable t) {
                    XposedBridge.log("[EdgeStableDevUnlock] hook fail " + key + " — " + t);
                }
            }
        }
        if (hooked.isEmpty()) {
            XposedBridge.log(
                    "[EdgeStableDevUnlock] 未找到可 Hook 的 FeatureList；请用 jadx 打开本机 emmx.apk，搜索 "
                            + "ChromeFeatureList / EdgeFeatureList / isEnabled(String)，把类名加入 FEATURE_LIST_CLASSES。");
        }
    }

    static boolean shouldForceFeatureOn(String featureName) {
        if (featureName == null || featureName.isEmpty()) {
            return false;
        }
        String u = featureName.toUpperCase(Locale.ROOT);
        for (String sub : FEATURE_SUBSTRINGS) {
            if (u.contains(sub.toUpperCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }
}
