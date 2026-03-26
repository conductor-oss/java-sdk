package numberporting.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class ValidateWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "npt_validate";
    }

    @Override
    public TaskResult execute(Task task) {

        String phoneNumber = (String) task.getInputData().get("phoneNumber");
        String fromCarrier = (String) task.getInputData().get("fromCarrier");
        System.out.printf("  [validate] Number %s eligible for porting from %s%n", phoneNumber, fromCarrier);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("eligible", true);
        result.getOutputData().put("accountActive", true);
        return result;
    }
}
