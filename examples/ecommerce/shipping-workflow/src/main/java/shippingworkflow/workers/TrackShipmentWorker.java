package shippingworkflow.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.time.Instant;
import java.util.*;

public class TrackShipmentWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "shp_track";
    }

    @Override
    public TaskResult execute(Task task) {
        String now = Instant.now().toString();
        List<Map<String, String>> events = List.of(
                Map.of("status", "picked_up", "location", "San Francisco, CA", "timestamp", now),
                Map.of("status", "in_transit", "location", "Memphis, TN", "timestamp", now),
                Map.of("status", "out_for_delivery", "location", "Austin, TX", "timestamp", now));

        System.out.println("  [track] " + task.getInputData().get("trackingNumber") + ": " + events.size() + " tracking events");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        Map<String, Object> output = new LinkedHashMap<>();
        output.put("events", events);
        output.put("currentStatus", "out_for_delivery");
        result.setOutputData(output);
        return result;
    }
}
