package com.nordstrom.automation.junit;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.internal.AssumptionViolatedException;
import org.junit.internal.runners.model.ReflectiveCallable;
import org.junit.runner.Description;
import org.junit.runners.model.FrameworkMethod;

import com.google.common.base.Function;

public class UnitTestWatcher implements MethodWatcher<FrameworkMethod>, RunWatcher {

    private static final ConcurrentHashMap<Integer, List<Notification>> NOTIFICATION_MAP;
    private static final Function<Integer, List<Notification>> NEW_INSTANCE;
    
    static {
        NOTIFICATION_MAP = new ConcurrentHashMap<>();
        NEW_INSTANCE = new Function<Integer, List<Notification>>() {
            @Override
            public List<Notification> apply(Integer input) {
                return new ArrayList<>();
            }
        };
    }
    
    private List<String> m_enterBeforeClass = Collections.synchronizedList(new ArrayList<String>());
    private List<String> m_enterBeforeMethod = Collections.synchronizedList(new ArrayList<String>());
    private List<String> m_enterTest = Collections.synchronizedList(new ArrayList<String>());
    private List<String> m_enterAfterMethod = Collections.synchronizedList(new ArrayList<String>());
    private List<String> m_enterAfterClass = Collections.synchronizedList(new ArrayList<String>());
    
    private List<String> m_leaveBeforeClass = Collections.synchronizedList(new ArrayList<String>());
    private List<String> m_leaveBeforeMethod = Collections.synchronizedList(new ArrayList<String>());
    private List<String> m_leaveTest = Collections.synchronizedList(new ArrayList<String>());
    private List<String> m_leaveAfterMethod = Collections.synchronizedList(new ArrayList<String>());
    private List<String> m_leaveAfterClass = Collections.synchronizedList(new ArrayList<String>());
    
    public enum Notification {
        STARTED, FINISHED, FAILED, ASSUMP, IGNORED, RETRIED
    }
    
    @Override
    public void beforeInvocation(Object runner, FrameworkMethod method, ReflectiveCallable callable) {
        if (null != method.getAnnotation(BeforeClass.class)) {
            m_enterBeforeClass.add(method.getName());
        } else if (null != method.getAnnotation(Before.class)) {
            m_enterBeforeMethod.add(method.getName());
        } else if (null != method.getAnnotation(Test.class)) {
            m_enterTest.add(method.getName());
        } else if (null != method.getAnnotation(After.class)) {
            m_enterAfterMethod.add(method.getName());
        } else if (null != method.getAnnotation(AfterClass.class)) {
            m_enterAfterClass.add(method.getName());
        }
    }

    @Override
    public void afterInvocation(Object runner, FrameworkMethod method, ReflectiveCallable callable, Throwable thrown) {
        if (null != method.getAnnotation(BeforeClass.class)) {
            m_leaveBeforeClass.add(method.getName());
        } else if (null != method.getAnnotation(Before.class)) {
            m_leaveBeforeMethod.add(method.getName());
        } else if (null != method.getAnnotation(Test.class)) {
            m_leaveTest.add(method.getName());
        } else if (null != method.getAnnotation(After.class)) {
            m_leaveAfterMethod.add(method.getName());
        } else if (null != method.getAnnotation(AfterClass.class)) {
            m_leaveAfterClass.add(method.getName());
        }
    }

    @Override
    public Class<FrameworkMethod> supportedType() {
        return FrameworkMethod.class;
    }
    
    public List<String> getEnterBeforeClass() {
        return m_enterBeforeClass;
    }
    
    public List<String> getEnterBeforeMethod() {
        return m_enterBeforeMethod;
    }
    
    public List<String> getEnterTest() {
        return m_enterTest;
    }
    
    public List<String> getEnterAfterMethod() {
        return m_enterAfterMethod;
    }
    
    public List<String> getEnterAfterClass() {
        return m_enterAfterClass;
    }
    
    public List<String> getLeaveBeforeClass() {
        return m_leaveBeforeClass;
    }
    
    public List<String> getLeaveBeforeMethod() {
        return m_leaveBeforeMethod;
    }
    
    public List<String> getLeaveTest() {
        return m_leaveTest;
    }
    
    public List<String> getLeaveAfterMethod() {
        return m_leaveAfterMethod;
    }
    
    public List<String> getLeaveAfterClass() {
        return m_leaveAfterClass;
    }

    @Override
    public void testStarted(AtomicTest atomicTest) {
        addNotification(atomicTest, Notification.STARTED);
    }

    @Override
    public void testFinished(AtomicTest atomicTest) {
        addNotification(atomicTest, Notification.FINISHED);
    }

    @Override
    public void testFailure(AtomicTest atomicTest, Throwable thrown) {
        addNotification(atomicTest, Notification.FAILED);
    }

    @Override
    public void testAssumptionFailure(AtomicTest atomicTest, AssumptionViolatedException thrown) {
        addNotification(atomicTest, Notification.ASSUMP);
    }

    @Override
    public void testIgnored(AtomicTest atomicTest) {
        if (RetriedTest.isRetriedTest(atomicTest.getDescription())) {
            addNotification(atomicTest, Notification.RETRIED);
        } else {
            addNotification(atomicTest, Notification.IGNORED);
        }
    }
    
    public List<Notification> getNotificationsFor(Description description) {
        return NOTIFICATION_MAP.get(description.hashCode());
    }
    
    private static void addNotification(AtomicTest atomicTest, Notification notification) {
        List<Notification> list = LifecycleHooks.computeIfAbsent(NOTIFICATION_MAP, atomicTest.getDescription().hashCode(), NEW_INSTANCE);
        list.add(notification);
    }
}