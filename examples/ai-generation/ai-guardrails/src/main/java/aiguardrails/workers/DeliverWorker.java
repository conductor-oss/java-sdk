package aiguardrails.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class DeliverWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "grl_deliver";
    }

    @Override
    public TaskResult execute(Task task) {

        String userId = (String) task.getInputData().get("userId");
        System.out.printf("  [deliver] Safe response delivered to user %s%n", userId);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("delivered", true);
        return result;
    }
}
