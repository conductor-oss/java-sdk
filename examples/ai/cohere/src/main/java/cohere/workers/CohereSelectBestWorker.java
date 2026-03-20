package cohere.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.List;
import java.util.Map;

/**
 * Selects the generation with the highest likelihood (least negative value).
 *
 * Input: generations (List of Maps with "text" and "likelihood")
 * Output: { bestGeneration, allTexts }
 */
public class CohereSelectBestWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "cohere_select_best";
    }

    @SuppressWarnings("unchecked")
    @Override
    public TaskResult execute(Task task) {
        Map<String, Object> input = task.getInputData();
        List<Map<String, Object>> generations = (List<Map<String, Object>>) input.get("generations");

        Map<String, Object> best = null;
        double bestLikelihood = Double.NEGATIVE_INFINITY;

        for (Map<String, Object> gen : generations) {
            double likelihood = ((Number) gen.get("likelihood")).doubleValue();
            if (likelihood > bestLikelihood) {
                bestLikelihood = likelihood;
                best = gen;
            }
        }

        List<String> allTexts = generations.stream()
                .map(gen -> (String) gen.get("text"))
                .toList();

        System.out.println("  [cohere_select_best worker] Selected best generation with likelihood: " + bestLikelihood);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("bestGeneration", best.get("text"));
        result.getOutputData().put("allTexts", allTexts);
        return result;
    }
}
