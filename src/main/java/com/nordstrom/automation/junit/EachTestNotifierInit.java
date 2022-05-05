package com.nordstrom.automation.junit;

import static com.nordstrom.automation.junit.LifecycleHooks.toMapKey;

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

/**
 * This class declares the interceptor for the constructor of the {@link EachTestNotifier} class.
 */
public class EachTestNotifierInit {
    
    private static final Map<Integer, AtomicTest> DESCRIPTION_TO_ATOMICTEST = new ConcurrentHashMap<>();
    private static final Map<String, Integer> TARGET_TO_DESCRIPTION = new ConcurrentHashMap<>();
    private static final Map<Integer, Object> DESCRIPTION_TO_TARGET = new ConcurrentHashMap<>();
    private static final Map<Integer, Integer> HASHCODE_TO_DESCRIPTION = new ConcurrentHashMap<>();
    
    /**
     * Interceptor for the constructor of the {@link EachTestNotifier} class.
     * 
     * @param notifier {@link RunNotifier} argument
     * @param description {@link Description} argument
     */
    public static void interceptor(@Argument(0) final RunNotifier notifier,
                                   @Argument(1) final Description description) {
        
        // if notifier for test
        if (AtomicTest.isTest(description)) {
            // create new atomic test object
            AtomicTest atomicTest = newAtomicTestFor(description);
            // get current thread runner
            Object runner = atomicTest.getRunner();
            
            FrameworkMethod method = null;
            // get particle methods of current runner
            List<Object> children = LifecycleHooks.invoke(runner, "getChildren");
            // iterate particle methods
            for (Object child : children) {
                // if this method matches subject description
                if (description.equals(LifecycleHooks.describeChild(runner, child))) {
                    // subject method resolved
                    method = (FrameworkMethod) child;
                    break;
                }
            }
            // if method unknown
            if (method == null) {
                try {
                    // get method => description mappings
                    Map<FrameworkMethod, Description> assoc = 
                            LifecycleHooks.getFieldValue(runner, "methodDescriptions");
                    // iterate method => description mappings
                    for (Entry<FrameworkMethod, Description> entry : assoc.entrySet()) {
                        // if this entry maps subject description
                        if (description.equals(entry.getValue())) {
                            // subject method resolved
                            method = entry.getKey();
                            break;
                        }
                    }
                } catch (IllegalAccessException | NoSuchFieldException | SecurityException e) {
                    // nothing to do here
                }
            }
            // if method resolved
            if (method != null) {
                // get target test class instance
                Object target = CreateTest.getTargetFor(runner, method);
                // if target exists
                if (target != null) {
                    // create description <=> target mappings
                    createMappingsFor(description.hashCode(), target);
                // otherwise (target not yet created)
                } else {
                    // store [runner + method] => description mapping for 'setTestTarget' method
                    HASHCODE_TO_DESCRIPTION.put(Objects.hash(runner, method.toString()), description.hashCode());
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
     * @return {@link AtomicTest} object
     */
    private static AtomicTest newAtomicTestFor(Description description) {
        // create new atomic test object
        AtomicTest atomicTest = new AtomicTest(description);
        // create description => atomic test mapping
        DESCRIPTION_TO_ATOMICTEST.put(description.hashCode(), atomicTest);
        
        return atomicTest;
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
    
    /**
     * Get the test class instance for the specified method description.
     * 
     * @param description JUnit method description
     * @return test class instance (may be {@code null})
     */
    static Object getTargetOf(Description description) {
        return DESCRIPTION_TO_TARGET.get(description.hashCode());
    }
    
    /**
     * Get the atomic test for the specified notifier.
     * 
     * @param notifier JUnit {@link EachTestNotifier} object
     * @return {@link AtomicTest} object (may be {@code null})
     */
    static AtomicTest getAtomicTestOf(EachTestNotifier notifier) {
        return DESCRIPTION_TO_ATOMICTEST.get(getDescriptionOf(notifier).hashCode());
    }
    
    /**
     * Set the target test class instance for the specified runner/method pair.
     * <p>
     * <b>NOTE</b>: If the associated notifier has yet to be instantiated, no mapping is established.
     * 
     * @param runner JUnit class runner
     * @param method JUnit framework method
     * @param target test class instance
     * @return {@code true} is mapping was established; otherwise {@code false}
     */
    static boolean setTestTarget(Object runner, FrameworkMethod method, Object target) {
        // get [runner + method] => description mapping
        Integer descriptionHash = getDescriptionHashFor(runner, method);
        // if mapping exists
        if (descriptionHash != null) {
            // create description <=> target mappings
            createMappingsFor(descriptionHash, target);
            return true;
        }
        return false;
    }

    /**
     * Create mappings for the specified description hash code/test class instance.
     * 
     * @param descriptionHash {@link Description} hash code
     * @param target test class instance
     */
    static void createMappingsFor(Integer descriptionHash, Object target) {
        TARGET_TO_DESCRIPTION.put(toMapKey(target), descriptionHash);
        DESCRIPTION_TO_TARGET.put(descriptionHash, target);
    }
    
    /**
     * Release mappings associated with the specified notifier.
     * 
     * @param notifier {@link EachTestNotifier} object
     */
    static void releaseMappingsFor(EachTestNotifier notifier) {
        Description description = getDescriptionOf(notifier);
        AtomicTest atomicTest = releaseAtomicTestOf(description);
        HASHCODE_TO_DESCRIPTION.remove(Objects.hash(atomicTest.getRunner(), atomicTest.getIdentity().toString()));
        Object target = DESCRIPTION_TO_TARGET.remove(description.hashCode());
        if (target != null) {
            TARGET_TO_DESCRIPTION.remove(toMapKey(target));
        }
        
        RunReflectiveCall.releaseCallableOf(description);
        ArtifactCollector.releaseWatchersOf(description);
        CreateTest.releaseMappingsFor(atomicTest.getRunner(), atomicTest.getIdentity(), target);
        GetAnnotations.releaseAnnotationsFor(atomicTest.getIdentity());
        TestMethodDescription.releaseDescriptionFor(atomicTest.getIdentity());
    }

    /**
     * Release the atomic test associated with the specified description.
     * 
     * @param description {@link Description} object
     * @return {@link AtomicTest} object
     */
    static AtomicTest releaseAtomicTestOf(Description description) {
        return DESCRIPTION_TO_ATOMICTEST.remove(description.hashCode());
    }

    /**
     * Get the atomic test object associated with the specified test class instance.
     * 
     * @param target test class instance
     * @return {@link AtomicTest} object (may be {@code null})
     */
    static AtomicTest getAtomicTestOf(Object target) {
        if (target != null) {
            Integer descriptionHash = TARGET_TO_DESCRIPTION.get(toMapKey(target));
            if (descriptionHash != null) {
                return DESCRIPTION_TO_ATOMICTEST.get(descriptionHash);
            }
        }
        return null;
    }
    
    /**
     * Get the description associated with the specified notifier.
     * 
     * @param notifier {@link EachTestNotifier} object
     * @return {@link Description} object
     */
    private static Description getDescriptionOf(EachTestNotifier notifier) {
        try {
            return LifecycleHooks.getFieldValue(notifier, "description");
        } catch (IllegalAccessException | NoSuchFieldException | SecurityException e) {
            throw UncheckedThrow.throwUnchecked(e);
        }
    }

    /**
     * Get the description hash code associated with the specified runner/method pair.
     * 
     * @param runner JUnit class runner
     * @param method JUnit framework method
     * @return {@link Description} hash code
     */
    private static Integer getDescriptionHashFor(Object runner, FrameworkMethod method) {
        return HASHCODE_TO_DESCRIPTION.get(Objects.hash(runner, method.toString()));
    }
}
