package numberporting.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class PortWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "npt_port";
    }

    @Override
    public TaskResult execute(Task task) {

        String phoneNumber = (String) task.getInputData().get("phoneNumber");
        System.out.printf("  [port] Number %s ported successfully%n", phoneNumber);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("ported", true);
        result.getOutputData().put("completedAt", "2024-03-12T03:15:00Z");
        return result;
    }
}
