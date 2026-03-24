# Gaming Matchmaking in Java Using Conductor

Matches players into a fair, balanced game session: searching the player pool, rating skill levels, creating a balanced match, provisioning a lobby, and starting the game.

## The Problem

You need to match players into a fair, balanced game session. The workflow searches for players queuing in the same game mode and region, evaluates each player's skill rating (ELO, MMR, Glicko), finds a balanced match that minimizes skill disparity, creates a game lobby with the matched players, and starts the session. Unbalanced matches frustrate both sides. skilled players get bored, new players get destroyed.

Without orchestration, you'd build a matchmaking service with a queue poller, skill-rating lookup, matching algorithm, lobby creation, and session start. manually managing queue timeouts, handling players who disconnect during matchmaking, expanding search criteria when wait times are too long, and logging match quality metrics.

## The Solution

**You just write the player search, skill rating, match balancing, lobby creation, and game start logic. Conductor handles queue retries, match formation timing, and matchmaking audit trails.**

Each matchmaking concern is a simple, independent worker. a plain Java class that does one thing. Conductor takes care of executing them in order (search players, rate skill, match, create lobby, start), retrying if the player database is slow, tracking every matchmaking operation, and resuming from the last step if the process crashes.

### What You Write: Workers

Player profiling, queue management, match formation, and lobby creation workers each solve one piece of the matchmaking problem.

| Worker | Task | What It Does |
|---|---|---|
| **CreateLobbyWorker** | `gmm_create_lobby` | Creates a game lobby on a server for the matched players |
| **MatchWorker** | `gmm_match` | Creates a balanced match by grouping players with similar skill ratings and returns a match ID |
| **RateSkillWorker** | `gmm_rate_skill` | Evaluates each candidate's MMR rating and returns a ranked list of players by skill |
| **SearchPlayersWorker** | `gmm_search_players` | Searches the player pool for candidates matching skill level and region |
| **StartWorker** | `gmm_start` | Starts the game session in the lobby and assigns the map |

### The Workflow

```
gmm_search_players
 │
 ▼
gmm_rate_skill
 │
 ▼
gmm_match
 │
 ▼
gmm_create_lobby
 │
 ▼
gmm_start

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
