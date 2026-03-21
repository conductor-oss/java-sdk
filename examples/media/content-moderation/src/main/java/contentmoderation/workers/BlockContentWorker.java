package contentmoderation.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class BlockContentWorker implements Worker {
    @Override public String getTaskDefName() { return "mod_block_content"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [block] Processing " + task.getInputData().getOrDefault("blocked", ""));
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("blocked", true);
        r.getOutputData().put("userNotified", true);
        r.getOutputData().put("appealAvailable", true);
        r.getOutputData().put("blockId", "BLK-516-001");
        return r;
    }
}
