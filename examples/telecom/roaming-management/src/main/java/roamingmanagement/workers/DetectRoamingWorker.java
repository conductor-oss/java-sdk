package roamingmanagement.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class DetectRoamingWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "rmg_detect_roaming";
    }

    @Override
    public TaskResult execute(Task task) {

        String subscriberId = (String) task.getInputData().get("subscriberId");
        String visitedNetwork = (String) task.getInputData().get("visitedNetwork");
        System.out.printf("  [detect] Subscriber %s roaming on %s%n", subscriberId, visitedNetwork);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("usage", java.util.Map.of("voice", 45, "data", 2.1, "sms", 10));
        result.getOutputData().put("country", "DE");
        return result;
    }
}
