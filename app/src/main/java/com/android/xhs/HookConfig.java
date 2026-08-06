package com.android.xhs;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * Configuration model for a single hook target.
 * Encapsulates which class/method/constructor to hook and the hook behavior.
 */
public class HookConfig {

    public enum HookType {
        METHOD,
        CONSTRUCTOR
    }

    public enum HookBehavior {
        /** Block the method call, return a default/null value */
        BLOCK,
        /** Log the call but proceed normally */
        LOG_ONLY,
        /** Replace return value */
        REPLACE_RETURN,
        /** Modify arguments before proceeding */
        MODIFY_ARGS,
        /** Skip execution and return custom value */
        SKIP_AND_RETURN,
        /** Execute additional code before/after */
        INTERCEPT
    }

    private final String className;
    private final String methodName;
    private final Class<?>[] parameterTypes;
    private final HookType hookType;
    private final HookBehavior behavior;
    private final Object returnValue;
    private final String hookId;

    public HookConfig(String className, String methodName, Class<?>[] parameterTypes,
                      HookType hookType, HookBehavior behavior, Object returnValue,
                      String hookId) {
        this.className = className;
        this.methodName = methodName;
        this.parameterTypes = parameterTypes;
        this.hookType = hookType;
        this.behavior = behavior;
        this.returnValue = returnValue;
        this.hookId = hookId;
    }

    public String getClassName() {
        return className;
    }

    public String getMethodName() {
        return methodName;
    }

    public Class<?>[] getParameterTypes() {
        return parameterTypes;
    }

    public HookType getHookType() {
        return hookType;
    }

    public HookBehavior getBehavior() {
        return behavior;
    }

    public Object getReturnValue() {
        return returnValue;
    }

    public String getHookId() {
        return hookId;
    }

    /**
     * Resolves and returns the target Method or Constructor.
     */
    public java.lang.reflect.Executable resolve(Class<?> targetClass) throws NoSuchMethodException {
        if (hookType == HookType.METHOD) {
            return targetClass.getDeclaredMethod(methodName, parameterTypes);
        } else {
            return targetClass.getDeclaredConstructor(parameterTypes);
        }
    }

    /**
     * Parses a simple config string format:
     * "className|methodName|paramType1,paramType2|hookType|behavior|returnValue"
     */
    public static HookConfig parse(String configLine) {
        String[] parts = configLine.split("\\|");
        if (parts.length < 4) return null;

        String className = parts[0].trim();
        String methodName = parts[1].trim();
        String[] paramStrs = parts[2].trim().isEmpty()
                ? new String[0]
                : parts[2].trim().split(",");

        Class<?>[] paramTypes = new Class<?>[paramStrs.length];
        for (int i = 0; i < paramStrs.length; i++) {
            paramTypes[i] = parseType(paramStrs[i].trim());
        }

        HookType hookType = HookType.valueOf(parts[3].trim().toUpperCase());
        HookBehavior behavior = parts.length > 4
                ? HookBehavior.valueOf(parts[4].trim().toUpperCase())
                : HookBehavior.INTERCEPT;

        Object returnValue = parts.length > 5 ? parts[5].trim() : null;
        String hookId = parts.length > 6 ? parts[6].trim() : null;

        return new HookConfig(className, methodName, paramTypes, hookType, behavior, returnValue, hookId);
    }

    private static Class<?> parseType(String typeName) {
        switch (typeName) {
            case "int": return int.class;
            case "long": return long.class;
            case "boolean": return boolean.class;
            case "float": return float.class;
            case "double": return double.class;
            case "short": return short.class;
            case "byte": return byte.class;
            case "char": return char.class;
            case "String": return String.class;
            case "void": return void.class;
            default:
                try {
                    return Class.forName(typeName);
                } catch (ClassNotFoundException e) {
                    return Object.class;
                }
        }
    }
}
