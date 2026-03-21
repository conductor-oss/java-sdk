package selfrag.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class GradeHallucinationWorkerTest {

    private final GradeHallucinationWorker worker = new GradeHallucinationWorker();

    @Test
    void taskDefName() {
        assertEquals("sr_grade_hallucination", worker.getTaskDefName());
    }

    @Test
    void returnsScore092() {
        Task task = taskWith(new HashMap<>(Map.of("answer", "some answer", "sourceDocs", "docs")));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(0.92, ((Number) result.getOutputData().get("score")).doubleValue(), 0.001);
    }

    @Test
    void returnsGroundedTrue() {
        Task task = taskWith(new HashMap<>(Map.of("answer", "some answer", "sourceDocs", "docs")));
        TaskResult result = worker.execute(task);

        assertEquals(true, result.getOutputData().get("grounded"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
