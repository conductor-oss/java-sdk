package soc2automation.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class CollectControlsWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "soc2_collect_controls";
    }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("  [controls] Collected 48 controls for security");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.addOutputData("collect_controlsId", "COLLECT_CONTROLS-1361");
        result.addOutputData("success", true);
        return result;
    }
}
