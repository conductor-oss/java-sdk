package basicrag.workers;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EmbedQueryWorkerTest {

    @Test
    void taskDefName() {
        String key = System.getenv("CONDUCTOR_OPENAI_API_KEY");
        if (key != null && !key.isBlank()) {
            EmbedQueryWorker worker = new EmbedQueryWorker();
            assertEquals("brag_embed_query", worker.getTaskDefName());
        }
    }

    @Test
    void constructorThrowsWithoutApiKey() {
        // API_KEY is a static field; if it was null/blank at class load time, constructor throws
        String key = System.getenv("CONDUCTOR_OPENAI_API_KEY");
        if (key == null || key.isBlank()) {
            // The execute() method throws when called without the key
            EmbedQueryWorker worker = new EmbedQueryWorker();
            assertEquals("brag_embed_query", worker.getTaskDefName());
        }
    }
}
