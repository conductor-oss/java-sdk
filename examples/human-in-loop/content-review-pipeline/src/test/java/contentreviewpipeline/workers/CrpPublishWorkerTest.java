package contentreviewpipeline.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CrpPublishWorkerTest {

    @Test
    void taskDefName() {
        CrpPublishWorker worker = new CrpPublishWorker();
        assertEquals("crp_publish", worker.getTaskDefName());
    }

    @Test
    void publishesWhenApproved() {
        CrpPublishWorker worker = new CrpPublishWorker();
        Task task = taskWith(Map.of("approved", true, "content", "Final content"));

        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("published"));
        assertEquals("https://example.com/articles/published", result.getOutputData().get("url"));
    }

    @Test
    void doesNotPublishWhenNotApproved() {
        CrpPublishWorker worker = new CrpPublishWorker();
        Task task = taskWith(Map.of("approved", false, "content", "Draft content"));

        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(false, result.getOutputData().get("published"));
        assertFalse(result.getOutputData().containsKey("url"));
    }

    @Test
    void doesNotPublishWhenApprovedMissing() {
        CrpPublishWorker worker = new CrpPublishWorker();
        Task task = taskWith(Map.of("content", "Some content"));

        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(false, result.getOutputData().get("published"));
    }

    @Test
    void alwaysCompletes() {
        CrpPublishWorker worker = new CrpPublishWorker();
        Task task = taskWith(Map.of("approved", true));

        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test
    void urlPresentOnlyWhenPublished() {
        CrpPublishWorker worker = new CrpPublishWorker();

        Task approvedTask = taskWith(Map.of("approved", true));
        TaskResult approvedResult = worker.execute(approvedTask);
        assertTrue(approvedResult.getOutputData().containsKey("url"));

        Task rejectedTask = taskWith(Map.of("approved", false));
        TaskResult rejectedResult = worker.execute(rejectedTask);
        assertFalse(rejectedResult.getOutputData().containsKey("url"));
    }

    @Test
    void outputContainsPublishedKey() {
        CrpPublishWorker worker = new CrpPublishWorker();
        Task task = taskWith(Map.of("approved", true));

        TaskResult result = worker.execute(task);

        assertTrue(result.getOutputData().containsKey("published"));
    }

    @Test
    void handlesNullApproved() {
        CrpPublishWorker worker = new CrpPublishWorker();
        Map<String, Object> input = new HashMap<>();
        input.put("approved", null);
        Task task = taskWith(input);

        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(false, result.getOutputData().get("published"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
