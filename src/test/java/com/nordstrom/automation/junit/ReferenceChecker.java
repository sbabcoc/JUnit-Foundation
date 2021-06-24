package com.nordstrom.automation.junit;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.junit.runner.Description;
import org.junit.runner.Result;
import org.junit.runner.notification.RunListener;
import org.junit.runners.model.FrameworkMethod;
import org.testng.Reporter;

/**
 * This run listener tracks the results of executed tests.
 * 
 * SCOPE: Atomic Test / Test Class Instance
 * - ArtifactCollector.WATCHER_MAP - checkWatcherFor(Description description)
 * - CreateTest.HASHCODE_TO_TARGET - checkTargetFor(Object runner, FrameworkMethod method)
 * - CreateTest.TARGET_TO_METHOD - checkMethodFor(Object target)
 * - CreateTest.TARGET_TO_RUNNER - checkRunnerFor(Object target)
 * - EachTestNotifierInit.DESCRIPTION_TO_ATOMICTEST - checkAtomicTestFor(Description description)
 * - EachTestNotifierInit.TARGET_TO_DESCRIPTION - checkDescriptionFor(Object target)
 * - EachTestNotifierInit.DESCRIPTION_TO_TARGET - checkTargetFor(Description description)
 * - EachTestNotifierInit.HASHCODE_TO_DESCRIPTION - checkDescriptionFor(Object runner, FrameworkMethod method)
 * - RunReflectiveCall.DESCRIPTION_TO_CALLABLE - checkCallableFor(Description description)
 * 
 * SCOPE: Class Runner
 * - Run.START_NOTIFIED
 * - Run.CHILD_TO_PARENT
 * - Run.RUNNER_TO_NOTIFIER
 * - RunChild.DID_NOTIFY
 * 
 * SCOPE: Test Run
 * - Run.RUNNER_STACK
 */
@RunAnnouncer.ThreadSafe
public class ReferenceChecker extends RunListener {
    
    private static Field UNIQUE_ID;
    
    /* SCOPE: Atomic Test / Test Class Instance */
    
    private static Field RUNNER_TO_STATEMENT;
    private static Field WATCHER_MAP;
    private static Field HASHCODE_TO_TARGET;
    private static Field TARGET_TO_METHOD;
    private static Field TARGET_TO_RUNNER;
    private static Field DESCRIPTION_TO_ATOMICTEST;
    private static Field TARGET_TO_DESCRIPTION;
    private static Field DESCRIPTION_TO_TARGET;
    private static Field HASHCODE_TO_DESCRIPTION;
    private static Field DESCRIPTION_TO_CALLABLE;
    
    /* SCOPE: Class Runner */

    private static Field START_NOTIFIED;
    private static Field CHILD_TO_PARENT;
    private static Field RUNNER_TO_NOTIFIER;
    private static Field DID_NOTIFY;
    
    private List<String> noAtomicTests = Collections.synchronizedList(new ArrayList<String>());
    private List<String> statementLeaks = Collections.synchronizedList(new ArrayList<String>());
    private List<String> watcherLeaks = Collections.synchronizedList(new ArrayList<String>());
    private List<String> atomicTestLeaks = Collections.synchronizedList(new ArrayList<String>());
    private List<String> targetLeaks = Collections.synchronizedList(new ArrayList<String>());
    private List<String> descriptionLeaks = Collections.synchronizedList(new ArrayList<String>());
    private List<String> methodLeaks = Collections.synchronizedList(new ArrayList<String>());
    private List<String> runnerLeaks = Collections.synchronizedList(new ArrayList<String>());
    private List<String> startFlagLeaks = Collections.synchronizedList(new ArrayList<String>());
    private List<String> notifierLeaks = Collections.synchronizedList(new ArrayList<String>());
    private List<String> notifyFlagLeaks = Collections.synchronizedList(new ArrayList<String>());
    private List<String> parentLeaks = Collections.synchronizedList(new ArrayList<String>());
    private String runnerStackLeak;
    
