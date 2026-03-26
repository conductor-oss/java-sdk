package soc2automation.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class GenerateEvidenceWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "soc2_generate_evidence";
    }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("  [evidence] Evidence package generated for auditor");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.addOutputData("generate_evidence", true);
        return result;
    }
}
