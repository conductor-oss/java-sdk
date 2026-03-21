package hronboarding.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class TrainingWorkerTest {

    private final TrainingWorker worker = new TrainingWorker();

    @Test
    void taskDefName() {
        assertEquals("hro_training", worker.getTaskDefName());
    }

    @Test
    void createsTrainingPlan() {
        Task task = taskWith(Map.of("employeeId", "EMP-605"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("TRN-605", result.getOutputData().get("planId"));
        assertEquals(5, result.getOutputData().get("courses"));
        assertEquals(2, result.getOutputData().get("durationWeeks"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
