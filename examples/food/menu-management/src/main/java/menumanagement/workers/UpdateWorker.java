package menumanagement.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.Map;

public class UpdateWorker implements Worker {
    @Override public String getTaskDefName() { return "mnu_update"; }

    @Override public TaskResult execute(Task task) {
        String menuId = (String) task.getInputData().get("menuId");
        System.out.println("  [update] Menu " + menuId + " live and synced");
        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.addOutputData("menu", Map.of("menuId", menuId != null ? menuId : "MENU-734", "status", "LIVE", "items", 3));
        return result;
    }
}
