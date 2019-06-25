package com.nordstrom.automation.junit;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.internal.runners.model.ReflectiveCallable;
import org.junit.runners.model.FrameworkMethod;

public class UnitTestWatcher implements MethodWatcher {

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
    
    @Override
    public void beforeInvocation(Object runner, Object child, ReflectiveCallable callable) {
        FrameworkMethod method = (FrameworkMethod) child;
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
    public void afterInvocation(Object runner, Object child, ReflectiveCallable callable, Throwable thrown) {
        FrameworkMethod method = (FrameworkMethod) child;
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
}