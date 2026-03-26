package governmentpermit.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class IssueWorkerTest {

    @Test
    void testIssueWorker() {
        IssueWorker worker = new IssueWorker();
        assertEquals("gvp_issue", worker.getTaskDefName());

        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(Map.of("applicantId", "CIT-100", "permitType", "building"));

        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("GOV-2024-government-permit", result.getOutputData().get("permitNumber"));
        assertEquals(true, result.getOutputData().get("issued"));
    }
}
