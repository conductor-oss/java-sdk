package menumanagement.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class PublishWorker implements Worker {
    @Override public String getTaskDefName() { return "mnu_publish"; }

    @Override public TaskResult execute(Task task) {
        System.out.println("  [publish] Publishing menu: " + task.getInputData().get("menuName"));
        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.addOutputData("menuId", "MENU-734");
        result.addOutputData("published", true);
        result.addOutputData("publishedAt", "2026-03-08");
        return result;
    }
}
