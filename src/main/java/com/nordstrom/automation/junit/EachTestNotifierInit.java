package com.nordstrom.automation.junit;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import org.junit.internal.runners.model.EachTestNotifier;
import org.junit.runner.Description;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.model.FrameworkMethod;
import com.nordstrom.common.base.UncheckedThrow;

import net.bytebuddy.implementation.bind.annotation.Argument;

import static com.nordstrom.automation.junit.LifecycleHooks.toMapKey;

public class EachTestNotifierInit {
    
    private static final Map<Integer, AtomicTest> DESCRIPTION_TO_ATOMICTEST = new ConcurrentHashMap<>();
    private static final Map<String, Integer> TARGET_TO_DESCRIPTION = new ConcurrentHashMap<>();
    private static final Map<Integer, Object> DESCRIPTION_TO_TARGET = new ConcurrentHashMap<>();
    private static final Map<Integer, Integer> HASHCODE_TO_DESCRIPTION = new ConcurrentHashMap<>();
    
    public static void interceptor(@Argument(0) final RunNotifier notifier,
                                   @Argument(1) final Description description) {
        
        if (description.isTest()) {
            newAtomicTestFor(description);
            Object runner = RunChildren.getThreadRunner();
            FrameworkMethod method = null;
            List<Object> children = LifecycleHooks.invoke(runner, "getChildren");
            for (Object child : children) {
                if (description.equals(LifecycleHooks.describeChild(runner, child))) {
                    method = (FrameworkMethod) child;
                    break;
                }
            }
            if (method == null) {
                try {
                    Map<FrameworkMethod, Description> assoc = 
                            LifecycleHooks.getFieldValue(runner, "methodDescriptions");
                    for (Entry<FrameworkMethod, Description> entry : assoc.entrySet()) {
                        if (description.equals(entry.getValue())) {
                            method = entry.getKey();
                        }
                    }
                } catch (IllegalAccessException | NoSuchFieldException | SecurityException e) {
                    // nothing to do here
                }
            }
            if (method != null) {
                Object target = CreateTest.getTargetFor(runner, method);
                if (target != null) {
                    createMappingsFor(description.hashCode(), target);
                } else {
                    HASHCODE_TO_DESCRIPTION.put(Objects.hash(runner, method), description.hashCode());
                }
            } else {
                throw new IllegalStateException("unable to determine method");
            }
        }
    }
    
    /**
     * Create new atomic test object for the specified description.
     * 
     * @param description description of the test that is about to be run
     * @return {@link AtomicTest} object (may be {@code null})
     */
    static AtomicTest newAtomicTestFor(Description description) {
        AtomicTest atomicTest = null;
        if (description.isTest()) {
            atomicTest = new AtomicTest(description);
            DESCRIPTION_TO_ATOMICTEST.put(description.hashCode(), atomicTest);
        }
        return atomicTest;
    }
    
    /**
    * Get the atomic test object for the specified method description; create if absent.
    * 
    * @param description JUnit method description
    * @return {@link AtomicTest} object (may be {@code null})
    */
    static AtomicTest ensureAtomicTestOf(Description description) {
        if (DESCRIPTION_TO_ATOMICTEST.containsKey(description.hashCode())) {
            return DESCRIPTION_TO_ATOMICTEST.get(description.hashCode());
        } else {
            return newAtomicTestFor(description);
        }
    }
    
    /**
     * Get the atomic test object for the specified method description.
     * 
     * @param description JUnit method description
     * @return {@link AtomicTest} object (may be {@code null})
     */
    static AtomicTest getAtomicTestOf(Description description) {
        if (description != null) {
            return DESCRIPTION_TO_ATOMICTEST.get(description.hashCode());
        }
        return null;
    }
    
    static AtomicTest getAtomicTestOf(EachTestNotifier notifier) {
        return DESCRIPTION_TO_ATOMICTEST.get(getDescriptionOf(notifier).hashCode());
    }
    
    static Object getTargetOf(EachTestNotifier notifier) {
        return DESCRIPTION_TO_TARGET.get(getDescriptionOf(notifier).hashCode());
    }
    
    static Integer getDescriptionHashFor(Object runner, FrameworkMethod method) {
        return HASHCODE_TO_DESCRIPTION.get(Objects.hash(runner, method));
    }
    
    static boolean setTestTarget(Object runner, FrameworkMethod method, Object target) {
        Integer descriptionHash = getDescriptionHashFor(runner, method);
        if (descriptionHash != null) {
            createMappingsFor(descriptionHash, target);
            return true;
        }
        return false;
    }

    static void createMappingsFor(Integer descriptionHash, Object target) {
        TARGET_TO_DESCRIPTION.put(toMapKey(target), descriptionHash);
        DESCRIPTION_TO_TARGET.put(descriptionHash, target);
    }
    
    static void releaseMappingsFor(EachTestNotifier notifier) {
        
        Description description = getDescriptionOf(notifier);
        AtomicTest atomicTest = releaseAtomicTestOf(description);
        HASHCODE_TO_DESCRIPTION.remove(Objects.hash(atomicTest.getRunner(), atomicTest.getIdentity()));
        Object target = DESCRIPTION_TO_TARGET.remove(description.hashCode());
        if (target != null) {
            TARGET_TO_DESCRIPTION.remove(toMapKey(target));
        }
        
        RunReflectiveCall.releaseCallableOf(description);
        CreateTest.releaseMappingsFor(atomicTest.getRunner(), atomicTest.getIdentity(), target);
    }

    static AtomicTest releaseAtomicTestOf(Description description) {
        return DESCRIPTION_TO_ATOMICTEST.remove(description.hashCode());
    }

    private static Description getDescriptionOf(EachTestNotifier notifier) {
        try {
            return LifecycleHooks.getFieldValue(notifier, "description");
        } catch (IllegalAccessException | NoSuchFieldException | SecurityException e) {
            throw UncheckedThrow.throwUnchecked(e);
        }
    }

    public static AtomicTest getAtomicTestOf(Object target) {
        if (target != null) {
            Integer descriptionHash = TARGET_TO_DESCRIPTION.get(toMapKey(target));
            if (descriptionHash != null) {
                return DESCRIPTION_TO_ATOMICTEST.get(descriptionHash);
            }
        }
        return null;
    }
}
