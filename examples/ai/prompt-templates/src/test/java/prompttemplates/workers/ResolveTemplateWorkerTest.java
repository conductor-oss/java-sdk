package prompttemplates.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ResolveTemplateWorkerTest {

    private final ResolveTemplateWorker worker = new ResolveTemplateWorker();

    @Test
    void taskDefName() {
        assertEquals("pt_resolve_template", worker.getTaskDefName());
    }

    @Test
    void resolveSummarizeTemplate() {
        Task task = taskWith(new HashMap<>(Map.of(
                "templateId", "summarize",
                "templateVersion", 2,
                "variables", new HashMap<>(Map.of(
                        "format", "technical document",
                        "topic", "Conductor Orchestration",
                        "audience", "developers",
                        "length", "concise"
                ))
        )));

        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("summarize:v2", result.getOutputData().get("templateKey"));

        String resolved = (String) result.getOutputData().get("resolvedPrompt");
        assertNotNull(resolved);
        assertTrue(resolved.contains("technical document"));
        assertTrue(resolved.contains("Conductor Orchestration"));
        assertTrue(resolved.contains("developers"));
        assertTrue(resolved.contains("concise"));
        assertFalse(resolved.contains("{{"));
        assertEquals(
                "Summarize the following technical document:\n\nTopic: Conductor Orchestration\n"
                + "Audience: developers\n\nProvide a concise summary.",
                resolved
        );
    }

    @Test
    void resolveClassifyTemplate() {
        Task task = taskWith(new HashMap<>(Map.of(
                "templateId", "classify",
                "templateVersion", 1,
                "variables", new HashMap<>(Map.of(
                        "categories", "positive, negative, neutral",
                        "text", "Conductor is great"
                ))
        )));

        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("classify:v1", result.getOutputData().get("templateKey"));

        String resolved = (String) result.getOutputData().get("resolvedPrompt");
        assertTrue(resolved.contains("positive, negative, neutral"));
        assertTrue(resolved.contains("Conductor is great"));
        assertFalse(resolved.contains("{{"));
    }

    @Test
    void failsWhenTemplateNotFound() {
        Task task = taskWith(new HashMap<>(Map.of(
                "templateId", "nonexistent",
                "templateVersion", 1,
                "variables", new HashMap<>(Map.of())
        )));

        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.FAILED, result.getStatus());
        String error = (String) result.getOutputData().get("error");
        assertNotNull(error);
        assertTrue(error.contains("nonexistent:v1"));
    }

    @Test
    void failsWhenVersionDoesNotMatch() {
        Task task = taskWith(new HashMap<>(Map.of(
                "templateId", "summarize",
                "templateVersion", 99,
                "variables", new HashMap<>(Map.of())
        )));

        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.FAILED, result.getStatus());
        assertTrue(((String) result.getOutputData().get("error")).contains("summarize:v99"));
    }

    @Test
    void templateStoreContainsExpectedEntries() {
        Map<String, ResolveTemplateWorker.PromptTemplate> store = ResolveTemplateWorker.getTemplateStore();
        assertEquals(2, store.size());
        assertTrue(store.containsKey("summarize:v2"));
        assertTrue(store.containsKey("classify:v1"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
