# Roadmap

| Breaking Change            | Example                               | Solution                                                           |
|----------------------------|---------------------------------------|--------------------------------------------------------------------|
| 1. Class Namings           | Task.Status → Task.StatusEnum         | Can be fixed quite easily by config                                |
|                            | TaskClient → TaskResourceApi          |                                                                    |
| 2. Method namings          | TaskClient.pollTask() -> poll()       | Can be fixed quite easily by config                                |
| 3. Custom methods          | ApplicationResource.upsertApplication | Can be fixed by inheritance or by AOP with custom templating       |
| 4. Custom static methods   | TaskResult.newTaskResult()            | Custom templating fix (with or without AOP)                        |
| 5. Custom enums            | Task.Status.isComplete                | Custom templating                                                  |
| 6. Custom constructors     | new TaskResult(Task task)             | Extracting mapping logic, custom templating needed for constructor |
| 7. Custom logic in methods | - event dispatch                      | Either templating (bad idea) or inheritance                        |
|                            | - custom exception handling           | Fixed using Http400 dto and exception with appropriate constructor |
|                            | - other logic                         | Hard to identify all cases, there could be much more               |

I no longer think generating clients and adapting our code to them is the best path. Instead, let’s first extract the custom logic and turn the existing clients into thin, pure HTTP wrappers. If that works, we can generate clients afterward.