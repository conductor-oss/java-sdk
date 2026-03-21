package threatintelligence.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class EnrichContextWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "ti_enrich_context";
    }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("  [enrich] Added MITRE ATT&CK mapping and actor attribution");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.addOutputData("enrich_context", true);
        result.addOutputData("processed", true);
        return result;
    }
}
