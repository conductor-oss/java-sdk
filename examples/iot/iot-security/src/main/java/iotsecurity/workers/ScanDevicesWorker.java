package iotsecurity.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
public class ScanDevicesWorker implements Worker {
    @Override public String getTaskDefName() { return "ios_scan_devices"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [scan] Processing task");
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("deviceCount", "devices.length");
        return r;
    }
}
