package inspectionworkflow.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.List;

public class DocumentWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "inw_document";
    }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("  [document] Findings documented — all systems pass");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("result", "pass");
        result.getOutputData().put("violations", List.of());
        return result;
    }
}
