package customerchurn.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class TrackWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "ccn_track";
    }

    @Override
    public TaskResult execute(Task task) {

        String customerId = (String) task.getInputData().get("customerId");
        System.out.printf("  [track] Customer %s accepted offer — retained%n", customerId);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("retained", true);
        result.getOutputData().put("acceptedAt", "2024-03-12");
        return result;
    }
}
