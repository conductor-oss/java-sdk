package networkmonitoring.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class VerifyWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "nmn_verify";
    }

    @Override
    public TaskResult execute(Task task) {

        String networkSegment = (String) task.getInputData().get("networkSegment");
        System.out.printf("  [verify] Segment %s healthy — latency normal%n", networkSegment);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("verified", true);
        result.getOutputData().put("latency", 12);
        return result;
    }
}
