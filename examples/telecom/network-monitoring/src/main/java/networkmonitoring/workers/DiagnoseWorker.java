package networkmonitoring.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class DiagnoseWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "nmn_diagnose";
    }

    @Override
    public TaskResult execute(Task task) {

        System.out.println("  [diagnose] Root cause: congested switch at node SW-14");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("diagnosis", "congested-switch");
        result.getOutputData().put("node", "SW-14");
        return result;
    }
}
