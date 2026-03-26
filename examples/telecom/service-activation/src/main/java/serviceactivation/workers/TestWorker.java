package serviceactivation.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class TestWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "sac_test";
    }

    @Override
    public TaskResult execute(Task task) {

        String serviceId = (String) task.getInputData().get("serviceId");
        System.out.printf("  [test] Service %s — connectivity test passed%n", serviceId);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("passed", true);
        result.getOutputData().put("latency", 5);
        result.getOutputData().put("bandwidth", "200Mbps");
        return result;
    }
}
