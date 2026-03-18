package firstaiworkflow.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class AiPreparePromptWorkerTest {

    private final AiPreparePromptWorker worker = new AiPreparePromptWorker();

    @Test
    void taskDefName() {
        assertEquals("ai_prepare_prompt", worker.getTaskDefName());
    }

    @Test
    void preparesPromptWithQuestionAndModel() {
        Task task = taskWith(new HashMap<>(Map.of("question", "What is Orkes Conductor?", "model", "gpt-4")));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        String prompt = (String) result.getOutputData().get("formattedPrompt");
        assertNotNull(prompt);
        assertTrue(prompt.contains("gpt-4"));
        assertTrue(prompt.contains("What is Orkes Conductor?"));
        assertTrue(prompt.contains("You are a helpful assistant"));
    }

    @Test
    void defaultsModelWhenMissing() {
        Task task = taskWith(new HashMap<>(Map.of("question", "Hello")));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        String prompt = (String) result.getOutputData().get("formattedPrompt");
        assertTrue(prompt.contains("gpt-4"));
    }

    @Test
    void defaultsQuestionWhenBlank() {
        Task task = taskWith(new HashMap<>(Map.of("question", "   ", "model", "gpt-4")));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        String prompt = (String) result.getOutputData().get("formattedPrompt");
        assertTrue(prompt.contains("Hello"));
    }

    @Test
    void defaultsBothWhenMissing() {
        Task task = taskWith(new HashMap<>(Map.of()));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        String prompt = (String) result.getOutputData().get("formattedPrompt");
        assertEquals("You are a helpful assistant. Using gpt-4, please answer: Hello", prompt);
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
