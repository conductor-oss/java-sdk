package competingconsumers.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class CcsAcknowledgeWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "ccs_acknowledge";
    }

    @Override
    public TaskResult execute(Task task) {

        System.out.println("  [ack] Processing");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("acknowledged", true);
        result.getOutputData().put("removedFromQueue", true);
        return result;
    }
}