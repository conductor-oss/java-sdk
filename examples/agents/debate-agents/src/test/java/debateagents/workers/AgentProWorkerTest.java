package debateagents.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class AgentProWorkerTest {

    private final AgentProWorker worker = new AgentProWorker();

    @Test
    void taskDefName() {
        assertEquals("da_agent_pro", worker.getTaskDefName());
    }

    @Test
    void returnsProArgumentForRound1() {
        Task task = taskWith(Map.of("topic", "Microservices", "round", 1));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("PRO", result.getOutputData().get("side"));
        assertEquals(1, result.getOutputData().get("round"));
        String argument = (String) result.getOutputData().get("argument");
        assertNotNull(argument);
        assertTrue(argument.contains("independent deployment"));
    }

    @Test
    void returnsProArgumentForRound2() {
        Task task = taskWith(Map.of("topic", "Microservices", "round", 2));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(2, result.getOutputData().get("round"));
        String argument = (String) result.getOutputData().get("argument");
        assertNotNull(argument);
        assertTrue(argument.contains("technology stack"));
    }

    @Test
    void returnsProArgumentForRound3() {
        Task task = taskWith(Map.of("topic", "Microservices", "round", 3));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(3, result.getOutputData().get("round"));
        String argument = (String) result.getOutputData().get("argument");
        assertNotNull(argument);
        assertTrue(argument.contains("Fault isolation"));
    }

    @Test
    void eachRoundReturnsDifferentArgument() {
        String arg1 = executeAndGetArgument(1);
        String arg2 = executeAndGetArgument(2);
        String arg3 = executeAndGetArgument(3);

        assertNotEquals(arg1, arg2);
        assertNotEquals(arg2, arg3);
        assertNotEquals(arg1, arg3);
    }

    @Test
    void round4WrapsToRound1Argument() {
        String arg1 = executeAndGetArgument(1);
        String arg4 = executeAndGetArgument(4);
        assertEquals(arg1, arg4);
    }

    @Test
    void isDeterministic() {
        String first = executeAndGetArgument(2);
        String second = executeAndGetArgument(2);
        assertEquals(first, second);
    }

    @Test
    void handlesStringRound() {
        Task task = taskWith(Map.of("topic", "Microservices", "round", "2"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(2, result.getOutputData().get("round"));
    }

    @Test
    void handlesNullRound() {
        Map<String, Object> input = new HashMap<>();
        input.put("topic", "Microservices");
        input.put("round", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(1, result.getOutputData().get("round"));
    }

    @Test
    void handlesMissingRound() {
        Task task = taskWith(Map.of("topic", "Microservices"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(1, result.getOutputData().get("round"));
    }

    @Test
    void handlesNullTopic() {
        Map<String, Object> input = new HashMap<>();
        input.put("topic", null);
        input.put("round", 1);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("PRO", result.getOutputData().get("side"));
        assertNotNull(result.getOutputData().get("argument"));
    }

    @Test
    void parseRoundValues() {
        assertEquals(1, AgentProWorker.parseRound(null));
        assertEquals(1, AgentProWorker.parseRound("invalid"));
        assertEquals(3, AgentProWorker.parseRound(3));
        assertEquals(2, AgentProWorker.parseRound("2"));
        assertEquals(5, AgentProWorker.parseRound(5L));
    }

    private String executeAndGetArgument(int round) {
        Task task = taskWith(Map.of("topic", "Microservices", "round", round));
        TaskResult result = worker.execute(task);
        return (String) result.getOutputData().get("argument");
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
