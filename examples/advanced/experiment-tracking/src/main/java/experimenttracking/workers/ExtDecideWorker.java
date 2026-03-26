package experimenttracking.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class ExtDecideWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "ext_decide";
    }

    @Override
    public TaskResult execute(Task task) {
        Object sig = task.getInputData().get("significant");
        boolean significant = Boolean.TRUE.equals(sig) || "true".equals(String.valueOf(sig));
        String decision = significant ? "adopt" : "reject";
        System.out.println("  [decide] Processing");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("decision", decision);
        return result;
    }
}