package io.orkes.conductor.client;

import java.util.List;

import com.netflix.conductor.client.automator.TaskRunnerConfigurer;
import com.netflix.conductor.client.http.TaskClient;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class Main {

    public static void main(String[] args) {
        ApiClient apiClient = new ApiClient("http://localhost:8080/api", "838dbb97-73c9-11f0-8aa4-5ce91e8abace", "2QEIBUOWPiGx4L1vM60F7yEuYf1cjh9LZl14nm68K4Z71PjX");
        TaskClient taskClient = new TaskClient(apiClient);
        TaskRunnerConfigurer configurer = new TaskRunnerConfigurer.Builder(taskClient, List.of(Worker.create("simple123", task -> {
            TaskResult result = new TaskResult(task);
            result.setStatus(TaskResult.Status.COMPLETED);
            result.addOutputData("hello", "world");
            return result;
        })))
            .withThreadCount(1)
            .build();

        configurer.init();
    }


}
