package dataversioning.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class DvrTagWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "dvr_tag";
    }

    @Override
    public TaskResult execute(Task task) {
        String tagName = (String) task.getInputData().getOrDefault("tagName", "v1");
        System.out.println("  [tag] Processing");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("tagged", true);
        result.getOutputData().put("tagName", tagName);
        return result;
    }
}