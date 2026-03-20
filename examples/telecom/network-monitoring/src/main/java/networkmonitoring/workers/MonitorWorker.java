package networkmonitoring.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class MonitorWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "nmn_monitor";
    }

    @Override
    public TaskResult execute(Task task) {

        String networkSegment = (String) task.getInputData().get("networkSegment");
        System.out.printf("  [monitor] Scanning network segment %s%n", networkSegment);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("metrics", java.util.Map.of("latency", 85, "packetLoss", 2.1, "uptime", 99.2));
        return result;
    }
}
