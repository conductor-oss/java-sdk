package agencymanagement.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class TrackWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "agm_track";
    }

    @Override
    public TaskResult execute(Task task) {

        String agentId = (String) task.getInputData().get("agentId");
        System.out.printf("  [track] Agent %s — 12 policies written%n", agentId);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("performance", java.util.Map.of("policiesWritten", 12, "premiumVolume", 48000));
        return result;
    }
}
