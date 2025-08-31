/*
 * Copyright 2021 Conductor Authors.
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
package com.netflix.conductor.sdk.workflow.executor.task;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.netflix.conductor.client.automator.TaskRunnerConfigurer;
import com.netflix.conductor.client.http.TaskClient;
import com.netflix.conductor.client.metrics.MetricsCollector;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.sdk.workflow.task.WorkerTask;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
import com.google.common.reflect.ClassPath;


public class AnnotatedWorkerExecutor {

    private static final Logger LOGGER = LoggerFactory.getLogger(AnnotatedWorkerExecutor.class);

    protected TaskClient taskClient;

    private TaskRunnerConfigurer taskRunner;

    protected List<Worker> workers = new ArrayList<>();

    protected Map<String, Integer> workerToThreadCount = new HashMap<>();

    protected Map<String, Integer> workerToPollingInterval = new HashMap<>();

    protected Map<String, Integer> workerToPollTimeout = new HashMap<>();

    protected Map<String, String> workerDomains = new HashMap<>();

    private MetricsCollector metricsCollector;

    private final Set<String> scannedPackages = new HashSet<>();

    private final WorkerConfiguration workerConfiguration;

    public AnnotatedWorkerExecutor(TaskClient taskClient) {
        this.taskClient = taskClient;
        this.workerConfiguration = new WorkerConfiguration();
    }

    public AnnotatedWorkerExecutor(TaskClient taskClient, int pollingIntervalInMillis) {
        this.taskClient = taskClient;
        this.workerConfiguration = new WorkerConfiguration(pollingIntervalInMillis);
    }

    public AnnotatedWorkerExecutor(TaskClient taskClient, WorkerConfiguration workerConfiguration) {
        this.taskClient = taskClient;
        this.workerConfiguration = workerConfiguration;
    }

    /**
     * Finds any worker implementation and starts polling for tasks
     *
     * @param basePackages list of packages - comma separated - to scan for annotated worker
     *     implementation
     */
    public synchronized void initWorkers(String... basePackages) {
        scanWorkers(basePackages);
        startPolling();
    }

    public synchronized void initWorkersFromInstances(List<Object> workerInstances) {
        for (var worker : workerInstances) {
            addBean(worker);
        }

        startPolling();
    }

    public synchronized void initWorkersFromClasses(List<? extends Class<?>> classesToScan) {
        var workerClasses = classesToScan.stream()
                .map(workerClass -> {
                    LOGGER.trace("Scanning class {} from class loader {}", workerClass, workerClass.getClassLoader());

                    try {
                        return Optional.of(workerClass.getConstructor().newInstance());
                    } catch (NoSuchMethodException | InvocationTargetException | InstantiationException | IllegalAccessException e) {
                        LOGGER.error("Error while scanning class {}: {}", workerClass.getName(), e.getMessage());

                        return Optional.empty();
                    }
                })
                .filter(Optional::isPresent)
                .map(optional -> (Object) optional.get())
                .toList();

        initWorkersFromInstances(workerClasses);
    }


    /** Shuts down the workers */
    public void shutdown() {
        if (taskRunner != null) {
            taskRunner.shutdown();
        }
    }

    private void scanWorkers(String... basePackages) {
        try {
            List<String> packagesToScan = new ArrayList<>();

            Arrays.stream(basePackages)
                    .flatMap(basePackage -> Arrays.stream(basePackage.split(",")))
                    .forEach(basePackage -> {
                        if (scannedPackages.contains(basePackage)) {
                            LOGGER.info("Package {} already scanned and will skip", basePackage);
                        } else {
                            // Add here so to avoid infinite recursion where a class in the package contains the
                            // code to init workers
                            scannedPackages.add(basePackage);
                            packagesToScan.add(basePackage);
                        }
                    });


            LOGGER.info("Packages to scan {}", packagesToScan);

            long s = System.currentTimeMillis();

            var classLoader = AnnotatedWorkerExecutor.class.getClassLoader();

            LOGGER.trace("Scanning for classes in package {} in classloader {}", packagesToScan, classLoader);

            var classesOnClasspath = ClassPath.from(classLoader).getAllClasses();

            var classes = classesOnClasspath.stream()
                    .filter(classMeta -> classBelongsToPackage(packagesToScan, classMeta.getName()))
                    .map(ClassPath.ClassInfo::load)
                    .toList();

            LOGGER.trace(
                    "Took {} ms to scan all the classes, loading {} tasks",
                    (System.currentTimeMillis() - s),
                    workers.size());

            initWorkersFromClasses(classes);

        } catch (Exception e) {
            LOGGER.error("Error while scanning for workers: ", e);
        }
    }

    private boolean classBelongsToPackage(List<String> packagesToScan, String className) {
        for (String scanPkg : packagesToScan) {
            if (className.startsWith(scanPkg)) return true;
        }
        return false;
    }

    public void addBean(Object bean) {
        Class<?> clazz = bean.getClass();
        for (Method method : clazz.getMethods()) {
            WorkerTask annotation = method.getAnnotation(WorkerTask.class);
            if (annotation == null) {
                continue;
            }
            addMethod(annotation, method, bean);
        }
    }

    private void addMethod(WorkerTask annotation, Method method, Object bean) {
        String name = annotation.value();

        int threadCount = workerConfiguration.getThreadCount(name);
        if (threadCount == 0) {
            threadCount = annotation.threadCount();
        }
        workerToThreadCount.put(name, threadCount);

        int pollingInterval = workerConfiguration.getPollingInterval(name);
        if (pollingInterval == 0) {
            pollingInterval = annotation.pollingInterval();
        }
        workerToPollingInterval.put(name, pollingInterval);

        int pollTimeout = workerConfiguration.getPollTimeout(name);
        if (pollTimeout == 0) {
            pollTimeout = annotation.pollTimeout();
        }
        workerToPollTimeout.put(name, pollTimeout);

        String domain = workerConfiguration.getDomain(name);
        if (Strings.isNullOrEmpty(domain)) {
            domain = annotation.domain();
        }
        if (!Strings.isNullOrEmpty(domain)) {
            workerDomains.put(name, domain);
        }

        AnnotatedWorker executor = new AnnotatedWorker(name, method, bean);
        executor.setPollingInterval(workerToPollingInterval.get(name));

        int pollerCount = workerConfiguration.getPollerCount(name);
        if (pollerCount == 0) {
            pollerCount = annotation.pollerCount();
        }
        if(pollerCount < 1) {
            pollerCount = 1;
        }

        for (int i = 0; i < pollerCount; i++) {
            workers.add(executor);
        }

        LOGGER.info(
                "Adding worker for task {}, method {} with threadCount {} and polling interval set to {} ms",
                name,
                method,
                threadCount,
                pollingInterval);
    }

    public void startPolling() {
        if (workers.isEmpty()) {
            return;
        }

        LOGGER.info("Starting {} with threadCount {}", workers.stream().map(Worker::getTaskDefName).toList(), workerToThreadCount);
        LOGGER.info("Worker domains {}", workerDomains);
        LOGGER.info("Worker workerToPollTimeout {}", workerToPollTimeout);

        var builder = new TaskRunnerConfigurer.Builder(taskClient, workers)
                .withTaskThreadCount(workerToThreadCount)
                .withTaskPollTimeout(workerToPollTimeout)
                .withTaskToDomain(workerDomains);
        if (metricsCollector != null) {
            builder.withMetricsCollector(metricsCollector);
        }

        var oldTaskRunner = Optional.ofNullable(taskRunner);

        taskRunner = builder.build();
        taskRunner.init();

        oldTaskRunner.ifPresent(taskRunner -> {
            LOGGER.trace("Shutting down previous task runner with {} workers.", taskRunner.getWorkerCount());
            taskRunner.shutdown();
        });
    }

    @VisibleForTesting
    List<Worker> getWorkers() {
        return workers;
    }

    @VisibleForTesting
    TaskRunnerConfigurer getTaskRunner() {
        return taskRunner;
    }

    public MetricsCollector getMetricsCollector() {
        return metricsCollector;
    }

    public void setMetricsCollector(MetricsCollector metricsCollector) {
        this.metricsCollector = metricsCollector;
    }
}
