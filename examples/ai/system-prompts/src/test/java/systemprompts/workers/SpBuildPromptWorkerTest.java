package systemprompts.workers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SpBuildPromptWorkerTest {

    private final SpBuildPromptWorker worker = new SpBuildPromptWorker();
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void taskDefName() {
        assertEquals("sp_build_prompt", worker.getTaskDefName());
    }

    @Test
    void buildsFormalPrompt() throws Exception {
        Task task = taskWith(Map.of("userPrompt", "Explain Conductor", "style", "formal", "model", "gpt-4"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("formal", result.getOutputData().get("style"));

        String fullPrompt = (String) result.getOutputData().get("fullPrompt");
        assertNotNull(fullPrompt);

        @SuppressWarnings("unchecked")
        Map<String, Object> parsed = mapper.readValue(fullPrompt, Map.class);
        assertEquals("You are a senior technical architect. Respond with precise, professional language. Use industry terminology.", parsed.get("system"));
        assertEquals("Explain Conductor", parsed.get("user"));

        @SuppressWarnings("unchecked")
        List<Map<String, String>> fewShot = (List<Map<String, String>>) parsed.get("fewShot");
        assertEquals(2, fewShot.size());
        assertEquals("user", fewShot.get(0).get("role"));
        assertEquals("What is a database?", fewShot.get(0).get("content"));
        assertEquals("assistant", fewShot.get(1).get("role"));
        assertTrue(fewShot.get(1).get("content").contains("DBMS"));
    }

    @Test
    void buildsCasualPrompt() throws Exception {
        Task task = taskWith(Map.of("userPrompt", "Explain Conductor", "style", "casual", "model", "gpt-4"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("casual", result.getOutputData().get("style"));

        String fullPrompt = (String) result.getOutputData().get("fullPrompt");
        assertNotNull(fullPrompt);

        @SuppressWarnings("unchecked")
        Map<String, Object> parsed = mapper.readValue(fullPrompt, Map.class);
        assertEquals("You're a friendly dev buddy. Keep it chill and use simple words. Throw in an analogy if it helps.", parsed.get("system"));
        assertEquals("Explain Conductor", parsed.get("user"));

        @SuppressWarnings("unchecked")
        List<Map<String, String>> fewShot = (List<Map<String, String>>) parsed.get("fewShot");
        assertEquals(2, fewShot.size());
        assertTrue(fewShot.get(1).get("content").contains("filing cabinet"));
    }

    @Test
    void defaultsToFormalWhenStyleMissing() throws Exception {
        Task task = taskWith(new HashMap<>(Map.of("userPrompt", "Explain Conductor")));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("formal", result.getOutputData().get("style"));

        String fullPrompt = (String) result.getOutputData().get("fullPrompt");
        @SuppressWarnings("unchecked")
        Map<String, Object> parsed = mapper.readValue(fullPrompt, Map.class);
        assertTrue(parsed.get("system").toString().contains("senior technical architect"));
    }

    @Test
    void defaultsPromptWhenUserPromptMissing() throws Exception {
        Task task = taskWith(new HashMap<>(Map.of("style", "casual")));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());

        String fullPrompt = (String) result.getOutputData().get("fullPrompt");
        @SuppressWarnings("unchecked")
        Map<String, Object> parsed = mapper.readValue(fullPrompt, Map.class);
        assertEquals("Explain Conductor", parsed.get("user"));
    }

    @Test
    void outputContainsFullPromptAndStyle() {
        Task task = taskWith(Map.of("userPrompt", "What is Conductor?", "style", "formal", "model", "gpt-4"));
        TaskResult result = worker.execute(task);

        assertNotNull(result.getOutputData().get("fullPrompt"));
        assertNotNull(result.getOutputData().get("style"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
