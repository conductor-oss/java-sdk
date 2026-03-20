package prompttemplates.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CallLlmWorkerTest {

    private final CallLlmWorker worker = new CallLlmWorker();

    @Test
    void taskDefName() {
        assertEquals("pt_call_llm", worker.getTaskDefName());
    }

    @Test
    void returnsFixedResponse() {
        Task task = taskWith(new HashMap<>(Map.of(
                "prompt", "Summarize the following technical document:\n\nTopic: Conductor\nAudience: developers\n\nProvide a concise summary.",
                "model", "gpt-4"
        )));

        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(
                "Conductor is a workflow orchestration platform that enables durable execution "
                + "of microservices and AI pipelines with built-in observability and retry mechanisms.",
                result.getOutputData().get("response")
        );
        assertEquals(42, result.getOutputData().get("tokens"));
    }

    @Test
    void responseSameRegardlessOfModel() {
        Task task1 = taskWith(new HashMap<>(Map.of("prompt", "Hello", "model", "gpt-4")));
        Task task2 = taskWith(new HashMap<>(Map.of("prompt", "Hello", "model", "claude-3")));

        TaskResult result1 = worker.execute(task1);
        TaskResult result2 = worker.execute(task2);

        assertEquals(result1.getOutputData().get("response"), result2.getOutputData().get("response"));
        assertEquals(result1.getOutputData().get("tokens"), result2.getOutputData().get("tokens"));
    }

    @Test
    void outputContainsResponseAndTokens() {
        Task task = taskWith(new HashMap<>(Map.of("prompt", "Test prompt", "model", "gpt-4")));

        TaskResult result = worker.execute(task);

        assertTrue(result.getOutputData().containsKey("response"));
        assertTrue(result.getOutputData().containsKey("tokens"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
