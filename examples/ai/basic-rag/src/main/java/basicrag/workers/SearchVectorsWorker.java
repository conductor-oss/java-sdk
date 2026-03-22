package basicrag.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Worker that performs a vector similarity search against a bundled knowledge base.
 *
 * <p>Computes TF-IDF cosine similarity between the query (from input) and a small set of
 * bundled sample documents. Returns the top-K most similar documents ranked by score.
 *
 * <p>For real vector search with embeddings, see the rag-pinecone, rag-chromadb, and
 * rag-pgvector examples.
 */
public class SearchVectorsWorker implements Worker {

    /** Bundled knowledge base documents for TF-IDF similarity search. */
    private static final List<Map<String, String>> KNOWLEDGE_BASE = List.of(
            Map.of("id", "doc-01",
                    "text", "Conductor is an open-source orchestration platform for microservices and AI workflows. "
                            + "It provides a centralized engine for defining, executing, and monitoring complex workflows."),
            Map.of("id", "doc-02",
                    "text", "RAG combines retrieval from a knowledge base with LLM generation for grounded answers. "
                            + "The retrieval step finds relevant documents, and the generation step produces a coherent response."),
            Map.of("id", "doc-03",
                    "text", "Vector embeddings represent text as numerical arrays enabling semantic similarity search. "
                            + "Models like text-embedding-3-small convert sentences into high-dimensional vectors."),
            Map.of("id", "doc-04",
                    "text", "Orkes provides a managed cloud version of Conductor with enterprise features including "
                            + "role-based access control, audit logging, and multi-tenant support."),
            Map.of("id", "doc-05",
                    "text", "Workflow orchestration coordinates multiple services to complete a business process. "
                            + "Unlike choreography, orchestration uses a central controller to manage task execution order."),
            Map.of("id", "doc-06",
                    "text", "Microservices architecture decomposes applications into small, independently deployable services. "
                            + "Each service handles a specific business capability and communicates via APIs."),
            Map.of("id", "doc-07",
                    "text", "Large language models like GPT-4 and Claude generate human-like text from prompts. "
                            + "They are trained on massive text corpora and can perform summarization, translation, and reasoning."),
            Map.of("id", "doc-08",
                    "text", "Task queues enable asynchronous processing by decoupling task producers from consumers. "
                            + "Workers poll for tasks, execute them, and report results back to the orchestrator.")
    );

    @Override
    public String getTaskDefName() {
        return "brag_search_vectors";
    }

    /** Minimum cosine similarity score for a document to be considered a match. */
    private static final double MIN_SCORE_THRESHOLD = 0.05;

