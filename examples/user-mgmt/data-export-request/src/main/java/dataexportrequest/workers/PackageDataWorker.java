package dataexportrequest.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class PackageDataWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "der_package";
    }

    @Override
    public TaskResult execute(Task task) {
        String format = (String) task.getInputData().get("format");
        if (format == null) format = "json";
        String url = "https://exports.example.com/downloads/export-" + System.currentTimeMillis() + "." + format;
        System.out.println("  [package] Data packaged as " + format + " -> " + url);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("packageUrl", url);
        result.getOutputData().put("sizeBytes", 2458624);
        return result;
    }
}
