package dataqualitychecks.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class GenerateReportWorkerTest {

    private final GenerateReportWorker worker = new GenerateReportWorker();

    @Test
    void taskDefName() {
        assertEquals("qc_generate_report", worker.getTaskDefName());
    }

    @Test
    void gradeAForHighScores() {
        Task task = taskWith(Map.of("completeness", 0.95, "accuracy", 0.92, "consistency", 1.0, "totalRecords", 10));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("A", result.getOutputData().get("grade"));
    }

    @Test
    void gradeBForMediumScores() {
        Task task = taskWith(Map.of("completeness", 0.85, "accuracy", 0.80, "consistency", 0.82, "totalRecords", 10));
        TaskResult result = worker.execute(task);

        assertEquals("B", result.getOutputData().get("grade"));
    }

    @Test
    void gradeDForLowScores() {
        Task task = taskWith(Map.of("completeness", 0.5, "accuracy", 0.4, "consistency", 0.3, "totalRecords", 10));
        TaskResult result = worker.execute(task);

        assertEquals("D", result.getOutputData().get("grade"));
    }

    @Test
    void overallIsAverage() {
        Task task = taskWith(Map.of("completeness", 0.9, "accuracy", 0.9, "consistency", 0.9, "totalRecords", 5));
        TaskResult result = worker.execute(task);

        assertEquals(0.9, result.getOutputData().get("overallScore"));
    }

    @Test
    void handlesZeroScores() {
        Task task = taskWith(Map.of("completeness", 0.0, "accuracy", 0.0, "consistency", 0.0, "totalRecords", 0));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("D", result.getOutputData().get("grade"));
        assertEquals(0.0, result.getOutputData().get("overallScore"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
