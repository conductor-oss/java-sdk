package correlationpattern.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class CrpMatchByIdWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "crp_match_by_id";
    }

    @Override
    public TaskResult execute(Task task) {

        System.out.println("  [match] Processing");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("correlatedGroups", java.util.Map.of("TXN-001", java.util.List.of("msg1","msg2"), "TXN-002", java.util.List.of("msg3","msg4")));
        result.getOutputData().put("groupCount", 3);
        return result;
    }
}