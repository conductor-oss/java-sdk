/*
 * Copyright 2020 Conductor Authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package com.netflix.conductor.client.spring;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.conductoross.conductor.sdk.Conductor;
import org.conductoross.conductor.sdk.Workers;
import org.conductoross.conductor.sdk.Workflow;
import org.conductoross.conductor.sdk.internal.TaskStubs;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.DependencyDescriptor;
import org.springframework.boot.autoconfigure.AutoConfigurationPackages;
import org.springframework.context.SmartLifecycle;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.MethodParameter;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;

import com.netflix.conductor.sdk.workflow.task.WorkerTask;

import lombok.extern.slf4j.Slf4j;

/**
 * Finds the classes with {@code @Workflow} methods under the application's packages and hosts
 * them once the application has started, stopping before the context closes.
 *
 * <p>A workflow class need not be a bean. Each replay gets a new instance: a constructor parameter
 * of a class with {@code @WorkerTask} methods gets a stub whose calls schedule those tasks, and
 * any other parameter or {@code @Autowired} field is injected from the context. A field holding a
 * real task object is rejected, since its calls would run in the workflow on every replay. Task beans keep running on the worker auto-configuration's executor; this only
 * registers their task definitions, from the {@code @WorkerTask} attributes.
 *
 * <p>Properties under {@code conductor.workflows}: {@code worker-id}, {@code build-id},
 * {@code threads}, {@code continuation-timeout} and {@code dev-mode}.
 */
@Slf4j
public class ConductorWorkflowHost implements SmartLifecycle {

    private final Conductor conductor;
    private final ConfigurableListableBeanFactory beanFactory;
    private final Environment environment;
    private volatile Workers workers;

    public ConductorWorkflowHost(Conductor conductor, ConfigurableListableBeanFactory beanFactory,
                                 Environment environment) {
        this.conductor = conductor;
        this.beanFactory = beanFactory;
        this.environment = environment;
    }

    @Override
    public void start() {
        Set<Class<?>> types = workflowTypes();
        if (types.isEmpty()) {
            log.info("No @Workflow classes under {}", packages());
            return;
        }
        Workers w = conductor.host();
        String workerId = environment.getProperty("conductor.workflows.worker-id");
        if (workerId != null) {
            w.workerId(workerId);
        }
        w.buildId(environment.getProperty("conductor.workflows.build-id"));
        w.threads(environment.getProperty("conductor.workflows.threads", Integer.class, 8));
        w.continuationTimeout(environment.getProperty("conductor.workflows.continuation-timeout", Duration.class,
                Duration.ofSeconds(30)));
        w.devMode(environment.getProperty("conductor.workflows.dev-mode", Boolean.class, false));
        types.forEach(type -> host(w, type));
        w.taskDefinitions(taskBeans().toArray());
        workers = w.start();
    }

    @Override
    public void stop() {
        Workers w = workers;
        workers = null;
        if (w != null) {
            w.shutdown();
        }
    }

    @Override
    public boolean isRunning() {
        return workers != null;
    }

    private <T> void host(Workers w, Class<T> type) {
        w.workflow(type, () -> {
            // task classes come as stubs, so their calls schedule tasks; anything else from the context
            T workflow = TaskStubs.newWorkflow(type,
                    p -> beanFactory.resolveDependency(new DependencyDescriptor(MethodParameter.forParameter(p), true), null));
            beanFactory.autowireBean(workflow);
            return workflow;
        });
    }

    private List<String> packages() {
        return AutoConfigurationPackages.has(beanFactory) ? AutoConfigurationPackages.get(beanFactory) : List.of();
    }

    Set<Class<?>> workflowTypes() {
        ClassPathScanningCandidateComponentProvider scanner =
                new ClassPathScanningCandidateComponentProvider(false, environment);
        scanner.addIncludeFilter((reader, factory) ->
                reader.getAnnotationMetadata().hasAnnotatedMethods(Workflow.class.getName()));
        Set<Class<?>> types = new LinkedHashSet<>();
        for (String pkg : packages()) {
            for (BeanDefinition candidate : scanner.findCandidateComponents(pkg)) {
                types.add(ClassUtils.resolveClassName(candidate.getBeanClassName(), beanFactory.getBeanClassLoader()));
            }
        }
        return types;
    }

    /** The same beans the worker auto-configuration polls: components with {@code @WorkerTask} methods. */
    private List<Object> taskBeans() {
        List<Object> beans = new ArrayList<>();
        for (Object bean : beanFactory.getBeansWithAnnotation(Component.class).values()) {
            boolean[] found = {false};
            ReflectionUtils.doWithMethods(AopUtils.getTargetClass(bean),
                    m -> found[0] = true, m -> m.isAnnotationPresent(WorkerTask.class));
            if (found[0]) {
                beans.add(bean);
            }
        }
        return beans;
    }
}
