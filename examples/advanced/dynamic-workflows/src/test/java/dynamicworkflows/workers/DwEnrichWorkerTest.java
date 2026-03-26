package dynamicworkflows.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class DwEnrichWorkerTest {

    private final DwEnrichWorker worker = new DwEnrichWorker();

    @Test
    void taskDefName() {
        assertEquals("dw_enrich", worker.getTaskDefName());
    }

    @Test
    void enrichesSuccessfully() {
        Task task = taskWith(Map.of("stepId", "enrich", "config", "{\"source\":\"external_api\"}"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("enrich_complete", result.getOutputData().get("result"));
    }

    @Test
    void outputContainsStepType() {
        Task task = taskWith(Map.of("stepId", "enrich"));
        TaskResult result = worker.execute(task);

        assertEquals("enrichment", result.getOutputData().get("stepType"));
    }

    @Test
    void handlesNullConfig() {
        Map<String, Object> input = new HashMap<>();
        input.put("config", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test
    void handlesEmptyInput() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test
    void handlesPreviousOutput() {
        Task task = taskWith(Map.of("previousOutput", "transform_complete", "config", "{}"));
        TaskResult result = worker.execute(task);

        assertEquals("enrich_complete", result.getOutputData().get("result"));
    }

    @Test
    void resultIsDeterministic() {
        Task task = taskWith(Map.of("config", "{}"));
        TaskResult r1 = worker.execute(task);
        TaskResult r2 = worker.execute(task);

        assertEquals(r1.getOutputData().get("result"), r2.getOutputData().get("result"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
