package googlegemini.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class GeminiPrepareContentWorkerTest {

    private final GeminiPrepareContentWorker worker = new GeminiPrepareContentWorker();

    @Test
    void taskDefName() {
        assertEquals("gemini_prepare_content", worker.getTaskDefName());
    }

    @SuppressWarnings("unchecked")
    @Test
    void buildsRequestBodyWithPartsStructure() {
        Task task = taskWith(new HashMap<>(Map.of(
                "prompt", "Create a launch plan",
                "context", "B2B SaaS company",
                "model", "gemini-2.5-flash",
                "temperature", 0.4,
                "topK", 40,
                "topP", 0.95,
                "maxOutputTokens", 1024
        )));

        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("gemini-2.5-flash", result.getOutputData().get("model"));

        Map<String, Object> requestBody = (Map<String, Object>) result.getOutputData().get("requestBody");
        assertNotNull(requestBody);

        // Verify contents with parts structure
        List<Map<String, Object>> contents = (List<Map<String, Object>>) requestBody.get("contents");
        assertNotNull(contents);
        assertEquals(1, contents.size());
        List<Map<String, Object>> parts = (List<Map<String, Object>>) contents.get(0).get("parts");
        String text = (String) parts.get(0).get("text");
        assertTrue(text.startsWith("Context: B2B SaaS company"));
        assertTrue(text.contains("Create a launch plan"));

        // Verify generationConfig
        Map<String, Object> genConfig = (Map<String, Object>) requestBody.get("generationConfig");
        assertEquals(0.4, genConfig.get("temperature"));
        assertEquals(40, genConfig.get("topK"));
        assertEquals(0.95, genConfig.get("topP"));
        assertEquals(1024, genConfig.get("maxOutputTokens"));

        // Verify safetySettings
        List<Map<String, Object>> safety = (List<Map<String, Object>>) requestBody.get("safetySettings");
        assertEquals(2, safety.size());
    }

    @SuppressWarnings("unchecked")
    @Test
    void combinedTextFormatIsCorrect() {
        Task task = taskWith(new HashMap<>(Map.of(
                "prompt", "Test prompt",
                "context", "Test context",
                "model", "gemini-2.5-flash",
                "temperature", 0.5,
                "topK", 20,
                "topP", 0.9,
                "maxOutputTokens", 512
        )));

        TaskResult result = worker.execute(task);

        Map<String, Object> requestBody = (Map<String, Object>) result.getOutputData().get("requestBody");
        List<Map<String, Object>> contents = (List<Map<String, Object>>) requestBody.get("contents");
        List<Map<String, Object>> parts = (List<Map<String, Object>>) contents.get(0).get("parts");
        String text = (String) parts.get(0).get("text");
        assertEquals("Context: Test context\n\nTest prompt", text);
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
