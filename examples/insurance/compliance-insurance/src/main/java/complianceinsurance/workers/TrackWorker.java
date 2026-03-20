package complianceinsurance.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class TrackWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "cpi_track";
    }

    @Override
    public TaskResult execute(Task task) {

        System.out.println("  [track] Remediation items tracked — all resolved");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("allResolved", true);
        result.getOutputData().put("resolvedCount", 2);
        return result;
    }
}
