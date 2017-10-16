# INTRODUCTION

**JUnit Foundation** is a lightweight collection of JUnit watchers, interfaces, and static utility classes that supplement and augment the functionality provided by the JUnit API. The facilities provided by **JUnit Foundation** include method invocation hooks and test artifact capture.

## Method Invocation Hooks

The standard **TestWatcher** feature of JUnit provides a basic facility for implementing setup and cleanup procedures. However, the granularity of control offered by this feature is relatively coarse, firing before the first **`@Before`** methods and after the last **`@After`** method. With **JUnit Foundation**, you can easily intercept the invocation of every configuration and test method in your test class. This method interception feature is analogous to the **IInvokedMethodListener** feature of TestNG.

Method invocation hooks are installed dynamically with bytecode enhancement performed by [Byte Buddy](http://bytebuddy.net). Basic support is provided by the **HookInstallingRunner**, which enables you to perform pre-processing and post-processing on every test method, 'before' configuration method, and 'after' configuration method. Extended support is provided by the **HookInstallingPlugin**, which extends pre-processing and post-processing support to every 'before class' method and 'after class' method as well.

### Basic Interception Support

Basic support for this **JUnit Foundation** feature is provided by the **HookInstallingRunner**. This test runner installs hooks on every method annotated with **`@Test`**, **`@Before`**, or **`@After`**. For classes that require method-level setup and cleanup processing, add the **`@MethodWatchers`** annotation. The value of this annotation is an array of classes that implement the **MethodWatcher** interface:

###### Implementing MethodWatcher
```java
package com.nordstrom.example;

import java.lang.reflect.Method;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.nordstrom.automation.junit.MethodWatcher;

public class LoggingWatcher implements MethodWatcher {

    private static final Logger LOGGER = LoggerFactory.getLogger(LoggingWatcher.class);
    
    @Override
    public void beforeInvocation(Object obj, Method method, Object[] args) {
        if (null != method.getAnnotation(Before.class)) {
            LOGGER.info(">>>>> ENTER 'before' method {}", method.getName());
        } else if (null != method.getAnnotation(Test.class)) {
            LOGGER.info(">>>>> ENTER 'test' method {}", method.getName());
        } else if (null != method.getAnnotation(After.class)) {
            LOGGER.info(">>>>> ENTER 'after' method {}", method.getName());
        }
    }

    @Override
    public void afterInvocation(Object obj, Method method, Object[] args) {
        if (null != method.getAnnotation(Before.class)) {
            LOGGER.info("<<<<< LEAVE 'before' method {}", method.getName());
        } else if (null != method.getAnnotation(Test.class)) {
            LOGGER.info("<<<<< LEAVE 'test' method {}", method.getName());
        } else if (null != method.getAnnotation(After.class)) {
            LOGGER.info("<<<<< LEAVE 'after' method {}", method.getName());
        }
    }
}

```

###### MethodWatchers annotation
```java
package com.nordstrom.example;

import org.junit.runner.RunWith;

import com.nordstrom.automation.junit.HookInstallingRunner;
import com.nordstrom.automation.junit.MethodWatchers;

@RunWith(HookInstallingRunner.class)
@MethodWatchers({LoggingWatcher.class})
public class ExampleTest {
    
    ...
    
}
```

As shown above, we use the **`@MethodWatchers`** annotation to attach <span style="color:blue">LoggingWatcher</span>.

### Extended Interception Support

Extended support for the **JUnit Foundation** method interception feature is provided by the **HookInstallingPlugin**. This plugin provides the implementation used by the **Byte Buddy Maven Plugin** to install hooks on every method annotated with **`@Test`**, **`@Before`**, **`@After`**, **`@BeforeClass`**, or **`@AfterClass`**. To activate this support, add the following sections to your project POM file:

###### pom.xml
```xml
  <build>
    <pluginManagement>
      <plugins>
        ...
        <plugin>
          <groupId>org.eclipse.m2e</groupId>
          <artifactId>lifecycle-mapping</artifactId>
          <version>1.0.0</version>
          <configuration>
            <lifecycleMappingMetadata>
              <pluginExecutions>
                <pluginExecution>
                  <pluginExecutionFilter>
                    <groupId>net.bytebuddy</groupId>
                    <artifactId>byte-buddy-maven-plugin</artifactId>
                    <versionRange>[1.0.0,)</versionRange>
                    <goals>
                      <goal>transform</goal>
                      <goal>transform-test</goal>
                    </goals>
                  </pluginExecutionFilter>
                  <action>
                    <execute />
                  </action>
                </pluginExecution>
              </pluginExecutions>
            </lifecycleMappingMetadata>
          </configuration>
        </plugin>
        ...
      </plugins>
    </pluginManagement>
    
    <plugins>
      ...
      <plugin>
        <groupId>net.bytebuddy</groupId>
        <artifactId>byte-buddy-maven-plugin</artifactId>
        <version>1.7.5</version>
        <executions>
          <execution>
            <goals>
              <goal>transform-test</goal>
            </goals>
          </execution>
        </executions>
        <configuration>
          <transformations>
            <transformation>
              <plugin>com.nordstrom.automation.junit.HookInstallingPlugin</plugin>
            </transformation>
          </transformations>
        </configuration>
      </plugin>
      ...
    </plugins>
  </build>

```

The `byte-buddy-maven-plugin` element informs Maven to execute the `transform-test` goal using the transformation specified by **HookInstallingPlugin**. The `lifecycle-mapping` element informs **M2Eclipse** that it should execute the `transform-test` goal as well. This avoids the dreaded `Plugin execution not covered by lifecycle configuration` error. With these POM changes in place, method invocation hooks will be installed during the `process-test-classes` phase of the build:

###### Implementing MethodWatcher
```java
package com.nordstrom.example;

import java.lang.reflect.Method;

import org.junit.AfterClass;
import org.junit.BeforeClass;

import com.nordstrom.automation.junit.MethodWatcher2;

public class LoggingWatcher2 implements MethodWatcher2 {

    private static final Logger LOGGER = LoggerFactory.getLogger(LoggingWatcher.class);
    
    @Override
    public void beforeInvocation(Object obj, Method method, Object[] args) {
        ...
    }

    @Override
    public void afterInvocation(Object obj, Method method, Object[] args) {
        ...
    }
    
    @Override
    public void beforeInvocation(Method method, Object[] args) {
        if (null != method.getAnnotation(BeforeClass.class)) {
            LOGGER.info(">>>>> ENTER 'before class' method {}", method.getName());
        } else if (null != method.getAnnotation(AfterClass.class)) {
            LOGGER.info(">>>>> ENTER 'after class' method {}", method.getName());
        }
    }
    
    @Override
    public void afterInvocation(Method method, Object[] args) {
        if (null != method.getAnnotation(BeforeClass.class)) {
            LOGGER.info("<<<<< LEAVE 'before class' method {}", method.getName());
        } else if (null != method.getAnnotation(AfterClass.class)) {
            LOGGER.info("<<<<< LEAVE 'after class' method {}", method.getName());
        }
    }
}

```

###### MethodWatchers annotation
```java
package com.nordstrom.example;

import org.junit.runner.RunWith;

import com.nordstrom.automation.junit.HookInstallingRunner;
import com.nordstrom.automation.junit.MethodWatchers;

@RunWith(HookInstallingRunner.class)
@MethodWatchers({LoggingWatcher2.class})
public class ExampleTest {
    
    ...
    
}
```

As shown above, we use the **`@MethodWatchers`** annotation to attach <span style="color:blue">LoggingWatcher2</span>.

## Artifact Capture

**ArtifactCollector** is a JUnit [test watcher](http://junit.org/junit4/javadoc/latest/org/junit/rules/TestWatcher.html) that serves as the foundation for artifact-capturing test watchers. This is a generic class, with the artifact-specific implementation provided by instances of the **ArtifactType** interface. See the **Interfaces** section below for more details.

## Interfaces

* [ArtifactParams](https://git.nordstrom.net/projects/MFATT/repos/junit-foundation/browse/src/main/java/com/nordstrom/automation/junit/ArtifactParams.java):  

* [ArtifactType](https://git.nordstrom.net/projects/MFATT/repos/junit-foundation/browse/src/main/java/com/nordstrom/automation/junit/ArtifactType.java):  
Classes that implement the **ArtifactType** interface provide the artifact-specific methods used by the **ArtifactCollector** watcher to capture and store test-related artifacts. The unit tests for this project include a reference implementation (**UnitTestArtifact**) provides a basic outline for a scenario-specific artifact provider. This artifact provider is specified as the superclass type parameter in the **UnitTestCapture** watcher, which is a lightweight extension of **ArtifactCollector**. The most basic example is shown below:

###### Implementing ArtifactType
```java
package com.nordstrom.example;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.junit.ITestResult;

import com.nordstrom.automation.junit.ArtifactType;

public class MyArtifactType implements ArtifactType {
    
    private static final Path ARTIFACT_PATH = Paths.get("artifacts");
    private static final String EXTENSION = "txt";
    private static final String ARTIFACT = "This text artifact was captured for '%s'";
    private static final Logger LOGGER = LoggerFactory.getLogger(MyArtifactType.class);

    @Override
    public boolean canGetArtifact(ITestResult result) {
        return true;
    }

    @Override
    public byte[] getArtifact(ITestResult result) {
            return String.format(ARTIFACT, result.getName()).getBytes().clone();
    }

    @Override
    public Path getArtifactPath(ITestResult result) {
        return ARTIFACT_PATH;
    }
    
    @Override
    public String getArtifactExtension() {
        return EXTENSION;
    }

    @Override
    public Logger getLogger() {
        return LOGGER;
    }
}
```

###### Creating a scenario-specific artifact capture watcher
```java
package com.nordstrom.example;

import com.nordstrom.automation.junit.ArtifactCollector;

public class MyArtifactCapture extends ArtifactCollector<MyArtifactType> {
    
    public UnitTestCapture() {
        super(new MyArtifactType());
    }
    
}
```

## Annotations

* [LinkedListeners](https://git.nordstrom.net/projects/MFATT/repos/junit-foundation/browse/src/main/java/com/nordstrom/automation/junit/LinkedListeners.java):  
To attach watchers to an active **ListenerChain**, mark your test class with the **LinkedListeners** annotation.

## Static Utility Classes

