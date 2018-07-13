package com.nordstrom.automation.junit;

import org.junit.runner.notification.RunListener;

/**
 * This JUnit 4 {@link RunListener} enables Maven projects to activate the core features of <b>JUnit Foundation</b>.
 * <p>
 * To activate core features with this listener, add this to your project configuration:
 * 
 * <pre><code> &lt;dependencies&gt;
 * [...]
 *   &lt;dependency&gt;
 *     &lt;groupId&gt;com.nordstrom.tools&lt;/groupId&gt;
 *     &lt;artifactId&gt;junit-foundation&lt;/artifactId&gt;
 *     &lt;version&gt;3.2.2&lt;/version&gt;
 *     &lt;scope&gt;test&lt;/scope&gt;
 *   &lt;/dependency&gt;
 * [...]
 * &lt;/dependencies&gt;
 * [...]
 * &lt;plugins&gt;
 * [...]
 *   &lt;plugin&gt;
 *     &lt;groupId&gt;org.apache.maven.plugins&lt;/groupId&gt;
 *     &lt;artifactId&gt;maven-surefire-plugin&lt;/artifactId&gt;
 *     &lt;version&gt;2.22.0&lt;/version&gt;
 *     &lt;configuration&gt;
 *       &lt;properties&gt;
 *         &lt;property&gt;
 *           &lt;name&gt;listener&lt;/name&gt;
 *           &lt;value&gt;com.nordstrom.automation.junit.HookInstallingListener&lt;/value&gt;
 *         &lt;/property&gt;
 *       &lt;/properties&gt;
 *     &lt;/configuration&gt;
 *   &lt;/plugin&gt;
 * [...]
 * &lt;/plugins&gt;</code></pre>
 * 
 * Core features are activated by loading the {@link com.nordstrom.automation.junit.LifecycleHooks LifecycleHooks}
 * class. A static initializer in this listener performs this action, ensuring activation in every JVM launched by
 * Surefire/Failsafe for test execution.
 */
public class HookInstallingListener extends RunListener {
    
    /**
     * This static initializer loads the LifecycleHooks class, which activated the core features of JUnit Foundation.
     * Note that the class is referred to indirectly by its name. This prevents unintended  
     */
    static {
        try {
            Class.forName("com.nordstrom.automation.junit.LifecycleHooks");
        } catch (ClassNotFoundException | ExceptionInInitializerError e) {
            e.printStackTrace();
        }
    }
}