    @Override
    public TaskResult execute(Task task) {
        Object topKObj = task.getInputData().get("topK");
        int topK = 3;
        if (topKObj instanceof Number) {
            topK = ((Number) topKObj).intValue();
        }
        if (topK <= 0) {
            TaskResult fail = new TaskResult(task);
            fail.setStatus(TaskResult.Status.FAILED_WITH_TERMINAL_ERROR);
            fail.setReasonForIncompletion("Input 'topK' must be a positive integer, got: " + topK);
            return fail;
        }

        // Build the query from input — use the original question text if available
        String query = null;
        Object queryText = task.getInputData().get("queryText");
        if (queryText instanceof String && !((String) queryText).isBlank()) {
            query = (String) queryText;
        } else {
            Object q = task.getInputData().get("question");
            if (q instanceof String && !((String) q).isBlank()) {
                query = (String) q;
            }
        }

        if (query == null || query.isBlank()) {
            TaskResult fail = new TaskResult(task);
            fail.setStatus(TaskResult.Status.FAILED_WITH_TERMINAL_ERROR);
            fail.setReasonForIncompletion("Input 'queryText' or 'question' is required and must not be blank");
            return fail;
        }

        // Compute TF-IDF similarity between query and each document
        Map<String, Double> idf = computeIdf(query);
        double[] queryVec = tfidfVector(query, idf);

        List<Map<String, Object>> scoredDocs = new ArrayList<>();
        for (Map<String, String> doc : KNOWLEDGE_BASE) {
            double[] docVec = tfidfVector(doc.get("text"), idf);
            double score = cosineSimilarity(queryVec, docVec);
            score = Math.round(score * 100.0) / 100.0;

            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("id", doc.get("id"));
            entry.put("text", doc.get("text"));
            entry.put("score", score);
            scoredDocs.add(entry);
        }

        // Sort by score descending and take top-K, filtering by minimum threshold
        scoredDocs.sort((a, b) -> Double.compare((Double) b.get("score"), (Double) a.get("score")));
        List<Map<String, Object>> documents = scoredDocs.stream()
                .filter(d -> ((Double) d.get("score")) >= MIN_SCORE_THRESHOLD)
                .limit(topK)
                .collect(Collectors.toList());

        if (documents.isEmpty()) {
            TaskResult fail = new TaskResult(task);
            fail.setStatus(TaskResult.Status.FAILED);
            fail.setReasonForIncompletion("No documents matched query above threshold ("
                    + MIN_SCORE_THRESHOLD + "). Query: \"" + query + "\"");
            fail.getOutputData().put("totalSearched", KNOWLEDGE_BASE.size());
            fail.getOutputData().put("threshold", MIN_SCORE_THRESHOLD);
            return fail;
        }

        System.out.println("  [search] Found " + documents.size() + " relevant docs (topK=" + topK
                + ", searched " + KNOWLEDGE_BASE.size() + " docs)");
        for (Map<String, Object> doc : documents) {
            String text = (String) doc.get("text");
            String preview = text.length() > 60 ? text.substring(0, 60) + "..." : text;
            System.out.println("    - " + doc.get("id") + " (score: " + doc.get("score") + "): \"" + preview + "\"");
        }

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("documents", documents);
        result.getOutputData().put("totalSearched", KNOWLEDGE_BASE.size());
        return result;
    }

    // --- TF-IDF helpers ---

    private static List<String> tokenize(String text) {
        if (text == null || text.isBlank()) return List.of();
        return Arrays.stream(text.toLowerCase().replaceAll("[^a-z0-9 ]", " ").split("\\s+"))
                .filter(t -> !t.isBlank() && t.length() > 1)
                .collect(Collectors.toList());
    }

    /** Compute IDF across the knowledge base + query as the corpus. */
    private Map<String, Double> computeIdf(String query) {
        List<List<String>> allDocs = new ArrayList<>();
        allDocs.add(tokenize(query));
        for (Map<String, String> doc : KNOWLEDGE_BASE) {
            allDocs.add(tokenize(doc.get("text")));
        }

        Set<String> vocabulary = new HashSet<>();
        for (List<String> tokens : allDocs) {
            vocabulary.addAll(tokens);
        }

        Map<String, Double> idf = new HashMap<>();
        int n = allDocs.size();
        for (String term : vocabulary) {
            long docCount = allDocs.stream()
                    .filter(tokens -> tokens.contains(term))
                    .count();
            idf.put(term, Math.log((double) n / (1 + docCount)) + 1.0);
        }
        return idf;
    }

    /** Convert text to a TF-IDF vector using the given IDF map. */
    private double[] tfidfVector(String text, Map<String, Double> idf) {
        List<String> tokens = tokenize(text);
        List<String> vocab = new ArrayList<>(idf.keySet());
        Collections.sort(vocab);

        // Compute term frequencies
        Map<String, Long> tf = new HashMap<>();
        for (String token : tokens) {
            tf.merge(token, 1L, Long::sum);
        }

        double[] vec = new double[vocab.size()];
        for (int i = 0; i < vocab.size(); i++) {
            String term = vocab.get(i);
            long count = tf.getOrDefault(term, 0L);
            double termFreq = tokens.isEmpty() ? 0 : (double) count / tokens.size();
            vec[i] = termFreq * idf.getOrDefault(term, 1.0);
        }
        return vec;
    }

    private static double cosineSimilarity(double[] a, double[] b) {
        double dot = 0, normA = 0, normB = 0;
        for (int i = 0; i < a.length; i++) {
            dot += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }
        if (normA == 0 || normB == 0) return 0.0;
        return dot / (Math.sqrt(normA) * Math.sqrt(normB));
    }
}
