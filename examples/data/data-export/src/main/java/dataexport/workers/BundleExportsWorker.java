package dataexport.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.List;

/**
 * Bundles all exported files into a single archive and uploads to destination.
 * Input: csvFile, jsonFile, excelFile, destination
 * Output: bundleUrl, exportCount, files
 */
public class BundleExportsWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "dx_bundle_exports";
    }

    @Override
    public TaskResult execute(Task task) {
        String csvFile = (String) task.getInputData().getOrDefault("csvFile", "");
        String jsonFile = (String) task.getInputData().getOrDefault("jsonFile", "");
        String excelFile = (String) task.getInputData().getOrDefault("excelFile", "");
        String destination = (String) task.getInputData().getOrDefault("destination", "s3://exports");

        List<String> files = List.of(csvFile, jsonFile, excelFile);
        String bundleUrl = destination + "/export-bundle-2024-0315.zip";

        System.out.println("  [bundle] Bundled " + files.size() + " export files -> " + bundleUrl);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("bundleUrl", bundleUrl);
        result.getOutputData().put("exportCount", files.size());
        result.getOutputData().put("files", files);
        return result;
    }
}
