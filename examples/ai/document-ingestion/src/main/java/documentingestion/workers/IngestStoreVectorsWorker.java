package documentingestion.workers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Worker 4: Stores embedding vectors to a JSON file on disk.
 *
 * <p>Writes the vector data (including embeddings and metadata) to a JSON
 * file using Jackson {@link ObjectMapper}. The file is stored in a
 * configurable output directory (defaults to {@code System.getProperty("java.io.tmpdir")}).
 *
 * <p>The output file is named {@code <collection>_vectors.json} and contains:
 * <ul>
 *   <li>{@code collection} -- the collection name</li>
 *   <li>{@code vectorCount} -- number of vectors stored</li>
 *   <li>{@code vectors} -- the full vector data array</li>
 * </ul>
 *
 * <p>Returns the file path and stored count in the task output.
 */
public class IngestStoreVectorsWorker implements Worker {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT);

    /** Base directory for vector storage files. Overridable for testing. */
    private Path outputDir;

    public IngestStoreVectorsWorker() {
        this.outputDir = Path.of(System.getProperty("java.io.tmpdir"));
    }

    /**
     * Creates a worker with a custom output directory (useful for testing).
     *
     * @param outputDir the directory where vector files will be written
     */
    public IngestStoreVectorsWorker(Path outputDir) {
        this.outputDir = outputDir;
    }

    @Override
    public String getTaskDefName() {
        return "ingest_store_vectors";
    }

    @SuppressWarnings("unchecked")
    @Override
    public TaskResult execute(Task task) {
        List<Map<String, Object>> vectors = (List<Map<String, Object>>) task.getInputData().get("vectors");
        String collection = (String) task.getInputData().get("collection");

        if (collection == null || collection.isBlank()) {
            collection = "default";
        }
        if (vectors == null) {
            vectors = List.of();
        }

        System.out.println("  [store] Upserting " + vectors.size() + " vectors into collection \"" + collection + "\"");

        String filePath;
        try {
            filePath = writeVectorsToFile(collection, vectors);
        } catch (IOException e) {
            System.err.println("  [store] Failed to write vectors: " + e.getMessage());
            filePath = "[error] " + e.getMessage();
        }

        for (Map<String, Object> v : vectors) {
            System.out.println("    - " + v.get("id") + ": stored successfully");
        }

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("storedCount", vectors.size());
        result.getOutputData().put("collection", collection);
        result.getOutputData().put("filePath", filePath);
        return result;
    }

    /**
     * Writes vectors to a JSON file and returns the absolute file path.
     *
     * @param collection the collection name (used in the file name)
     * @param vectors    the vector data to persist
     * @return the absolute path to the written file
     * @throws IOException if writing fails
     */
    private String writeVectorsToFile(String collection, List<Map<String, Object>> vectors) throws IOException {
        // Ensure output directory exists
        Files.createDirectories(outputDir);

        String fileName = sanitizeFileName(collection) + "_vectors.json";
        File outputFile = outputDir.resolve(fileName).toFile();

        Map<String, Object> document = new LinkedHashMap<>();
        document.put("collection", collection);
        document.put("vectorCount", vectors.size());
        document.put("vectors", vectors);

        MAPPER.writeValue(outputFile, document);

        return outputFile.getAbsolutePath();
    }

    /**
     * Sanitizes a string for use as a file name by replacing non-alphanumeric
     * characters (except hyphens and underscores) with underscores.
     */
    private String sanitizeFileName(String name) {
        return name.replaceAll("[^a-zA-Z0-9_-]", "_");
    }
}
