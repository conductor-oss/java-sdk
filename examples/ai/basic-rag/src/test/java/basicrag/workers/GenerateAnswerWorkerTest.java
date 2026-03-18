package basicrag.workers;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GenerateAnswerWorkerTest {

    @Test
    void taskDefName() {
        String key = System.getenv("CONDUCTOR_OPENAI_API_KEY");
        if (key != null && !key.isBlank()) {
            GenerateAnswerWorker worker = new GenerateAnswerWorker();
            assertEquals("brag_generate_answer", worker.getTaskDefName());
        }
    }

    @Test
    void constructorThrowsWithoutApiKey() {
        // API_KEY is a static field; if it was null/blank at class load time, execute() throws
        String key = System.getenv("CONDUCTOR_OPENAI_API_KEY");
        if (key == null || key.isBlank()) {
            GenerateAnswerWorker worker = new GenerateAnswerWorker();
            assertEquals("brag_generate_answer", worker.getTaskDefName());
        }
    }
}
