package patentfiling.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class DraftWorker implements Worker {
    @Override public String getTaskDefName() { return "ptf_draft"; }

    @Override public TaskResult execute(Task task) {
        String title = (String) task.getInputData().get("inventionTitle");
        System.out.println("  [draft] Drafting patent application for: " + title);
        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("draftId", "DRF-" + System.currentTimeMillis());
        result.getOutputData().put("claims", 12);
        return result;
    }
}
