package performancereview.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class FinalizeWorkerTest {

    private final FinalizeWorker worker = new FinalizeWorker();

    @Test
    void taskDefName() {
        assertEquals("pfr_finalize", worker.getTaskDefName());
    }

    @Test
    void finalizesReview() {
        Task task = taskWith(Map.of("employeeId", "EMP-300", "calibratedRating", "4.1"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("REV-608", result.getOutputData().get("reviewId"));
        assertEquals(true, result.getOutputData().get("finalized"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
