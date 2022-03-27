package com.nordstrom.automation.junit;

import static net.bytebuddy.matcher.ElementMatchers.hasSignature;
import static net.bytebuddy.matcher.ElementMatchers.hasSuperType;
import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.matcher.ElementMatchers.takesArgument;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.Modifier;
import java.util.Arrays;

import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.agent.builder.AgentBuilder.Transformer;
import net.bytebuddy.description.field.FieldDescription;
import net.bytebuddy.description.field.FieldDescription.Token;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.method.MethodDescription.SignatureToken;
import net.bytebuddy.description.modifier.FieldManifestation;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.description.type.TypeDescription.Generic;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.Transformer.ForField;
import net.bytebuddy.implementation.FieldAccessor;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.implementation.SuperMethodCall;
import net.bytebuddy.implementation.bytecode.StackManipulation;
import net.bytebuddy.implementation.bytecode.assign.Assigner;
import net.bytebuddy.implementation.bytecode.assign.Assigner.Typing;
import net.bytebuddy.implementation.bytecode.member.FieldAccess;
import net.bytebuddy.implementation.bytecode.member.MethodReturn;
import net.bytebuddy.implementation.bytecode.member.MethodVariableAccess;
import net.bytebuddy.pool.TypePool;
import net.bytebuddy.utility.JavaModule;

/**
 * This class implements byte code transformations that enable the enhancements provided by <b>JUnit Foundation</b>.
 */
public class JUnitAgent {

    /**
     * This is the main entry point for the Java agent used to transform the following classes:
     * <ul>
     *     <li>{@code org.junit.runner.Description}</li>
     *     <li>{@code org.junit.runners.model.FrameworkMethod}</li>
     *     <li>{@code org.junit.runners.model.TestClass}</li>
     *     <li>{@code org.junit.internal.runners.model.EachTestNotifier}</li>
     *     <li>{@code org.junit.internal.runners.model.ReflectiveCallable}</li>
     *     <li>{@code org.junit.runners.model.RunnerScheduler}</li>
     *     <li>{@code org.junit.runners.ParentRunner}</li>
     *     <li>{@code org.junit.runners.BlockJUnit4ClassRunner}</li>
     *     <li>{@code org.junit.experimental.theories.Theories$TheoryAnchor}</li>
     *     <li>{@code org.junit.runner.notification.RunNotifier}</li>
     *     <li>{@code junitparams.internal.ParameterisedTestMethodRunner}</li>
     *     <li>{@code junitparams.internal.TestMethod}</li>
     * </ul>
     *  
     * @param agentArgs agent options
     * @param instrumentation {@link Instrumentation} object used to transform JUnit core classes
     */
    public static void premain(String agentArgs, Instrumentation instrumentation) {
        installTransformer(instrumentation);
    }
    
