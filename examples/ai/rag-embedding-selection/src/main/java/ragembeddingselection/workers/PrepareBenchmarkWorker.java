package ragembeddingselection.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.List;
import java.util.Map;

/**
 * Worker that prepares a benchmark dataset for embedding evaluation.
 * Returns benchmark queries with expected document IDs, a corpus of documents,
 * and ground truth mappings.
 */
public class PrepareBenchmarkWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "es_prepare_benchmark";
    }

    @Override
    public TaskResult execute(Task task) {
        List<Map<String, Object>> benchmarkQueries = List.of(
                Map.of("id", "q1", "text", "How does vector search work?",
                        "expectedDocIds", List.of("doc1", "doc3")),
                Map.of("id", "q2", "text", "What are transformer models?",
                        "expectedDocIds", List.of("doc2", "doc4")),
                Map.of("id", "q3", "text", "Explain nearest neighbor algorithms",
                        "expectedDocIds", List.of("doc1", "doc5"))
        );

        List<Map<String, Object>> benchmarkCorpus = List.of(
                Map.of("id", "doc1", "text", "Vector search uses mathematical representations to find similar items in high-dimensional space."),
                Map.of("id", "doc2", "text", "Transformer models use self-attention mechanisms to process sequential data in parallel."),
                Map.of("id", "doc3", "text", "Approximate nearest neighbor search enables fast similarity lookups in large vector databases."),
                Map.of("id", "doc4", "text", "Large language models are built on transformer architectures with billions of parameters."),
                Map.of("id", "doc5", "text", "KNN and ANN algorithms trade off between search accuracy and computational efficiency.")
        );

        Map<String, List<String>> groundTruth = Map.of(
                "q1", List.of("doc1", "doc3"),
                "q2", List.of("doc2", "doc4"),
                "q3", List.of("doc1", "doc5")
        );

        System.out.println("  [benchmark] Prepared " + benchmarkQueries.size()
                + " queries and " + benchmarkCorpus.size() + " corpus documents");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("benchmarkQueries", benchmarkQueries);
        result.getOutputData().put("benchmarkCorpus", benchmarkCorpus);
        result.getOutputData().put("groundTruth", groundTruth);
        return result;
    }
}