    private Map<Integer, String> descriptionMap = new ConcurrentHashMap<>();
    private Map<Integer, String> hashCodeMap = new ConcurrentHashMap<>();
    private Map<String, String> targetMap = new ConcurrentHashMap<>();
    private Map<String, String> runnerMap = new ConcurrentHashMap<>();
    private Map<String, String> childMap = new ConcurrentHashMap<>();
    
    /**
     * Called when all tests have finished. This may be called on an
     * arbitrary thread.
     *
     * @param result the summary of the test run, including all the tests that failed
     */
    @Override
    public void testRunFinished(Result result) throws Exception {
        String message = null;
        Reporter.log("Run finished: " + result.toString());
        
        for (String runnerKey : runnerMap.keySet()) {
            String name = runnerMap.remove(runnerKey);
            
            message = checkStartFlag(runnerKey);
            if (message != null) {
                startFlagLeaks.add(message + name);
            }
            
            message = checkNotifierFor(runnerKey);
            if (message != null) {
                notifierLeaks.add(message + name);
            }
            
            message = checkNotifyFlag(runnerKey);
            if (message != null) {
                notifyFlagLeaks.add(message + name);
            }
        }
        
        for (String childKey : childMap.keySet()) {
            String name = childMap.remove(childKey);
            
            message = checkParentOf(childKey);
            if (message != null) {
                parentLeaks.add(message + name);
            }
        }
        
        runnerStackLeak = checkRunnerStack();
    }
    
    /**
     * Called when a test suite has finished, whether the test suite succeeds or fails.
     * This method will not be called for a given {@link Description} unless
     * {@link #testSuiteStarted(Description)} was called for the same @code Description}.
     *
     * @param description the description of the test suite that just ran
     */
    @Override
    public void testSuiteFinished(Description suite) throws Exception {
        String message = null;
        Reporter.log("Suite finished: " + suite.getDisplayName());
        
        for (Integer descriptionHashCode : descriptionMap.keySet()) {
            String name = descriptionMap.remove(descriptionHashCode);
            
            message = checkWatcherFor(descriptionHashCode);
            if (message != null) {
                watcherLeaks.add(message + name);
            }
            
            message = checkAtomicTestFor(descriptionHashCode);
            if (message != null) {
                atomicTestLeaks.add(message + name);
            }
            
            message = checkTargetFor(descriptionHashCode);
            if (message != null) {
                atomicTestLeaks.add(message + name);
            }
            
            message = checkCallableFor(descriptionHashCode);
            if (message != null) {
                atomicTestLeaks.add(message + name);
            }
        }
        
        for (Integer hashCode : hashCodeMap.keySet()) {
            String name = hashCodeMap.remove(hashCode);
            
            message = checkTargetForHashCode(hashCode);
            if (message != null) {
                targetLeaks.add(message + name);
            }
            
            message = checkDescriptionForHashCode(hashCode);
            if (message != null) {
                descriptionLeaks.add(message + name);
            }
        }
        
        for (String targetKey : targetMap.keySet()) {
            String name = targetMap.remove(targetKey);
            
            message = checkMethodFor(targetKey);
            if (message != null) {
                methodLeaks.add(message + name);
            }
            
            message = checkRunnerFor(targetKey);
            if (message != null) {
                runnerLeaks.add(message + name);
            }
            
            message = checkDescriptionFor(targetKey);
            if (message != null) {
                descriptionLeaks.add(message + name);
            }
        }
    }
    
