package toolusesequential.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.List;
import java.util.Map;

/**
 * Extracts structured data from page content.
 * Takes pageContent and query, returns extracted facts, key features, use cases, and metadata.
 */
public class ExtractDataWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "ts_extract_data";
    }

    @Override
    public TaskResult execute(Task task) {
        @SuppressWarnings("unchecked")
        Map<String, Object> pageContent = (Map<String, Object>) task.getInputData().get("pageContent");

        String query = (String) task.getInputData().get("query");
        if (query == null || query.isBlank()) {
            query = "general query";
        }

        System.out.println("  [ts_extract_data] Extracting data for query: " + query);

        List<String> facts = List.of(
                "Conductor is an open-source workflow orchestration engine",
                "Originally developed at Netflix for media processing pipelines",
                "Supports distributed task execution across microservices",
                "Provides built-in fault tolerance with configurable retries",
                "Offers a visual workflow designer for building and monitoring workflows"
        );

        List<String> keyFeatures = List.of(
                "Task queuing and worker polling",
                "Workflow versioning and sub-workflows",
                "Error handling with automatic retries",
                "Visual workflow designer",
                "Synchronous and asynchronous task execution"
        );

        List<String> useCases = List.of(
                "Media processing pipelines",
                "Microservice orchestration",
                "Data processing workflows",
                "CI/CD pipelines",
                "AI/ML pipeline orchestration"
        );

        Map<String, Object> data = Map.of(
                "facts", facts,
                "keyFeatures", keyFeatures,
                "useCases", useCases,
                "origin", "Netflix",
                "type", "open-source"
        );

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("data", data);
        result.getOutputData().put("relevanceScore", 0.92);
        return result;
    }
}
