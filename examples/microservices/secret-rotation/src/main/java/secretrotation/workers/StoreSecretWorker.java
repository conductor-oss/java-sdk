package secretrotation.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class StoreSecretWorker implements Worker {
    @Override public String getTaskDefName() { return "sr_store_secret"; }

    @Override public TaskResult execute(Task task) {
        String secretId = (String) task.getInputData().getOrDefault("secretId", "unknown");
        String vault = (String) task.getInputData().getOrDefault("vault", "vault");
        String secretName = (String) task.getInputData().getOrDefault("secretName", "unknown");
        System.out.println("  [store] Storing secret " + secretId + " in " + vault + "...");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("vaultPath", "secret/data/" + secretName);
        result.getOutputData().put("version", 3);
        result.getOutputData().put("stored", true);
        return result;
    }
}
