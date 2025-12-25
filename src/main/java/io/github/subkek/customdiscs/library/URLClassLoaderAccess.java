package io.github.subkek.customdiscs.library;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collection;

public abstract class URLClassLoaderAccess {


  public static URLClassLoaderAccess create(URLClassLoader classLoader) {
    if (Reflection.isSupported()) {
      return new Reflection(classLoader);
    } else if (Unsafe.isSupported()) {
      return new Unsafe(classLoader);
    } else {
      return Noop.INSTANCE;
    }
  }

  private static void throwError(Throwable cause) throws UnsupportedOperationException {
    throw new UnsupportedOperationException("""
                CustomDiscs is unable to inject into the plugin URLClassLoader.
                You may be able to fix this problem by adding the following command-line argument directly after the 'java' command in your start script:\s
                '--add-opens java.base/java.lang=ALL-UNNAMED'""", cause);
  }

  private final URLClassLoader classLoader;


  protected URLClassLoaderAccess(URLClassLoader classLoader) {
    this.classLoader = classLoader;
  }

  public abstract void addURL(URL url);

  private static class Reflection extends URLClassLoaderAccess {

    private static final Method ADD_URL_METHOD;

    static {
      Method addUrlMethod;
      try {
        addUrlMethod = URLClassLoader.class.getDeclaredMethod("addURL", URL.class);
        addUrlMethod.setAccessible(true);
      } catch (Exception e) {
        addUrlMethod = null;
      }
      ADD_URL_METHOD = addUrlMethod;
    }

    private static boolean isSupported() {
      return ADD_URL_METHOD != null;
    }

    Reflection(URLClassLoader classLoader) {
      super(classLoader);
    }

    @Override
    public void addURL(URL url) {
      try {
        ADD_URL_METHOD.invoke(super.classLoader, url);
      } catch (ReflectiveOperationException e) {
        URLClassLoaderAccess.throwError(e);
      }
    }

  }

  private static class Unsafe extends URLClassLoaderAccess {

    private static final Object UNSAFE;
    private static final Method OBJECT_FIELD_OFFSET_METHOD;
    private static final Method GET_OBJECT_METHOD;

    static {
      Object unsafe;
      Method objectFieldOffsetMethod;
      Method getObjectMethod;
      try {
        Class<?> unsafeClass = Class.forName("sun.misc.Unsafe");
        Field unsafeField = unsafeClass.getDeclaredField("theUnsafe");
        unsafeField.setAccessible(true);
        unsafe = unsafeField.get(null);
        objectFieldOffsetMethod = unsafe.getClass().getMethod("objectFieldOffset", Field.class);
        getObjectMethod = unsafe.getClass().getMethod("getObject", Object.class, long.class);
      } catch (Throwable t) {
        unsafe = null;
        objectFieldOffsetMethod = null;
        getObjectMethod = null;
      }
      UNSAFE = unsafe;
      OBJECT_FIELD_OFFSET_METHOD = objectFieldOffsetMethod;
      GET_OBJECT_METHOD = getObjectMethod;
    }

    private static boolean isSupported() {
      return UNSAFE != null;
    }

    private static Object fetchField(final Class<?> clazz, final Object object, final String name) throws NoSuchFieldException, InvocationTargetException, IllegalAccessException {
      Field field = clazz.getDeclaredField(name);
      long offset = (long) OBJECT_FIELD_OFFSET_METHOD.invoke(UNSAFE, field);
      return GET_OBJECT_METHOD.invoke(UNSAFE, object, offset);
    }

    private final Collection<URL> unopenedURLs;
    private final Collection<URL> pathURLs;

    @SuppressWarnings("all")
    Unsafe(URLClassLoader classLoader) {
      super(classLoader);

      Collection<URL> unopenedURLs;
      Collection<URL> pathURLs;
      try {
        Object ucp = fetchField(URLClassLoader.class, classLoader, "ucp");
        unopenedURLs = (Collection<URL>) fetchField(ucp.getClass(), ucp, "unopenedUrls");
        pathURLs = (Collection<URL>) fetchField(ucp.getClass(), ucp, "path");
      } catch (Throwable e) {
        unopenedURLs = null;
        pathURLs = null;
      }

      this.unopenedURLs = unopenedURLs;
      this.pathURLs = pathURLs;
    }

    @Override
    public void addURL(URL url) {
      if (this.unopenedURLs == null || this.pathURLs == null) {
        URLClassLoaderAccess.throwError(new NullPointerException("unopenedURLs or pathURLs"));
      }

      this.unopenedURLs.add(url);
      this.pathURLs.add(url);
    }

  }

  private static class Noop extends URLClassLoaderAccess {

    private static final Noop INSTANCE = new Noop();

    private Noop() {
      super(null);
    }

    @Override
    public void addURL(URL url) {
      URLClassLoaderAccess.throwError(null);
    }

  }
}