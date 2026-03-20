package genomicspipeline.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.*;

public class AnnotateWorker implements Worker {

    @Override
    public String getTaskDefName() { return "gen_annotate"; }

    @Override
    @SuppressWarnings("unchecked")
    public TaskResult execute(Task task) {
        List<Map<String, Object>> variants =
                (List<Map<String, Object>>) task.getInputData().getOrDefault("variants", List.of());
        System.out.println("  [annotate] Annotating " + variants.size() + " variants with clinical databases");

        List<Map<String, Object>> annotated = new ArrayList<>();
        for (Map<String, Object> v : variants) {
            Map<String, Object> a = new LinkedHashMap<>(v);
            a.put("clinicalSignificance", "BRCA1".equals(v.get("gene")) ? "pathogenic" : "uncertain");
            a.put("frequency", 0.001);
            a.put("database", "ClinVar");
            annotated.add(a);
        }

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        Map<String, Object> output = new LinkedHashMap<>();
        output.put("annotatedVariants", annotated);
        result.setOutputData(output);
        return result;
    }
}
