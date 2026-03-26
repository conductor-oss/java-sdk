package inapppurchase.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.Map;
public class SelectItemWorker implements Worker {
    @Override public String getTaskDefName() { return "iap_select_item"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [select] Player " + task.getInputData().get("playerId") + " selected item " + task.getInputData().get("itemId"));
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.addOutputData("item", Map.of("id", task.getInputData().getOrDefault("itemId","ITEM-DragonArmor"), "name", "Dragon Armor", "type", "cosmetic"));
        return r;
    }
}
