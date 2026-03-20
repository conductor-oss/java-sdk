package votingworkflow.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CastBallotWorkerTest {

    @Test
    void testCastBallotWorker() {
        CastBallotWorker worker = new CastBallotWorker();
        assertEquals("vtw_cast_ballot", worker.getTaskDefName());

        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(Map.of("electionId", "ELEC-2024", "verified", true));

        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("BAL-voting-workflow-001", result.getOutputData().get("ballotId"));
    }
}
