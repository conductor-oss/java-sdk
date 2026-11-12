# Native workflows in a Spring Boot service

The starter (`conductor-client-spring`) does the wiring. The application has no Conductor setup
code:

| File | What it shows |
|---|---|
| `OrderWorkflow.java` | The workflow: a plain class with a `@Workflow` method. It is not a bean; the starter finds it under the application's package and creates a new instance for each replay. It takes `OrderTasks` as a constructor parameter and calls the tasks directly: Conductor passes a stand-in whose calls schedule the tasks, so `tasks.charge(...)` runs once, on a worker, however often the workflow is replayed. The `if` that ships an order of 100 or more insured is plain Java; because its arms call tasks, the execution shows it as a SWITCH named `if_amount`, with both arms and the one taken. |
| `OrderTasks.java` | The tasks: an ordinary `@Component` with an injected dependency. The starter polls its `@WorkerTask` methods and registers their task definitions from the annotation attributes (`retryCount = 5` on `charge`). |
| `OrderController.java` | Starts executions and reads them back through the injected `Conductor` bean. |
| `application.properties` | The server URL, and the port of this service. |

Run it against a server (`CONDUCTOR_SERVER_URL`, default `http://localhost:8080/api`):

```shell
./gradlew :native-examples-spring:run
curl -s -X POST localhost:8095/orders -H 'Content-Type: application/json' -d '{"item":"book","quantity":2}'
curl -s -X POST localhost:8095/orders -H 'Content-Type: application/json' -d '{"item":"book","quantity":10}'
curl -s localhost:8095/orders/<workflow-id>
```

The service starts even when the Conductor server is down: the definitions are registered in the
background, retried with backoff until the server answers, with an error logged for each failed
attempt.

Settings under `conductor.workflows`: `enabled` (set `false` in a service that only starts
workflows), `worker-id`, `build-id`, `threads`, `continuation-timeout` and `dev-mode`.
