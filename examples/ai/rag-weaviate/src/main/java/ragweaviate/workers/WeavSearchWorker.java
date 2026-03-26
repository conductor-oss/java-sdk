package ragweaviate.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Performs a vector-similarity search against Weaviate (deterministic..
 *
 * Input:  embedding (List), className (String), properties (List), limit (int)
 * Output: objects (List of Map), queryInfo (Map)
 */
public class WeavSearchWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "weav_search";
    }

    @Override
    public TaskResult execute(Task task) {
        Map<String, Object> input = task.getInputData();

        String className = (String) input.getOrDefault("className", "Document");

        System.out.println("  [weav_search worker] Searching class: " + className);

        // Build three fixed result objects
        Map<String, Object> obj1 = new LinkedHashMap<>();
        obj1.put("title", "Weaviate Overview");
        obj1.put("content", "Weaviate is an open-source vector database with built-in vectorization modules.");
        obj1.put("source", "docs/intro.md");
        Map<String, Object> additional1 = new LinkedHashMap<>();
        additional1.put("certainty", 0.95);
        additional1.put("distance", 0.05);
        obj1.put("_additional", additional1);

        Map<String, Object> obj2 = new LinkedHashMap<>();
        obj2.put("title", "Schema Design");
        obj2.put("content", "Classes in Weaviate define the data structure with properties and vectorizer config.");
        obj2.put("source", "docs/schema.md");
        Map<String, Object> additional2 = new LinkedHashMap<>();
        additional2.put("certainty", 0.91);
        additional2.put("distance", 0.09);
        obj2.put("_additional", additional2);

        Map<String, Object> obj3 = new LinkedHashMap<>();
        obj3.put("title", "Modules");
        obj3.put("content", "Weaviate supports text2vec-openai, text2vec-huggingface, and generative-openai modules.");
        obj3.put("source", "docs/modules.md");
        Map<String, Object> additional3 = new LinkedHashMap<>();
        additional3.put("certainty", 0.88);
        additional3.put("distance", 0.12);
        obj3.put("_additional", additional3);

        List<Map<String, Object>> objects = List.of(obj1, obj2, obj3);

        Map<String, Object> queryInfo = new LinkedHashMap<>();
        queryInfo.put("className", className);
        queryInfo.put("vectorizer", "text2vec-openai");
        queryInfo.put("totalObjects", 8432);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("objects", objects);
        result.getOutputData().put("queryInfo", queryInfo);
        return result;
    }
}
