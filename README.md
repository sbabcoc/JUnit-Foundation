[![Maven Central](https://img.shields.io/maven-central/v/com.nordstrom.tools/junit-foundation.svg)](https://mvnrepository.com/artifact/com.nordstrom.tools/junit-foundation)

# INTRODUCTION

**JUnit Foundation** is a lightweight collection of JUnit watchers, interfaces, and static utility classes that supplement and augment the functionality provided by the JUnit API. The facilities provided by **JUnit Foundation** include method invocation hooks, test method timeout management, automatic retry of failed tests, shutdown hook installation, and test artifact capture.

## Test Lifecycle Notifications

The standard **RunListener** feature of JUnit provides a basic facility for implementing setup, cleanup, and monitoring procedures. However, the granularity of notifications offered by this feature is relatively coarse, firing before the first **`@Before`** method and after the last **`@After`** method - a unit of functionality known as an `atomic test`. Notifications are available for the start, finish, and failure of atomic tests, but not for the `particle methods` of which they're composed - individual **`@Test`** and configuration methods (**`@Before`**, **`@After`**, **`@BeforeClass`**, and **`@AfterClass`**).

With **JUnit Foundation**, you can get notifications for the invocation of every configuration and test method. This method interception feature is analogous to the **IInvokedMethodListener** feature of TestNG. You can also get notifications for the creation of test class instances, the creation and invocation of JUnit runners (both test classes and suites), and the completion of test runs. **JUnit Foundation** also provides notifications for the start, finish, and failure of `atomic tests`, with all of the details and context that are omitted by the standard JUnit **RunListener**.

### Notification Context and Test Run Hierarchy

The notifications provided by **JUnit Foundation** include the context that owns them - the JUnit runner. With this context and associated mapping methods, you're able to explore the entire hierarchy of the test run. For example, you can get the class runner that owns an invoked method or the suite runner that owns a class runner:

###### Exploring the Test Run Hierarchy
```java
package com.nordstrom.example;

import com.nordstrom.automation.junit.AtomicTest;
import com.nordstrom.automation.junit.LifecycleHooks;
import com.nordstrom.automation.junit.MethodWatcher;
import com.nordstrom.automation.junit.RunReflectiveCall;
import com.nordstrom.automation.junit.TestClassWatcher;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.TestClass;

public class ExploringWatcher implements TestClassWatcher, MethodWatcher {

    ...

    @Override
    public void testClassStarted(TestClass testClass) {
        // get the 'atomic test' for this runner
        AtomicTest atomicTest = RunReflectiveCall.getAtomicTestFor(testClass);
        // get the 'particle' methods of this 'atomic test'
        List<FrameworkMethod> particles = atomicTest.getParticles();
        // get the parent of this runner
        TestClass parent = LifecycleHooks.getParentOf(testClass);
        ...
    }

    @Override
    public void beforeInvocation(Object target, FrameworkMethod method, Object... params) {
        // get the 'atomic test' for this method
        AtomicTest atomicTest = RunReflectiveCall.getAtomicTestFor(method);
        // get the test class of the runner that owns this method
        TestClass testClass = LifecycleHooks.getTestClassFor(target);
        ...
    }

    ...

}
```

### How to Enable Notifications

The hooks that enable **JUnit Foundation** test lifecycle notifications are installed dynamically with bytecode enhancement performed by [Byte Buddy](http://bytebuddy.net). To maintain compatibility with solution-specific runners like [SpringRunner](https://spring.io/guides/tutorials/bookmarks/#_testing_a_rest_service), **JUnit Foundation** intercepts calls to several core JUnit classes directly via its Java agent implementation:

#### Maven Configuration for JUnit Foundation
```xml
[pom.xml]
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
  xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/maven-v4_0_0.xsd">
  
  [...]
  
  <properties>
    <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
    <junit-foundation.version>6.0.0</junit-foundation.version>
    <compiler-plugin.version>3.6.0</compiler-plugin.version>
    <dependency-plugin.version>3.1.1</dependency-plugin.version>
    <surefire-plugin.version>2.22.0</surefire-plugin.version>
  </properties>
  
  <dependencies>
    <dependency>
      <groupId>com.nordstrom.tools</groupId>
      <artifactId>junit-foundation</artifactId>
      <version>${junit-foundation.version}</version>
      <scope>test</scope>
    </dependency>
  </dependencies>
  
  <build>
    <pluginManagement>
      <plugins>
        <plugin>
          <groupId>org.apache.maven.plugins</groupId>
          <artifactId>maven-compiler-plugin</artifactId>
          <version>${compiler-plugin.version}</version>
          <configuration>
            <source>1.8</source>
            <target>1.8</target>
          </configuration>
        </plugin>
        <plugin>
          <groupId>org.apache.maven.plugins</groupId>
          <artifactId>maven-dependency-plugin</artifactId>
          <version>${dependency-plugin.version}</version>
        </plugin>
        <!-- Add this if you plan to import into Eclipse -->
        <plugin>
          <groupId>org.eclipse.m2e</groupId>
          <artifactId>lifecycle-mapping</artifactId>
          <version>1.0.0</version>
          <configuration>
            <lifecycleMappingMetadata>
              <pluginExecutions>
                <pluginExecution>
                  <pluginExecutionFilter>
                    <groupId>org.apache.maven.plugins</groupId>
                    <artifactId>maven-dependency-plugin</artifactId>
                    <versionRange>[1.0.0,)</versionRange>
                    <goals>
                      <goal>properties</goal>
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
      </plugins>
    </pluginManagement>
    <plugins>
      <!-- This provides the path to the Java agent -->
      <plugin>
        <groupId>org.apache.maven.plugins</groupId>
        <artifactId>maven-dependency-plugin</artifactId>
        <version>${dependency-plugin.version}</version>
        <executions>
          <execution>
            <id>getClasspathFilenames</id>
            <goals>
              <goal>properties</goal>
            </goals>
          </execution>
        </executions>
      </plugin>
      <plugin>
        <groupId>org.apache.maven.plugins</groupId>
        <artifactId>maven-surefire-plugin</artifactId>
        <version>${surefire-plugin.version}</version>
        <configuration>
          <argLine>-javaagent:${com.nordstrom.tools:junit-foundation:jar}</argLine>
        </configuration>
      </plugin>
    </plugins>
  </build>
</project>
```

#### Gradle Configuration for JUnit Foundation
```
// build.gradle
...
apply plugin: 'maven'
sourceCompatibility = 1.8
targetCompatibility = 1.8
repositories {
    mavenLocal()
    mavenCentral()
    ...
}
configurations {
    ...
    junitAgent
}
test.doFirst {
    jvmArgs "-javaagent:${configurations.junitAgent.files.iterator().next()}"
}
test {
//  debug true
    // not required, but definitely useful
    testLogging.showStandardStreams = true
}
dependencies {
    ...
    compile 'com.nordstrom.tools:junit-foundation:6.0.0'
    junitAgent 'com.nordstrom.tools:junit-foundation:6.0.0'
}
```

#### IDE Configuration for JUnit Foundation

To enable notifications in the native test runner of IDEs like Eclipse or IDEA, add the `-javaagent` option to the JVM options of your run/debug configuration.

#### ServiceLoader Configuration Files

To provide reliable, consistent behavior regardless of execution environment, **JUnit Foundation** notification subscribers are registered through the standard Java **ServiceLoader** mechanism. To attach **JUnit Foundation** watchers and standard JUnit run listeners to your tests, declare them in **ServiceLoader** [provider configuration files](https://docs.oracle.com/javase/tutorial/ext/basics/spi.html#register-service-providers) in a **_META-INF/services/_** folder of your project resorces:

###### com.nordstrom.automation.junit.MethodWatcher
```
com.mycompany.example.MyWatcher
```

###### org.junit.runner.notification.RunListener
```
com.mycompany.example.MyListener
```

The preceding **ServiceLoader** provider configuration files declare a **JUnit Foundation** [MethodWatcher](https://github.com/Nordstrom/JUnit-Foundation/blob/master/src/main/java/com/nordstrom/automation/junit/MethodWatcher.java) and a standard JUnit [RunListener](https://github.com/junit-team/junit4/blob/41d44734f41aba0cf6ba5a11ff5d32ffed155027/src/main/java/org/junit/runner/notification/RunListener.java).

### Defined Service Provider Interfaces

**JUnit Foundation** defines several service provider interfaces that notification subscribers can implement:

* [ShutdownListener](https://github.com/Nordstrom/JUnit-Foundation/blob/master/src/main/java/com/nordstrom/automation/junit/ShutdownListener.java)  
**ShutdownListener** provides callbacks for events in the lifecycle of the JVM that runs the Java code that comprises your tests. It receives the following notification:
  * The JVM that's running the tests is about to close. This signals the completion the the test run.
* [TestClassWatcher](https://github.com/Nordstrom/JUnit-Foundation/blob/master/src/main/java/com/nordstrom/automation/junit/TestClassWatcher.java)  
**TestClassWatcher** provides callbacks for events in the lifecycle of **`TestClass`** objects. It receives the following notifications:
  * A **`TestClass`** object has been created to represent a JUnit test class or suite. Each **`TestClass`** has a one-to-one relationship with the JUnit runner that created it.
  * A **`TestClass`** object has been scheduled to run. This signals that the first child of the JUnit test class or suite is about start.
  * A **`TestClass`** object has finished running. This signals that the last child of the JUnit test class or suite is done.
* [TestObjectWatcher](https://github.com/Nordstrom/JUnit-Foundation/blob/master/src/main/java/com/nordstrom/automation/junit/TestObjectWatcher.java)  
**TestObjectWatcher** provides callbacks for events in the lifecycle of Java test class instances. It receives the following notification:
  * An instance of a JUnit test class has been created for the execution of a single `atomic test`.
* [RunWatcher](https://github.com/Nordstrom/JUnit-Foundation/blob/master/src/main/java/com/nordstrom/automation/junit/RunWatcher.java)  
**RunWatcher** provides callbacks for events in the lifecycle of `atomic tests`. This is a functional replacement for the standard JUnit **RunListener**, with the execution context that the standard interface lacks. It receives the following notifications:
  * An `atomic test` is about to start.
  * An `atomic test` has finished, pass or fail.
  * An `atomic test` has failed.
  * An `atomic test` flags that it assumes a condition that is false.
  * An `atomic test` has been ignored.
* [MethodWatcher](https://github.com/Nordstrom/JUnit-Foundation/blob/master/src/main/java/com/nordstrom/automation/junit/MethodWatcher.java)  
**MethodWatcher** provides callbacks for events in the lifecycle of a `particle method`, which is a component of an `atomic test`. It receives the following notifications:
  * A `particle method` is about to be invoked.
  * A `particle method` has just been invoked.

###### Service Provider Example - Implementing MethodWatcher
```java
package com.nordstrom.example;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runners.model.FrameworkMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoggingWatcher implements MethodWatcher {

    private static final Logger LOGGER = LoggerFactory.getLogger(LoggingWatcher.class);

    @Override
    public void beforeInvocation(Object target, FrameworkMethod method, Object... params) {
        if (null != method.getAnnotation(Test.class)) {
            LOGGER.info(">>>>> ENTER 'test' method {}", method.getName());
        } else if (null != method.getAnnotation(Before.class)) {
            LOGGER.info(">>>>> ENTER 'before' method {}", method.getName());
        } else if (null != method.getAnnotation(After.class)) {
            LOGGER.info(">>>>> ENTER 'after' method {}", method.getName());
        } else if (null != method.getAnnotation(BeforeClass.class)) {
            LOGGER.info(">>>>> ENTER 'before-class' method {}", method.getName());
        } else if (null != method.getAnnotation(AfterClass.class)) {
            LOGGER.info(">>>>> ENTER 'after-class' method {}", method.getName());
        }
    }

    @Override
    public void afterInvocation(Object obj, FrameworkMethod method, Object... params) {
        if (null != method.getAnnotation(Test.class)) {
            LOGGER.info("<<<<< LEAVE 'test' method {}", method.getName());
        } else if (null != method.getAnnotation(Before.class)) {
            LOGGER.info("<<<<< LEAVE 'before' method {}", method.getName());
        } else if (null != method.getAnnotation(After.class)) {
            LOGGER.info("<<<<< LEAVE 'after' method {}", method.getName());
        } else if (null != method.getAnnotation(BeforeClass.class)) {
            LOGGER.info("<<<<< LEAVE 'before-class' method {}", method.getName());
        } else if (null != method.getAnnotation(AfterClass.class)) {
            LOGGER.info("<<<<< LEAVE 'after-class' method {}", method.getName());
        }
    }
}

```

Note that the implementation in this method watcher uses the annotations attached to the method objects to determine the type of method they're intercepting. Because each test method can have multiple configuration methods (both before and after), you may need to define additional conditions to control when your implementation runs. Examples of additional conditions include method name, method annotation, or an execution flag.

### Support for Standard JUnit RunListener Providers

As indicated previously, **JUnit Foundation** will automatically attach standard JUnit **RunListener** providers that are declared in the associated **ServiceLoader** provider configuration file. Declared run listeners are attached to the **RunNotifier** supplied to the `run()` method of JUnit runners. This feature eliminates behavioral differences between the various test execution environments like Maven, Gradle, and native IDE test runners.

## Test Method Timeout Management

**JUnit** provides test method timeout functionality via the `timeout` parameter of the **`@Test`** annotation. With this parameter, you can set an explicit timeout interval in milliseconds on an individual test method. If a test fails to complete within the specified interval, **JUnit** terminates the test and throws **TestTimedOutException**.

**JUnit Foundation** extends this functionality, providing configurable test timeout management. Timeout management is applied by the **JUnit Foundation** Java agent, activated by setting the `TEST_TIMEOUT` configuration option to the desired default test timeout interval in milliseconds. This timeout specification is applied to every test method that doesn't explicitly specify a longer interval.

## Automatic retry of failed tests

Some types of tests are inherently non-deterministic, which can cause them to fail sporadically in the absence of an actual defect. Most of the time, these tests will pass if you run them again. For these sorts of "noise" failures, **JUnit Foundation** provides an automatic retry feature.

Automatic retry is applied by the **JUnit Foundation** Java agent, activated by setting the `MAX_RETRY` configuration option to the maximum retry attempts that will be made if a test method fails. The automatic retry feature can be disabled on a per-method or per-class basis via the **`@NoRetry`** annotation.

**_META-INF/services/com.nordstrom.automation.junit.JUnitRetryAnalyzer_** is the service loader retry analyzer configuration file. By default, this file is absent. To add managed analyzers, create this file and add the fully-qualified names of their classes, one line per item.

Failed attempts of tests that are selected for retry are tallied as ignored tests. These tests are differentiated from actual ignored tests by the presence of a **`@RetriedTest`** annotation in place of the original **`@Test`** annotation. See `RunListenerAdapter.testIgnored(Description)` for more details.

## Shutdown hook installation

**JUnit** provides a run listener feature, but this operates most readily on a per-class basis. The method for attaching these run listeners also imposes structural and operational constraints on **JUnit** projects, and the configuration required to register for end-of-suite notifications necessitates hard-coding the composition of the suite. All of these factors make run listeners unattractive or ineffectual for final cleanup operations.

**JUnit Foundation** enables you to declare shutdown listeners in a service loader configuration file.  
**_META-INF/services/com.nordstrom.automation.junit.ShutdownListener_** is the service loader shutdown listener configuration file. By default, this file is absent. To add managed listeners, create this file and add the fully-qualified names of their classes, one line per item. When it loads, the **JUnit Foundation** Java agent uses the service loader to instantiate your shutdown listeners and attaches them to the active JVM.

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
