package multimodelcompare.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class McCallGpt4WorkerTest {

    private final McCallGpt4Worker worker = new McCallGpt4Worker();

    @Test
    void taskDefName() {
        assertEquals("mc_call_gpt4", worker.getTaskDefName());
    }

    @Test
    void returnsGpt4Response() {
        Task task = taskWith(new HashMap<>(Map.of("prompt", "What is Conductor?")));

        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("gpt-4", result.getOutputData().get("model"));
        assertEquals("Conductor provides durable, observable workflow orchestration for microservices and AI.",
                result.getOutputData().get("response"));
        assertEquals(1200, result.getOutputData().get("latencyMs"));
        assertEquals(85, result.getOutputData().get("tokens"));
        assertEquals(0.0051, result.getOutputData().get("cost"));
        assertEquals(9.2, result.getOutputData().get("quality"));
    }

    @Test
    void outputContainsAllExpectedKeys() {
        Task task = taskWith(new HashMap<>(Map.of("prompt", "test")));
        TaskResult result = worker.execute(task);

        assertTrue(result.getOutputData().containsKey("model"));
        assertTrue(result.getOutputData().containsKey("response"));
        assertTrue(result.getOutputData().containsKey("latencyMs"));
        assertTrue(result.getOutputData().containsKey("tokens"));
        assertTrue(result.getOutputData().containsKey("cost"));
        assertTrue(result.getOutputData().containsKey("quality"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
