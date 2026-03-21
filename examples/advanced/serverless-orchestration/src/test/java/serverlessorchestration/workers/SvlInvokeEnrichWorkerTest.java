package serverlessorchestration.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SvlInvokeEnrichWorkerTest {

    private final SvlInvokeEnrichWorker worker = new SvlInvokeEnrichWorker();

    @Test
    void taskDefName() {
        assertEquals("svl_invoke_enrich", worker.getTaskDefName());
    }

    @Test
    void completesSuccessfully() {
        Task task = taskWith(Map.of(
                "functionArn", "arn:aws:lambda:us-east-1:123:function:enrich-data",
                "parsedData", Map.of("type", "click")));
        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test
    void outputContainsEnrichedData() {
        Task task = taskWith(Map.of("parsedData", Map.of()));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        Map<String, Object> enriched = (Map<String, Object>) result.getOutputData().get("enriched");
        assertNotNull(enriched);
        assertEquals("premium", enriched.get("userTier"));
        assertEquals(42, enriched.get("sessionCount"));
        assertEquals("US", enriched.get("geo"));
    }

    @Test
    void outputContainsBilledMs() {
        Task task = taskWith(Map.of("parsedData", Map.of()));
        TaskResult result = worker.execute(task);
        assertEquals(120, result.getOutputData().get("billedMs"));
    }

    @Test
    void handlesMissingInputs() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test
    void enrichedDataHasThreeFields() {
        Task task = taskWith(Map.of("parsedData", Map.of()));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        Map<String, Object> enriched = (Map<String, Object>) result.getOutputData().get("enriched");
        assertEquals(3, enriched.size());
    }

    @Test
    void sessionCountIsInteger() {
        Task task = taskWith(Map.of("parsedData", Map.of()));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        Map<String, Object> enriched = (Map<String, Object>) result.getOutputData().get("enriched");
        assertInstanceOf(Integer.class, enriched.get("sessionCount"));
    }

    @Test
    void deterministicOutput() {
        Task t1 = taskWith(Map.of("parsedData", Map.of()));
        Task t2 = taskWith(Map.of("parsedData", Map.of()));
        TaskResult r1 = worker.execute(t1);
        TaskResult r2 = worker.execute(t2);
        assertEquals(r1.getOutputData().get("enriched"), r2.getOutputData().get("enriched"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
