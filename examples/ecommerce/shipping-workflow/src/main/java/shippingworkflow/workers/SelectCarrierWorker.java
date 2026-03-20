package shippingworkflow.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.LinkedHashMap;
import java.util.Map;

public class SelectCarrierWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "shp_select_carrier";
    }

    @Override
    public TaskResult execute(Task task) {
        String speed = (String) task.getInputData().getOrDefault("speed", "standard");
        String carrier, serviceType;
        double cost;

        if ("express".equals(speed)) {
            carrier = "FedEx"; serviceType = "Priority Overnight"; cost = 29.99;
        } else {
            carrier = "USPS"; serviceType = "Priority Mail"; cost = 12.99;
        }

        System.out.println("  [carrier] Selected " + carrier + " " + serviceType
                + " ($" + cost + ") for " + task.getInputData().get("weight") + "lbs");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        Map<String, Object> output = new LinkedHashMap<>();
        output.put("carrier", carrier);
        output.put("serviceType", serviceType);
        output.put("cost", cost);
        result.setOutputData(output);
        return result;
    }
}
