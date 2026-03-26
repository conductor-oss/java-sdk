package genomicspipeline.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.time.Instant;
import java.util.*;

public class GenomicsReportWorker implements Worker {

    @Override
    public String getTaskDefName() { return "gen_report"; }

    @Override
    @SuppressWarnings("unchecked")
    public TaskResult execute(Task task) {
        List<Map<String, Object>> annotated =
                (List<Map<String, Object>>) task.getInputData().getOrDefault("annotatedVariants", List.of());
        long significant = annotated.stream()
                .filter(v -> "pathogenic".equals(v.get("clinicalSignificance")))
                .count();
        System.out.println("  [report] " + significant + " clinically significant variant(s) found");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        Map<String, Object> output = new LinkedHashMap<>();
        output.put("reportId", "GEN-RPT-2024-001");
        output.put("clinicallySignificant", (int) significant);
        output.put("actionableFindings", significant > 0);
        output.put("generatedAt", Instant.now().toString());
        result.setOutputData(output);
        return result;
    }
}
