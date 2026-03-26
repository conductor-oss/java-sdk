package warehousemanagement.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.*;

public class PackWorker implements Worker {
    @Override public String getTaskDefName() { return "wm_pack"; }

    @SuppressWarnings("unchecked")
    @Override public TaskResult execute(Task task) {
        List<?> items = (List<?>) task.getInputData().get("pickedItems");
        int count = items != null ? items.size() : 0;
        System.out.println("  [pack] Packed " + count + " items into 1 package");
        TaskResult r = new TaskResult(task);
        r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("packageId", "PKG-657-001");
        r.getOutputData().put("weight", "4.2 kg");
        return r;
    }
}
