package workerhealthchecks.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class WhcWorkerTest {

    @Test
    void taskDefName() {
        WhcWorker worker = new WhcWorker();
        assertEquals("whc_task", worker.getTaskDefName());
    }

    @Test
    void processesTaskAndReturnsResult() {
        WhcWorker worker = new WhcWorker();
        Task task = taskWith(Map.of("data", "hello"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("done-hello", result.getOutputData().get("result"));
    }

    @Test
    void incrementsPollCount() {
        WhcWorker worker = new WhcWorker();
        assertEquals(0, worker.getPollCount());

        worker.execute(taskWith(Map.of("data", "a")));
        assertEquals(1, worker.getPollCount());

        worker.execute(taskWith(Map.of("data", "b")));
        assertEquals(2, worker.getPollCount());

        worker.execute(taskWith(Map.of("data", "c")));
        assertEquals(3, worker.getPollCount());
    }

    @Test
    void incrementsCompletedCount() {
        WhcWorker worker = new WhcWorker();
        assertEquals(0, worker.getCompletedCount());

        worker.execute(taskWith(Map.of("data", "x")));
        assertEquals(1, worker.getCompletedCount());

        worker.execute(taskWith(Map.of("data", "y")));
        assertEquals(2, worker.getCompletedCount());
    }

    @Test
    void pollCountAndCompletedCountStayInSync() {
        WhcWorker worker = new WhcWorker();

        for (int i = 0; i < 5; i++) {
            worker.execute(taskWith(Map.of("data", "item-" + i)));
        }

        assertEquals(5, worker.getPollCount());
        assertEquals(5, worker.getCompletedCount());
    }

    @Test
    void outputIncludesCounters() {
        WhcWorker worker = new WhcWorker();
        Task task = taskWith(Map.of("data", "test"));
        TaskResult result = worker.execute(task);

        assertTrue(result.getOutputData().containsKey("pollCount"));
        assertTrue(result.getOutputData().containsKey("completedCount"));
        assertEquals(1, result.getOutputData().get("pollCount"));
        assertEquals(1, result.getOutputData().get("completedCount"));
    }

    @Test
    void handlesNullDataInput() {
        WhcWorker worker = new WhcWorker();
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("done-", result.getOutputData().get("result"));
        assertEquals(1, worker.getPollCount());
        assertEquals(1, worker.getCompletedCount());
    }

    @Test
    void handlesEmptyStringData() {
        WhcWorker worker = new WhcWorker();
        Task task = taskWith(Map.of("data", ""));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("done-", result.getOutputData().get("result"));
    }

    @Test
    void countersAreIndependentPerWorkerInstance() {
        WhcWorker worker1 = new WhcWorker();
        WhcWorker worker2 = new WhcWorker();

        worker1.execute(taskWith(Map.of("data", "a")));
        worker1.execute(taskWith(Map.of("data", "b")));
        worker2.execute(taskWith(Map.of("data", "c")));

        assertEquals(2, worker1.getPollCount());
        assertEquals(2, worker1.getCompletedCount());
        assertEquals(1, worker2.getPollCount());
        assertEquals(1, worker2.getCompletedCount());
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
