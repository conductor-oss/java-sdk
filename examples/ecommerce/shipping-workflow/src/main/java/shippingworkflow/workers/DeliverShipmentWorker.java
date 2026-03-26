package shippingworkflow.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

public class DeliverShipmentWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "shp_deliver";
    }

    @SuppressWarnings("unchecked")
    @Override
    public TaskResult execute(Task task) {
        String city = "destination";
        Object dest = task.getInputData().get("destination");
        if (dest instanceof Map) {
            Object c = ((Map<String, Object>) dest).get("city");
            if (c != null) city = c.toString();
        }

        System.out.println("  [deliver] " + task.getInputData().get("trackingNumber") + ": delivered to " + city);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        Map<String, Object> output = new LinkedHashMap<>();
        output.put("delivered", true);
        output.put("deliveredAt", Instant.now().toString());
        output.put("signedBy", "Recipient");
        result.setOutputData(output);
        return result;
    }
}
