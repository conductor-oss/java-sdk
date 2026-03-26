package ragsql.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ParseNlWorkerTest {

    private final ParseNlWorker worker = new ParseNlWorker();

    @Test
    void taskDefName() {
        assertEquals("sq_parse_nl", worker.getTaskDefName());
    }

    @Test
    void returnsAggregateIntent() {
        Task task = taskWith(new HashMap<>(Map.of(
                "question", "What are the most executed workflows?",
                "dbSchema", "workflow_executions(id, workflow_name, status)"
        )));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("aggregate_query", result.getOutputData().get("intent"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void returnsEntitiesWithAllFields() {
        Task task = taskWith(new HashMap<>(Map.of(
                "question", "Show top workflows by count",
                "dbSchema", "workflow_executions(id, workflow_name, status)"
        )));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());

        Map<String, String> entities = (Map<String, String>) result.getOutputData().get("entities");
        assertNotNull(entities);
        assertEquals("workflow_executions", entities.get("table"));
        assertEquals("execution_count", entities.get("metric"));
        assertEquals("status = 'COMPLETED'", entities.get("filter"));
        assertEquals("workflow_name", entities.get("groupBy"));
        assertEquals("last 7 days", entities.get("timeRange"));
    }

    @Test
    void handlesNullQuestion() {
        Task task = taskWith(new HashMap<>());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("intent"));
        assertNotNull(result.getOutputData().get("entities"));
    }

    @Test
    void handlesNullDbSchema() {
        Task task = taskWith(new HashMap<>(Map.of("question", "test")));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("entities"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
