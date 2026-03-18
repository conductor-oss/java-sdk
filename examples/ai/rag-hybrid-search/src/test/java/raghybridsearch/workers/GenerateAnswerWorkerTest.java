package raghybridsearch.workers;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GenerateAnswerWorkerTest {

    @Test
    void taskDefName() {
        GenerateAnswerWorker worker = new GenerateAnswerWorker("test-key", null);
        assertEquals("hs_generate_answer", worker.getTaskDefName());
    }

    @Test
    void constructorThrowsWithoutApiKey() {
        String key = System.getenv("CONDUCTOR_OPENAI_API_KEY");
        if (key == null || key.isBlank()) {
            assertThrows(IllegalStateException.class, GenerateAnswerWorker::new);
        }
    }
}
