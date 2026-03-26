package selfrag.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class GradeUsefulnessWorkerTest {

    private final GradeUsefulnessWorker worker = new GradeUsefulnessWorker();

    @Test
    void taskDefName() {
        assertEquals("sr_grade_usefulness", worker.getTaskDefName());
    }

    @Test
    void returnsPassWhenBothScoresHigh() {
        Task task = taskWith(new HashMap<>(Map.of(
                "question", "test",
                "answer", "some answer",
                "halScore", 0.92)));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(0.88, ((Number) result.getOutputData().get("score")).doubleValue(), 0.001);
        assertEquals("pass", result.getOutputData().get("verdict"));
    }

    @Test
    void returnsFailWhenHalScoreLow() {
        Task task = taskWith(new HashMap<>(Map.of(
                "question", "test",
                "answer", "some answer",
                "halScore", 0.3)));
        TaskResult result = worker.execute(task);

        assertEquals("fail", result.getOutputData().get("verdict"));
    }

    @Test
    void returnsPassWhenHalScoreExactly07() {
        Task task = taskWith(new HashMap<>(Map.of(
                "question", "test",
                "answer", "some answer",
                "halScore", 0.7)));
        TaskResult result = worker.execute(task);

        assertEquals("pass", result.getOutputData().get("verdict"));
    }

    @Test
    void halScoreAsIntegerInput() {
        Task task = taskWith(new HashMap<>(Map.of(
                "question", "test",
                "answer", "some answer",
                "halScore", 1)));
        TaskResult result = worker.execute(task);

        assertEquals("pass", result.getOutputData().get("verdict"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
