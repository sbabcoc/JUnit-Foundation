[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.nordstrom.tools/junit-foundation/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.nordstrom.tools/junit-foundation)

# INTRODUCTION

**JUnit Foundation** is a lightweight collection of JUnit watchers, interfaces, and static utility classes that supplement and augment the functionality provided by the JUnit API. The facilities provided by **JUnit Foundation** include method invocation hooks and test artifact capture.

## Method Invocation Hooks

The standard **TestWatcher** feature of JUnit provides a basic facility for implementing setup and cleanup procedures. However, the granularity of control offered by this feature is relatively coarse, firing before the first **`@Before`** method and after the last **`@After`** method. With **JUnit Foundation**, you can easily intercept the invocation of every configuration and test method in your test class. This method interception feature is analogous to the **IInvokedMethodListener** feature of TestNG.

Method invocation hooks are installed dynamically with bytecode enhancement performed by [Byte Buddy](http://bytebuddy.net). Basic support is provided by the **HookInstallingRunner**, which enables you to perform pre-processing and post-processing on every test method, 'before' configuration method, and 'after' configuration method. Extended support is provided by the **HookInstallingPlugin**, which extends pre-processing and post-processing support to every 'before class' method and 'after class' method as well.

### Basic Interception Support

Basic support for the method interception feature of **JUnit Foundation** is provided by the **HookInstallingRunner**. This test runner installs hooks on every method annotated with **`@Test`**, **`@Before`**, or **`@After`**. For classes that require method-level setup and cleanup processing, add the **`@MethodWatchers`** annotation. The value of this annotation is an array of classes that implement the **MethodWatcher** interface:

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

Note that the implementations in this method watcher uses the annotations attached to the method objects to determine the type of method they're intercepting. Because each test method can have multiple configuration methods (both before and after), you may need to define additional conditions to control when your implementation runs. Examples of additional conditions include method name, method annotation, or an execution flag.

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

As shown above, we use the **`@MethodWatchers`** annotation to attach **LoggingWatcher**. Running with the **HookInstallingRunner** connects the method watchers declared in the **`@MethodWatchers`** annotation to the chain (in this case, **LoggingWatcher**). This activates the method watchers' `beforeInvocation(Object, Method, Object[])` and `afterInvocation(Object, Method, Object[])` methods, enabling them to perform their respective method-level pre-processing and post-processing tasks.

### Extended Interception Support

Extended support for the method interception feature of **JUnit Foundation** is provided by the **HookInstallingPlugin**. This plugin provides the implementation used by the **Byte Buddy Maven Plugin** to install hooks on every method annotated with **`@Test`**, **`@Before`**, **`@After`**, **`@BeforeClass`**, or **`@AfterClass`**. To activate this support, add the following sections to your project POM file:

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
        // perform method-level processing
        ...
    }

    @Override
    public void afterInvocation(Object obj, Method method, Object[] args) {
        // perform method-level processing
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

This method watcher implements the **MethodWatcher2** interface, which extends the **MethodWatcher** interface to add methods for intercepted class-level configuration methods. Note that the implementations in this method watcher uses the annotations attached to the method objects to determine the type of method they're intercepting. Because each class can have multiple configuration methods (both before class and after class), you may need to define additional conditions to control when your implementation runs. Examples of additional conditions include method name, method annotation, or an execution flag.

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

As shown above, we use the **`@MethodWatchers`** annotation to attach **LoggingWatcher2**. Running with the **HookInstallingRunner** connects the method watchers declared in the **`@MethodWatchers`** annotation to the chain (in this case, **LoggingWatchers2**). This activates the method watchers' `beforeInvocation(Method, Object[])` and `afterInvocation(Method, Object[])` methods, enabling them to perform their respective class-level pre-processing and post-processing tasks. Note that the method-level interfaces defined in the **MethodWatcher** interface are also connected in watchers that implement **MethodWatcher2**.

For a complete reference implementation of the **MethodWatcher2** interface, check out **UnitTestWatcher** in the unit tests collection of this project.

## Artifact Capture

* [ArtifactCollector](https://github.com/Nordstrom/JUnit-Foundation/blob/master/src/main/java/com/nordstrom/automation/junit/ArtifactCollector.java):  
**ArtifactCollector** is a JUnit [test watcher](http://junit.org/junit4/javadoc/latest/org/junit/rules/TestWatcher.html) that serves as the foundation for artifact-capturing test watchers. This is a generic class, with the artifact-specific implementation provided by instances of the **ArtifactType** interface. For artifact capture scenarios where you need access to the current method description or the values provided to parameterized tests, the test class can implement the **ArtifactParams** interface.

* [ArtifactParams](https://github.com/Nordstrom/JUnit-Foundation/blob/master/src/main/java/com/nordstrom/automation/junit/ArtifactParams.java):  
By implementing the **ArtifactParams** interface in your test classes, you enable the artifact capture framework to access test method description objects and parameterized test values. These can be used for composing, naming, and storing artifacts. 
* [ArtifactType](https://github.com/Nordstrom/JUnit-Foundation/blob/master/src/main/java/com/nordstrom/automation/junit/ArtifactType.java):  
Classes that implement the **ArtifactType** interface provide the artifact-specific methods used by the **ArtifactCollector** watcher to capture and store test-related artifacts. The unit tests for this project include a reference implementation (**UnitTestArtifact**) that provides a basic outline for a scenario-specific artifact provider. This artifact provider is specified as the superclass type parameter in the **UnitTestCapture** watcher, which is a lightweight extension of **ArtifactCollector**. The most basic example is shown below:

###### Implementing ArtifactType
```java
package com.nordstrom.example;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.nordstrom.automation.junit.ArtifactType;

public class MyArtifactType implements ArtifactType {
    
    private static final Path ARTIFACT_PATH = Paths.get("artifacts");
    private static final String EXTENSION = "txt";
    private static final String ARTIFACT = "This text artifact was captured for '%s'";
    private static final Logger LOGGER = LoggerFactory.getLogger(MyArtifactType.class);

    @Override
    public boolean canGetArtifact(Object instance) {
        return true;
    }

    @Override
    public byte[] getArtifact(Object instance, Throwable reason) {
        if (instance instanceof ArtifactParams) {
            ArtifactParams params = (ArtifactParams) instance;
            return String.format(ARTIFACT, params.getDescription().getMethodName()).getBytes().clone();
        } else {
            return new byte[0];
        }
    }

    @Override
    public Path getArtifactPath() {
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

Pay special attention to the implementation of `getArtifact(Object, Throwable)` above. This code will only capture artifacts for test classes that implement the **ArtifactParams** interface, and it uses this interface to get the description object for the current test.

###### Creating a type-specific artifact collector
```java
package com.nordstrom.example;

import com.nordstrom.automation.junit.ArtifactCollector;

public class MyArtifactCapture extends ArtifactCollector<MyArtifactType> {
    
    public MyArtifactCapture(Object instance) {
        super(instance, new MyArtifactType());
    }
    
}
```

The preceding code is an example of how the artifact type definition can be assigned as the type parameter in a subclass of **ArtifactCollector**. This isn't strictly necessary, but will make your code more concise, as the next example demonstrates. This technique also provides the opportunity to extend or alter the basic artifact capture behavior.

###### Attaching artifact collectors to test classes

```java
package com.nordstrom.example;

import org.junit.Rule;
import org.junit.runner.Description;

public class ExampleTest implements ArtifactParams {

    @Rule   // Option #1: Attach a pre-composed artifact capture subclass
    public final MyArtifactCapture watcher1 = new MyArtifactCapture(this);
    
    @Rule   // Option #2: Compose type-specific artifact collector in-line
    public final ArtifactCollector<MyArtifactType> watcher2 = new ArtifactCollector<>(this, new MyArtifactType());
    
    ...
    
    @Override
    public Description getDescription() {
        return watcher.getDescription();
    }
}
```

This example demonstrates two techniques for attaching artifact collectors to test classes. Either technique will activate basic artifact capture functionality. Of course, the first option is required to activate extended behavior implemented in a type-specific subclass of **ArtifactCapture**.