    /**
     * Called when an atomic test has finished, whether the test succeeds or fails.
     * 
     * @param description the description of the test that just ran
     * @throws IllegalAccessException if field is inaccessible
     * @throws IllegalArgumentException cannot be thrown here
     */
    @Override
    public void testFinished(Description description) throws IllegalArgumentException, IllegalAccessException {
        AtomicTest atomicTest = LifecycleHooks.getAtomicTestOf(description);
        if (atomicTest != null) {
            String message = checkStatementOf(atomicTest.getRunner());
            if (message != null) {
                statementLeaks.add(message + nameFor(description));
            }
        } else {
            noAtomicTests.add(nameFor(description));
        }
        
        descriptionMap.put(description.hashCode(), nameFor(description));
        
        Object runner = atomicTest.getRunner();
        FrameworkMethod method = atomicTest.getIdentity();
        String name = method.getName() + "(" + LifecycleHooks.toMapKey(runner) + ")";
        hashCodeMap.put(Objects.hash(runner, method), name);
        
        Object target = LifecycleHooks.getTargetOf(description);
        if (target != null) {
            String key = LifecycleHooks.toMapKey(target);
            targetMap.put(key, key);
        }
    }
    
    public void runFinished(Object runner) {
        String key = LifecycleHooks.toMapKey(runner);
        runnerMap.put(key, key);
        
        for (Object child : (List<?>) LifecycleHooks.invoke(runner, "getChildren")) {
            key = LifecycleHooks.toMapKey(child);
            childMap.put(key, key);
        }
    }
    
    public int reportNoAtomicTests() {
        for (String description : noAtomicTests) {
            Reporter.log("ERROR: No atomic test object found for: " + description);
        }
        return noAtomicTests.size();
    }
    
    public int reportStatementLeaks() {
        for (String message : statementLeaks) {
            Reporter.log(message);
        }
        return statementLeaks.size();
    }
    
    public int reportWatcherLeaks() {
        for (String message : watcherLeaks) {
            Reporter.log(message);
        }
        return watcherLeaks.size();
    }
    
    public int reportAtomicTestLeaks() {
        for (String message : atomicTestLeaks) {
            Reporter.log(message);
        }
        return atomicTestLeaks.size();
    }
    
    public int reportTargetLeaks() {
        for (String message : targetLeaks) {
            Reporter.log(message);
        }
        return targetLeaks.size();
    }
    
    public int reportDescriptionLeaks() {
        for (String message : descriptionLeaks) {
            Reporter.log(message);
        }
        return descriptionLeaks.size();
    }
    
    public int reportMethodLeaks() {
        for (String message : methodLeaks) {
            Reporter.log(message);
        }
        return methodLeaks.size();
    }
    
    public int reportRunnerLeaks() {
        for (String message : runnerLeaks) {
            Reporter.log(message);
        }
        return runnerLeaks.size();
    }
    
    public int reportStartFlagLeaks() {
        for (String message : startFlagLeaks) {
            Reporter.log(message);
        }
        return startFlagLeaks.size();
    }
    
    public int reportNotifierLeaks() {
        for (String message : notifierLeaks) {
            Reporter.log(message);
        }
        return notifierLeaks.size();
    }
    
    public int reportNotifyFlagLeaks() {
        for (String message : notifyFlagLeaks) {
            Reporter.log(message);
        }
        return notifyFlagLeaks.size();
    }
    
    public int reportParentLeaks() {
        for (String message : parentLeaks) {
            Reporter.log(message);
        }
        return parentLeaks.size();
    }
    
    public int reportRunnerStackLeak() {
        if (runnerStackLeak == null) return 0;
        
        Reporter.log(runnerStackLeak);
        return 1;
    }
    
    private static String nameFor(Description description) {
        return "[" + uniqueIdOf(description) + "] " + description.getDisplayName();
    }
    
    private static String uniqueIdOf(Description description) {
        String uniqueId = null;
        if (UNIQUE_ID == null) {
            try {
                UNIQUE_ID = Description.class.getDeclaredField("fUniqueId");
                UNIQUE_ID.setAccessible(true);
            } catch (NoSuchFieldException | SecurityException e) {
                // nothing to do here
            }
        }
        if (UNIQUE_ID != null) {
            try {
                uniqueId = (String) UNIQUE_ID.get(description);
            } catch (IllegalArgumentException | IllegalAccessException e) {
                // nothing to do here
            }
        }
        return (uniqueId != null) ? uniqueId : "{unknown}";
    }
    
