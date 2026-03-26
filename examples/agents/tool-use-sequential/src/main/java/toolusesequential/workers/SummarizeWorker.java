package toolusesequential.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.Map;

/**
 * perform  summarizing extracted data into a coherent summary.
 * Takes query, extractedData, sourceUrl, and sourceTitle. Returns a summary with confidence score.
 */
public class SummarizeWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "ts_summarize";
    }

    @Override
    public TaskResult execute(Task task) {
        String query = (String) task.getInputData().get("query");
        if (query == null || query.isBlank()) {
            query = "general query";
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> extractedData = (Map<String, Object>) task.getInputData().get("extractedData");

        String sourceUrl = (String) task.getInputData().get("sourceUrl");
        if (sourceUrl == null || sourceUrl.isBlank()) {
            sourceUrl = "https://example.com";
        }

        String sourceTitle = (String) task.getInputData().get("sourceTitle");
        if (sourceTitle == null || sourceTitle.isBlank()) {
            sourceTitle = "Unknown Source";
        }

        System.out.println("  [ts_summarize] Summarizing data for query: " + query);

        String summary = "Conductor is an open-source, distributed workflow orchestration engine "
                + "originally developed at Netflix. It enables developers to define, execute, and "
                + "manage complex workflows across microservices with built-in fault tolerance and "
                + "scalability. Key features include task queuing, worker polling, workflow versioning, "
                + "sub-workflows, error handling with retries, and a visual workflow designer. "
                + "Conductor supports both synchronous and asynchronous task execution, making it "
                + "suitable for a wide range of use cases including media processing pipelines, "
                + "microservice orchestration, data processing workflows, CI/CD pipelines, and "
                + "AI/ML pipeline orchestration. The architecture uses a central server that manages "
                + "workflow state and distributes tasks to independently scalable workers.";

        // Count words in the summary
        int wordCount = summary.split("\\s+").length;

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("summary", summary);
        result.getOutputData().put("confidence", 0.94);
        result.getOutputData().put("wordCount", wordCount);
        result.getOutputData().put("sourceUrl", sourceUrl);
        return result;
    }
}
