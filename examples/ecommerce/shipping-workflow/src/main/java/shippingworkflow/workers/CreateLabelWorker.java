package shippingworkflow.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;

public class CreateLabelWorker implements Worker {

    private static final Random RNG = new Random();

    @Override
    public String getTaskDefName() {
        return "shp_create_label";
    }

    @Override
    public TaskResult execute(Task task) {
        String carrier = (String) task.getInputData().getOrDefault("carrier", "USPS");
        String prefix = "FedEx".equals(carrier) ? "FX" : "US";
        String trackingNumber = prefix + (1000000000L + RNG.nextLong(9000000000L));

        System.out.println("  [label] Created shipping label: " + trackingNumber
                + " (" + carrier + " " + task.getInputData().get("serviceType") + ")");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        Map<String, Object> output = new LinkedHashMap<>();
        output.put("trackingNumber", trackingNumber);
        output.put("labelUrl", "https://shipping.example.com/labels/" + trackingNumber + ".pdf");
        result.setOutputData(output);
        return result;
    }
}
