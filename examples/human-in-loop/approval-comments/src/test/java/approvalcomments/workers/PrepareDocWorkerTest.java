package approvalcomments.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.*;

class PrepareDocWorkerTest {

    @Test
    void taskDefName() {
        PrepareDocWorker worker = new PrepareDocWorker();
        assertEquals("ac_prepare_doc", worker.getTaskDefName());
    }

    @Test
    void returnsReadyTrue() {
        PrepareDocWorker worker = new PrepareDocWorker();
        Task task = createTask();
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("ready"));
    }

    @Test
    void outputContainsReadyKey() {
        PrepareDocWorker worker = new PrepareDocWorker();
        Task task = createTask();
        TaskResult result = worker.execute(task);

        assertTrue(result.getOutputData().containsKey("ready"));
    }

    @Test
    void alwaysCompletesSuccessfully() {
        PrepareDocWorker worker = new PrepareDocWorker();
        for (int i = 0; i < 5; i++) {
            Task task = createTask();
            task.setWorkflowInstanceId("wf-" + i);
            TaskResult result = worker.execute(task);
            assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        }
    }

    private Task createTask() {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>());
        return task;
    }
}
