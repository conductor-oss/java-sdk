package multiagentresearch.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class DefineResearchWorkerTest {

    private final DefineResearchWorker worker = new DefineResearchWorker();

    @Test
    void taskDefName() {
        assertEquals("ra_define_research", worker.getTaskDefName());
    }

    @Test
    void returnsSearchQueriesDomainsDatabases() {
        Task task = taskWith(Map.of("topic", "AI in healthcare", "depth", "comprehensive"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());

        @SuppressWarnings("unchecked")
        List<String> queries = (List<String>) result.getOutputData().get("searchQueries");
        assertNotNull(queries);
        assertEquals(3, queries.size());
        assertTrue(queries.get(0).contains("AI in healthcare"));

        @SuppressWarnings("unchecked")
        List<String> domains = (List<String>) result.getOutputData().get("domains");
        assertNotNull(domains);
        assertFalse(domains.isEmpty());

        @SuppressWarnings("unchecked")
        List<String> databases = (List<String>) result.getOutputData().get("databases");
        assertNotNull(databases);
        assertFalse(databases.isEmpty());

        assertEquals("comprehensive", result.getOutputData().get("scope"));
    }

    @Test
    void searchQueriesIncludeTopicInEach() {
        Task task = taskWith(Map.of("topic", "quantum computing", "depth", "deep"));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<String> queries = (List<String>) result.getOutputData().get("searchQueries");
        for (String query : queries) {
            assertTrue(query.contains("quantum computing"),
                    "Each query should contain the topic");
        }
    }

    @Test
    void scopeIsAlwaysComprehensive() {
        Task task = taskWith(Map.of("topic", "testing", "depth", "shallow"));
        TaskResult result = worker.execute(task);

        assertEquals("comprehensive", result.getOutputData().get("scope"));
    }

    @Test
    void handlesNullTopic() {
        Map<String, Object> input = new HashMap<>();
        input.put("topic", null);
        input.put("depth", "standard");
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("searchQueries"));
    }

    @Test
    void handlesMissingTopic() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("searchQueries"));
    }

    @Test
    void handlesBlankTopic() {
        Task task = taskWith(Map.of("topic", "   "));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("searchQueries"));
    }

    @Test
    void handlesNullDepth() {
        Map<String, Object> input = new HashMap<>();
        input.put("topic", "some topic");
        input.put("depth", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test
    void outputContainsAllExpectedKeys() {
        Task task = taskWith(Map.of("topic", "test", "depth", "standard"));
        TaskResult result = worker.execute(task);

        assertTrue(result.getOutputData().containsKey("searchQueries"));
        assertTrue(result.getOutputData().containsKey("domains"));
        assertTrue(result.getOutputData().containsKey("databases"));
        assertTrue(result.getOutputData().containsKey("scope"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
