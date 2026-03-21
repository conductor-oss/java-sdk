package competingconsumers.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class CcsPublishWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "ccs_publish";
    }

    @Override
    public TaskResult execute(Task task) {
        String msgId = "MSG-" + Long.toString(System.currentTimeMillis(), 36);
        System.out.println("  [publish] Processing");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("messageId", msgId);
        result.getOutputData().put("published", true);
        return result;
    }
}