    private static String checkStatementOf(Object runner) {
        try {
            Map<String, Object> map = getRunnerToStatement();
            if (map != null) {
                if (null != map.get(LifecycleHooks.toMapKey(runner))) {
                    return "Statement leak detected for: ";
                }
            } else {
                return "Map not found; statement leak not checked for: ";
            }
        } catch (IllegalArgumentException | IllegalAccessException e) {
            return "Map inaccessible; statement leak not checked for: ";
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> getRunnerToStatement() throws IllegalArgumentException, IllegalAccessException {
        if (RUNNER_TO_STATEMENT == null) {
            try {
                RUNNER_TO_STATEMENT = MethodBlock.class.getDeclaredField("RUNNER_TO_STATEMENT");
                RUNNER_TO_STATEMENT.setAccessible(true);
            } catch (NoSuchFieldException | SecurityException e) {
                // nothing to do here
            }
        }
        if (RUNNER_TO_STATEMENT != null) {
            return (Map<String, Object>) RUNNER_TO_STATEMENT.get(null);
        }
        return null;
    }
    
    private static String checkWatcherFor(Integer descriptionHashCode) {
        try {
            Map<Integer, Object> map = getWatcherMap();
            if (map != null) {
                if (null != map.get(descriptionHashCode)) {
                    return "Watcher leak detected for: ";
                }
            } else {
                return "Map not found; watcher leak not checked for: ";
            }
        } catch (IllegalArgumentException | IllegalAccessException e) {
            return "Map inaccessible; watcher leak not checked for: ";
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private static Map<Integer, Object> getWatcherMap() throws IllegalArgumentException, IllegalAccessException {
        if (WATCHER_MAP == null) {
            try {
                WATCHER_MAP = ArtifactCollector.class.getDeclaredField("WATCHER_MAP");
                WATCHER_MAP.setAccessible(true);
            } catch (NoSuchFieldException | SecurityException e) {
                // nothing to do here
            }
        }
        if (WATCHER_MAP != null) {
            return (Map<Integer, Object>) WATCHER_MAP.get(null);
        }
        return null;
    }
    
    private static String checkTargetForHashCode(Integer runnerMethodHash) {
        try {
            Map<Integer, Object> map = getHashcodeToTarget();
            if (map != null) {
                if (null != map.get(runnerMethodHash)) {
                    return "Target leak detected for: ";
                }
            } else {
                return "Map not found; target leak not checked for: ";
            }
        } catch (IllegalArgumentException | IllegalAccessException e) {
            return "Map inaccessible; target leak not checked for: ";
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private static Map<Integer, Object> getHashcodeToTarget() throws IllegalArgumentException, IllegalAccessException {
        if (HASHCODE_TO_TARGET == null) {
            try {
                HASHCODE_TO_TARGET = CreateTest.class.getDeclaredField("HASHCODE_TO_TARGET");
                HASHCODE_TO_TARGET.setAccessible(true);
            } catch (NoSuchFieldException | SecurityException e) {
                // nothing to do here
            }
        }
        if (HASHCODE_TO_TARGET != null) {
            return (Map<Integer, Object>) HASHCODE_TO_TARGET.get(null);
        }
        return null;
    }
    
    private static String checkMethodFor(String targetKey) {
        try {
            Map<String, Object> map = getTargetToMethod();
            if (map != null) {
                if (null != map.get(targetKey)) {
                    return "Method leak detected for: ";
                }
            } else {
                return "Map not found; method leak not checked for: ";
            }
        } catch (IllegalArgumentException | IllegalAccessException e) {
            return "Map inaccessible; method leak not checked for: ";
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> getTargetToMethod() throws IllegalArgumentException, IllegalAccessException {
        if (TARGET_TO_METHOD == null) {
            try {
                TARGET_TO_METHOD = CreateTest.class.getDeclaredField("TARGET_TO_METHOD");
                TARGET_TO_METHOD.setAccessible(true);
            } catch (NoSuchFieldException | SecurityException e) {
                // nothing to do here
            }
        }
        if (TARGET_TO_METHOD != null) {
            return (Map<String, Object>) TARGET_TO_METHOD.get(null);
        }
        return null;
    }
    
    private static String checkRunnerFor(String targetKey) {
        try {
            Map<String, Object> map = getTargetToRunner();
            if (map != null) {
                if (null != map.get(targetKey)) {
                    return "Runner leak detected for: ";
                }
            } else {
                return "Map not found; runner leak not checked for: ";
            }
        } catch (IllegalArgumentException | IllegalAccessException e) {
            return "Map inaccessible; runner leak not checked for: ";
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> getTargetToRunner() throws IllegalArgumentException, IllegalAccessException {
        if (TARGET_TO_RUNNER == null) {
            try {
                TARGET_TO_RUNNER = CreateTest.class.getDeclaredField("TARGET_TO_RUNNER");
                TARGET_TO_RUNNER.setAccessible(true);
            } catch (NoSuchFieldException | SecurityException e) {
                // nothing to do here
            }
        }
        if (TARGET_TO_RUNNER != null) {
            return (Map<String, Object>) TARGET_TO_RUNNER.get(null);
        }
        return null;
    }
    
    private static String checkAtomicTestFor(Integer descriptionHashCode) {
        try {
            Map<Integer, Object> map = getDescriptionToAtomicTest();
            if (map != null) {
                if (null != map.get(descriptionHashCode)) {
                    return "Atomic test leak detected for: ";
                }
            } else {
                return "Map not found; atomic test leak not checked for: ";
            }
        } catch (IllegalArgumentException | IllegalAccessException e) {
            return "Map inaccessible; atomic test leak not checked for: ";
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private static Map<Integer, Object> getDescriptionToAtomicTest() throws IllegalArgumentException, IllegalAccessException {
        if (DESCRIPTION_TO_ATOMICTEST == null) {
            try {
                DESCRIPTION_TO_ATOMICTEST = EachTestNotifierInit.class.getDeclaredField("DESCRIPTION_TO_ATOMICTEST");
                DESCRIPTION_TO_ATOMICTEST.setAccessible(true);
            } catch (NoSuchFieldException | SecurityException e) {
                // nothing to do here
            }
        }
        if (DESCRIPTION_TO_ATOMICTEST != null) {
            return (Map<Integer, Object>) DESCRIPTION_TO_ATOMICTEST.get(null);
        }
        return null;
    }
    
    private static String checkDescriptionFor(String targetKey) {
        try {
            Map<String, Object> map = getTargetToDescription();
            if (map != null) {
                if (null != map.get(targetKey)) {
                    return "Description leak detected for: ";
                }
            } else {
                return "Map not found; description leak not checked for: ";
            }
        } catch (IllegalArgumentException | IllegalAccessException e) {
            return "Map inaccessible; description leak not checked for: ";
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> getTargetToDescription() throws IllegalArgumentException, IllegalAccessException {
        if (TARGET_TO_DESCRIPTION == null) {
            try {
                TARGET_TO_DESCRIPTION = EachTestNotifierInit.class.getDeclaredField("TARGET_TO_DESCRIPTION");
                TARGET_TO_DESCRIPTION.setAccessible(true);
            } catch (NoSuchFieldException | SecurityException e) {
                // nothing to do here
            }
        }
        if (TARGET_TO_DESCRIPTION != null) {
            return (Map<String, Object>) TARGET_TO_DESCRIPTION.get(null);
        }
        return null;
    }
    
    private static String checkTargetFor(Integer descriptionHashCode) {
        try {
            Map<Integer, Object> map = getDescriptionToTarget();
            if (map != null) {
                if (null != map.get(descriptionHashCode)) {
                    return "Target leak detected for: ";
                }
            } else {
                return "Map not found; target leak not checked for: ";
            }
        } catch (IllegalArgumentException | IllegalAccessException e) {
            return "Map inaccessible; target leak not checked for: ";
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private static Map<Integer, Object> getDescriptionToTarget() throws IllegalArgumentException, IllegalAccessException {
        if (DESCRIPTION_TO_TARGET == null) {
            try {
                DESCRIPTION_TO_TARGET = EachTestNotifierInit.class.getDeclaredField("DESCRIPTION_TO_TARGET");
                DESCRIPTION_TO_TARGET.setAccessible(true);
            } catch (NoSuchFieldException | SecurityException e) {
                // nothing to do here
            }
        }
        if (DESCRIPTION_TO_TARGET != null) {
            return (Map<Integer, Object>) DESCRIPTION_TO_TARGET.get(null);
        }
        return null;
    }
    
    private static String checkDescriptionForHashCode(Integer hashCode) {
            try {
                Map<Integer, Object> map = getHashCodeToDescription();
                if (map != null) {
                    if (null != map.get(hashCode)) {
                        return "Description leak detected for: ";
                    }
                } else {
                    return "Map not found; description leak not checked for: ";
                }
            } catch (IllegalArgumentException | IllegalAccessException e) {
                return "Map inaccessible; description leak not checked for: ";
            }
        return null;
    }

    @SuppressWarnings("unchecked")
    private static Map<Integer, Object> getHashCodeToDescription() throws IllegalArgumentException, IllegalAccessException {
        if (HASHCODE_TO_DESCRIPTION == null) {
            try {
                HASHCODE_TO_DESCRIPTION = EachTestNotifierInit.class.getDeclaredField("HASHCODE_TO_DESCRIPTION");
                HASHCODE_TO_DESCRIPTION.setAccessible(true);
            } catch (NoSuchFieldException | SecurityException e) {
                // nothing to do here
            }
        }
        if (HASHCODE_TO_DESCRIPTION != null) {
            return (Map<Integer, Object>) HASHCODE_TO_DESCRIPTION.get(null);
        }
        return null;
    }
    
    private static String checkCallableFor(Integer descriptionHashCode) {
        try {
            Map<Integer, Object> map = getDescriptionToCallable();
            if (map != null) {
                if (null != map.get(descriptionHashCode)) {
                    return "Callable leak detected for: ";
                }
            } else {
                return "Map not found; callable leak not checked for: ";
            }
        } catch (IllegalArgumentException | IllegalAccessException e) {
            return "Map inaccessible; callable leak not checked for: ";
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private static Map<Integer, Object> getDescriptionToCallable() throws IllegalArgumentException, IllegalAccessException {
        if (DESCRIPTION_TO_CALLABLE == null) {
            try {
                DESCRIPTION_TO_CALLABLE = RunReflectiveCall.class.getDeclaredField("DESCRIPTION_TO_CALLABLE");
                DESCRIPTION_TO_CALLABLE.setAccessible(true);
            } catch (NoSuchFieldException | SecurityException e) {
                // nothing to do here
            }
        }
        if (DESCRIPTION_TO_CALLABLE != null) {
            return (Map<Integer, Object>) DESCRIPTION_TO_CALLABLE.get(null);
        }
        return null;
    }
    
    private static String checkStartFlag(String runnerKey) {
        try {
            Set<String> set = getStartNotified();
            if (set != null) {
                if (set.contains(runnerKey)) {
                    return "Start flag leak detected for: ";
                }
            } else {
                return "Set not found; start flag leak not checked for: ";
            }
        } catch (IllegalArgumentException | IllegalAccessException e) {
            return "Set inaccessible; start flag leak not checked for: ";
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private static Set<String> getStartNotified() throws IllegalArgumentException, IllegalAccessException {
        if (START_NOTIFIED == null) {
            try {
                START_NOTIFIED = Run.class.getDeclaredField("START_NOTIFIED");
                START_NOTIFIED.setAccessible(true);
            } catch (NoSuchFieldException | SecurityException e) {
                // nothing to do here
            }
        }
        if (START_NOTIFIED != null) {
            return (Set<String>) START_NOTIFIED.get(null);
        }
        return null;
    }
    
    private static String checkParentOf(Object childKey) {
        try {
            Map<String, Object> map = getChildToParent();
            if (map != null) {
                if (null != map.get(childKey)) {
                    return "Parent leak detected for: ";
                }
            } else {
                return "Map not found; parent leak not checked for: ";
            }
        } catch (IllegalArgumentException | IllegalAccessException e) {
            return "Map inaccessible; parent leak not checked for: ";
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> getChildToParent() throws IllegalArgumentException, IllegalAccessException {
        if (CHILD_TO_PARENT == null) {
            try {
                CHILD_TO_PARENT = Run.class.getDeclaredField("CHILD_TO_PARENT");
                CHILD_TO_PARENT.setAccessible(true);
            } catch (NoSuchFieldException | SecurityException e) {
                // nothing to do here
            }
        }
        if (CHILD_TO_PARENT != null) {
            return (Map<String, Object>) CHILD_TO_PARENT.get(null);
        }
        return null;
    }
    
    private static String checkNotifierFor(String runnerKey) {
            try {
                Map<String, Object> map = getRunnerToNotifier();
                if (map != null) {
                    if (null != map.get(runnerKey)) {
                        return "Notifier leak detected for: ";
                    }
                } else {
                    return "Map not found; notifier leak not checked for: ";
                }
            } catch (IllegalArgumentException | IllegalAccessException e) {
                return "Map inaccessible; notifier leak not checked for: ";
            }
        return null;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> getRunnerToNotifier() throws IllegalArgumentException, IllegalAccessException {
        if (RUNNER_TO_NOTIFIER == null) {
            try {
                RUNNER_TO_NOTIFIER = Run.class.getDeclaredField("RUNNER_TO_NOTIFIER");
                RUNNER_TO_NOTIFIER.setAccessible(true);
            } catch (NoSuchFieldException | SecurityException e) {
                // nothing to do here
            }
        }
        if (RUNNER_TO_NOTIFIER != null) {
            return (Map<String, Object>) RUNNER_TO_NOTIFIER.get(null);
        }
        return null;
    }
    
    private static String checkNotifyFlag(String runnerKey) {
            try {
                Map<String, Object> map = getDidNotify();
                if (map != null) {
                    if (null != map.get(runnerKey)) {
                        return "Notify flag leak detected for: ";
                    }
                } else {
                    return "Map not found; notify flag not checked for: ";
                }
            } catch (IllegalArgumentException | IllegalAccessException e) {
                return "Map inaccessible; notify flag leak not checked for: ";
            }
        return null;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> getDidNotify() throws IllegalArgumentException, IllegalAccessException {
        if (DID_NOTIFY == null) {
            try {
                DID_NOTIFY = RunChild.class.getDeclaredField("DID_NOTIFY");
                DID_NOTIFY.setAccessible(true);
            } catch (NoSuchFieldException | SecurityException e) {
                // nothing to do here
            }
        }
        if (DID_NOTIFY != null) {
            return (Map<String, Object>) DID_NOTIFY.get(null);
        }
        return null;
    }
    
    private static String checkRunnerStack() {
        Object runner = LifecycleHooks.getThreadRunner();
        if (runner != null) {
            return "Runner stack isn't empty";
        }
        return null;
    }

    static void ensureCompleteRelease() {
        
    }
}
