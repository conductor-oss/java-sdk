package splitterpattern.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class SplReceiveCompositeWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "spl_receive_composite";
    }

    @Override
    public TaskResult execute(Task task) {
        Object msg = task.getInputData().getOrDefault("compositeMessage", java.util.Map.of());
        System.out.println("  [receive] Processing");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("compositeMessage", msg);
        return result;
    }
}