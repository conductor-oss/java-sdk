package numberporting.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class RequestWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "npt_request";
    }

    @Override
    public TaskResult execute(Task task) {

        String phoneNumber = (String) task.getInputData().get("phoneNumber");
        String toCarrier = (String) task.getInputData().get("toCarrier");
        System.out.printf("  [request] Port request for %s to %s%n", phoneNumber, toCarrier);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("portId", "PORT-number-porting-001");
        return result;
    }
}
