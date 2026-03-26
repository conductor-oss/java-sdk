package conditionalapproval.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ClassifyWorkerTest {

    @Test
    void taskDefName() {
        ClassifyWorker worker = new ClassifyWorker();
        assertEquals("car_classify", worker.getTaskDefName());
    }

    @Test
    void classifiesLowTierForAmountBelow1000() {
        ClassifyWorker worker = new ClassifyWorker();
        Task task = taskWith(Map.of("amount", 500));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("low", result.getOutputData().get("tier"));
    }

    @Test
    void classifiesLowTierForAmountZero() {
        ClassifyWorker worker = new ClassifyWorker();
        Task task = taskWith(Map.of("amount", 0));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("low", result.getOutputData().get("tier"));
    }

    @Test
    void classifiesLowTierForAmount999() {
        ClassifyWorker worker = new ClassifyWorker();
        Task task = taskWith(Map.of("amount", 999));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("low", result.getOutputData().get("tier"));
    }

    @Test
    void classifiesMediumTierForAmount1000() {
        ClassifyWorker worker = new ClassifyWorker();
        Task task = taskWith(Map.of("amount", 1000));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("medium", result.getOutputData().get("tier"));
    }

    @Test
    void classifiesMediumTierForAmount5000() {
        ClassifyWorker worker = new ClassifyWorker();
        Task task = taskWith(Map.of("amount", 5000));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("medium", result.getOutputData().get("tier"));
    }

    @Test
    void classifiesMediumTierForAmount9999() {
        ClassifyWorker worker = new ClassifyWorker();
        Task task = taskWith(Map.of("amount", 9999));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("medium", result.getOutputData().get("tier"));
    }

    @Test
    void classifiesHighTierForAmount10000() {
        ClassifyWorker worker = new ClassifyWorker();
        Task task = taskWith(Map.of("amount", 10000));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("high", result.getOutputData().get("tier"));
    }

    @Test
    void classifiesHighTierForAmount50000() {
        ClassifyWorker worker = new ClassifyWorker();
        Task task = taskWith(Map.of("amount", 50000));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("high", result.getOutputData().get("tier"));
    }

    @Test
    void classifiesLowTierWhenAmountMissing() {
        ClassifyWorker worker = new ClassifyWorker();
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("low", result.getOutputData().get("tier"));
    }

    @Test
    void outputContainsTier() {
        ClassifyWorker worker = new ClassifyWorker();
        Task task = taskWith(Map.of("amount", 5000));
        TaskResult result = worker.execute(task);

        assertTrue(result.getOutputData().containsKey("tier"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
