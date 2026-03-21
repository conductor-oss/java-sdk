package ragmultiquery.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class GenerateAnswerWorkerTest {

    private final GenerateAnswerWorker worker = new GenerateAnswerWorker();

    @Test
    void taskDefName() {
        assertEquals("mq_generate_answer", worker.getTaskDefName());
    }

    @Test
    void generatesAnswerWithContext() {
        List<String> context = new ArrayList<>(List.of(
                "Workflow orchestration provides centralized control and visibility.",
                "Orchestration enables retry, timeout, and compensation logic.",
                "Conductor decouples task execution from workflow logic."
        ));

        Task task = taskWith(new HashMap<>(Map.of(
                "question", "Why should I use workflow orchestration?",
                "context", context
        )));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());

        String answer = (String) result.getOutputData().get("answer");
        assertNotNull(answer);
        assertTrue(answer.contains("Conductor"));
        assertTrue(answer.contains("orchestration"));
    }

    @Test
    void returnsTokensUsed() {
        List<String> context = new ArrayList<>(List.of("Some doc text"));

        Task task = taskWith(new HashMap<>(Map.of(
                "question", "test",
                "context", context
        )));
        TaskResult result = worker.execute(task);

        assertNotNull(result.getOutputData().get("tokensUsed"));
    }

    @Test
    void handlesNullContext() {
        Task task = taskWith(new HashMap<>(Map.of(
                "question", "test"
        )));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("answer"));
    }

    @Test
    void answerMentionsKeyTopics() {
        List<String> context = new ArrayList<>(List.of("doc1", "doc2"));

        Task task = taskWith(new HashMap<>(Map.of(
                "question", "test",
                "context", context
        )));
        TaskResult result = worker.execute(task);

        String answer = (String) result.getOutputData().get("answer");
        assertTrue(answer.contains("retries"));
        assertTrue(answer.contains("choreography"));
        assertTrue(answer.contains("hidden dependencies"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
