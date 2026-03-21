package searchagent.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class FormulateQueriesWorkerTest {

    private final FormulateQueriesWorker worker = new FormulateQueriesWorker();

    @Test
    void taskDefName() {
        assertEquals("sa_formulate_queries", worker.getTaskDefName());
    }

    @Test
    void returnsThreeQueries() {
        Task task = taskWith(Map.of(
                "question", "What is quantum computing?",
                "maxResults", 5));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());

        @SuppressWarnings("unchecked")
        List<String> queries = (List<String>) result.getOutputData().get("queries");
        assertNotNull(queries);
        assertEquals(3, queries.size());
    }

    @Test
    void returnsFactualResearchIntentForWhatQuestion() {
        Task task = taskWith(Map.of(
                "question", "What is the current state of quantum computing in 2026?",
                "maxResults", 5));
        TaskResult result = worker.execute(task);

        assertEquals("factual_research", result.getOutputData().get("intent"));
    }

    @Test
    void returnsHowToIntentForHowQuestion() {
        Task task = taskWith(Map.of(
                "question", "How does quantum error correction work?",
                "maxResults", 3));
        TaskResult result = worker.execute(task);

        assertEquals("how_to", result.getOutputData().get("intent"));
    }

    @Test
    void returnsHighComplexityForLongQuestion() {
        Task task = taskWith(Map.of(
                "question", "What is the current state of quantum computing in 2026?",
                "maxResults", 5));
        TaskResult result = worker.execute(task);

        assertEquals("high", result.getOutputData().get("complexity"));
    }

    @Test
    void returnsOriginalQuestionInOutput() {
        String question = "What is quantum computing?";
        Task task = taskWith(Map.of("question", question, "maxResults", 5));
        TaskResult result = worker.execute(task);

        assertEquals(question, result.getOutputData().get("originalQuestion"));
    }

    @Test
    void handlesNullQuestion() {
        Map<String, Object> input = new HashMap<>();
        input.put("question", null);
        input.put("maxResults", 5);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("queries"));
    }

    @Test
    void handlesBlankQuestion() {
        Task task = taskWith(Map.of("question", "   ", "maxResults", 3));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("queries"));
    }

    @Test
    void defaultsMaxResultsWhenMissing() {
        Task task = taskWith(Map.of("question", "Test query"));
        TaskResult result = worker.execute(task);

        assertEquals(5, result.getOutputData().get("maxResults"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
