package votingworkflow.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class CountWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "vtw_count";
    }

    @Override
    public TaskResult execute(Task task) {
        String ballotId = (String) task.getInputData().get("ballotId");
        System.out.printf("  [count] Ballot %s counted%n", ballotId);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("totalVotes", 15432);
        result.getOutputData().put("counted", true);
        return result;
    }
}
