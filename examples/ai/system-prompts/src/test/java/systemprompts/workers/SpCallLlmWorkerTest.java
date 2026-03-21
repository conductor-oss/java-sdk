package systemprompts.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SpCallLlmWorkerTest {

    private final SpCallLlmWorker worker = new SpCallLlmWorker();

    @Test
    void taskDefName() {
        assertEquals("sp_call_llm", worker.getTaskDefName());
    }

    @Test
    void returnsFormalResponse() {
        Task task = taskWith(Map.of("fullPrompt", "{}", "style", "formal", "model", "gpt-4"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("formal", result.getOutputData().get("style"));

        String response = (String) result.getOutputData().get("response");
        assertNotNull(response);
        assertNotNull(response);
        assertTrue(response.contains("distributed workflow orchestration engine"));
        assertTrue(response.contains("enterprise-grade"));
    }

    @Test
    void returnsCasualResponse() {
        Task task = taskWith(Map.of("fullPrompt", "{}", "style", "casual", "model", "gpt-4"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("casual", result.getOutputData().get("style"));

        String response = (String) result.getOutputData().get("response");
        assertNotNull(response);
        assertNotNull(response);
        assertTrue(response.contains("traffic controller"));
        assertTrue(response.contains("picks up right where it left off"));
    }

    @Test
    void defaultsToFormalWhenStyleMissing() {
        Task task = taskWith(new HashMap<>(Map.of("fullPrompt", "{}")));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("formal", result.getOutputData().get("style"));

        String response = (String) result.getOutputData().get("response");
        assertNotNull(response);
        assertTrue(response.contains("distributed workflow orchestration engine"));
    }

    @Test
    void outputContainsResponseAndStyle() {
        Task task = taskWith(Map.of("fullPrompt", "{}", "style", "casual", "model", "gpt-4"));
        TaskResult result = worker.execute(task);

        assertNotNull(result.getOutputData().get("response"));
        assertNotNull(result.getOutputData().get("style"));
    }

    @Test
    void formalAndCasualResponsesDiffer() {
        Task formalTask = taskWith(Map.of("style", "formal"));
        Task casualTask = taskWith(Map.of("style", "casual"));

        TaskResult formalResult = worker.execute(formalTask);
        TaskResult casualResult = worker.execute(casualTask);

        assertNotEquals(
                formalResult.getOutputData().get("response"),
                casualResult.getOutputData().get("response")
        );
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
