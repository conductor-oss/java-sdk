package networksegmentation.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class DefineZonesWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "ns_define_zones";
    }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("  [zones] Defined 4 zones: DMZ, app, data, mgmt");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.addOutputData("define_zonesId", "DEFINE_ZONES-1600");
        result.addOutputData("success", true);
        return result;
    }
}
