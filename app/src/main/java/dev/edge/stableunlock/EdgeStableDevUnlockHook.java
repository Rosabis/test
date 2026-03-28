package dev.edge.stableunlock;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

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
 * Edge Stable 上扩展开关主要走 {@code EdgeAndroidExtensionsAPI#j()}（内部为 {@code ProfileManager#b}
 * + native {@code J.N#Z(int)}），而不是旧版 Chromium {@code ChromeFeatureList#isEnabled(String)}。
 * 仅改 FeatureList 往往无效。参见 Reddit：可通过 Root Activity Launcher 启动
 * {@code com.microsoft.edge.extensions.developer.ExtensionInstallByCrxActivity}。
 */
public final class EdgeStableDevUnlockHook implements IXposedHookLoadPackage {

    private static final String EDGE_STABLE = "com.microsoft.emmx";

    private static final String PROFILE_MANAGER =
            "org.chromium.chrome.browser.profiles.ProfileManager";
    private static final String EDGE_EXT_API =
            "com.microsoft.edge.extensions.EdgeAndroidExtensionsAPI";

    /** 同进程显式启动，不依赖 exported。 */
    private static final String EXTENSION_INSTALL_BY_CRX_ACTIVITY =
            "com.microsoft.edge.extensions.developer.ExtensionInstallByCrxActivity";

    private static final String EDGE_SETTINGS_ACTIVITY =
            "org.chromium.chrome.browser.edge_settings.EdgeSettingsActivity";
    private static final String CHROMIUM_SETTINGS_ACTIVITY =
            "org.chromium.chrome.browser.settings.SettingsActivity";

    /** 自定义菜单项 ID，勿与 Edge 内置 id 冲突。 */
    private static final int MENU_ID_INSTALL_CRX = 0x7E5010C9;

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
        hookEdgeAndroidExtensionsApi(lpparam);
        hookFeatureLists(lpparam);
        hookCrxInstallerMenuEntry(lpparam);
    }

    /**
     * 在「设置」界面工具栏菜单增加「从 CRX 安装」，从应用内启动 {@link #EXTENSION_INSTALL_BY_CRX_ACTIVITY}。
     */
    private static void hookCrxInstallerMenuEntry(XC_LoadPackage.LoadPackageParam lpparam) {
        Class<?> edgeSettings = XposedHelpers.findClassIfExists(EDGE_SETTINGS_ACTIVITY, lpparam.classLoader);
        if (edgeSettings != null) {
            try {
                XposedHelpers.findAndHookMethod(edgeSettings, "onMAMCreate", Bundle.class, new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        final Activity activity = (Activity) param.thisObject;
                        Runnable add = () -> tryAddCrxMenuToToolbar(activity);
                        activity.getWindow().getDecorView().post(add);
                        activity.getWindow().getDecorView().postDelayed(add, 300L);
                    }
                });
                XposedHelpers.findAndHookMethod(
                        edgeSettings, "onOptionsItemSelected", MenuItem.class, new XC_MethodHook() {
                            @Override
                            protected void beforeHookedMethod(MethodHookParam param) {
                                MenuItem item = (MenuItem) param.args[0];
                                if (item.getItemId() != MENU_ID_INSTALL_CRX) {
                                    return;
                                }
                                startExtensionInstallByCrx((Activity) param.thisObject);
                                param.setResult(true);
                            }
                        });
                XposedBridge.log("[EdgeStableDevUnlock] CRX menu: hooked " + EDGE_SETTINGS_ACTIVITY);
            } catch (Throwable t) {
                XposedBridge.log("[EdgeStableDevUnlock] CRX menu EdgeSettings failed: " + t);
            }
        }

        Class<?> chromiumSettings =
                XposedHelpers.findClassIfExists(CHROMIUM_SETTINGS_ACTIVITY, lpparam.classLoader);
        if (chromiumSettings != null) {
            try {
                XposedHelpers.findAndHookMethod(chromiumSettings, "onCreateOptionsMenu", Menu.class, new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        Menu menu = (Menu) param.args[0];
                        if (menu.findItem(MENU_ID_INSTALL_CRX) != null) {
                            return;
                        }
                        menu.add(0, MENU_ID_INSTALL_CRX, 999, "Install extension (CRX)");
                    }
                });
                XposedHelpers.findAndHookMethod(
                        chromiumSettings, "onOptionsItemSelected", MenuItem.class, new XC_MethodHook() {
                            @Override
                            protected void beforeHookedMethod(MethodHookParam param) {
                                MenuItem item = (MenuItem) param.args[0];
                                if (item.getItemId() != MENU_ID_INSTALL_CRX) {
                                    return;
                                }
                                startExtensionInstallByCrx((Activity) param.thisObject);
                                param.setResult(true);
                            }
                        });
                XposedBridge.log("[EdgeStableDevUnlock] CRX menu: hooked " + CHROMIUM_SETTINGS_ACTIVITY);
            } catch (Throwable t) {
                XposedBridge.log("[EdgeStableDevUnlock] CRX menu SettingsActivity failed: " + t);
            }
        }
    }

    private static void tryAddCrxMenuToToolbar(Activity activity) {
        View bar = null;
        String[] toolbarNames = {"action_bar", "toolbar", "collapsing_toolbar"};
        for (String name : toolbarNames) {
            int rid = activity.getResources().getIdentifier(name, "id", activity.getPackageName());
            if (rid == 0) {
                continue;
            }
            View v = activity.findViewById(rid);
            if (v != null) {
                bar = v;
                break;
            }
        }
        if (bar == null) {
            return;
        }
        try {
            Method getMenu = bar.getClass().getMethod("getMenu");
            Object menuObj = getMenu.invoke(bar);
            if (!(menuObj instanceof Menu)) {
                return;
            }
            Menu menu = (Menu) menuObj;
            if (menu.findItem(MENU_ID_INSTALL_CRX) != null) {
                return;
            }
            menu.add(0, MENU_ID_INSTALL_CRX, 999, "Install extension (CRX)");
        } catch (Throwable t) {
            XposedBridge.log("[EdgeStableDevUnlock] tryAddCrxMenuToToolbar: " + t);
        }
    }

    private static void startExtensionInstallByCrx(Activity activity) {
        try {
            Intent intent = new Intent();
            intent.setClassName(activity.getPackageName(), EXTENSION_INSTALL_BY_CRX_ACTIVITY);
            activity.startActivity(intent);
        } catch (Throwable t) {
            XposedBridge.log("[EdgeStableDevUnlock] startExtensionInstallByCrx: " + t);
        }
    }

    /**
     * Stable 上「是否视为已开启 Android 扩展 / 开发者能力」的核心入口；内部依赖 native J.N.Z(2)。
     */
    private static void hookEdgeAndroidExtensionsApi(XC_LoadPackage.LoadPackageParam lpparam) {
        Class<?> api = XposedHelpers.findClassIfExists(EDGE_EXT_API, lpparam.classLoader);
        Class<?> pm = XposedHelpers.findClassIfExists(PROFILE_MANAGER, lpparam.classLoader);
        if (api == null) {
            XposedBridge.log("[EdgeStableDevUnlock] 未找到 " + EDGE_EXT_API);
            return;
        }
        if (pm == null) {
            XposedBridge.log("[EdgeStableDevUnlock] 未找到 " + PROFILE_MANAGER);
            return;
        }
        try {
            XposedHelpers.findAndHookMethod(api, "j", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    if (!XposedHelpers.getStaticBooleanField(pm, "b")) {
                        return;
                    }
                    param.setResult(Boolean.TRUE);
                }
            });
            XposedBridge.log("[EdgeStableDevUnlock] hooked EdgeAndroidExtensionsAPI.j()");
        } catch (Throwable t) {
            XposedBridge.log("[EdgeStableDevUnlock] hook j() failed: " + t);
        }
        try {
            XposedHelpers.findAndHookMethod(api, "f", String.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    if (!XposedHelpers.getStaticBooleanField(pm, "b")) {
                        return;
                    }
                    param.setResult(Boolean.TRUE);
                }
            });
            XposedBridge.log("[EdgeStableDevUnlock] hooked EdgeAndroidExtensionsAPI.f(String)");
        } catch (Throwable t) {
            XposedBridge.log("[EdgeStableDevUnlock] hook f() failed: " + t);
        }
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
