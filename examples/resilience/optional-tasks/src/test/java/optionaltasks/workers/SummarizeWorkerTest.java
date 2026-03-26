package optionaltasks.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SummarizeWorkerTest {

    @Test
    void taskDefName() {
        SummarizeWorker worker = new SummarizeWorker();
        assertEquals("opt_summarize", worker.getTaskDefName());
    }

    @Test
    void summarizesWithEnrichment() {
        SummarizeWorker worker = new SummarizeWorker();
        Task task = taskWith(Map.of(
                "processedData", "processed-hello",
                "enrichedData", "extra-data-added"
        ));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("Processed: processed-hello, Enriched: extra-data-added",
                result.getOutputData().get("summary"));
    }

    @Test
    void summarizesWithoutEnrichment() {
        SummarizeWorker worker = new SummarizeWorker();
        Task task = taskWith(Map.of(
                "processedData", "processed-hello"
        ));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("Processed: processed-hello, Enrichment: skipped",
                result.getOutputData().get("summary"));
    }

    @Test
    void summarizesWithNullEnrichment() {
        SummarizeWorker worker = new SummarizeWorker();
        Map<String, Object> input = new HashMap<>();
        input.put("processedData", "processed-hello");
        input.put("enrichedData", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("Processed: processed-hello, Enrichment: skipped",
                result.getOutputData().get("summary"));
    }

    @Test
    void summarizesWithEmptyEnrichment() {
        SummarizeWorker worker = new SummarizeWorker();
        Task task = taskWith(Map.of(
                "processedData", "processed-hello",
                "enrichedData", ""
        ));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("Processed: processed-hello, Enrichment: skipped",
                result.getOutputData().get("summary"));
    }

    @Test
    void outputContainsSummaryKey() {
        SummarizeWorker worker = new SummarizeWorker();
        Task task = taskWith(Map.of("processedData", "test"));
        TaskResult result = worker.execute(task);

        assertTrue(result.getOutputData().containsKey("summary"));
    }

    @Test
    void resultIsDeterministic() {
        SummarizeWorker worker = new SummarizeWorker();
        Task task1 = taskWith(Map.of("processedData", "data1", "enrichedData", "enrich1"));
        Task task2 = taskWith(Map.of("processedData", "data1", "enrichedData", "enrich1"));
        TaskResult result1 = worker.execute(task1);
        TaskResult result2 = worker.execute(task2);

        assertEquals(result1.getOutputData().get("summary"), result2.getOutputData().get("summary"));
    }

    @Test
    void handlesNullProcessedData() {
        SummarizeWorker worker = new SummarizeWorker();
        Map<String, Object> input = new HashMap<>();
        input.put("processedData", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("Processed: null, Enrichment: skipped",
                result.getOutputData().get("summary"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
