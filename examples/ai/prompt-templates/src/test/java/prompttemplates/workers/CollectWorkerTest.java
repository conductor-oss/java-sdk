package prompttemplates.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CollectWorkerTest {

    private final CollectWorker worker = new CollectWorker();

    @Test
    void taskDefName() {
        assertEquals("pt_collect", worker.getTaskDefName());
    }

    @Test
    void logsAndReturnsTrue() {
        Task task = taskWith(new HashMap<>(Map.of(
                "templateId", "summarize",
                "version", 2,
                "response", "Conductor is a workflow orchestration platform that enables durable execution of microservices and AI pipelines with built-in observability and retry mechanisms."
        )));

        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("logged"));
    }

    @Test
    void handlesShortResponse() {
        Task task = taskWith(new HashMap<>(Map.of(
                "templateId", "classify",
                "version", 1,
                "response", "positive"
        )));

        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("logged"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
