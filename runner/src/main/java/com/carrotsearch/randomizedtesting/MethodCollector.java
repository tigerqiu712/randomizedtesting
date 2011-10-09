package com.carrotsearch.randomizedtesting;

import static java.lang.reflect.Modifier.isPrivate;
import static java.lang.reflect.Modifier.isProtected;
import static java.lang.reflect.Modifier.isPublic;
import static java.lang.reflect.Modifier.isStatic;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

/**
 * Method collection and filtering utilities (using reflection).
 * No particular pressure on performance, should be a fraction of tests' time anyway.
 */
final class MethodCollector {
  /**
   * Processes the result of {@link #allDeclaredMethods(Class)} and removes
   * any overridden methods (public, protected, package scope if in the same package.). 
   * 
   * <p>The order of the input is assumed to be: clazz..super.
   */
  public static List<List<Method>> removeOverrides(List<List<Method>> declaredMethods) {
    List<List<Method>> result = new ArrayList<List<Method>>();
    final HashMap<String, List<Method>> subclasses = new HashMap<String, List<Method>>();
    for (List<Method> classLevel : declaredMethods) {
      List<Method> pruned = new ArrayList<Method>();
      for (Method  m : classLevel) {
        final String sig = signature(m);
        final int modifiers = m.getModifiers();
        
        if (isPrivate(modifiers) || isStatic(modifiers)) {
          pruned.add(m);
          continue;
        }

        if (!subclasses.containsKey(sig)) {
          pruned.add(m);
          subclasses.put(sig, new ArrayList<Method>());
          subclasses.get(sig).add(m);
          continue;
        }

        if (isPackageScope(modifiers)) {
          boolean samePackage = false;
          for (Method other : subclasses.get(sig)) {
            final Package p = m.getDeclaringClass().getPackage();
            if (other.getDeclaringClass().getPackage().equals(p)) {
              samePackage = true;
              break;
            }
          }
          if (!samePackage) {
            pruned.add(m);
            subclasses.get(sig).add(m);
          }
        }
      }
      result.add(pruned);
    }
    return result;
  }

  /**
   * Collect all declared methods in the class stack up to 
   * <code>java.lang.Object</code> (exclusive). The methods in the returned
   * list are laid from <code>clazz</code> to the class extending <code>Object</code>.
   */
  public static List<List<Method>> allDeclaredMethods(Class<?> clazz) {
    List<List<Method>> result = new ArrayList<List<Method>>();
    for (Class<?> c = clazz; c != Object.class; c = c.getSuperclass()) {
      Method[] declaredMethods = c.getDeclaredMethods();
      if (declaredMethods.length > 0) {
        result.add(new ArrayList<Method>(Arrays.asList(declaredMethods)));
      }
    }
    return result;
  }

  /**
   * Sort a list of methods by name and parameters.
   */
  public static List<List<Method>> sort(List<List<Method>> methods) {
    List<List<Method>> result = new ArrayList<List<Method>>();
    for (List<Method> classMethods : methods) {
      List<Method> clone = new ArrayList<Method>(classMethods);
      Collections.sort(clone, new Comparator<Method>() {
        public int compare(Method o1, Method o2) {
          return signature(o1).compareTo(signature(o2));
        }
      });
      result.add(clone);
    }
    return result;
  }  

  private static boolean isPackageScope(int modifiers) {
    return !isPublic(modifiers) && !isProtected(modifiers) && !isPrivate(modifiers);
  }

  private static String signature(Method m) {
    return m.getName() + Arrays.toString(m.getParameterTypes());
  }
}
