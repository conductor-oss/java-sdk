package firstaiworkflow.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class AiParseResponseWorkerTest {

    private final AiParseResponseWorker worker = new AiParseResponseWorker();

    @Test
    void taskDefName() {
        assertEquals("ai_parse_response", worker.getTaskDefName());
    }

    @Test
    void parsesResponseAndSetsValid() {
        Task task = taskWith(new HashMap<>(Map.of(
                "rawResponse", "Orkes Conductor is a workflow orchestration platform.",
                "tokenUsage", Map.of("promptTokens", 45, "completionTokens", 38, "totalTokens", 83)
        )));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("Orkes Conductor is a workflow orchestration platform.",
                result.getOutputData().get("answer"));
        assertEquals(true, result.getOutputData().get("valid"));
    }

    @Test
    void handlesNullRawResponse() {
        Task task = taskWith(new HashMap<>(Map.of()));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNull(result.getOutputData().get("answer"));
        assertEquals(true, result.getOutputData().get("valid"));
    }

    @Test
    void outputContainsAnswerField() {
        Task task = taskWith(new HashMap<>(Map.of(
                "rawResponse", "Some AI response"
        )));
        TaskResult result = worker.execute(task);

        assertEquals("Some AI response", result.getOutputData().get("answer"));
        assertEquals(true, result.getOutputData().get("valid"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
