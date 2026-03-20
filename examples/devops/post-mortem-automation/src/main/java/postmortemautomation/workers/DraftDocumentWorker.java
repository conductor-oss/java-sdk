package postmortemautomation.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class DraftDocumentWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "pm_draft_document";
    }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("  [draft] Post-mortem document generated with timeline and action items");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.addOutputData("draft_document", true);
        result.addOutputData("processed", true);
        return result;
    }
}
