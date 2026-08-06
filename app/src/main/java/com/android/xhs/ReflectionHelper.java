package com.android.xhs;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility class for reflection operations used by the hook framework.
 * Provides safe reflection methods with error handling.
 */
public class ReflectionHelper {

    /**
     * Safely gets a declared method from a class.
     */
    public static Method getDeclaredMethod(Class<?> clazz, String name, Class<?>... parameterTypes) {
        try {
            Method method = clazz.getDeclaredMethod(name, parameterTypes);
            method.setAccessible(true);
            return method;
        } catch (NoSuchMethodException e) {
            return null;
        }
    }

    /**
     * Safely gets a declared constructor from a class.
     */
    public static Constructor<?> getDeclaredConstructor(Class<?> clazz, Class<?>... parameterTypes) {
        try {
            Constructor<?> constructor = clazz.getDeclaredConstructor(parameterTypes);
            constructor.setAccessible(true);
            return constructor;
        } catch (NoSuchMethodException e) {
            return null;
        }
    }

    /**
     * Safely gets a declared field from a class.
     */
    public static Field getDeclaredField(Class<?> clazz, String name) {
        try {
            Field field = clazz.getDeclaredField(name);
            field.setAccessible(true);
            return field;
        } catch (NoSuchFieldException e) {
            return null;
        }
    }

    /**
     * Gets the value of a field from an object.
     */
    public static Object getFieldValue(Object obj, String fieldName) {
        if (obj == null) return null;
        Class<?> clazz = obj.getClass();
        while (clazz != null) {
            Field field = getDeclaredField(clazz, fieldName);
            if (field != null) {
                try {
                    return field.get(obj);
                } catch (IllegalAccessException e) {
                    return null;
                }
            }
            clazz = clazz.getSuperclass();
        }
        return null;
    }

    /**
     * Sets the value of a field on an object.
     */
    public static boolean setFieldValue(Object obj, String fieldName, Object value) {
        if (obj == null) return false;
        Class<?> clazz = obj.getClass();
        while (clazz != null) {
            Field field = getDeclaredField(clazz, fieldName);
            if (field != null) {
                try {
                    field.set(obj, value);
                    return true;
                } catch (IllegalAccessException e) {
                    return false;
                }
            }
            clazz = clazz.getSuperclass();
        }
        return false;
    }

    /**
     * Invokes a method on an object safely.
     */
    public static Object invokeMethod(Object obj, String methodName, Object... args) {
        if (obj == null) return null;
        Class<?>[] paramTypes = new Class<?>[args.length];
        for (int i = 0; i < args.length; i++) {
            paramTypes[i] = args[i] != null ? args[i].getClass() : Object.class;
        }
        Method method = getDeclaredMethod(obj.getClass(), methodName, paramTypes);
        if (method != null) {
            try {
                return method.invoke(obj, args);
            } catch (Exception e) {
                return null;
            }
        }
        return null;
    }

    /**
     * Loads a class safely using a specific ClassLoader.
     */
    public static Class<?> loadClass(ClassLoader classLoader, String className) {
        try {
            return classLoader.loadClass(className);
        } catch (ClassNotFoundException e) {
            return null;
        }
    }

    /**
     * Finds all methods with a given name in a class hierarchy.
     */
    public static List<Method> findMethodsByName(Class<?> clazz, String name) {
        List<Method> methods = new ArrayList<>();
        Class<?> current = clazz;
        while (current != null) {
            for (Method method : current.getDeclaredMethods()) {
                if (method.getName().equals(name)) {
                    method.setAccessible(true);
                    methods.add(method);
                }
            }
            current = current.getSuperclass();
        }
        return methods;
    }

    /**
     * Checks if a class exists and is accessible.
     */
    public static boolean classExists(ClassLoader classLoader, String className) {
        try {
            Class.forName(className, false, classLoader);
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    /**
     * Gets all interfaces implemented by a class.
     */
    public static List<Class<?>> getAllInterfaces(Class<?> clazz) {
        List<Class<?>> interfaces = new ArrayList<>();
        Class<?> current = clazz;
        while (current != null) {
            for (Class<?> iface : current.getInterfaces()) {
                if (!interfaces.contains(iface)) {
                    interfaces.add(iface);
                }
            }
            current = current.getSuperclass();
        }
        return interfaces;
    }

    /**
     * Converts a primitive type name to its Class object.
     */
    public static Class<?> getPrimitiveType(String typeName) {
        switch (typeName) {
            case "void": return void.class;
            case "int": return int.class;
            case "long": return long.class;
            case "boolean": return boolean.class;
            case "float": return float.class;
            case "double": return double.class;
            case "short": return short.class;
            case "byte": return byte.class;
            case "char": return char.class;
            default: return null;
        }
    }

    /**
     * Checks if a type is a primitive or wrapper type.
     */
    public static boolean isPrimitiveOrWrapper(Class<?> type) {
        return type.isPrimitive()
                || type == Boolean.class
                || type == Integer.class
                || type == Long.class
                || type == Float.class
                || type == Double.class
                || type == Short.class
                || type == Byte.class
                || type == Character.class
                || type == String.class;
    }
}
