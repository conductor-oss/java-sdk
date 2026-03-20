package multicluster.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class MclClusterWestWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "mcl_cluster_west";
    }

    @Override
    public TaskResult execute(Task task) {
        String cluster = (String) task.getInputData().getOrDefault("cluster", "us-west-2");
        System.out.println("  [west] Processing");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("processed", true);
        result.getOutputData().put("recordCount", 50000);
        result.getOutputData().put("latencyMs", 1180);
        return result;
    }
}