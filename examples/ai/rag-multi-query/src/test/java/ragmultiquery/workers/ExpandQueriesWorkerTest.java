package ragmultiquery.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ExpandQueriesWorkerTest {

    private final ExpandQueriesWorker worker = new ExpandQueriesWorker();

    @Test
    void taskDefName() {
        assertEquals("mq_expand_queries", worker.getTaskDefName());
    }

    @Test
    void returnsThreeQueryVariants() {
        Task task = taskWith(new HashMap<>(Map.of("question", "Why should I use workflow orchestration?")));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());

        @SuppressWarnings("unchecked")
        List<String> queries = (List<String>) result.getOutputData().get("queries");
        assertNotNull(queries);
        assertEquals(3, queries.size());
    }

    @Test
    void queriesContainExpectedContent() {
        Task task = taskWith(new HashMap<>(Map.of("question", "test")));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<String> queries = (List<String>) result.getOutputData().get("queries");
        assertTrue(queries.get(0).contains("benefits"));
        assertTrue(queries.get(1).contains("Conductor"));
        assertTrue(queries.get(2).contains("orchestration"));
    }

    @Test
    void handlesEmptyQuestion() {
        Task task = taskWith(new HashMap<>(Map.of("question", "")));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("queries"));
    }

    @Test
    void handlesMissingQuestion() {
        Task task = taskWith(new HashMap<>());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("queries"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
