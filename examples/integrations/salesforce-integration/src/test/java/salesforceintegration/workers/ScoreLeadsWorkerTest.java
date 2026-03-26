package salesforceintegration.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ScoreLeadsWorkerTest {

    private final ScoreLeadsWorker worker = new ScoreLeadsWorker();

    @Test
    void taskDefName() {
        assertEquals("sfc_score_leads", worker.getTaskDefName());
    }

    @Test
    void executes() {
        Task task = taskWith(Map.of("leads", List.of(Map.of("id", "l1", "name", "Acme")), "model", "ml-v2"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(1, result.getOutputData().get("scoredCount"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
