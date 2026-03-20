package mistralai.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class MistralExtractAnswerWorkerTest {

    private final MistralExtractAnswerWorker worker = new MistralExtractAnswerWorker();

    @Test
    void taskDefName() {
        assertEquals("mistral_extract_answer", worker.getTaskDefName());
    }

    @Test
    void extractsAnswerFromChatResponse() {
        Map<String, Object> chatResponse = buildChatResponse("This is the extracted answer.");

        Task task = taskWith(new HashMap<>(Map.of("chatResponse", chatResponse)));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("This is the extracted answer.", result.getOutputData().get("answer"));
    }

    @Test
    void extractsFullContractAnalysis() {
        String content = "Key contract analysis:\n\n**Obligations:**\n1. Licensee must comply.";
        Map<String, Object> chatResponse = buildChatResponse(content);

        Task task = taskWith(new HashMap<>(Map.of("chatResponse", chatResponse)));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(content, result.getOutputData().get("answer"));
    }

    @Test
    void outputContainsOnlyAnswerField() {
        Map<String, Object> chatResponse = buildChatResponse("Some answer");

        Task task = taskWith(new HashMap<>(Map.of("chatResponse", chatResponse)));
        TaskResult result = worker.execute(task);

        assertEquals(1, result.getOutputData().size());
        assertTrue(result.getOutputData().containsKey("answer"));
    }

    private Map<String, Object> buildChatResponse(String content) {
        Map<String, Object> message = new HashMap<>(Map.of(
                "role", "assistant",
                "content", content
        ));
        Map<String, Object> choice = new HashMap<>(Map.of(
                "index", 0,
                "message", message,
                "finish_reason", "stop"
        ));
        return new HashMap<>(Map.of(
                "id", "cmpl-mst-abc123xyz",
                "object", "chat.completion",
                "model", "mistral-large-latest",
                "choices", List.of(choice)
        ));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
