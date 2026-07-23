import java.util.Locale;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestExecutionExceptionHandler;
import org.opentest4j.TestAbortedException;

/** Skips live LLM tests when a provider's configured API usage budget is exhausted. */
final class LlmBudgetExhaustionExtension implements TestExecutionExceptionHandler {

    static final String USAGE_LIMIT_MESSAGE = "reached your specified api usage limits";

    @Override
    public void handleTestExecutionException(ExtensionContext context, Throwable throwable) throws Throwable {
        if (isUsageLimitError(throwable)) {
            throw new TestAbortedException("LLM API usage budget is exhausted", throwable);
        }
        throw throwable;
    }

    static boolean isUsageLimitError(Throwable throwable) {
        for (Throwable current = throwable; current != null; current = current.getCause()) {
            String message = current.getMessage();
            if (message != null && message.toLowerCase(Locale.ROOT).contains(USAGE_LIMIT_MESSAGE)) {
                return true;
            }
        }
        return false;
    }
}
