package workerpools.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class WplAssignPoolWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "wpl_assign_pool";
    }

    @Override
    public TaskResult execute(Task task) {
        String profile = (String) task.getInputData().getOrDefault("resourceProfile", "balanced");
        java.util.Map<String,String> poolMap = java.util.Map.of("high-cpu","compute-pool","high-io","io-pool","balanced","general-pool","gpu","gpu-pool");
        String pool = poolMap.getOrDefault(profile, "general-pool");
        String workerId = pool + "-worker-" + ((int)(Math.random() * 10) + 1);
        System.out.println("  [assign] Processing");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("assignedPool", pool);
        result.getOutputData().put("workerId", workerId);
        result.getOutputData().put("poolSize", 10);
        return result;
    }
}