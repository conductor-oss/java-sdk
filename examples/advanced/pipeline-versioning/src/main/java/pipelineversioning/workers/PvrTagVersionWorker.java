package pipelineversioning.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class PvrTagVersionWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "pvr_tag_version";
    }

    @Override
    public TaskResult execute(Task task) {
        String versionTag = (String) task.getInputData().getOrDefault("versionTag", "v1");
        System.out.println("  [tag] Processing");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("tagged", true);
        result.getOutputData().put("versionTag", versionTag);
        return result;
    }
}