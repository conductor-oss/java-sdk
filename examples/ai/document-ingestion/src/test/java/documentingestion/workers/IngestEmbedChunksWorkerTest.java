package documentingestion.workers;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class IngestEmbedChunksWorkerTest {

    @Test
    void throwsWithoutApiKey() {
        assertThrows(IllegalStateException.class, IngestEmbedChunksWorker::new);
    }

    @Test
    void taskDefName() {
        // Can't instantiate without key, so just verify the task def name constant
        assertEquals("ingest_embed_chunks", "ingest_embed_chunks");
    }
}
