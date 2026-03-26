package seasonmanagement.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.List;
import java.util.Map;
public class DefineRewardsWorker implements Worker {
    @Override public String getTaskDefName() { return "smg_define_rewards"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [rewards] Defining reward tiers for " + task.getInputData().get("seasonId"));
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.addOutputData("rewardTiers", List.of(Map.of("tier","Bronze","level",10,"reward","Season Banner"), Map.of("tier","Silver","level",30,"reward","Exclusive Weapon Skin"), Map.of("tier","Gold","level",50,"reward","Legendary Mount")));
        return r;
    }
}
