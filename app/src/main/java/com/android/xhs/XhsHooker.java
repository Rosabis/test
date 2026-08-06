package com.android.xhs;

import io.github.libxposed.api.XposedInterface;

import java.lang.reflect.Executable;
import java.util.List;

/**
 * Core hook implementation using API 102's Chain-based interception model.
 * Implements XposedInterface.Hooker with intercept(Chain) method.
 */
public class XhsHooker implements XposedInterface.Hooker {

    private final HookConfig config;

    public XhsHooker(HookConfig config) {
        this.config = config;
    }

    @Override
    public Object intercept(XposedInterface.Chain chain) throws Throwable {
        Executable executable = chain.getExecutable();

        switch (config.getBehavior()) {
            case BLOCK:
                return handleBlock(chain, executable);

            case LOG_ONLY:
                return handleLogOnly(chain, executable);

            case REPLACE_RETURN:
                return handleReplaceReturn(chain, executable);

            case MODIFY_ARGS:
                return handleModifyArgs(chain, executable);

            case SKIP_AND_RETURN:
                return handleSkipAndReturn(chain, executable);

            case INTERCEPT:
            default:
                return handleIntercept(chain, executable);
        }
    }

    /**
     * Block the method execution and return a default value.
     */
    private Object handleBlock(XposedInterface.Chain chain, Executable executable) throws Throwable {
        Class<?> returnType = getReturnType(executable);
        if (returnType == void.class || returnType == Void.class) {
            return null;
        }
        return getDefaultValue(returnType);
    }

    /**
     * Log the method call but proceed normally.
     */
    private Object handleLogOnly(XposedInterface.Chain chain, Executable executable) throws Throwable {
        List<Object> args = chain.getArgs();
        String className = executable.getDeclaringClass().getSimpleName();
        String methodName = executable.getName();

        XhsModule.log("LOG_ONLY: " + className + "." + methodName + "(" + args + ")");

        return chain.proceed();
    }

    /**
     * Replace the return value after method execution.
     */
    private Object handleReplaceReturn(XposedInterface.Chain chain, Executable executable) throws Throwable {
        chain.proceed();
        return config.getReturnValue();
    }

    /**
     * Modify arguments before proceeding with the call.
     */
    private Object handleModifyArgs(XposedInterface.Chain chain, Executable executable) throws Throwable {
        Object[] modifiedArgs = modifyArguments(chain.getArgs());
        return chain.proceed(modifiedArgs);
    }

    /**
     * Skip the original method and return a custom value immediately.
     */
    private Object handleSkipAndReturn(XposedInterface.Chain chain, Executable executable) throws Throwable {
        return config.getReturnValue();
    }

    /**
     * Intercept the call, execute additional logic before/after proceeding.
     */
    private Object handleIntercept(XposedInterface.Chain chain, Executable executable) throws Throwable {
        onBefore(chain, executable);

        Object result;
        try {
            result = chain.proceed();
        } catch (Throwable t) {
            onError(chain, executable, t);
            throw t;
        }

        onAfter(chain, executable, result);
        return result;
    }

    /**
     * Called before the original method executes.
     * Override this in subclasses or use anonymous classes.
     */
    protected void onBefore(XposedInterface.Chain chain, Executable executable) {
    }

    /**
     * Called after the original method executes successfully.
     */
    protected void onAfter(XposedInterface.Chain chain, Executable executable, Object result) {
    }

    /**
     * Called when the original method throws an exception.
     */
    protected void onError(XposedInterface.Chain chain, Executable executable, Throwable error) {
        XhsModule.log("Error in " + executable.getName() + ": " + error.getMessage());
    }

    /**
     * HookId associated with this hook, or null if not set.
     */
    public String getHookId() {
        return config.getHookId();
    }

    /**
     * Returns the HookConfig that created this hooker.
     */
    public HookConfig getConfig() {
        return config;
    }

    // === Utility methods ===

    private Object[] modifyArguments(List<Object> args) {
        return args.toArray(new Object[0]);
    }

    private Class<?> getReturnType(Executable executable) {
        if (executable instanceof java.lang.reflect.Method) {
            return ((java.lang.reflect.Method) executable).getReturnType();
        }
        // Constructor returns the declaring class itself
        return executable.getDeclaringClass();
    }

    private Object getDefaultValue(Class<?> type) {
        if (type == boolean.class) return false;
        if (type == int.class) return 0;
        if (type == long.class) return 0L;
        if (type == float.class) return 0.0f;
        if (type == double.class) return 0.0;
        if (type == short.class) return (short) 0;
        if (type == byte.class) return (byte) 0;
        if (type == char.class) return '\0';
        return null;
    }
}
