package projectclosure.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class ArchiveWorker implements Worker {
    @Override public String getTaskDefName() { return "pcl_archive"; }

    @Override
    public TaskResult execute(Task task) {
        String projectId = (String) task.getInputData().get("projectId");
        System.out.println("  [Archive] Archiving project " + projectId + " artifacts");
        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("archiveId", "ARC-909");
        result.getOutputData().put("location", "s3://project-archives/" + projectId);
        result.getOutputData().put("archived", true);
        return result;
    }
}
