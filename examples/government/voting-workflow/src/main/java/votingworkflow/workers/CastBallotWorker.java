package votingworkflow.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class CastBallotWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "vtw_cast_ballot";
    }

    @Override
    public TaskResult execute(Task task) {
        String electionId = (String) task.getInputData().get("electionId");
        System.out.printf("  [cast] Ballot cast for election %s%n", electionId);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("ballotId", "BAL-voting-workflow-001");
        result.getOutputData().put("recorded", true);
        return result;
    }
}
