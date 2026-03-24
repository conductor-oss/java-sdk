# Pro vs Con Debate with Iterative Rounds and Moderation

A topic is set with two sides (PRO and CON). Two agents present arguments drawn from static `ARGUMENTS` lists in alternating rounds via a DO_WHILE loop. After the debate ends, a moderator summarizes and delivers a verdict.

## Workflow

```
topic -> da_set_topic -> DO_WHILE(da_agent_pro + da_agent_con) -> da_moderator_summarize
```

## Workers

**SetTopicWorker** (`da_set_topic`) -- Sets the topic and `sides: ["PRO", "CON"]`.

**AgentProWorker** (`da_agent_pro`) -- Presents arguments from a static `ARGUMENTS` list. Tags `side: "PRO"`.

**AgentConWorker** (`da_agent_con`) -- Presents counter-arguments from its own `ARGUMENTS` list. Tags `side: "CON"`.

**ModeratorSummarizeWorker** (`da_moderator_summarize`) -- Produces `summary` and `verdict`.

## Tests

40 tests cover topic setting, pro/con argument selection, and moderation.

## Further Reading

- [RUNNING.md](../../RUNNING.md) -- how to build and run this example
