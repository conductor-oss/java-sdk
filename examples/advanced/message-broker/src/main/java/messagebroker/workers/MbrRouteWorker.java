package messagebroker.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class MbrRouteWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "mbr_route";
    }

    @Override
    public TaskResult execute(Task task) {
        String topic = (String) task.getInputData().getOrDefault("topic", "default");
        java.util.Map<String,String> routes = java.util.Map.of("orders","order-service","payments","payment-service","notifications","notification-service");
        String destination = routes.getOrDefault(topic, "default-service");
        System.out.println("  [route] Processing");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("destination", destination);
        result.getOutputData().put("routedAt", java.time.Instant.now().toString());
        return result;
    }
}