    /**
     * Install the {@code Byte Buddy} byte code transformations that provide test fine-grained test lifecycle hooks.
     * 
     * @param instrumentation {@link Instrumentation} object used to transform JUnit core classes
     * @return The installed class file transformer
     */
    public static ClassFileTransformer installTransformer(Instrumentation instrumentation) {
        // org.junit.runner.Description
        final TypeDescription description = TypePool.Default.ofSystemLoader().describe("org.junit.runner.Description").resolve();
        final Generic _void_ = TypeDescription.VOID.asGenericType();
        final Generic serializable = TypePool.Default.ofSystemLoader().describe("java.io.Serializable").resolve().asGenericType();
        final MethodDescription.Token setUniqueIdToken = new MethodDescription.Token("setUniqueId", Modifier.PUBLIC, _void_, Arrays.asList(serializable));
        final MethodDescription setUniqueId = new MethodDescription.Latent(description, setUniqueIdToken);
        final Token fUniqueIdToken = new FieldDescription.Token("fUniqueId", Modifier.PRIVATE, serializable);
        final FieldDescription fUniqueId = new FieldDescription.Latent(description, fUniqueIdToken);
        final StackManipulation setUniqueIdImpl = new StackManipulation.Compound(
                MethodVariableAccess.loadThis(),
                MethodVariableAccess.load(setUniqueId.getParameters().get(0)),
                Assigner.DEFAULT.assign(serializable, serializable, Typing.STATIC),
                FieldAccess.forField(fUniqueId).write(),
                MethodReturn.VOID
        );
        // org.junit.runners.model.FrameworkMethod
        final TypeDescription getAnnotations = TypePool.Default.ofSystemLoader().describe("com.nordstrom.automation.junit.GetAnnotations").resolve();
        final TypeDescription getAnnotation = TypePool.Default.ofSystemLoader().describe("com.nordstrom.automation.junit.GetAnnotation").resolve();
        // org.junit.internal.runners.model.EachTestNotifier
        final TypeDescription runNotifier = TypePool.Default.ofSystemLoader().describe("org.junit.runner.notification.RunNotifier").resolve();
        final TypeDescription eachTestNotifierInit = TypePool.Default.ofSystemLoader().describe("com.nordstrom.automation.junit.EachTestNotifierInit").resolve();
        final TypeDescription addFailure = TypePool.Default.ofSystemLoader().describe("com.nordstrom.automation.junit.AddFailure").resolve();
        final TypeDescription fireTestFinished = TypePool.Default.ofSystemLoader().describe("com.nordstrom.automation.junit.FireTestFinished").resolve();
        // org.junit.internal.runners.model.ReflectiveCallable
        final TypeDescription runReflectiveCall = TypePool.Default.ofSystemLoader().describe("com.nordstrom.automation.junit.RunReflectiveCall").resolve();
        // org.junit.runners.model.RunnerScheduler
        final TypeDescription finished = TypePool.Default.ofSystemLoader().describe("com.nordstrom.automation.junit.Finished").resolve();
        // org.junit.runners.ParentRunner
        final TypeDescription runChild = TypePool.Default.ofSystemLoader().describe("com.nordstrom.automation.junit.RunChild").resolve();
        final SignatureToken runToken = new SignatureToken("run", TypeDescription.VOID, Arrays.asList(runNotifier));
        final TypeDescription run = TypePool.Default.ofSystemLoader().describe("com.nordstrom.automation.junit.Run").resolve();
        final TypeDescription describeChild = TypePool.Default.ofSystemLoader().describe("com.nordstrom.automation.junit.DescribeChild").resolve();
        final TypeDescription methodBlock = TypePool.Default.ofSystemLoader().describe("com.nordstrom.automation.junit.MethodBlock").resolve();
        final TypeDescription frameworkMethod = TypePool.Default.ofSystemLoader().describe("org.junit.runners.model.FrameworkMethod").resolve();
        final SignatureToken createTestToken = new SignatureToken("createTest", TypeDescription.OBJECT, Arrays.asList(frameworkMethod));
        final TypeDescription createTest = TypePool.Default.ofSystemLoader().describe("com.nordstrom.automation.junit.CreateTest").resolve();
        final TypeDescription getTestRules = TypePool.Default.ofSystemLoader().describe("com.nordstrom.automation.junit.GetTestRules").resolve();
        // org.junit.experimental.theories.Theories$TheoryAnchor
        final TypeDescription runWithCompleteAssignment = TypePool.Default.ofSystemLoader().describe("com.nordstrom.automation.junit.RunWithCompleteAssignment").resolve();
        // junitparams.internal.ParameterisedTestMethodRunner
        final TypeDescription nextCount = TypePool.Default.ofSystemLoader().describe("com.nordstrom.automation.junit.NextCount").resolve();
        // junitparams.internal.TestMethod
        final TypeDescription testMethodDescription = TypePool.Default.ofSystemLoader().describe("com.nordstrom.automation.junit.TestMethodDescription").resolve();
        
        return new AgentBuilder.Default()
                .type(hasSuperType(named("org.junit.runner.Description")))
                .transform(new Transformer() {
                    @Override
                    public DynamicType.Builder<?> transform(DynamicType.Builder<?> builder, TypeDescription typeDescription,
                            ClassLoader classLoader, JavaModule module) {
                        return builder.field(named("fUniqueId")).transform(ForField.withModifiers(FieldManifestation.PLAIN))
                                      .implement(AnnotationsAccessor.class).intercept(FieldAccessor.ofField("fAnnotations"))
                                      .implement(UniqueIdAccessor.class).intercept(FieldAccessor.ofField("fUniqueId"))
                                      .implement(UniqueIdMutator.class).intercept(new Implementation.Simple(setUniqueIdImpl))
                                      .implement(Hooked.class);
                    }
                })
                .type(hasSuperType(named("org.junit.runners.model.FrameworkMethod")))
                .transform(new Transformer() {
                    @Override
                    public DynamicType.Builder<?> transform(DynamicType.Builder<?> builder, TypeDescription typeDescription,
                            ClassLoader classLoader, JavaModule module) {
                        return builder.method(named("getAnnotations")).intercept(MethodDelegation.to(getAnnotations))
                                      .method(named("getAnnotation")).intercept(MethodDelegation.to(getAnnotation))
                                      .implement(Hooked.class);
                    }
                })
                .type(hasSuperType(named("org.junit.runners.model.TestClass")))
                .transform(new Transformer() {
                    @Override
                    public DynamicType.Builder<?> transform(DynamicType.Builder<?> builder, TypeDescription typeDescription,
                            ClassLoader classLoader, JavaModule module) {
                        return builder.implement(FieldsForAnnotationsAccessor.class).intercept(FieldAccessor.ofField("fieldsForAnnotations"))
                                      .implement(Hooked.class);
                    }
                })
                .type(hasSuperType(named("org.junit.internal.runners.model.EachTestNotifier")))
                .transform(new Transformer() {
                    @Override
                    public DynamicType.Builder<?> transform(DynamicType.Builder<?> builder, TypeDescription type,
                                    ClassLoader classloader, JavaModule module) {
                        return builder.constructor(takesArgument(0, runNotifier).and(takesArgument(1, description)))
                                              .intercept(MethodDelegation.to(eachTestNotifierInit).andThen(SuperMethodCall.INSTANCE))
                                      .method(named("addFailure")).intercept(MethodDelegation.to(addFailure))
                                      .method(named("fireTestFinished")).intercept(MethodDelegation.to(fireTestFinished))
                                      .implement(Hooked.class);
                    }
                })
                .type(hasSuperType(named("org.junit.internal.runners.model.ReflectiveCallable")))
                .transform(new Transformer() {
                    @Override
                    public DynamicType.Builder<?> transform(DynamicType.Builder<?> builder, TypeDescription type,
                                    ClassLoader classloader, JavaModule module) {
                        return builder.method(named("runReflectiveCall")).intercept(MethodDelegation.to(runReflectiveCall))
                                      .implement(Hooked.class);
                    }
                })
                .type(hasSuperType(named("org.junit.runners.model.RunnerScheduler")))
                .transform(new Transformer() {
                    @Override
                    public DynamicType.Builder<?> transform(DynamicType.Builder<?> builder, TypeDescription type,
                                    ClassLoader classloader, JavaModule module) {
                        return builder.method(named("finished")).intercept(MethodDelegation.to(finished))
                                      .implement(Hooked.class);
                    }
                })
                .type(hasSuperType(named("org.junit.runners.ParentRunner")))
                .transform(new Transformer() {
                    @Override
                    public DynamicType.Builder<?> transform(DynamicType.Builder<?> builder, TypeDescription type,
                                    ClassLoader classloader, JavaModule module) {
                        return builder.method(named("runChild")).intercept(MethodDelegation.to(runChild))
                                      .method(hasSignature(runToken)).intercept(MethodDelegation.to(run))
                                      .method(named("describeChild")).intercept(MethodDelegation.to(describeChild))
                                      // NOTE: The 'methodBlock', 'createTest', and 'getTestRules' methods
                                      //       are defined in BlockJUnit4ClassRunner, but I've been unable
                                      //       to transform this ParentRunner subclass.
                                      .method(named("methodBlock")).intercept(MethodDelegation.to(methodBlock))
                                      .method(hasSignature(createTestToken)).intercept(MethodDelegation.to(createTest))
                                      .method(named("getTestRules")).intercept(MethodDelegation.to(getTestRules))
                                      .implement(Hooked.class);
                    }
                })
                .type(hasSuperType(named("org.junit.experimental.theories.Theories$TheoryAnchor")))
                .transform(new Transformer() {
                    @Override
                    public DynamicType.Builder<?> transform(DynamicType.Builder<?> builder, TypeDescription type,
                                    ClassLoader classloader, JavaModule module) {
                        return builder.method(named("runWithCompleteAssignment")).intercept(MethodDelegation.to(runWithCompleteAssignment))
                                      .implement(Hooked.class);
                    }
                })
                .type(hasSuperType(named("org.junit.runner.notification.RunNotifier")))
                .transform(new Transformer() {
                    @Override
                    public DynamicType.Builder<?> transform(DynamicType.Builder<?> builder, TypeDescription type,
                                    ClassLoader classloader, JavaModule module) {
                        return builder.method(named("fireTestFailure")).intercept(MethodDelegation.to(addFailure))
                                      .method(named("fireTestAssumptionFailed")).intercept(MethodDelegation.to(addFailure))
                                      .implement(Hooked.class);
                    }
                })
                .type(hasSuperType(named("junitparams.internal.ParameterisedTestMethodRunner")))
                .transform(new Transformer() {
                    @Override
                    public DynamicType.Builder<?> transform(DynamicType.Builder<?> builder, TypeDescription type,
                                    ClassLoader classloader, JavaModule module) {
                        return builder.method(named("nextCount")).intercept(MethodDelegation.to(nextCount))
                                      .implement(MethodAccessor.class).intercept(FieldAccessor.ofField("method"))
                                      .implement(Hooked.class);
                    }
                })
                .type(hasSuperType(named("junitparams.internal.TestMethod")))
                .transform(new Transformer() {
                    @Override
                    public DynamicType.Builder<?> transform(DynamicType.Builder<?> builder, TypeDescription type,
                                    ClassLoader classloader, JavaModule module) {
                        return builder.method(named("description")).intercept(MethodDelegation.to(testMethodDescription))
                                      .implement(Hooked.class);
                    }
                })
                .installOn(instrumentation);
    }
    
}
