package networkmonitoring.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class RepairWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "nmn_repair";
    }

    @Override
    public TaskResult execute(Task task) {

        String networkSegment = (String) task.getInputData().get("networkSegment");
        System.out.printf("  [repair] Traffic rerouted on %s%n", networkSegment);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("repairId", "RPR-network-monitoring-001");
        result.getOutputData().put("repaired", true);
        result.getOutputData().put("action", "traffic-reroute");
        return result;
    }
}
