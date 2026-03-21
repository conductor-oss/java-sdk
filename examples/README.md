# Conductor Java SDK Examples

791 self-contained examples for the Conductor Java SDK. Each example is an independent Maven project with its own `pom.xml`, workers, workflow definition, tests, and launcher script.

## Quick Start

```bash
# Start Conductor
docker run -d -p 8080:8080 -p 1234:5000 orkesio/orkes-conductor-standalone:1.2.3

# Pick any example and run it
cd examples/basics/hello-world
mvn package -DskipTests && java -jar target/hello-world-1.0.0.jar

```

## Prerequisites

- Java 21+
- Maven 3.8+
- Conductor server (local Docker or [Orkes Cloud](https://orkes.io))

## Machine-Readable Metadata

See [`manifest.json`](manifest.json) for per-example metadata: category, workflow name, task types, required API keys, difficulty level, and Conductor primitives used.

---

## Advanced (50)

| Example | Description |
|---------|-------------|
| [aggregator-pattern](advanced/aggregator-pattern/) | A Java Conductor workflow example for message aggregation .  collecting related messages that arrive... |
| [at-least-once](advanced/at-least-once/) | A Java Conductor workflow example for at-least-once message delivery .  receiving a message from a q... |
| [backpressure](advanced/backpressure/) | A Java Conductor workflow example for queue backpressure management .  monitoring queue depth agains... |
| [batch-ml-training](advanced/batch-ml-training/) | A Java Conductor workflow example for batch ML training .  loading a dataset, splitting it into trai... |
| [claim-check](advanced/claim-check/) | A Java Conductor workflow example for the claim check pattern .  storing a large payload (images, do... |
| [competing-consumers](advanced/competing-consumers/) | A Java Conductor workflow example for the competing consumers pattern .  publishing a task to a shar... |
| [container-orchestration](advanced/container-orchestration/) | A Java Conductor workflow example for container deployment .  building a Docker image from a service... |
| [content-enricher](advanced/content-enricher/) | A webhook fires: `{"customerId": "CUST-42", "orderId": "ORD-999", "amount": 1250.00}`. That's it. Yo... |
| [correlation-pattern](advanced/correlation-pattern/) | A Java Conductor workflow example for message correlation .  receiving a batch of messages from diff... |
| [cross-region](advanced/cross-region/) | A Java Conductor workflow example for cross-region data replication .  copying a dataset from a prim... |
| [data-versioning](advanced/data-versioning/) | A Java Conductor workflow example for dataset versioning .  taking a point-in-time snapshot of a dat... |
| [dynamic-workflows](advanced/dynamic-workflows/) | A Java Conductor workflow example for dynamic data pipelines .  validating incoming payloads against... |
| [edge-orchestration](advanced/edge-orchestration/) | A Java Conductor workflow example for edge computing orchestration .  dispatching a job to multiple ... |
| [exactly-once](advanced/exactly-once/) | The payment service processes the $49.99 debit, then crashes before acknowledging the message. The b... |
| [experiment-tracking](advanced/experiment-tracking/) | A Java Conductor workflow example for ML experiment tracking .  defining an experiment with a hypoth... |
| [feature-store](advanced/feature-store/) | A Java Conductor workflow example for feature store management .  computing features from a source t... |
| [gpu-orchestration](advanced/gpu-orchestration/) | A Java Conductor workflow example for GPU resource orchestration .  checking GPU availability by typ... |
| [graceful-shutdown](advanced/graceful-shutdown/) | A Java Conductor workflow example for graceful worker shutdown .  signaling a worker group to stop a... |
| [hybrid-cloud](advanced/hybrid-cloud/) | A Java Conductor workflow example for hybrid cloud data routing .  classifying incoming data by sens... |
| [idempotent-processing](advanced/idempotent-processing/) | Kafka delivers a payment event. Your service processes it, charges the customer $49.99, and then: ne... |
| [map-reduce](advanced/map-reduce/) | You have 10 million log entries and need to find every occurrence of "ERROR" with context. A single-... |
| [message-broker](advanced/message-broker/) | A Java Conductor workflow example for message brokering .  receiving a message with topic and priori... |
| [model-registry](advanced/model-registry/) | A Java Conductor workflow example for ML model lifecycle management .  registering a trained model w... |
| [model-serving](advanced/model-serving/) | A Java Conductor workflow example for deploying ML models to production serving .  loading a model f... |
| [multi-cluster](advanced/multi-cluster/) | A Java Conductor workflow example for multi-cluster data processing .  preparing a job by partitioni... |
| [normalizer](advanced/normalizer/) | A Java Conductor workflow example for data normalization .  detecting the input format of incoming d... |
| [ordered-processing](advanced/ordered-processing/) | A Java Conductor workflow example for ordered message processing .  receiving a batch of out-of-orde... |
| [parallel-processing](advanced/parallel-processing/) | A Java Conductor workflow example for parallel data processing .  splitting a dataset into chunks ba... |
| [pipeline-pattern](advanced/pipeline-pattern/) | A Java Conductor workflow example for the pipeline pattern .  passing raw data through a series of s... |
| [pipeline-versioning](advanced/pipeline-versioning/) | A Java Conductor workflow example for pipeline versioning .  snapshotting the current pipeline confi... |
| [publish-subscribe](advanced/publish-subscribe/) | A Java Conductor workflow example for publish-subscribe .  publishing an event to a topic, fanning i... |
| [request-reply](advanced/request-reply/) | A Java Conductor workflow example for the request-reply pattern .  sending an asynchronous request t... |
| [scatter-gather](advanced/scatter-gather/) | You need a price quote from five vendors. Vendor A responds in 200ms. Vendor B in 400ms. Vendor C is... |
| [serverless-orchestration](advanced/serverless-orchestration/) | A Java Conductor workflow example for serverless function orchestration .  invoking a parse function... |
| [splitter-pattern](advanced/splitter-pattern/) | A Java Conductor workflow example for the splitter pattern .  receiving a composite message containi... |
| [task-dedup](advanced/task-dedup/) | A Java Conductor workflow example for task deduplication .  hashing the task input to create a finge... |
| [task-priority](advanced/task-priority/) | A Java Conductor workflow example for task priority routing .  classifying incoming tasks by urgency... |
| [task-routing](advanced/task-routing/) | A Java Conductor workflow example for intelligent task routing .  analyzing a task's resource requir... |
| [wire-tap](advanced/wire-tap/) | A Java Conductor workflow example for the wire tap pattern .  receiving a message and simultaneously... |
| [worker-pools](advanced/worker-pools/) | A Java Conductor workflow example for worker pool management .  categorizing incoming tasks by type,... |
| [worker-scaling](advanced/worker-scaling/) | A Java Conductor workflow example for worker auto-scaling .  monitoring queue depth and latency, cal... |
| [workflow-composition](advanced/workflow-composition/) | A Java Conductor workflow example for workflow composition .  combining two independent sub-workflow... |
| [workflow-debugging](advanced/workflow-debugging/) | A Java Conductor workflow example for workflow debugging .  instrumenting a workflow with debug hook... |
| [workflow-inheritance](advanced/workflow-inheritance/) | A Java Conductor workflow example for workflow inheritance .  defining a base workflow pattern (init... |
| [workflow-migration](advanced/workflow-migration/) | A Java Conductor workflow example for workflow migration .  exporting workflow definitions and execu... |
| [workflow-optimization](advanced/workflow-optimization/) | A Java Conductor workflow example for workflow optimization .  analyzing execution history to measur... |
| [workflow-patterns](advanced/workflow-patterns/) | A Java Conductor workflow example showcasing multiple workflow patterns in a single definition .  a ... |
| [workflow-profiling](advanced/workflow-profiling/) | A Java Conductor workflow example for workflow profiling .  instrumenting a workflow to capture timi... |
| [workflow-templating](advanced/workflow-templating/) | A Java Conductor workflow example for ETL workflow templating .  extracting data from a source (data... |
| [workflow-testing](advanced/workflow-testing/) | A Java Conductor workflow example for automated workflow test orchestration: defining test fixtures,... |

## Agents (45)

| Example | Description |
|---------|-------------|
| [agent-collaboration](agents/agent-collaboration/) | Four specialized AI agents chained in sequence sounds clean on a whiteboard, but in practice Agent 2... |
| [agent-handoff](agents/agent-handoff/) | A customer writes "I was charged twice" and your first-line agent can't figure out whether that's bi... |
| [agent-memory](agents/agent-memory/) | Agent with Memory .  loads conversation history, thinks with context, updates memory, and responds. ... |
| [agent-supervisor](agents/agent-supervisor/) | Assign code review to three unsupervised AI agents and watch what happens: one rewrites your archite... |
| [agent-swarm](agents/agent-swarm/) | Agent Swarm .  decompose a research topic into subtasks, run 4 swarm agents in parallel, then merge ... |
| [agentic-loop](agents/agentic-loop/) | You tell the agent "research distributed consensus algorithms." It searches, finds three papers, and... |
| [api-calling-agent](agents/api-calling-agent/) | A user says "cancel my last order" and your AI understands the intent perfectly; but it has no idea ... |
| [autonomous-agent](agents/autonomous-agent/) | You tell the agent "set up production monitoring for the platform." It provisions Grafana, wires up ... |
| [calculator-agent](agents/calculator-agent/) | Calculator Agent .  parse a math expression, compute step-by-step following PEMDAS, and explain the ... |
| [calendar-agent](agents/calendar-agent/) | Calendar Agent .  parse meeting request, check attendee calendars, find available slots, and book th... |
| [chain-of-thought](agents/chain-of-thought/) | You ask the model "What's the compound interest on $10,000 at 5% for 3 years?" and it confidently re... |
| [code-interpreter](agents/code-interpreter/) | Code Interpreter Agent .  analyzes a data question, generates Python code, executes in a sandbox, an... |
| [competitive-agents](agents/competitive-agents/) | Competitive Agents .  three solvers propose solutions in parallel, a judge scores them, and a winner... |
| [crm-agent](agents/crm-agent/) | CRM Agent .  lookup customer, check history, update record, and generate response through a sequenti... |
| [database-agent](agents/database-agent/) | The support engineer needs to know which departments have the highest revenue. The data is in the da... |
| [debate-agents](agents/debate-agents/) | Debate Agents. PRO and CON agents argue over a topic for multiple rounds, then a moderator summarize... |
| [email-agent](agents/email-agent/) | Email Agent .  analyze request, draft email, review tone, and send through a sequential pipeline. Us... |
| [file-processing-agent](agents/file-processing-agent/) | File Processing Agent .  detect file type, extract content, analyze, and generate summary through a ... |
| [function-calling](agents/function-calling/) | You ask "What's Apple's stock price?" and the model calls `get_stock_price(ticker="APPL")`. a ticker... |
| [goal-decomposition](agents/goal-decomposition/) | Goal Decomposition .  decomposes a high-level goal into subgoals, executes them in parallel via FORK... |
| [hierarchical-agents](agents/hierarchical-agents/) | Hierarchical agents .  manager plans, team leads delegate to workers in parallel branches, manager m... |
| [multi-agent-code-review](agents/multi-agent-code-review/) | Multi-Agent Code Review .  parses code, runs security/performance/style reviews in parallel, then co... |
| [multi-agent-content](agents/multi-agent-content/) | Multi-Agent Content Creation .  research, write, optimize SEO, edit, and publish content through a s... |
| [multi-agent-planning](agents/multi-agent-planning/) | Multi-Agent Project Planning .  architect designs the system, three estimators run in parallel (fron... |
| [multi-agent-research](agents/multi-agent-research/) | Your research intern searches Google, finds three blog posts, and writes the report. No academic pap... |
| [multi-agent-support](agents/multi-agent-support/) | Tier 1 support copies a customer's "I can't log in. error 403 after password reset" into the billing... |
| [plan-execute-agent](agents/plan-execute-agent/) | Tell an AI agent "deploy the new version" without a planning step and watch it start deploying immed... |
| [react-agent](agents/react-agent/) | Someone asks "What's the GDP per capita of the country that hosted the 2024 Olympics?" A standard LL... |
| [reflection-agent](agents/reflection-agent/) | Reflection Agent .  generates content on a topic, iteratively reflects and improves through a DO_WHI... |
| [search-agent](agents/search-agent/) | Search Agent .  formulate queries, search Google and Wikipedia in parallel, rank/merge results, and ... |
| [self-correction](agents/self-correction/) | Self-Correction .  generates code, runs tests, and if tests fail diagnoses and fixes the code before... |
| [three-agent-pipeline](agents/three-agent-pipeline/) | Three-Agent Pipeline. Researcher + Writer + Reviewer with final output assembly. Uses [Conductor](ht... |
| [tool-augmented-generation](agents/tool-augmented-generation/) | Tool-Augmented Generation .  detect knowledge gaps during text generation, invoke external tools to ... |
| [tool-use-basics](agents/tool-use-basics/) | Your AI chatbot can eloquently explain how to check the weather in Tokyo. It just can't actually che... |
| [tool-use-caching](agents/tool-use-caching/) | Tool Use Caching .  checks a cache before executing a tool, and caches the result afterward. Uses a ... |
| [tool-use-conditional](agents/tool-use-conditional/) | Tool Use Conditional .  classifies a user query and routes to the appropriate tool (calculator, inte... |
| [tool-use-error-handling](agents/tool-use-error-handling/) | Tool Use Error Handling .  tries a primary tool and falls back to an alternative tool on failure via... |
| [tool-use-logging](agents/tool-use-logging/) | Tool Use Logging: log tool requests and responses, execute tools, and create audit entries through a... |
| [tool-use-parallel](agents/tool-use-parallel/) | Your agent calls the weather API, waits 3 seconds for the response, then calls the news API, waits 2... |
| [tool-use-rate-limiting](agents/tool-use-rate-limiting/) | Tool Use Rate Limiting .  checks API rate limits before tool execution, queuing and delaying request... |
| [tool-use-sequential](agents/tool-use-sequential/) | Tool Use Sequential: search the web, read a page, extract data, and summarize through a sequential p... |
| [tool-use-validation](agents/tool-use-validation/) | Tool Use Validation .  generate tool call, validate input, execute tool, validate output, and delive... |
| [tree-of-thought](agents/tree-of-thought/) | Tree of Thought .  define a problem, explore three parallel reasoning paths (analytical, creative, e... |
| [two-agent-pipeline](agents/two-agent-pipeline/) | Sequential writer-editor pipeline: writer agent drafts content, editor agent refines it, final outpu... |
| [web-browsing-agent](agents/web-browsing-agent/) | Web Browsing Agent .  plans search queries, executes searches, selects relevant pages, reads content... |

## AI & LLM Workflows (60)

| Example | Description |
|---------|-------------|
| [adaptive-rag](ai/adaptive-rag/) | "What's the capital of France?" gets routed through the full RAG pipeline: embed, search, rerank, ge... |
| [amazon-bedrock](ai/amazon-bedrock/) | A Java Conductor workflow example for orchestrating Amazon Bedrock model invocations .  building the... |
| [anthropic-claude](ai/anthropic-claude/) | You need Claude for long-context analysis in a production pipeline: security audits, document review... |
| [basic-rag](ai/basic-rag/) | A user asks your chatbot "What's our refund policy?" and it confidently invents a policy that doesn'... |
| [chatbot-orchestration](ai/chatbot-orchestration/) | A Java Conductor workflow that processes a chatbot conversation turn .  receiving the user message w... |
| [cohere](ai/cohere/) | A Java Conductor workflow example for generating marketing copy using Cohere .  building a prompt ta... |
| [conversational-rag](ai/conversational-rag/) | A Java Conductor workflow that powers multi-turn conversational retrieval-augmented generation. Each... |
| [corrective-rag](ai/corrective-rag/) | Your vector store retrieves three documents for a question, but two are about a completely different... |
| [document-ingestion](ai/document-ingestion/) | Someone dumps 10,000 PDFs into a shared drive and expects the RAG system to answer questions about t... |
| [document-qa](ai/document-qa/) | A Java Conductor workflow that answers questions about documents .  ingesting a document from a URL,... |
| [enterprise-rag](ai/enterprise-rag/) | A Java Conductor workflow that wraps a RAG pipeline with the guardrails enterprises need before goin... |
| [fine-tuned-deployment](ai/fine-tuned-deployment/) | A Java Conductor workflow that takes a fine-tuned model from training output to production serving .... |
| [first-ai-workflow](ai/first-ai-workflow/) | Your LLM feature works beautifully in a Jupyter notebook. Then you deploy it. The first OpenAI rate-... |
| [google-gemini](ai/google-gemini/) | Your team picks Gemini for multimodal tasks, but the API latency varies wildly: sometimes 2 seconds,... |
| [huggingface](ai/huggingface/) | A Java Conductor workflow that routes NLP tasks (summarization, text generation, sentiment analysis)... |
| [incremental-rag](ai/incremental-rag/) | A Java Conductor workflow that keeps a vector store in sync with a source document collection by det... |
| [knowledge-base-sync](ai/knowledge-base-sync/) | A Java Conductor workflow that keeps a knowledge base in sync with a source .  crawling the source U... |
| [llm-caching](ai/llm-caching/) | A Java Conductor workflow that wraps LLM calls with a caching layer .  hashing each prompt to create... |
| [llm-chain](ai/llm-chain/) | First LLM call summarizes a customer email. Second one extracts product IDs. Third one validates aga... |
| [llm-cost-tracking](ai/llm-cost-tracking/) | End of month AWS bill: $12,000 in OpenAI API calls. Nobody knows which feature consumed what, or tha... |
| [llm-fallback-chain](ai/llm-fallback-chain/) | GPT-4 returns a 429 and your entire AI feature goes dark. Because you bet everything on a single pro... |
| [llm-retry](ai/llm-retry/) | A Java Conductor workflow that demonstrates Conductor's built-in retry mechanism for LLM API calls. ... |
| [mistral-ai](ai/mistral-ai/) | A Java Conductor workflow that orchestrates Mistral AI chat completion calls for document-based ques... |
| [multi-document-rag](ai/multi-document-rag/) | A Java Conductor workflow that searches three document collections simultaneously. API documentation... |
| [multi-model-compare](ai/multi-model-compare/) | A Java Conductor workflow that sends the same prompt to GPT-4, Claude, and Gemini in parallel, then ... |
| [multimodal-rag](ai/multimodal-rag/) | A Java Conductor workflow that handles questions with mixed-media attachments .  detecting which mod... |
| [ollama-local](ai/ollama-local/) | A Java Conductor workflow that runs code review through a locally-hosted Ollama model .  checking th... |
| [openai-gpt4](ai/openai-gpt4/) | You want to call GPT-4 from a workflow, but the API times out sometimes, the response format varies ... |
| [prompt-templates](ai/prompt-templates/) | A Java Conductor workflow that manages prompt engineering as a first-class concern .  resolving a ve... |
| [question-answering](ai/question-answering/) | A Java Conductor workflow that answers natural language questions from a knowledge base .  parsing t... |
| [rag-access-control](ai/rag-access-control/) | A Java Conductor workflow that wraps a RAG pipeline with enterprise access controls .  authenticatin... |
| [rag-chromadb](ai/rag-chromadb/) | A Java Conductor workflow that implements a RAG pipeline using ChromaDB as the vector store .  embed... |
| [rag-citation](ai/rag-citation/) | Your RAG system gives a great answer, but when the VP asks "where did you get that number?" you can'... |
| [rag-code](ai/rag-code/) | A Java Conductor workflow that implements RAG specifically for code .  parsing the natural language ... |
| [rag-elasticsearch](ai/rag-elasticsearch/) | A Java Conductor workflow that implements RAG using Elasticsearch's native dense vector search (kNN)... |
| [rag-embedding-selection](ai/rag-embedding-selection/) | A Java Conductor workflow that benchmarks three embedding providers (OpenAI, Cohere, and a local mod... |
| [rag-evaluation](ai/rag-evaluation/) | A Java Conductor workflow that runs a RAG pipeline and then evaluates the output on three quality di... |
| [rag-fusion](ai/rag-fusion/) | A Java Conductor workflow that implements RAG Fusion .  rewriting the user's question into multiple ... |
| [rag-hybrid-search](ai/rag-hybrid-search/) | Pure vector search returns documents about "network connectivity issues" when the user searched for ... |
| [rag-knowledge-graph](ai/rag-knowledge-graph/) | A Java Conductor workflow that combines knowledge graph traversal with vector similarity search .  e... |
| [rag-langchain](ai/rag-langchain/) | A Java Conductor workflow that implements the full LangChain-style RAG pipeline .  loading documents... |
| [rag-milvus](ai/rag-milvus/) | A Java Conductor workflow that implements RAG using Milvus as the vector database .  embedding the q... |
| [rag-mongodb](ai/rag-mongodb/) | A Java Conductor workflow that implements RAG using MongoDB Atlas Vector Search .  embedding the que... |
| [rag-multi-query](ai/rag-multi-query/) | A Java Conductor workflow that expands a single user question into multiple query variants (paraphra... |
| [rag-pgvector](ai/rag-pgvector/) | A Java Conductor workflow that implements RAG using pgvector .  the PostgreSQL extension that adds v... |
| [rag-pinecone](ai/rag-pinecone/) | A Java Conductor workflow that implements RAG using Pinecone .  embedding the question, querying a P... |
| [rag-qdrant](ai/rag-qdrant/) | A Java Conductor workflow that implements RAG using Qdrant .  embedding the question, searching a Qd... |
| [rag-quality-gates](ai/rag-quality-gates/) | A Java Conductor workflow that adds two quality gates to a RAG pipeline .  a relevance gate after re... |
| [rag-redis](ai/rag-redis/) | A Java Conductor workflow that implements RAG using Redis's vector similarity search (RediSearch) . ... |
| [rag-reranking](ai/rag-reranking/) | A Java Conductor workflow that adds a cross-encoder reranking step between retrieval and generation ... |
| [rag-sql](ai/rag-sql/) | A Java Conductor workflow that turns natural language questions into SQL .  parsing the question to ... |
| [rag-weaviate](ai/rag-weaviate/) | A Java Conductor workflow that implements RAG using Weaviate .  embedding the query, searching a Wea... |
| [raptor-rag](ai/raptor-rag/) | A Java Conductor workflow that implements RAPTOR (Recursive Abstractive Processing for Tree-Organize... |
| [self-rag](ai/self-rag/) | A Java Conductor workflow that implements Self-RAG .  a pipeline that retrieves documents, grades th... |
| [semi-structured-rag](ai/semi-structured-rag/) | A Java Conductor workflow that classifies a question's data needs (structured, unstructured, or both... |
| [streaming-llm](ai/streaming-llm/) | A Java Conductor workflow that handles LLM streaming responses .  preparing the request, collecting ... |
| [structured-output](ai/structured-output/) | The LLM returns beautiful prose when you need a JSON object. You parse it, it breaks, missing closin... |
| [system-prompts](ai/system-prompts/) | A Java Conductor workflow that runs the same user prompt through two different system prompts .  for... |
| [voice-bot](ai/voice-bot/) | A Java Conductor workflow that powers a voice-based conversational bot .  transcribing caller audio ... |
| [web-scraping-rag](ai/web-scraping-rag/) | A Java Conductor workflow that scrapes web pages from a list of URLs, chunks the extracted content, ... |

## AI Generation (18)

| Example | Description |
|---------|-------------|
| [ai-data-labeling](ai-generation/ai-data-labeling/) | A Java Conductor workflow that orchestrates data labeling at scale .  preparing the dataset, dispatc... |
| [ai-fine-tuning](ai-generation/ai-fine-tuning/) | A Java Conductor workflow that orchestrates model fine-tuning end-to-end .  preparing the training d... |
| [ai-guardrails](ai-generation/ai-guardrails/) | A Java Conductor workflow that wraps AI generation with safety guardrails .  checking the user's pro... |
| [ai-image-generation](ai-generation/ai-image-generation/) | A Java Conductor workflow that generates images from text prompts through a five-stage pipeline .  e... |
| [ai-model-evaluation](ai-generation/ai-model-evaluation/) | A Java Conductor workflow that evaluates a machine learning model end-to-end .  loading the model ar... |
| [ai-music-generation](ai-generation/ai-music-generation/) | A Java Conductor workflow that generates music through a five-stage production pipeline .  composing... |
| [ai-orchestration-platform](ai-generation/ai-orchestration-platform/) | A Java Conductor workflow that acts as an AI request gateway .  receiving incoming AI requests, rout... |
| [ai-prompt-engineering](ai-generation/ai-prompt-engineering/) | A Java Conductor workflow that automates prompt optimization .  defining the task and evaluation cri... |
| [ai-video-generation](ai-generation/ai-video-generation/) | A Java Conductor workflow that produces AI-generated videos through a five-stage production pipeline... |
| [ai-voice-cloning](ai-generation/ai-voice-cloning/) | A Java Conductor workflow that clones a speaker's voice .  collecting voice samples from the target ... |
| [code-generation](ai-generation/code-generation/) | A Java Conductor workflow that generates code from natural language requirements .  parsing requirem... |
| [code-review-ai](ai-generation/code-review-ai/) | A Java Conductor workflow that reviews pull requests automatically .  parsing the diff to extract ch... |
| [deployment-ai](ai-generation/deployment-ai/) | A Java Conductor workflow that makes deployment decisions intelligently .  analyzing code changes in... |
| [documentation-ai](ai-generation/documentation-ai/) | A Java Conductor workflow that generates documentation from source code .  analyzing a repository to... |
| [incident-ai](ai-generation/incident-ai/) | A Java Conductor workflow that handles production incidents end-to-end .  detecting an anomaly from ... |
| [monitoring-ai](ai-generation/monitoring-ai/) | A Java Conductor workflow that provides intelligent monitoring .  collecting system metrics from a s... |
| [pr-review-ai](ai-generation/pr-review-ai/) | A Java Conductor workflow that automates pull request reviews .  fetching the diff from the reposito... |
| [release-notes-ai](ai-generation/release-notes-ai/) | A Java Conductor workflow that generates release notes automatically .  collecting commits between t... |

## Basics (10)

| Example | Description |
|---------|-------------|
| [conductor-ui](basics/conductor-ui/) | A Java Conductor workflow designed specifically for exploring the Conductor UI at `http://localhost:... |
| [creating-workers](basics/creating-workers/) | You've defined a workflow in JSON, registered it with Conductor, and hit "start"; but nothing happen... |
| [docker-setup](basics/docker-setup/) | A minimal Java Conductor workflow with a single task that verifies your Docker-based Conductor setup... |
| [end-to-end-app](basics/end-to-end-app/) | A complete Java Conductor application that processes support tickets end-to-end: classifying the tic... |
| [hello-world](basics/hello-world/) | The absolute minimum Conductor example. One workflow, one task, one worker. Takes a `name` as input,... |
| [orkes-cloud](basics/orkes-cloud/) | A minimal Java Conductor workflow that verifies your connection to Orkes Cloud, the managed Conducto... |
| [registering-workflows](basics/registering-workflows/) | A Java example that demonstrates how to register workflow and task definitions with Conductor using ... |
| [sdk-setup](basics/sdk-setup/) | A minimal Java Conductor workflow that verifies your SDK setup is correct, the Maven dependency is p... |
| [understanding-workflows](basics/understanding-workflows/) | Your team calls everything a "workflow", the JIRA board, the CI pipeline, the Slack approval chain. ... |
| [workflow-input-output](basics/workflow-input-output/) | Your workflow runs, all tasks complete, but the output is empty, or worse, silently wrong. You passe... |

## CRM (30)

| Example | Description |
|---------|-------------|
| [api-test-generation](crm/api-test-generation/) | A Java Conductor workflow that automatically generates API tests from an OpenAPI specification .  pa... |
| [bug-triage](crm/bug-triage/) | A Java Conductor workflow that automatically triages bug reports .  parsing the report text, classif... |
| [campaign-automation](crm/campaign-automation/) | A Java Conductor workflow that runs a complete marketing campaign lifecycle .  designing the campaig... |
| [chatbot-orchestration](crm/chatbot-orchestration/) | A Java Conductor workflow that processes a chatbot conversation turn .  receiving the user message w... |
| [code-generation](crm/code-generation/) | A Java Conductor workflow that generates code from natural language requirements .  parsing requirem... |
| [code-review-ai](crm/code-review-ai/) | A Java Conductor workflow that reviews pull requests automatically .  parsing the diff to extract ch... |
| [commit-analysis](crm/commit-analysis/) | A Java Conductor workflow that analyzes a repository's commit history .  parsing commits from a bran... |
| [customer-journey](crm/customer-journey/) | A Java Conductor workflow that maps a customer's journey from first contact to conversion .  trackin... |
| [deployment-ai](crm/deployment-ai/) | A Java Conductor workflow that makes deployment decisions intelligently .  analyzing code changes in... |
| [document-qa](crm/document-qa/) | A Java Conductor workflow that answers questions about documents .  ingesting a document from a URL,... |
| [documentation-ai](crm/documentation-ai/) | A Java Conductor workflow that generates documentation from source code .  analyzing a repository to... |
| [drip-campaign](crm/drip-campaign/) | A Java Conductor workflow that runs a drip email campaign for a contact .  enrolling them in a campa... |
| [event-management](crm/event-management/) | A Java Conductor workflow that manages an event lifecycle .  planning the event with venue and sched... |
| [helpdesk-routing](crm/helpdesk-routing/) | A Java Conductor workflow that routes helpdesk tickets to the right support tier .  classifying the ... |
| [incident-ai](crm/incident-ai/) | A Java Conductor workflow that handles production incidents end-to-end .  detecting an anomaly from ... |
| [knowledge-base-sync](crm/knowledge-base-sync/) | A Java Conductor workflow that keeps a knowledge base in sync with a source .  crawling the source U... |
| [lead-nurturing](crm/lead-nurturing/) | A Java Conductor workflow that nurtures a lead through a personalized outreach sequence .  segmentin... |
| [lead-scoring](crm/lead-scoring/) | Your top rep just spent three weeks nurturing a lead who was never going to buy: meanwhile, a VP of ... |
| [monitoring-ai](crm/monitoring-ai/) | A Java Conductor workflow that provides intelligent monitoring .  collecting system metrics from a s... |
| [named-entity-extraction](crm/named-entity-extraction/) | A Java Conductor workflow that extracts named entities from text .  tokenizing the input into words,... |
| [pr-review-ai](crm/pr-review-ai/) | A Java Conductor workflow that automates pull request reviews .  fetching the diff from the reposito... |
| [question-answering](crm/question-answering/) | A Java Conductor workflow that answers natural language questions from a knowledge base .  parsing t... |
| [release-notes-ai](crm/release-notes-ai/) | A Java Conductor workflow that generates release notes automatically .  collecting commits between t... |
| [sentiment-analysis](crm/sentiment-analysis/) | A Java Conductor workflow that analyzes sentiment in customer text .  preprocessing the input (clean... |
| [summarization-pipeline](crm/summarization-pipeline/) | A Java Conductor workflow that summarizes long documents .  extracting logical sections from the inp... |
| [test-generation](crm/test-generation/) | A Java Conductor workflow that automatically generates unit tests from source code .  analyzing the ... |
| [text-classification](crm/text-classification/) | A Java Conductor workflow that classifies text into categories .  preprocessing the input, extractin... |
| [ticket-management](crm/ticket-management/) | A Java Conductor workflow that manages the full lifecycle of a support ticket .  creating the ticket... |
| [voice-bot](crm/voice-bot/) | A Java Conductor workflow that powers a voice-based conversational bot .  transcribing caller audio ... |
| [webinar-registration](crm/webinar-registration/) | A Java Conductor workflow that manages the end-to-end webinar registration experience .  registering... |

## Data (44)

| Example | Description |
|---------|-------------|
| [audio-transcription](data/audio-transcription/) | A Java Conductor workflow example for audio transcription pipelines: preprocessing raw audio, runnin... |
| [batch-processing](data/batch-processing/) | Your nightly ETL job processes 10 million rows from the transactions database. At row 8.7 million, t... |
| [clickstream-analytics](data/clickstream-analytics/) | A Java Conductor workflow example for clickstream analytics: ingesting raw click events, grouping th... |
| [csv-processing](data/csv-processing/) | A partner sends you a 500MB CSV of customer records every Monday. Your API endpoint reads the whole ... |
| [dashboard-data](data/dashboard-data/) | A Java Conductor workflow example for dashboard data preparation: aggregating raw metrics over a tim... |
| [data-aggregation](data/data-aggregation/) | The VP of Sales opens the regional revenue dashboard Monday morning and it takes 45 seconds to load.... |
| [data-anonymization](data/data-anonymization/) | A Java Conductor workflow example for data anonymization. scanning datasets for personally identifia... |
| [data-archival](data/data-archival/) | A Java Conductor workflow example for data archival. identifying records that exceed a configurable ... |
| [data-catalog](data/data-catalog/) | A Java Conductor workflow example for building a data catalog. discovering data assets across schema... |
| [data-compression](data/data-compression/) | A Java Conductor workflow example for intelligent data compression. analyzing data characteristics t... |
| [data-dedup](data/data-dedup/) | A Java Conductor workflow example for data deduplication: loading records, computing dedup keys from... |
| [data-encryption](data/data-encryption/) | A Java Conductor workflow example for field-level data encryption. generating an encryption key for ... |
| [data-enrichment](data/data-enrichment/) | Marketing hands you a spreadsheet of 10,000 leads. Each row has a name, an email, and a zip code. Sa... |
| [data-export](data/data-export/) | A Java Conductor workflow example for data export: querying a data source, then exporting the result... |
| [data-lake-ingestion](data/data-lake-ingestion/) | A Java Conductor workflow example for data lake ingestion: validating incoming records against a sch... |
| [data-lineage](data/data-lineage/) | A Java Conductor workflow example for data lineage tracking: registering the data source origin, app... |
| [data-masking](data/data-masking/) | A Java Conductor workflow example for data masking: loading records, detecting PII fields (SSNs, ema... |
| [data-migration](data/data-migration/) | A Java Conductor workflow example for database-to-database data migration. extracting records from a... |
| [data-partitioning](data/data-partitioning/) | A Java Conductor workflow example for data partitioning. splitting a dataset into two partitions bas... |
| [data-quality-checks](data/data-quality-checks/) | The executive dashboard shows 15% revenue growth this quarter. The CEO quotes it in the board meetin... |
| [data-reconciliation](data/data-reconciliation/) | A Java Conductor workflow example for data reconciliation. fetching records from two independent sou... |
| [data-sampling](data/data-sampling/) | A Java Conductor workflow example for sample-based data quality gating: loading a dataset, drawing a... |
| [data-sync](data/data-sync/) | A Java Conductor workflow example for bidirectional data synchronization: detecting changes in two s... |
| [data-validation](data/data-validation/) | A Java Conductor workflow example for multi-layer data validation: loading records, checking that re... |
| [data-warehouse-load](data/data-warehouse-load/) | A Java Conductor workflow example for data warehouse loading: staging incoming records to a temporar... |
| [etl-basics](data/etl-basics/) | Your company's data lives in 3 databases, 2 third-party APIs, and a shared Google Drive folder that ... |
| [feature-engineering](data/feature-engineering/) | A Java Conductor workflow example for ML feature engineering: extracting raw features from source da... |
| [gdpr-data-deletion](data/gdpr-data-deletion/) | A Java Conductor workflow example for GDPR Article 17 right-to-erasure compliance. discovering all r... |
| [image-processing](data/image-processing/) | A Java Conductor workflow example for image processing: loading an image from a URL, then running th... |
| [json-transformation](data/json-transformation/) | A Java Conductor workflow example for JSON-to-JSON transformation: parsing an incoming JSON record, ... |
| [log-processing](data/log-processing/) | A Java Conductor workflow example for log processing. ingesting raw log entries from a source within... |
| [ml-data-pipeline](data/ml-data-pipeline/) | A Java Conductor workflow example for an end-to-end ML training pipeline: collecting labeled data fr... |
| [named-entity-extraction](data/named-entity-extraction/) | A Java Conductor workflow that extracts named entities from text .  tokenizing the input into words,... |
| [ocr-pipeline](data/ocr-pipeline/) | A Java Conductor workflow example for document OCR. preprocessing a document image (deskewing, binar... |
| [pdf-processing](data/pdf-processing/) | Five hundred vendor invoices arrive in accounts payable every month as PDF attachments. They come in... |
| [real-time-analytics](data/real-time-analytics/) | A Java Conductor workflow example for real-time analytics: ingesting a batch of events, processing t... |
| [report-generation](data/report-generation/) | A Java Conductor workflow example for automated report generation. querying raw data for a specific ... |
| [schema-evolution](data/schema-evolution/) | A Java Conductor workflow example for schema evolution. comparing a current schema against a target ... |
| [sentiment-analysis](data/sentiment-analysis/) | A Java Conductor workflow that analyzes sentiment in customer text .  preprocessing the input (clean... |
| [stream-processing](data/stream-processing/) | A Java Conductor workflow example for stream processing with windowed analytics: ingesting a batch o... |
| [summarization-pipeline](data/summarization-pipeline/) | A Java Conductor workflow that summarizes long documents .  extracting logical sections from the inp... |
| [text-classification](data/text-classification/) | A Java Conductor workflow that classifies text into categories .  preprocessing the input, extractin... |
| [video-transcoding](data/video-transcoding/) | A Java Conductor workflow example for adaptive bitrate video transcoding: analyzing the source video... |
| [xml-parsing](data/xml-parsing/) | A Java Conductor workflow example for XML-to-JSON transformation. receiving raw XML content with a c... |

## Devops (50)

| Example | Description |
|---------|-------------|
| [api-test-generation](devops/api-test-generation/) | A Java Conductor workflow that automatically generates API tests from an OpenAPI specification .  pa... |
| [apm-workflow](devops/apm-workflow/) | Automates Application Performance Monitoring (APM) analysis using [Conductor](https://github.com/con... |
| [artifact-management](devops/artifact-management/) | Build artifact lifecycle orchestration: build, sign, publish, and cleanup old artifacts. Uses [Condu... |
| [auto-scaling](devops/auto-scaling/) | Analyzes service metrics, plans scaling action, executes scaling, and verifies the result. Pattern: ... |
| [automated-testing](devops/automated-testing/) | Orchestrates a test suite: setup environment, run unit/integration/e2e tests in parallel, aggregate ... |
| [bug-triage](devops/bug-triage/) | A Java Conductor workflow that automatically triages bug reports .  parsing the report text, classif... |
| [capacity-planning](devops/capacity-planning/) | Automates infrastructure capacity planning using [Conductor](https://github.com/conductor-oss/conduc... |
| [certificate-rotation](devops/certificate-rotation/) | It's 2 AM on a Saturday. Your wildcard TLS cert expired eleven minutes ago. Every service behind the... |
| [change-management](devops/change-management/) | Automates ITIL-style change management using [Conductor](https://github.com/conductor-oss/conductor)... |
| [chaos-engineering](devops/chaos-engineering/) | Orchestrates controlled chaos experiments using [Conductor](https://github.com/conductor-oss/conduct... |
| [ci-cd-pipeline](devops/ci-cd-pipeline/) | Someone pushed to main. Seven CI jobs kicked off in three different systems. The unit tests passed, ... |
| [commit-analysis](devops/commit-analysis/) | A Java Conductor workflow that analyzes a repository's commit history .  parsing commits from a bran... |
| [compliance-scanning](devops/compliance-scanning/) | Orchestrates infrastructure compliance scanning using [Conductor](https://github.com/conductor-oss/c... |
| [container-orchestration](devops/container-orchestration/) | Orchestrates a container build-scan-deploy pipeline using [Conductor](https://github.com/conductor-o... |
| [cost-optimization](devops/cost-optimization/) | Orchestrates cloud cost optimization using [Conductor](https://github.com/conductor-oss/conductor). ... |
| [custom-metrics](devops/custom-metrics/) | Automates custom metrics pipelines using [Conductor](https://github.com/conductor-oss/conductor). Th... |
| [database-backup](devops/database-backup/) | The production disk died on a Tuesday. The team pulled up the backup schedule and discovered the las... |
| [database-migration-devops](devops/database-migration-devops/) | Automates database schema migrations using [Conductor](https://github.com/conductor-oss/conductor). ... |
| [dependency-update](devops/dependency-update/) | Orchestrates automated dependency updates using [Conductor](https://github.com/conductor-oss/conduct... |
| [deployment-rollback](devops/deployment-rollback/) | Automates deployment rollback using [Conductor](https://github.com/conductor-oss/conductor). This wo... |
| [disaster-recovery](devops/disaster-recovery/) | Orchestrates a full disaster recovery failover using [Conductor](https://github.com/conductor-oss/co... |
| [dns-management](devops/dns-management/) | Orchestrates safe DNS record changes using [Conductor](https://github.com/conductor-oss/conductor). ... |
| [environment-management](devops/environment-management/) | Environment lifecycle orchestration: create, configure, seed data, and verify. Uses [Conductor](http... |
| [feature-environment](devops/feature-environment/) | Automates on-demand feature environment provisioning using [Conductor](https://github.com/conductor-... |
| [gitops-workflow](devops/gitops-workflow/) | Automates GitOps reconciliation using [Conductor](https://github.com/conductor-oss/conductor). This ... |
| [incident-response](devops/incident-response/) | The PagerDuty alert fired at 2:14 AM. The on-call engineer saw the Slack notification, opened their ... |
| [infrastructure-provisioning](devops/infrastructure-provisioning/) | Orchestrates infrastructure provisioning: plan, validate, provision, configure, and verify across cl... |
| [load-balancer-config](devops/load-balancer-config/) | Load balancer configuration workflow: discover backends, configure rules, apply config, and health c... |
| [log-aggregation](devops/log-aggregation/) | Aggregate logs: collect raw logs, parse them into structured format, enrich with metadata, and store... |
| [maintenance-window](devops/maintenance-window/) | Automates scheduled maintenance windows using [Conductor](https://github.com/conductor-oss/conductor... |
| [metrics-collection](devops/metrics-collection/) | Collect metrics from multiple sources in parallel using FORK/JOIN, then aggregate the results. Patte... |
| [monitoring-alerting](devops/monitoring-alerting/) | Orchestrates a monitoring and alerting pipeline using [Conductor](https://github.com/conductor-oss/c... |
| [multi-region-deploy](devops/multi-region-deploy/) | Automates multi-region deployments using [Conductor](https://github.com/conductor-oss/conductor). Th... |
| [network-automation](devops/network-automation/) | Automates network infrastructure changes using [Conductor](https://github.com/conductor-oss/conducto... |
| [observability-pipeline](devops/observability-pipeline/) | Orchestrates a full observability pipeline using [Conductor](https://github.com/conductor-oss/conduc... |
| [on-call-rotation](devops/on-call-rotation/) | Automates on-call rotation handoffs using [Conductor](https://github.com/conductor-oss/conductor). T... |
| [patch-management](devops/patch-management/) | Automates security patch management using [Conductor](https://github.com/conductor-oss/conductor). T... |
| [performance-testing](devops/performance-testing/) | Orchestrates automated performance testing using [Conductor](https://github.com/conductor-oss/conduc... |
| [post-mortem-automation](devops/post-mortem-automation/) | Automates post-incident post-mortem generation using [Conductor](https://github.com/conductor-oss/co... |
| [predictive-monitoring](devops/predictive-monitoring/) | Automates predictive monitoring using [Conductor](https://github.com/conductor-oss/conductor). This ... |
| [release-management](devops/release-management/) | Orchestrates the software release lifecycle using [Conductor](https://github.com/conductor-oss/condu... |
| [rolling-update](devops/rolling-update/) | Orchestrates zero-downtime rolling updates by analyzing current state, planning the update strategy,... |
| [runbook-automation](devops/runbook-automation/) | Your runbooks live in a Confluence wiki that was last updated eight months ago. When the database fa... |
| [service-discovery-devops](devops/service-discovery-devops/) | You deployed v2.4.1 of the recommendation engine ten minutes ago. It's running, it's healthy, and it... |
| [service-migration](devops/service-migration/) | Orchestrates a service migration between environments using [Conductor](https://github.com/conductor... |
| [sla-monitoring](devops/sla-monitoring/) | Automates SLA/SLO monitoring using [Conductor](https://github.com/conductor-oss/conductor). This wor... |
| [smoke-testing](devops/smoke-testing/) | Orchestrates post-deployment smoke testing using [Conductor](https://github.com/conductor-oss/conduc... |
| [test-generation](devops/test-generation/) | A Java Conductor workflow that automatically generates unit tests from source code .  analyzing the ... |
| [threshold-alerting](devops/threshold-alerting/) | Automates threshold-based alerting using [Conductor](https://github.com/conductor-oss/conductor). Th... |
| [uptime-monitor](devops/uptime-monitor/) | A Java Conductor workflow example for uptime monitoring, endpoint health checks, Slack/email alertin... |

## Ecommerce (20)

| Example | Description |
|---------|-------------|
| [abandoned-cart](ecommerce/abandoned-cart/) | Abandoned cart recovery: detect, wait, remind, offer discount, convert. Uses [Conductor](https://git... |
| [auction-workflow](ecommerce/auction-workflow/) | Auction workflow: open bidding, collect bids, close, determine winner, settle. Uses [Conductor](http... |
| [checkout-flow](ecommerce/checkout-flow/) | Your checkout abandonment rate is 68%. Not because customers changed their minds. because your check... |
| [coupon-engine](ecommerce/coupon-engine/) | Coupon engine: validate code, check eligibility, apply discount, record usage. Uses [Conductor](http... |
| [customer-segmentation](ecommerce/customer-segmentation/) | Customer segmentation: collect data, cluster, label segments, target. Uses [Conductor](https://githu... |
| [flash-sale](ecommerce/flash-sale/) | Flash sale: prepare inventory, open sale, process orders, close, report. Uses [Conductor](https://gi... |
| [fraud-detection](ecommerce/fraud-detection/) | A legitimate customer buys a $5,000 camera for a trip leaving in 2 hours. Your fraud system flags it... |
| [inventory-management](ecommerce/inventory-management/) | It's Black Friday. Your product page shows 500 units of the hot new headphones "in stock"; but that ... |
| [loyalty-program](ecommerce/loyalty-program/) | Loyalty program: earn points, check tier, upgrade, deliver rewards. Uses [Conductor](https://github.... |
| [marketplace-seller](ecommerce/marketplace-seller/) | Marketplace seller onboarding: register, verify, list products, manage orders. Uses [Conductor](http... |
| [order-management](ecommerce/order-management/) | A customer orders a laptop and a USB-C hub. The warehouse picks the laptop but grabs the wrong hub: ... |
| [payment-processing](ecommerce/payment-processing/) | A customer pays $259.97 for their order. The payment gateway charges the card successfully, but the ... |
| [price-optimization](ecommerce/price-optimization/) | A Java Conductor workflow example demonstrating Price Optimization. Uses [Conductor](https://github.... |
| [product-catalog](ecommerce/product-catalog/) | Product catalog management: add, validate, enrich, publish, and index products. Uses [Conductor](htt... |
| [recommendation-engine](ecommerce/recommendation-engine/) | A Java Conductor workflow example demonstrating Recommendation Engine. Uses [Conductor](https://gith... |
| [returns-processing](ecommerce/returns-processing/) | A Java Conductor workflow example for e-commerce returns .  receiving returned items, inspecting the... |
| [shipping-workflow](ecommerce/shipping-workflow/) | A Java Conductor workflow example for end-to-end shipment fulfillment .  selecting the optimal carri... |
| [shopping-cart](ecommerce/shopping-cart/) | A Java Conductor workflow example for shopping cart processing .  adding items to a cart, calculatin... |
| [subscription-billing](ecommerce/subscription-billing/) | A Java Conductor workflow example for recurring subscription billing .  determining the current bill... |
| [tax-calculation](ecommerce/tax-calculation/) | A Java Conductor workflow example for sales tax calculation .  resolving the tax jurisdiction from a... |

## Education (10)

| Example | Description |
|---------|-------------|
| [assessment-creation](education/assessment-creation/) | A Java Conductor workflow example for creating educational assessments .  defining grading criteria ... |
| [certificate-issuance](education/certificate-issuance/) | A Java Conductor workflow example for issuing educational certificates .  verifying that a student c... |
| [course-management](education/course-management/) | A Java Conductor workflow example for setting up a new course .  creating the course record with dep... |
| [education-enrollment](education/education-enrollment/) | A Java Conductor workflow example for student enrollment .  accepting an application, reviewing acad... |
| [grading-workflow](education/grading-workflow/) | A Java Conductor workflow example for assignment grading .  receiving a student submission, scoring ... |
| [lesson-planning](education/lesson-planning/) | A Java Conductor workflow example for building lesson plans .  defining learning objectives for a co... |
| [plagiarism-detection](education/plagiarism-detection/) | A Java Conductor workflow example for academic plagiarism detection .  ingesting a student submissio... |
| [scholarship-processing](education/scholarship-processing/) | A Java Conductor workflow example for scholarship processing .  accepting student applications, eval... |
| [student-progress](education/student-progress/) | A Java Conductor workflow example for tracking student academic progress .  collecting all course gr... |
| [tutoring-match](education/tutoring-match/) | A Java Conductor workflow example for matching students with tutors .  receiving a tutoring request ... |

## Events (42)

| Example | Description |
|---------|-------------|
| [cdc-pipeline](events/cdc-pipeline/) | A customer updates their shipping address at 2:03 PM. The downstream cache still shows the old addre... |
| [complex-event-processing](events/complex-event-processing/) | Complex event processing workflow that ingests events, detects sequences, absences, and timing viola... |
| [cron-trigger](events/cron-trigger/) | Cron-like scheduled workflow: check if the current time matches a schedule expression, decide whethe... |
| [dead-letter-events](events/dead-letter-events/) | A `payment.charge` event hits your processor with a malformed payload. The catch block logs a warnin... |
| [delayed-event](events/delayed-event/) | Delayed event processing workflow that receives an event, computes a delay, applies it, processes th... |
| [event-aggregation](events/event-aggregation/) | Event Aggregation Pipeline: collect events from a time window, aggregate metrics, generate a summary... |
| [event-audit-trail](events/event-audit-trail/) | Sequential event audit trail workflow: log_received -> validate_event -> log_validated -> process_ev... |
| [event-batching](events/event-batching/) | Event Batching .  collects events, creates batches, then processes each batch in a DO_WHILE loop. Us... |
| [event-choreography](events/event-choreography/) | Choreography pattern: services communicate through events with no central orchestrator. Each service... |
| [event-correlation](events/event-correlation/) | Event Correlation .  init correlation session, fork to receive order/payment/shipping events in para... |
| [event-dedup](events/event-dedup/) | Event deduplication workflow .  computes a hash of the event payload, checks if the event has been s... |
| [event-driven-microservices](events/event-driven-microservices/) | Event-driven microservices workflow: order_service -> emit_order_created -> payment_service -> emit_... |
| [event-driven-saga](events/event-driven-saga/) | A customer places an order. Your service creates the order record, charges their credit card, and th... |
| [event-driven-workflow](events/event-driven-workflow/) | Event-driven workflow that receives events, classifies them by type, and routes to the appropriate h... |
| [event-fanout](events/event-fanout/) | Event fan-out workflow that receives an event, fans out to analytics, storage, and notification proc... |
| [event-filtering](events/event-filtering/) | Event filtering workflow that receives events, classifies them by priority, and routes to urgent, st... |
| [event-management](events/event-management/) | A Java Conductor workflow that manages an event lifecycle .  planning the event with venue and sched... |
| [event-merge](events/event-merge/) | Event merge workflow that collects events from three parallel streams via FORK_JOIN, merges the resu... |
| [event-monitoring](events/event-monitoring/) | Sequential event monitoring workflow that collects metrics, analyzes throughput, latency, and errors... |
| [event-notification](events/event-notification/) | A customer's payment fails. They need to know immediately: via email, SMS, and push. But your notifi... |
| [event-ordering](events/event-ordering/) | Event Ordering .  buffers incoming events, sorts them by sequence number, and processes each in orde... |
| [event-priority](events/event-priority/) | Event priority workflow that classifies events by priority and routes to the appropriate processing ... |
| [event-replay](events/event-replay/) | Event Replay Workflow .  load event history, filter by criteria, replay failed events, and generate ... |
| [event-replay-testing](events/event-replay-testing/) | Event Replay Testing .  loads recorded events, sets up a sandbox environment, replays each event in ... |
| [event-routing](events/event-routing/) | An order-cancellation event lands in the user-profile handler. The handler doesn't know what to do w... |
| [event-schema-validation](events/event-schema-validation/) | Event Schema Validation .  validate an incoming event against a named schema, then route valid event... |
| [event-sourcing](events/event-sourcing/) | Event Sourcing .  load event log, append new event, rebuild aggregate state, and snapshot for a bank... |
| [event-split](events/event-split/) | Splits a composite event into multiple sub-events for parallel processing using FORK_JOIN. Uses [Con... |
| [event-transformation](events/event-transformation/) | Event Transformation Pipeline .  parse raw events, enrich with context, map to CloudEvents schema, a... |
| [event-ttl](events/event-ttl/) | Event TTL workflow that checks if an event has expired, processes it if still valid, or logs it if t... |
| [event-versioning](events/event-versioning/) | Event versioning workflow that detects event schema version, transforms older versions to the latest... |
| [event-windowing](events/event-windowing/) | Event Windowing .  collect events into a time window, compute aggregate statistics, and emit the win... |
| [kafka-consumer](events/kafka-consumer/) | Kafka consumer pipeline: receives a message, deserializes it, processes the payload, and commits the... |
| [pubsub-consumer](events/pubsub-consumer/) | Pub/Sub Consumer .  receive a Pub/Sub message, decode the base64 payload, process sensor data with t... |
| [scheduled-event](events/scheduled-event/) | Sequential scheduled-event workflow: queue_event -> check_schedule -> wait_until_ready -> execute_ev... |
| [sqs-consumer](events/sqs-consumer/) | SQS Consumer .  receive an SQS message, validate it, process the event, and delete the message from ... |
| [webhook-callback](events/webhook-callback/) | Webhook Callback Workflow .  receive an incoming webhook request, process the data, and notify the c... |
| [webhook-rate-limiting](events/webhook-rate-limiting/) | Rate limit incoming webhooks per sender. Identifies the sender, checks their request rate, and uses ... |
| [webhook-retry](events/webhook-retry/) | Webhook delivery workflow with DO_WHILE retry loop. Prepares the webhook, attempts delivery up to 3 ... |
| [webhook-security](events/webhook-security/) | Webhook security workflow that computes an HMAC signature, verifies it against the provided signatur... |
| [webhook-trigger](events/webhook-trigger/) | Webhook Trigger .  process incoming webhook event, validate payload, transform data, and store the r... |
| [webinar-registration](events/webinar-registration/) | A Java Conductor workflow that manages the end-to-end webinar registration experience .  registering... |

## Finance (20)

| Example | Description |
|---------|-------------|
| [account-opening](finance/account-opening/) | A customer spent eight minutes filling out your online account application on their phone during the... |
| [budget-approval](finance/budget-approval/) | Budget approval with SWITCH for approve/revise/reject decisions. Uses [Conductor](https://github.com... |
| [claims-processing](finance/claims-processing/) | A policyholder rear-ends someone at a stoplight and files a claim that afternoon. Eight days pass be... |
| [credit-scoring](finance/credit-scoring/) | Credit scoring: collect data, calculate factors, compute score, classify applicant. Uses [Conductor]... |
| [cryptocurrency-trading](finance/cryptocurrency-trading/) | Crypto trading: monitor market, analyze signals, SWITCH(buy/sell/hold), confirm. Uses [Conductor](ht... |
| [expense-management](finance/expense-management/) | Expense management: submit, validate receipts, categorize, approve, reimburse. Uses [Conductor](http... |
| [financial-audit](finance/financial-audit/) | Financial audit: define scope, collect evidence, test controls, generate report, remediate. Uses [Co... |
| [insurance-underwriting](finance/insurance-underwriting/) | Insurance underwriting with SWITCH decision routing for accept/decline/refer. Uses [Conductor](https... |
| [investment-workflow](finance/investment-workflow/) | Investment lifecycle: research, analyze, decide, execute, monitor. Uses [Conductor](https://github.c... |
| [invoice-processing](finance/invoice-processing/) | Invoices arrive as PDF email attachments, portal downloads, and occasionally faxes. An AP clerk spen... |
| [kyc-aml](finance/kyc-aml/) | KYC/AML workflow that verifies customer identity, screens against watchlists, assesses risk, and mak... |
| [loan-origination](finance/loan-origination/) | Loan origination: application intake, credit check, underwriting, approval, and funding. Uses [Condu... |
| [payment-reconciliation](finance/payment-reconciliation/) | Reconcile payments: match transactions, identify discrepancies, resolve mismatches, and generate rep... |
| [payroll-workflow](finance/payroll-workflow/) | Payroll processing: collect hours, calculate gross, apply deductions, process, distribute stubs. Use... |
| [portfolio-rebalancing](finance/portfolio-rebalancing/) | Portfolio rebalancing workflow that analyzes drift, determines trades, executes, verifies, and repor... |
| [regulatory-reporting](finance/regulatory-reporting/) | Regulatory reporting workflow: collect data, validate, format, submit, and confirm. Uses [Conductor]... |
| [risk-assessment](finance/risk-assessment/) | Risk assessment workflow with parallel market, credit, and operational risk analysis via FORK_JOIN. ... |
| [tax-filing](finance/tax-filing/) | Tax filing: collect data, calculate tax, validate, file return, confirm. Uses [Conductor](https://gi... |
| [trade-execution](finance/trade-execution/) | Trade execution workflow that validates orders, checks compliance, routes to optimal exchange, execu... |
| [wire-transfer](finance/wire-transfer/) | Wire transfer workflow: validate, verify sender, compliance check, execute, and confirm. Uses [Condu... |

## Food (10)

| Example | Description |
|---------|-------------|
| [catering-management](food/catering-management/) | Orchestrates a catering order from client inquiry through menu planning, event execution, and invoic... |
| [delivery-tracking](food/delivery-tracking/) | Tracks a food delivery end-to-end: assigning a driver, recording pickup, tracking location en route,... |
| [food-ordering](food/food-ordering/) | Processes a food order from menu browsing through payment, kitchen preparation, and delivery. Uses [... |
| [food-safety](food/food-safety/) | Conducts a food safety inspection: visiting the facility, checking temperatures, verifying hygiene, ... |
| [kitchen-workflow](food/kitchen-workflow/) | Manages the kitchen flow for a restaurant order: receiving it from the POS, prepping ingredients, co... |
| [loyalty-rewards](food/loyalty-rewards/) | Processes restaurant loyalty rewards: calculating points earned, evaluating tier status, applying re... |
| [menu-management](food/menu-management/) | Manages a restaurant menu lifecycle: creating items with descriptions, setting prices, categorizing ... |
| [nutrition-tracking](food/nutrition-tracking/) | Tracks a user's nutritional intake: logging meals, looking up calories and macros, calculating daily... |
| [reservation-system](food/reservation-system/) | Manages restaurant reservations end-to-end: checking table availability, booking, sending confirmati... |
| [restaurant-management](food/restaurant-management/) | Manages the full restaurant guest experience: locating a reservation, seating the party, taking a fo... |

## Gaming (10)

| Example | Description |
|---------|-------------|
| [anti-cheat](gaming/anti-cheat/) | Detects and acts on cheating in an online game: monitoring player behavior, running anomaly detectio... |
| [game-analytics](gaming/game-analytics/) | Runs a game analytics pipeline: collecting raw event data, processing it into structured records, ag... |
| [gaming-matchmaking](gaming/gaming-matchmaking/) | Matches players into a fair, balanced game session: searching the player pool, rating skill levels, ... |
| [in-app-purchase](gaming/in-app-purchase/) | Processes an in-app purchase: selecting an item from the game catalog, verifying eligibility, chargi... |
| [leaderboard-update](gaming/leaderboard-update/) | Updates a game's leaderboard for a season: collecting match scores, validating for integrity, rankin... |
| [live-ops](gaming/live-ops/) | Runs a time-limited live ops event in a game: scheduling the event, configuring rewards and difficul... |
| [player-progression](gaming/player-progression/) | Processes player progression after completing a quest: recording the completion, awarding XP, checki... |
| [season-management](gaming/season-management/) | Manages a competitive season lifecycle: creating the season with a theme, defining reward tiers and ... |
| [tournament-bracket](gaming/tournament-bracket/) | Runs a competitive tournament from registration to finals: accepting registrations, seeding by skill... |
| [virtual-economy](gaming/virtual-economy/) | Processes virtual economy transactions: recording the transaction, validating balance and ownership,... |

## Government (10)

| Example | Description |
|---------|-------------|
| [benefit-determination](government/benefit-determination/) | A Java Conductor workflow example for government benefit determination .  receiving applications, ve... |
| [case-management-gov](government/case-management-gov/) | A Java Conductor workflow example for government case management .  opening cases from citizen repor... |
| [citizen-request](government/citizen-request/) | Processes citizen service requests (pothole repairs, streetlight outages, noise complaints): submitt... |
| [emergency-response](government/emergency-response/) | A Java Conductor workflow example for emergency response .  detecting incidents, classifying severit... |
| [government-permit](government/government-permit/) | Processes a government permit application: receiving the application, validating documents, routing ... |
| [inspection-workflow](government/inspection-workflow/) | Conducts a government property inspection: scheduling the visit, performing the on-site assessment, ... |
| [public-health](government/public-health/) | A Java Conductor workflow example for public health surveillance .  monitoring disease case counts b... |
| [public-records](government/public-records/) | Fulfills a public records request (FOIA): receiving the request, searching government databases, ver... |
| [tax-assessment](government/tax-assessment/) | A Java Conductor workflow example for municipal property tax assessment .  collecting property data,... |
| [voting-workflow](government/voting-workflow/) | Processes a voter's participation in an election: confirming registration, verifying identity, casti... |

## Healthcare (20)

| Example | Description |
|---------|-------------|
| [appointment-scheduling](healthcare/appointment-scheduling/) | A Java Conductor workflow example for healthcare appointment scheduling .  checking provider availab... |
| [billing-medical](healthcare/billing-medical/) | A Java Conductor workflow example for medical billing .  coding clinical encounters with CPT and ICD... |
| [care-coordination](healthcare/care-coordination/) | A Java Conductor workflow example for care coordination .  assessing a patient's clinical needs base... |
| [clinical-decision](healthcare/clinical-decision/) | A Java Conductor workflow example for clinical decision support .  gathering patient clinical data, ... |
| [clinical-trials](healthcare/clinical-trials/) | A promising cardiac drug candidate has 200 patients waiting to enroll. Eligibility screening is a ma... |
| [discharge-planning](healthcare/discharge-planning/) | A Java Conductor workflow example for hospital discharge planning .  assessing patient readiness for... |
| [drug-interaction](healthcare/drug-interaction/) | A Java Conductor workflow example for drug interaction checking .  pulling a patient's current medic... |
| [ehr-integration](healthcare/ehr-integration/) | A Java Conductor workflow example for EHR integration .  querying patient records from a source syst... |
| [genomics-pipeline](healthcare/genomics-pipeline/) | A Java Conductor workflow example for a clinical genomics pipeline .  processing DNA samples through... |
| [insurance-claims](healthcare/insurance-claims/) | A Java Conductor workflow example for health insurance claims processing .  submitting claims with p... |
| [lab-results](healthcare/lab-results/) | A Java Conductor workflow example for laboratory results processing .  collecting patient samples, p... |
| [medical-imaging](healthcare/medical-imaging/) | A Java Conductor workflow example for medical imaging .  acquiring DICOM images from modalities (CT,... |
| [mental-health](healthcare/mental-health/) | A Java Conductor workflow example for mental health care management .  performing patient intake wit... |
| [patient-intake](healthcare/patient-intake/) | The patient filled out her name, date of birth, and insurance information on a clipboard in the wait... |
| [population-health](healthcare/population-health/) | A Java Conductor workflow example for population health management .  aggregating clinical and claim... |
| [prescription-workflow](healthcare/prescription-workflow/) | A Java Conductor workflow example for prescription processing .  verifying the prescription and pull... |
| [prior-authorization](healthcare/prior-authorization/) | A Java Conductor workflow example for prior authorization .  submitting authorization requests with ... |
| [referral-management](healthcare/referral-management/) | A Java Conductor workflow example for referral management .  creating a referral from a PCP with cli... |
| [remote-monitoring](healthcare/remote-monitoring/) | A Java Conductor workflow example for remote patient monitoring (RPM) .  collecting vital signs from... |
| [telemedicine](healthcare/telemedicine/) | A Java Conductor workflow example for telemedicine visits .  scheduling the virtual appointment, est... |

## HR (10)

| Example | Description |
|---------|-------------|
| [background-check](hr/background-check/) | A Java Conductor workflow example for pre-employment background checks .  collecting candidate conse... |
| [benefits-enrollment](hr/benefits-enrollment/) | A Java Conductor workflow example for employee benefits enrollment .  presenting available medical, ... |
| [hr-onboarding](hr/hr-onboarding/) | A Java Conductor workflow example for employee onboarding .  creating the new hire's profile in the ... |
| [interview-scheduling](hr/interview-scheduling/) | A Java Conductor workflow example for interview scheduling .  checking interviewer panel calendar av... |
| [leave-management](hr/leave-management/) | A Java Conductor workflow example for leave management .  submitting a leave request with type and d... |
| [offer-management](hr/offer-management/) | A Java Conductor workflow example for job offer management .  generating an offer letter with positi... |
| [performance-review](hr/performance-review/) | A Java Conductor workflow example for performance reviews .  collecting the employee's self-evaluati... |
| [recruitment-pipeline](hr/recruitment-pipeline/) | A Java Conductor workflow example for recruitment .  posting a job requisition to job boards, screen... |
| [time-tracking](hr/time-tracking/) | A Java Conductor workflow example for employee time tracking .  submitting a weekly timesheet with h... |
| [training-management](hr/training-management/) | A Java Conductor workflow example for employee training management .  assigning a course to an emplo... |

## Human In Loop (32)

| Example | Description |
|---------|-------------|
| [approval-comments](human-in-loop/approval-comments/) | Human-in-the-loop approval with rich feedback .  prepares a document for review, pauses the workflow... |
| [approval-dashboard-nextjs](human-in-loop/approval-dashboard-nextjs/) | A Java Conductor workflow example paired with a Next.js full-stack dashboard .  a SIMPLE task valida... |
| [approval-dashboard-react](human-in-loop/approval-dashboard-react/) | A Java Conductor workflow example paired with a React dashboard .  a SIMPLE task processes an incomi... |
| [approval-delegation](human-in-loop/approval-delegation/) | Approval delegation .  prepares a request, pauses at a WAIT task for the initial approver, then uses... |
| [conditional-approval](human-in-loop/conditional-approval/) | Conditional approval routing .  classifies a request amount into a tier (low/medium/high), then uses... |
| [content-review-pipeline](human-in-loop/content-review-pipeline/) | A Java Conductor workflow example for AI-assisted content creation .  an AI model generates a draft ... |
| [customer-onboarding-kyc](human-in-loop/customer-onboarding-kyc/) | A Java Conductor workflow example for Know Your Customer (KYC) onboarding. performing an automated r... |
| [document-verification](human-in-loop/document-verification/) | A Java Conductor workflow example for document verification .  using AI/OCR to extract structured da... |
| [email-approval](human-in-loop/email-approval/) | A Java Conductor workflow example for email-based approvals .  preparing a request, sending an email... |
| [escalation-chain](human-in-loop/escalation-chain/) | A Java Conductor workflow example for multi-level escalation: submitting a request, pausing at a WAI... |
| [escalation-timer](human-in-loop/escalation-timer/) | A Java Conductor workflow example demonstrating timeout-based auto-approval .  submitting a request,... |
| [exception-handling](human-in-loop/exception-handling/) | A Java Conductor workflow example demonstrating exception-based human-in-the-loop routing .  analyzi... |
| [expense-approval](human-in-loop/expense-approval/) | A Java Conductor workflow example for expense approval. validating an expense against policy rules (... |
| [four-eyes-approval](human-in-loop/four-eyes-approval/) | A Java Conductor workflow example implementing the four-eyes principle: submitting a request, then r... |
| [helpdesk-routing](human-in-loop/helpdesk-routing/) | A Java Conductor workflow that routes helpdesk tickets to the right support tier .  classifying the ... |
| [human-group-claim](human-in-loop/human-group-claim/) | A Java Conductor workflow example for group-based task assignment .  processing a ticket intake and ... |
| [human-task](human-in-loop/human-task/) | A Java Conductor workflow example demonstrating human task forms .  collecting initial data, pausing... |
| [human-user-assignment](human-in-loop/human-user-assignment/) | A Java Conductor workflow example demonstrating user-specific task assignment .  preparing a documen... |
| [legal-contract-review](human-in-loop/legal-contract-review/) | A Java Conductor workflow example for legal contract review: using AI to extract key terms (parties,... |
| [medical-records-review](human-in-loop/medical-records-review/) | A Java Conductor workflow example for medical records review .  automatically validating HIPAA compl... |
| [mobile-approval-flutter](human-in-loop/mobile-approval-flutter/) | A Java Conductor workflow example for mobile-first approvals .  submitting a request, sending a push... |
| [multi-level-approval](human-in-loop/multi-level-approval/) | A Java Conductor workflow example for sequential multi-level approval .  routing a request through M... |
| [multi-tenant-approval](human-in-loop/multi-tenant-approval/) | A Java Conductor workflow example for multi-tenant SaaS approval routing .  loading each tenant's ap... |
| [quality-gate](human-in-loop/quality-gate/) | A Java Conductor workflow example for deployment quality gates .  running an automated test suite (4... |
| [sla-monitoring](human-in-loop/sla-monitoring/) | A Java Conductor workflow example for measuring human approval response times against SLA targets . ... |
| [slack-approval](human-in-loop/slack-approval/) | A Java Conductor workflow example for Slack-native approvals .  submitting a request, posting a Slac... |
| [ticket-management](human-in-loop/ticket-management/) | A Java Conductor workflow that manages the full lifecycle of a support ticket .  creating the ticket... |
| [training-data-labeling](human-in-loop/training-data-labeling/) | A Java Conductor workflow example for ML training data quality .  preparing a labeling batch, using ... |
| [wait-rest-api](human-in-loop/wait-rest-api/) | A Java Conductor workflow example demonstrating how external systems resume paused workflows via RES... |
| [wait-sdk](human-in-loop/wait-sdk/) | A Java Conductor workflow example demonstrating how to resume paused workflows programmatically from... |
| [wait-task-basics](human-in-loop/wait-task-basics/) | A Java Conductor workflow example demonstrating the fundamental WAIT task pattern .  running a prepa... |
| [wait-timeout-escalation](human-in-loop/wait-timeout-escalation/) | A Java Conductor workflow example for deadline-driven escalation .  preparing a request, pausing at ... |

## Insurance (10)

| Example | Description |
|---------|-------------|
| [actuarial-workflow](insurance/actuarial-workflow/) | A Java Conductor workflow example demonstrating actuarial-workflow Actuarial Workflow. Uses [Conduct... |
| [agency-management](insurance/agency-management/) | A Java Conductor workflow example demonstrating agency-management Agency Management. Uses [Conductor... |
| [commission-insurance](insurance/commission-insurance/) | A Java Conductor workflow example demonstrating commission-insurance Commission Insurance. Uses [Con... |
| [compliance-insurance](insurance/compliance-insurance/) | A Java Conductor workflow example demonstrating compliance-insurance Compliance Insurance. Uses [Con... |
| [endorsement-processing](insurance/endorsement-processing/) | A Java Conductor workflow example for mid-term policy endorsement processing .  receiving a change r... |
| [insurance-renewal](insurance/insurance-renewal/) | A Java Conductor workflow example for automated insurance policy renewal .  sending a renewal notice... |
| [policy-issuance](insurance/policy-issuance/) | A Java Conductor workflow example for end-to-end insurance policy issuance .  underwriting the appli... |
| [premium-calculation](insurance/premium-calculation/) | A Java Conductor workflow example for multi-step insurance premium calculation .  collecting rating ... |
| [reinsurance](insurance/reinsurance/) | A Java Conductor workflow example demonstrating reinsurance Reinsurance. Uses [Conductor](https://gi... |
| [salvage-recovery](insurance/salvage-recovery/) | A Java Conductor workflow example demonstrating salvage-recovery Salvage Recovery. Uses [Conductor](... |

## Integrations (20)

| Example | Description |
|---------|-------------|
| [aws-integration](integrations/aws-integration/) | A Java Conductor workflow that coordinates writes across three AWS services in parallel .  uploading... |
| [azure-integration](integrations/azure-integration/) | A Java Conductor workflow that coordinates writes across three Azure services in parallel .  uploadi... |
| [cloudwatch-integration](integrations/cloudwatch-integration/) | A Java Conductor workflow that sets up CloudWatch monitoring .  publishing a custom metric, creating... |
| [database-integration](integrations/database-integration/) | A Java Conductor workflow that performs a database-to-database ETL migration .  connecting to source... |
| [elasticsearch-integration](integrations/elasticsearch-integration/) | It's 3 AM and your production service is throwing 500 errors. You open Kibana to search the logs fro... |
| [gcp-integration](integrations/gcp-integration/) | A Java Conductor workflow that coordinates writes across three GCP services in parallel .  uploading... |
| [github-integration](integrations/github-integration/) | A critical bug fix gets merged to main at 3 PM. The deployment pipeline should have triggered automa... |
| [hubspot-integration](integrations/hubspot-integration/) | A Java Conductor workflow that onboards a new contact in HubSpot .  creating the contact record, enr... |
| [jira-integration](integrations/jira-integration/) | Your sprint board shows 47 tickets "In Progress." The standup takes 25 minutes because nobody can te... |
| [lambda-integration](integrations/lambda-integration/) | A Java Conductor workflow that orchestrates an AWS Lambda invocation .  preparing the payload, invok... |
| [redis-integration](integrations/redis-integration/) | A Java Conductor workflow that exercises core Redis operations .  connecting to a Redis instance, pe... |
| [s3-integration](integrations/s3-integration/) | A Java Conductor workflow that manages an S3 file upload lifecycle .  uploading an object to a bucke... |
| [salesforce-integration](integrations/salesforce-integration/) | A Java Conductor workflow that runs a Salesforce lead scoring and sync pipeline .  querying leads fr... |
| [sendgrid-integration](integrations/sendgrid-integration/) | A Java Conductor workflow that sends personalized emails via SendGrid .  composing an email from a t... |
| [slack-integration](integrations/slack-integration/) | A Java Conductor workflow that processes Slack events end-to-end .  receiving an incoming Slack even... |
| [sns-sqs-integration](integrations/sns-sqs-integration/) | A Java Conductor workflow that runs an AWS SNS/SQS messaging pipeline end-to-end .  publishing a mes... |
| [stripe-integration](integrations/stripe-integration/) | A Java Conductor workflow that processes a Stripe payment end-to-end .  creating a customer in Strip... |
| [teams-integration](integrations/teams-integration/) | A Java Conductor workflow that processes Microsoft Teams webhook events end-to-end .  receiving an i... |
| [twilio-integration](integrations/twilio-integration/) | A Java Conductor workflow that runs a two-way SMS conversation via Twilio .  sending an outbound SMS... |
| [zendesk-integration](integrations/zendesk-integration/) | A Java Conductor workflow that manages a Zendesk support ticket lifecycle end-to-end .  creating a t... |

## IoT (20)

| Example | Description |
|---------|-------------|
| [agriculture-iot](iot/agriculture-iot/) | A Java Conductor workflow example that orchestrates precision agriculture .  reading soil moisture a... |
| [air-quality](iot/air-quality/) | A Java Conductor workflow example that orchestrates air quality monitoring .  collecting pollutant r... |
| [asset-tracking](iot/asset-tracking/) | A shipping container carrying $180,000 in electronics left the port of Long Beach 6 hours ago, and t... |
| [building-automation](iot/building-automation/) | A Java Conductor workflow example that orchestrates building automation .  monitoring HVAC, lighting... |
| [connected-vehicles](iot/connected-vehicles/) | A Java Conductor workflow example that orchestrates connected vehicle monitoring .  collecting vehic... |
| [device-management](iot/device-management/) | You have 10,000 temperature sensors deployed across 200 facilities, all running firmware v2.1. A cri... |
| [edge-computing](iot/edge-computing/) | A Java Conductor workflow example that orchestrates an edge computing pipeline .  offloading compute... |
| [energy-management](iot/energy-management/) | A Java Conductor workflow example that orchestrates building energy management .  collecting kWh con... |
| [environmental-monitoring](iot/environmental-monitoring/) | A Java Conductor workflow example that orchestrates environmental monitoring .  collecting air quali... |
| [firmware-update](iot/firmware-update/) | A Java Conductor workflow example that orchestrates over-the-air firmware updates for IoT devices . ... |
| [fleet-management](iot/fleet-management/) | A Java Conductor workflow example that orchestrates fleet operations .  tracking vehicle GPS positio... |
| [geofencing](iot/geofencing/) | A Java Conductor workflow example that orchestrates geofence monitoring .  normalizing device GPS co... |
| [industrial-iot](iot/industrial-iot/) | A Java Conductor workflow example that orchestrates industrial equipment monitoring .  collecting ma... |
| [iot-security](iot/iot-security/) | A Java Conductor workflow example that orchestrates IoT security operations .  scanning the device n... |
| [predictive-maintenance](iot/predictive-maintenance/) | A Java Conductor workflow example that orchestrates predictive maintenance for industrial assets .  ... |
| [sensor-data-processing](iot/sensor-data-processing/) | A Java Conductor workflow example that orchestrates a sensor data processing pipeline .  collecting ... |
| [smart-home](iot/smart-home/) | A Java Conductor workflow example that orchestrates smart home automation .  detecting sensor events... |
| [supply-chain-iot](iot/supply-chain-iot/) | A Java Conductor workflow example that orchestrates supply chain monitoring .  tracking shipment loc... |
| [water-management](iot/water-management/) | A Java Conductor workflow example that orchestrates water infrastructure monitoring .  reading water... |
| [wearable-data](iot/wearable-data/) | A Java Conductor workflow example that orchestrates a wearable health data pipeline .  collecting vi... |

## Legal (10)

| Example | Description |
|---------|-------------|
| [compliance-review](legal/compliance-review/) | A Java Conductor workflow example demonstrating Compliance Review. Uses [Conductor](https://github.c... |
| [contract-analysis](legal/contract-analysis/) | A Java Conductor workflow example demonstrating Contract Analysis. Uses [Conductor](https://github.c... |
| [document-review](legal/document-review/) | A Java Conductor workflow example demonstrating Document Review. Uses [Conductor](https://github.com... |
| [e-discovery](legal/e-discovery/) | A Java Conductor workflow example demonstrating E Discovery. Uses [Conductor](https://github.com/con... |
| [legal-billing](legal/legal-billing/) | A Java Conductor workflow example demonstrating Legal Billing. Uses [Conductor](https://github.com/c... |
| [legal-case-management](legal/legal-case-management/) | A Java Conductor workflow example demonstrating Legal Case Management. Uses [Conductor](https://gith... |
| [litigation-hold](legal/litigation-hold/) | A Java Conductor workflow example demonstrating Litigation Hold. Uses [Conductor](https://github.com... |
| [patent-filing](legal/patent-filing/) | A Java Conductor workflow example demonstrating Patent Filing. Uses [Conductor](https://github.com/c... |
| [regulatory-filing](legal/regulatory-filing/) | A Java Conductor workflow example demonstrating Regulatory Filing. Uses [Conductor](https://github.c... |
| [trademark-search](legal/trademark-search/) | A Java Conductor workflow example demonstrating Trademark Search. Uses [Conductor](https://github.co... |

## Media (20)

| Example | Description |
|---------|-------------|
| [ab-testing](media/ab-testing/) | A Java Conductor workflow example that orchestrates an end-to-end A/B test .  defining experiment va... |
| [advertising-workflow](media/advertising-workflow/) | A Java Conductor workflow example that orchestrates a digital advertising campaign lifecycle .  crea... |
| [analytics-reporting](media/analytics-reporting/) | A Java Conductor workflow example that orchestrates an analytics reporting pipeline .  collecting ra... |
| [content-archival](media/content-archival/) | A Java Conductor workflow example that orchestrates content archival .  scanning and identifying con... |
| [content-moderation](media/content-moderation/) | A Java Conductor workflow example that orchestrates content moderation .  submitting user content fo... |
| [content-publishing](media/content-publishing/) | A Java Conductor workflow example that orchestrates a content publishing pipeline .  creating drafts... |
| [content-recommendation](media/content-recommendation/) | A Java Conductor workflow example that orchestrates a content recommendation pipeline .  analyzing u... |
| [content-syndication](media/content-syndication/) | A Java Conductor workflow example that orchestrates content syndication .  selecting content from yo... |
| [digital-asset-management](media/digital-asset-management/) | A Java Conductor workflow example that orchestrates a digital asset management pipeline .  ingesting... |
| [email-campaign](media/email-campaign/) | A Java Conductor workflow example that orchestrates an email marketing campaign .  segmenting subscr... |
| [image-pipeline](media/image-pipeline/) | A Java Conductor workflow example that orchestrates an image processing pipeline .  uploading origin... |
| [live-streaming](media/live-streaming/) | A Java Conductor workflow example that orchestrates a live streaming pipeline .  provisioning ingest... |
| [personalization](media/personalization/) | A Java Conductor workflow example that orchestrates content personalization .  collecting user profi... |
| [podcast-workflow](media/podcast-workflow/) | A Java Conductor workflow example that orchestrates podcast production .  ingesting raw audio record... |
| [rights-management](media/rights-management/) | A Java Conductor workflow example that orchestrates media rights management .  checking license vali... |
| [seo-workflow](media/seo-workflow/) | A Java Conductor workflow example that orchestrates an SEO optimization pipeline .  auditing site he... |
| [social-media](media/social-media/) | A Java Conductor workflow example that orchestrates social media management .  creating formatted po... |
| [translation-pipeline](media/translation-pipeline/) | A Java Conductor workflow example that orchestrates a content translation pipeline .  detecting the ... |
| [user-generated-content](media/user-generated-content/) | A Java Conductor workflow example that orchestrates a UGC pipeline .  receiving user submissions wit... |
| [video-processing](media/video-processing/) | A creator uploads a 742MB 4K video. Your monolithic transcoder starts the 1080p rendition, runs out ... |

## Microservices (40)

| Example | Description |
|---------|-------------|
| [api-gateway](microservices/api-gateway/) | A mobile client hits your API. The request needs to be authenticated, routed to the right backend, a... |
| [api-gateway-routing](microservices/api-gateway-routing/) | API gateway routing workflow that authenticates requests, checks rate limits, routes to backend serv... |
| [backend-for-frontend](microservices/backend-for-frontend/) | Backend for Frontend pattern with platform-specific responses. Uses [Conductor](https://github.com/c... |
| [blue-green-deploy](microservices/blue-green-deploy/) | You need to ship v2.5.0 of the payment service. The old deploy process takes the service down for 90... |
| [blue-green-deployment](microservices/blue-green-deployment/) | Orchestrates blue-green deployment: deploy to green, validate, switch traffic, and monitor. Uses [Co... |
| [bulkhead-pattern](microservices/bulkhead-pattern/) | It's Tuesday at 3 AM. The recommendation service starts returning 504s because a third-party ML endp... |
| [canary-deployment](microservices/canary-deployment/) | You merge the PR, CI goes green, and you deploy to prod. Two minutes later, 100% of your users are h... |
| [canary-release](microservices/canary-release/) | Canary release with progressive traffic increase. Uses [Conductor](https://github.com/conductor-oss/... |
| [centralized-config-management](microservices/centralized-config-management/) | Centralized config management with staged rollout. Uses [Conductor](https://github.com/conductor-oss... |
| [choreography-vs-orchestration](microservices/choreography-vs-orchestration/) | Service A publishes an `order.created` event. Service B picks it up and reserves inventory. Service ... |
| [circuit-breaker-microservice](microservices/circuit-breaker-microservice/) | Circuit breaker pattern for resilient service calls. Uses [Conductor](https://github.com/conductor-o... |
| [config-management](microservices/config-management/) | Load, validate, deploy, and verify configuration. Uses [Conductor](https://github.com/conductor-oss/... |
| [cqrs-pattern](microservices/cqrs-pattern/) | CQRS pattern - Command side with validation, persistence, and read model update. Uses [Conductor](ht... |
| [data-migration](microservices/data-migration/) | Data migration with backup, transform, migrate, validate, and cutover. Uses [Conductor](https://gith... |
| [database-per-service](microservices/database-per-service/) | Database per service pattern with parallel queries and view composition. Uses [Conductor](https://gi... |
| [distributed-locking](microservices/distributed-locking/) | Distributed locking for concurrency control. Uses [Conductor](https://github.com/conductor-oss/condu... |
| [distributed-tracing](microservices/distributed-tracing/) | Distributed tracing with end-to-end request tracking. Uses [Conductor](https://github.com/conductor-... |
| [distributed-transactions](microservices/distributed-transactions/) | Distributed transactions with prepare-commit saga pattern. Uses [Conductor](https://github.com/condu... |
| [event-driven-microservices](microservices/event-driven-microservices/) | Event-driven microservices choreography via Conductor. Uses [Conductor](https://github.com/conductor... |
| [event-sourcing](microservices/event-sourcing/) | Event sourcing with append-only event log and state rebuild. Uses [Conductor](https://github.com/con... |
| [feature-flag-rollout](microservices/feature-flag-rollout/) | Manages feature flag lifecycle: create flag, staged rollout, monitor impact, and full activation or ... |
| [feature-flags](microservices/feature-flags/) | Route execution based on feature flag status. Uses [Conductor](https://github.com/conductor-oss/cond... |
| [graceful-service-shutdown](microservices/graceful-service-shutdown/) | Orchestrates graceful shutdown: stop accepting new work, drain in-flight tasks, checkpoint state, de... |
| [health-check-aggregation](microservices/health-check-aggregation/) | System-wide health check aggregation using FORK/JOIN. Uses [Conductor](https://github.com/conductor-... |
| [health-checks](microservices/health-checks/) | Check health of multiple services in parallel. Uses [Conductor](https://github.com/conductor-oss/con... |
| [idempotent-operations](microservices/idempotent-operations/) | Idempotent operations with duplicate detection. Uses [Conductor](https://github.com/conductor-oss/co... |
| [inter-service-communication](microservices/inter-service-communication/) | Orchestrates request-response communication between microservices. Uses [Conductor](https://github.c... |
| [load-balancing](microservices/load-balancing/) | Distribute requests across service instances in parallel. Uses [Conductor](https://github.com/conduc... |
| [multi-tenancy](microservices/multi-tenancy/) | Tenant-isolated workflows with per-tenant routing. Uses [Conductor](https://github.com/conductor-oss... |
| [outbox-pattern](microservices/outbox-pattern/) | Transactional outbox pattern for reliable event publishing. Uses [Conductor](https://github.com/cond... |
| [rate-limiter-microservice](microservices/rate-limiter-microservice/) | Distributed rate limiting workflow that checks quotas, processes or rejects requests, and updates co... |
| [request-aggregation](microservices/request-aggregation/) | Aggregates data from multiple microservices in parallel using FORK_JOIN, then merges results into a ... |
| [secret-rotation](microservices/secret-rotation/) | Rotate secrets across services securely. Uses [Conductor](https://github.com/conductor-oss/conductor... |
| [service-decomposition](microservices/service-decomposition/) | Strangler fig pattern: routes requests to monolith or microservice based on feature flags, with opti... |
| [service-discovery](microservices/service-discovery/) | Discover services, select instance, call with failover. Uses [Conductor](https://github.com/conducto... |
| [service-mesh-orchestration](microservices/service-mesh-orchestration/) | Orchestrates service mesh configuration: deploy sidecar proxies, configure mTLS, set traffic policie... |
| [service-orchestration](microservices/service-orchestration/) | Orchestrate auth, catalog, cart, and checkout microservices. Uses [Conductor](https://github.com/con... |
| [service-registry](microservices/service-registry/) | Service registry workflow that registers a service, performs a health check, and discovers the servi... |
| [service-versioning](microservices/service-versioning/) | API version management with version routing. Uses [Conductor](https://github.com/conductor-oss/condu... |
| [shared-nothing-architecture](microservices/shared-nothing-architecture/) | Shared nothing architecture with fully independent services. Uses [Conductor](https://github.com/con... |

## Nonprofit (10)

| Example | Description |
|---------|-------------|
| [beneficiary-tracking](nonprofit/beneficiary-tracking/) | A Java Conductor workflow example demonstrating Beneficiary Tracking. Uses [Conductor](https://githu... |
| [compliance-nonprofit](nonprofit/compliance-nonprofit/) | A Java Conductor workflow example demonstrating Compliance Nonprofit. Uses [Conductor](https://githu... |
| [donor-management](nonprofit/donor-management/) | A Java Conductor workflow example demonstrating Donor Management. Uses [Conductor](https://github.co... |
| [event-fundraising](nonprofit/event-fundraising/) | A Java Conductor workflow example demonstrating Event Fundraising. Uses [Conductor](https://github.c... |
| [fundraising-campaign](nonprofit/fundraising-campaign/) | A Java Conductor workflow example demonstrating Fundraising Campaign. Uses [Conductor](https://githu... |
| [grant-management](nonprofit/grant-management/) | A Java Conductor workflow example demonstrating Grant Management. Uses [Conductor](https://github.co... |
| [impact-reporting](nonprofit/impact-reporting/) | A Java Conductor workflow example demonstrating Impact Reporting. Uses [Conductor](https://github.co... |
| [nonprofit-donation](nonprofit/nonprofit-donation/) | A Java Conductor workflow example demonstrating Nonprofit Donation. Uses [Conductor](https://github.... |
| [program-evaluation](nonprofit/program-evaluation/) | A Java Conductor workflow example demonstrating Program Evaluation. Uses [Conductor](https://github.... |
| [volunteer-coordination](nonprofit/volunteer-coordination/) | A Java Conductor workflow example demonstrating Volunteer Coordination. Uses [Conductor](https://git... |

## Project Mgmt (10)

| Example | Description |
|---------|-------------|
| [change-request](project-mgmt/change-request/) | A Java Conductor workflow example for managing project change requests end-to-end .  from initial su... |
| [milestone-tracking](project-mgmt/milestone-tracking/) | A Java Conductor workflow example that automates milestone tracking .  checking progress by counting... |
| [project-closure](project-mgmt/project-closure/) | A Java Conductor workflow example for closing out a project .  reviewing all deliverables against ac... |
| [project-kickoff](project-mgmt/project-kickoff/) | A Java Conductor workflow example that automates project kickoff .  defining project scope with obje... |
| [resource-allocation](project-mgmt/resource-allocation/) | A Java Conductor workflow example that automates resource allocation for projects .  assessing deman... |
| [retrospective](project-mgmt/retrospective/) | A Java Conductor workflow example for automating sprint retrospectives .  collecting team feedback (... |
| [risk-management](project-mgmt/risk-management/) | A Java Conductor workflow example for project risk management .  identifying risks, assessing their ... |
| [sprint-planning](project-mgmt/sprint-planning/) | A Java Conductor workflow example that automates sprint planning .  selecting user stories from the ... |
| [stakeholder-reporting](project-mgmt/stakeholder-reporting/) | A Java Conductor workflow example for automated stakeholder reporting .  collecting project updates ... |
| [task-assignment](project-mgmt/task-assignment/) | A Java Conductor workflow example that automates task assignment .  analyzing the task to extract re... |

## Real Estate (10)

| Example | Description |
|---------|-------------|
| [commission-calculation](real-estate/commission-calculation/) | A Java Conductor workflow example for calculating real estate agent commissions .  computing the bas... |
| [escrow-management](real-estate/escrow-management/) | A Java Conductor workflow example for managing the escrow lifecycle in a real estate transaction .  ... |
| [lease-management](real-estate/lease-management/) | A Java Conductor workflow example for managing the full lease lifecycle .  creating a lease agreemen... |
| [maintenance-request](real-estate/maintenance-request/) | A Java Conductor workflow example for handling tenant maintenance requests end-to-end .  submitting ... |
| [mortgage-application](real-estate/mortgage-application/) | A Java Conductor workflow example for processing mortgage applications .  accepting the application,... |
| [property-inspection](real-estate/property-inspection/) | A Java Conductor workflow example for managing property inspections .  scheduling the inspection wit... |
| [property-valuation](real-estate/property-valuation/) | A Java Conductor workflow example for automated property valuation .  collecting comparable sales da... |
| [real-estate-listing](real-estate/real-estate-listing/) | A Java Conductor workflow example for publishing property listings .  creating the listing with addr... |
| [tenant-screening](real-estate/tenant-screening/) | A Java Conductor workflow example for screening prospective tenants .  accepting the rental applicat... |
| [title-search](real-estate/title-search/) | A Java Conductor workflow example for performing property title searches .  searching county records... |

## Resilience (30)

| Example | Description |
|---------|-------------|
| [circuit-breaker](resilience/circuit-breaker/) | A Java Conductor workflow example implementing the circuit breaker pattern. Checking the circuit sta... |
| [compensation-workflows](resilience/compensation-workflows/) | A Java Conductor workflow example demonstrating the compensation pattern. Executing a sequence of st... |
| [dead-letter](resilience/dead-letter/) | A Java Conductor workflow example demonstrating the dead letter queue pattern. Processing messages w... |
| [error-classification](resilience/error-classification/) | A Java Conductor workflow example demonstrating error classification .  distinguishing retryable err... |
| [error-notification](resilience/error-notification/) | A Java Conductor workflow example demonstrating error notification .  processing orders with a failu... |
| [exponential-max-retry](resilience/exponential-max-retry/) | A Java Conductor workflow example demonstrating exponential backoff retry with a maximum retry limit... |
| [failure-workflow](resilience/failure-workflow/) | A Java Conductor workflow example demonstrating Conductor's failure workflow feature .  when the mai... |
| [fallback-tasks](resilience/fallback-tasks/) | A Java Conductor workflow example demonstrating tiered fallback .  trying a primary API first, falli... |
| [graceful-degradation](resilience/graceful-degradation/) | The recommendation engine goes down at 9 AM on Black Friday. Your product page has a "You might also... |
| [idempotent-workers](resilience/idempotent-workers/) | A Java Conductor workflow example demonstrating idempotent workers.  processing a payment charge an... |
| [multi-step-compensation](resilience/multi-step-compensation/) | A Java Conductor workflow example demonstrating multi-step compensation .  creating an account, sett... |
| [network-partitions](resilience/network-partitions/) | A Java Conductor workflow example demonstrating resilience to network partitions .  a worker that tr... |
| [optional-tasks](resilience/optional-tasks/) | A Java Conductor workflow example demonstrating optional tasks .  a required core task that must suc... |
| [partial-failure-recovery](resilience/partial-failure-recovery/) | A Java Conductor workflow example demonstrating partial failure recovery .  a three-step sequential ... |
| [per-task-retry](resilience/per-task-retry/) | A Java Conductor workflow example demonstrating per-task retry configuration .  each task in the wor... |
| [poll-timeout](resilience/poll-timeout/) | A Java Conductor workflow example demonstrating the pollTimeoutSeconds setting .  defining how long ... |
| [response-timeout](resilience/response-timeout/) | A Java Conductor workflow example demonstrating the responseTimeoutSeconds setting .  detecting work... |
| [retry-exponential](resilience/retry-exponential/) | A Java Conductor workflow example demonstrating exponential backoff retry .  a worker that simulates... |
| [retry-fixed](resilience/retry-fixed/) | A Java Conductor workflow example demonstrating fixed retry strategy .  retrying a failing task with... |
| [retry-jitter](resilience/retry-jitter/) | A Java Conductor workflow example demonstrating jitter in retry delays .  adding randomized delay be... |
| [retry-linear](resilience/retry-linear/) | A Java Conductor workflow example demonstrating linear backoff retry .  delays increase linearly wit... |
| [saga-fork-join](resilience/saga-fork-join/) | A Java Conductor workflow example demonstrating the saga pattern with parallel execution .  booking ... |
| [saga-pattern](resilience/saga-pattern/) | A Java Conductor workflow example demonstrating the saga pattern. Booking a flight, reserving a hote... |
| [saga-payment-inventory](resilience/saga-payment-inventory/) | A Java Conductor workflow example demonstrating the saga pattern for e-commerce .  reserving invento... |
| [self-healing](resilience/self-healing/) | A Java Conductor workflow example demonstrating self-healing .  checking service health, diagnosing ... |
| [timeout-policies](resilience/timeout-policies/) | A Java Conductor workflow example demonstrating Conductor's timeout policies .  configuring differen... |
| [transient-vs-permanent](resilience/transient-vs-permanent/) | A Java Conductor workflow example demonstrating smart error classification .  a worker that distingu... |
| [worker-health-checks](resilience/worker-health-checks/) | A Java Conductor workflow example demonstrating worker health monitoring .  running a task and using... |
| [workflow-recovery](resilience/workflow-recovery/) | A Java Conductor workflow example demonstrating workflow recovery. Showing that Conductor persists w... |
| [workflow-timeout](resilience/workflow-timeout/) | A Java Conductor workflow example demonstrating workflow-level timeouts .  setting a maximum executi... |

## Scheduling (30)

| Example | Description |
|---------|-------------|
| [alerting-pipeline](scheduling/alerting-pipeline/) | A Java Conductor workflow example for building an alerting pipeline .  evaluating metric rules again... |
| [anomaly-detection](scheduling/anomaly-detection/) | Request latency on your checkout service crept from 120ms to 450ms over six hours. Nobody noticed be... |
| [apm-workflow](scheduling/apm-workflow/) | A Java Conductor workflow example for application performance monitoring (APM) .  collecting distrib... |
| [batch-scheduling](scheduling/batch-scheduling/) | Every night at 2 AM, four cron jobs fire simultaneously: the ETL import, the report generator, the d... |
| [calendar-integration](scheduling/calendar-integration/) | A Java Conductor workflow example for calendar integration .  fetching events from a calendar, compa... |
| [capacity-monitoring](scheduling/capacity-monitoring/) | A Java Conductor workflow example for capacity monitoring .  measuring current resource utilization ... |
| [change-tracking](scheduling/change-tracking/) | A Java Conductor workflow example for tracking infrastructure changes .  detecting when a resource c... |
| [compliance-monitoring](scheduling/compliance-monitoring/) | A Java Conductor workflow example for compliance monitoring .  scanning infrastructure resources, ev... |
| [cost-monitoring](scheduling/cost-monitoring/) | A Java Conductor workflow example for cloud cost monitoring .  collecting billing data across accoun... |
| [cron-job-orchestration](scheduling/cron-job-orchestration/) | The nightly data export runs at 2 AM. It creates 4 GB of temp files in `/tmp`, writes results to S3,... |
| [custom-metrics](scheduling/custom-metrics/) | A Java Conductor workflow example for custom metrics .  defining business-specific metrics, collecti... |
| [deadline-management](scheduling/deadline-management/) | The SOC 2 compliance filing was due Friday. On Monday morning, the auditor emails asking where it is... |
| [dependency-mapping](scheduling/dependency-mapping/) | A Java Conductor workflow example for mapping service dependencies .  discovering services in an env... |
| [distributed-logging](scheduling/distributed-logging/) | A Java Conductor workflow example for distributed logging .  collecting logs from multiple services ... |
| [health-dashboard](scheduling/health-dashboard/) | A Java Conductor workflow example for building a health dashboard .  checking the health of API serv... |
| [log-aggregation](scheduling/log-aggregation/) | A Java Conductor workflow example for log aggregation .  collecting logs from multiple sources, pars... |
| [maintenance-windows](scheduling/maintenance-windows/) | A Java Conductor workflow example for maintenance window management .  checking whether the current ... |
| [metrics-collection](scheduling/metrics-collection/) | A Java Conductor workflow example for metrics collection .  gathering infrastructure metrics (CPU, m... |
| [performance-profiling](scheduling/performance-profiling/) | A Java Conductor workflow example for performance profiling .  instrumenting a service, collecting C... |
| [predictive-monitoring](scheduling/predictive-monitoring/) | A Java Conductor workflow example for predictive monitoring .  collecting historical metric data, tr... |
| [recurring-billing](scheduling/recurring-billing/) | A Java Conductor workflow example for recurring billing .  generating invoices on a recurring schedu... |
| [root-cause-analysis](scheduling/root-cause-analysis/) | A Java Conductor workflow example for automated root cause analysis .  detecting an issue, collectin... |
| [scheduled-reports](scheduling/scheduled-reports/) | A Java Conductor workflow example for scheduled report generation .  querying data sources, formatti... |
| [sla-scheduling](scheduling/sla-scheduling/) | A Java Conductor workflow example for SLA-aware scheduling .  prioritizing tickets by SLA urgency, e... |
| [threshold-alerting](scheduling/threshold-alerting/) | A Java Conductor workflow example for threshold alerting .  checking a metric against warning and cr... |
| [time-based-triggers](scheduling/time-based-triggers/) | A Java Conductor workflow example for time-based triggering .  checking the current time and routing... |
| [timezone-handling](scheduling/timezone-handling/) | A Java Conductor workflow example for timezone handling .  detecting a user's timezone, converting r... |
| [trace-collection](scheduling/trace-collection/) | A Java Conductor workflow example for distributed trace collection .  instrumenting services, collec... |
| [uptime-monitoring](scheduling/uptime-monitoring/) | A Java Conductor workflow example for uptime monitoring .  checking endpoint availability, logging r... |
| [user-behavior-analytics](scheduling/user-behavior-analytics/) | A Java Conductor workflow example for user behavior analytics .  collecting user events, sessionizin... |

## Security (30)

| Example | Description |
|---------|-------------|
| [access-review](security/access-review/) | A Java Conductor workflow example for access review .  collecting user entitlements across systems, ... |
| [api-key-rotation](security/api-key-rotation/) | A Java Conductor workflow example for API key rotation .  generating a new key, running both old and... |
| [audit-logging](security/audit-logging/) | A Java Conductor workflow example for audit logging .  capturing security-relevant events, enriching... |
| [authentication-workflow](security/authentication-workflow/) | A Java Conductor workflow example implementing a multi-step authentication pipeline .  validating us... |
| [authorization-rbac](security/authorization-rbac/) | A Java Conductor workflow example implementing role-based access control .  resolving a user's roles... |
| [certificate-management](security/certificate-management/) | A Java Conductor workflow example for TLS certificate management .  inventorying all certificates, a... |
| [compliance-reporting](security/compliance-reporting/) | A Java Conductor workflow example automating compliance report generation .  collecting evidence art... |
| [data-classification](security/data-classification/) | A Java Conductor workflow example automating data classification .  scanning data stores (databases,... |
| [data-encryption](security/data-encryption/) | A Java Conductor workflow example for data encryption .  classifying data sensitivity, generating ap... |
| [data-masking](security/data-masking/) | A Java Conductor workflow example automating data masking .  scanning a data source to identify sens... |
| [ddos-mitigation](security/ddos-mitigation/) | A Java Conductor workflow example for DDoS mitigation .  detecting abnormal traffic patterns, classi... |
| [devsecops-pipeline](security/devsecops-pipeline/) | A Java Conductor workflow example for a DevSecOps pipeline .  running SAST (static analysis), SCA (d... |
| [gdpr-compliance](security/gdpr-compliance/) | A customer emailed asking you to delete all their data. Legal forwarded it to engineering, who found... |
| [identity-provisioning](security/identity-provisioning/) | A Java Conductor workflow example for identity provisioning .  creating a user identity, assigning d... |
| [intrusion-detection](security/intrusion-detection/) | A Java Conductor workflow example for intrusion detection .  analyzing security events, correlating ... |
| [network-segmentation](security/network-segmentation/) | A Java Conductor workflow example for network segmentation .  defining network zones (DMZ, internal,... |
| [oauth-token-management](security/oauth-token-management/) | A Java Conductor workflow example for OAuth 2.0 token lifecycle management .  validating client cred... |
| [penetration-testing](security/penetration-testing/) | A Java Conductor workflow example for automated penetration testing .  discovering target endpoints ... |
| [privileged-access](security/privileged-access/) | A Java Conductor workflow example for just-in-time privileged access management (PAM) .  receiving a... |
| [secrets-management](security/secrets-management/) | An engineer who left the company six weeks ago still has working API keys. You know this because one... |
| [security-incident](security/security-incident/) | A Java Conductor workflow example for security incident response .  triaging alerts by type and seve... |
| [security-orchestration](security/security-orchestration/) | A Java Conductor workflow example for security orchestration .  ingesting security alerts, enriching... |
| [security-posture](security/security-posture/) | A Java Conductor workflow example for security posture assessment .  evaluating infrastructure secur... |
| [security-training](security/security-training/) | A Java Conductor workflow example for automated security awareness campaigns .  assigning training m... |
| [soc2-automation](security/soc2-automation/) | A Java Conductor workflow example for automating SOC2 compliance .  collecting control implementatio... |
| [threat-intelligence](security/threat-intelligence/) | A Java Conductor workflow example for threat intelligence .  ingesting threat feeds, enriching indic... |
| [vendor-risk](security/vendor-risk/) | A Java Conductor workflow example for vendor risk assessment .  collecting security questionnaires f... |
| [vulnerability-scanning](security/vulnerability-scanning/) | A critical CVE was announced on Monday. Your security team added it to the sprint on Tuesday. The pa... |
| [waf-management](security/waf-management/) | A Java Conductor workflow example for Web Application Firewall (WAF) management .  analyzing traffic... |
| [zero-trust-verification](security/zero-trust-verification/) | A Java Conductor workflow example for zero trust verification .  verifying user identity, assessing ... |

## Supply Chain (20)

| Example | Description |
|---------|-------------|
| [bid-management](supply-chain/bid-management/) | A Java Conductor workflow example for competitive bid management .  creating bid packages for projec... |
| [cold-chain](supply-chain/cold-chain/) | A Java Conductor workflow example for cold chain monitoring .  reading temperature sensor data for s... |
| [compliance-vendor](supply-chain/compliance-vendor/) | A Java Conductor workflow example for vendor compliance management .  assessing a vendor's adherence... |
| [contract-lifecycle](supply-chain/contract-lifecycle/) | Your $250K logistics contract auto-renewed last month at a 20% rate increase. The opt-out window clo... |
| [customs-clearance](supply-chain/customs-clearance/) | A Java Conductor workflow example for international customs clearance .  filing customs declarations... |
| [demand-forecasting](supply-chain/demand-forecasting/) | A Java Conductor workflow example for demand forecasting .  collecting historical sales and market d... |
| [freight-management](supply-chain/freight-management/) | A Java Conductor workflow example for freight management .  booking a carrier (e.g., FastFreight Exp... |
| [goods-receipt](supply-chain/goods-receipt/) | A Java Conductor workflow example for inbound goods receipt processing .  receiving a shipment at th... |
| [inventory-optimization](supply-chain/inventory-optimization/) | A Java Conductor workflow example for inventory optimization .  analyzing current stock levels acros... |
| [last-mile-delivery](supply-chain/last-mile-delivery/) | A Java Conductor workflow example for last mile delivery .  assigning a driver to an order (e.g., OR... |
| [logistics-optimization](supply-chain/logistics-optimization/) | A Java Conductor workflow example for logistics optimization .  analyzing demand across 40+ orders d... |
| [procurement-workflow](supply-chain/procurement-workflow/) | The ops team submitted a purchase request for 50 server racks on Tuesday. It sat in a VP's inbox for... |
| [purchase-order](supply-chain/purchase-order/) | A Java Conductor workflow example for purchase order lifecycle management .  creating a PO with line... |
| [quality-inspection](supply-chain/quality-inspection/) | A Java Conductor workflow example for quality inspection .  pulling samples from a production batch,... |
| [reverse-logistics](supply-chain/reverse-logistics/) | A Java Conductor workflow example for reverse logistics .  receiving returned products (e.g., defect... |
| [rfp-automation](supply-chain/rfp-automation/) | A Java Conductor workflow example for request-for-proposal automation .  creating an RFP for a proje... |
| [supplier-evaluation](supply-chain/supplier-evaluation/) | A Java Conductor workflow example for supplier evaluation .  collecting performance data across all ... |
| [supply-chain-mgmt](supply-chain/supply-chain-mgmt/) | A Java Conductor workflow example for end-to-end supply chain management following the SCOR model . ... |
| [vendor-onboarding](supply-chain/vendor-onboarding/) | A Java Conductor workflow example for vendor onboarding .  receiving a new vendor application with b... |
| [warehouse-management](supply-chain/warehouse-management/) | A Java Conductor workflow example for warehouse management .  receiving inbound goods at the dock, p... |

## Task Patterns (40)

| Example | Description |
|---------|-------------|
| [bulk-operations](task-patterns/bulk-operations/) | Bulk operations demo .  two-step workflow used for bulk start, pause, resume, and terminate. Uses [C... |
| [chaining-http-tasks](task-patterns/chaining-http-tasks/) | Chain HTTP system tasks for API orchestration. Uses [Conductor](https://github.com/conductor-oss/con... |
| [do-while](task-patterns/do-while/) | DO_WHILE loop demo: processes items in a batch one at a time, iterating until the batch is complete,... |
| [dynamic-fork](task-patterns/dynamic-fork/) | You need to process N items in parallel, but N is only known at runtime. a user submits 3 URLs today... |
| [event-handlers](task-patterns/event-handlers/) | Workflow triggered by external events. Processes the event type and payload. Uses [Conductor](https:... |
| [exclusive-join](task-patterns/exclusive-join/) | EXCLUSIVE_JOIN demo .  query three vendors in parallel, wait for all responses, then select the best... |
| [external-payload](task-patterns/external-payload/) | External payload storage .  generate a summary and storage reference instead of returning large data... |
| [fan-out-fan-in](task-patterns/fan-out-fan-in/) | Fan-Out/Fan-In. scatter-gather image processing using FORK_JOIN_DYNAMIC. Splits a variable-length im... |
| [fork-in-do-while](task-patterns/fork-in-do-while/) | FORK inside DO_WHILE demo .  iterative parallel processing. Each iteration forks parallel batch-proc... |
| [fork-join](task-patterns/fork-join/) | FORK_JOIN demo: fetch product details, inventory status, and customer reviews in parallel, then merg... |
| [graphql-api](task-patterns/graphql-api/) | GraphQL API demo .  single task workflow to demonstrate REST vs GraphQL query patterns. Uses [Conduc... |
| [idempotent-start](task-patterns/idempotent-start/) | Idempotent start demo .  demonstrates correlationId-based dedup and search-based idempotency. Uses [... |
| [inline-tasks](task-patterns/inline-tasks/) | Demonstrates INLINE tasks. JavaScript that runs on the Conductor server with no workers. Uses [Condu... |
| [jq-transform-advanced](task-patterns/jq-transform-advanced/) | Advanced JQ data transformations .  flatten orders, aggregate by customer, classify into tiers. Uses... |
| [map-reduce](task-patterns/map-reduce/) | MapReduce Pattern. Splits log files into parallel analysis tasks using FORK_JOIN_DYNAMIC, then aggre... |
| [nested-sub-workflows](task-patterns/nested-sub-workflows/) | Three-level nested order processing .  order fulfillment (Level 1) delegates to a payment sub-workfl... |
| [nested-switch](task-patterns/nested-switch/) | Multi-level decision tree using nested SWITCH tasks with value-param. Uses [Conductor](https://githu... |
| [passing-output-to-input](task-patterns/passing-output-to-input/) | Shows all the ways to pass data between tasks. Uses [Conductor](https://github.com/conductor-oss/con... |
| [rate-limiting](task-patterns/rate-limiting/) | Rate limiting demo. demonstrates task-level rate limiting with concurrency and frequency constraints... |
| [sequential-tasks](task-patterns/sequential-tasks/) | Sequential ETL pipeline .  extract, transform, load. Three workers process data in order. Uses [Cond... |
| [set-variable](task-patterns/set-variable/) | Demonstrates SET_VARIABLE system task for storing intermediate state accessible via ${workflow.varia... |
| [signals](task-patterns/signals/) | Signals demo .  send data to running workflows via WAIT task completion. Two WAIT tasks pause the wo... |
| [simple-plus-system](task-patterns/simple-plus-system/) | Combines SIMPLE workers with INLINE system tasks. Uses [Conductor](https://github.com/conductor-oss/... |
| [sub-workflows](task-patterns/sub-workflows/) | SUB_WORKFLOW demo: an order processing workflow that delegates payment handling to a reusable child ... |
| [switch-default-case](task-patterns/switch-default-case/) | Fallback routing for unmatched payment methods using SWITCH with defaultCase. Uses [Conductor](https... |
| [switch-javascript](task-patterns/switch-javascript/) | SWITCH with JavaScript evaluator for complex routing based on amount, customerType, and region. Uses... |
| [switch-plus-fork](task-patterns/switch-plus-fork/) | SWITCH + FORK demo .  conditional parallel execution. Batch type triggers parallel lanes A and B; de... |
| [switch-task](task-patterns/switch-task/) | SWITCH task demo. routes support tickets to different handlers based on priority level (LOW/MEDIUM/H... |
| [sync-execution](task-patterns/sync-execution/) | Simple workflow for demonstrating sync vs async execution. Uses [Conductor](https://github.com/condu... |
| [system-tasks](task-patterns/system-tasks/) | Demonstrates INLINE and JSON_JQ_TRANSFORM system tasks .  no workers needed. Uses [Conductor](https:... |
| [task-definitions](task-patterns/task-definitions/) | Task definitions test .  runs td_fast_task to verify task definition configuration. Uses [Conductor]... |
| [task-domains](task-patterns/task-domains/) | Task Domains demo .  route tasks to specific worker groups using domains. Uses [Conductor](https://g... |
| [task-input-templates](task-patterns/task-input-templates/) | Shows reusable parameter mapping patterns. Uses [Conductor](https://github.com/conductor-oss/conduct... |
| [task-priority](task-patterns/task-priority/) | Workflow priority demo .  priority levels 0-99 (higher = more important). Uses [Conductor](https://g... |
| [terminate-task](task-patterns/terminate-task/) | Early exit with TERMINATE based on validation. Uses [Conductor](https://github.com/conductor-oss/con... |
| [wait-for-event](task-patterns/wait-for-event/) | WAIT task demo. pauses a workflow durably until an external system sends a signal (approval, webhook... |
| [workflow-archival](task-patterns/workflow-archival/) | Archival demo workflow .  single task for demonstrating cleanup policies. Uses [Conductor](https://g... |
| [workflow-metadata](task-patterns/workflow-metadata/) | Demonstrates workflow metadata and search. Uses [Conductor](https://github.com/conductor-oss/conduct... |
| [workflow-variables](task-patterns/workflow-variables/) | Shows how variables and expressions work across tasks. Uses [Conductor](https://github.com/conductor... |
| [workflow-versioning](task-patterns/workflow-versioning/) | Run multiple versions of the same workflow side by side .  version 1 does calculate-then-audit, vers... |

## Telecom (10)

| Example | Description |
|---------|-------------|
| [billing-telecom](telecom/billing-telecom/) | A Java Conductor workflow example that orchestrates the telecom billing cycle .  collecting usage re... |
| [capacity-mgmt-telecom](telecom/capacity-mgmt-telecom/) | A Java Conductor workflow example that orchestrates telecom network capacity management .  monitorin... |
| [customer-churn](telecom/customer-churn/) | A Java Conductor workflow example that orchestrates customer churn prevention .  detecting at-risk s... |
| [network-monitoring](telecom/network-monitoring/) | A Java Conductor workflow example that orchestrates telecom network monitoring .  polling metrics fr... |
| [number-porting](telecom/number-porting/) | A Java Conductor workflow example that orchestrates phone number porting between carriers .  submitt... |
| [roaming-management](telecom/roaming-management/) | A Java Conductor workflow example that orchestrates telecom roaming management .  detecting when a s... |
| [service-activation](telecom/service-activation/) | A Java Conductor workflow example that orchestrates telecom service activation .  validating a servi... |
| [telecom-provisioning](telecom/telecom-provisioning/) | A Java Conductor workflow example that orchestrates telecom service provisioning .  creating a servi... |
| [trouble-ticket](telecom/trouble-ticket/) | A Java Conductor workflow example that orchestrates the telecom trouble ticket lifecycle .  opening ... |
| [usage-analytics](telecom/usage-analytics/) | A Java Conductor workflow example that orchestrates telecom usage analytics .  collecting call detai... |

## Travel (10)

| Example | Description |
|---------|-------------|
| [car-rental](travel/car-rental/) | Car rental: search, select, book, pickup, return. Uses [Conductor](https://github.com/conductor-oss/... |
| [expense-reporting](travel/expense-reporting/) | Expense reporting: collect, categorize, submit, approve, reimburse. Uses [Conductor](https://github.... |
| [hotel-booking](travel/hotel-booking/) | Hotel booking: search, filter, book, confirm, reminder. Uses [Conductor](https://github.com/conducto... |
| [itinerary-planning](travel/itinerary-planning/) | Itinerary planning: preferences, search, optimize, book, finalize. Uses [Conductor](https://github.c... |
| [reimbursement](travel/reimbursement/) | Reimbursement: submit, validate, approve, process, notify. Uses [Conductor](https://github.com/condu... |
| [travel-analytics](travel/travel-analytics/) | Travel analytics: collect, aggregate, analyze, report. Uses [Conductor](https://github.com/conductor... |
| [travel-approval](travel/travel-approval/) | An engineer submits a $4,000 conference trip request. Their manager approves it by replying "looks g... |
| [travel-booking](travel/travel-booking/) | A traveler books a flight from SFO to JFK, a hotel in Manhattan, and then the car rental fails. the ... |
| [travel-policy](travel/travel-policy/) | Travel policy with SWITCH for compliant/exception. Uses [Conductor](https://github.com/conductor-oss... |
| [visa-processing](travel/visa-processing/) | Visa processing: collect docs, validate, submit, track, receive. Uses [Conductor](https://github.com... |

## User Mgmt (20)

| Example | Description |
|---------|-------------|
| [account-deletion](user-mgmt/account-deletion/) | A Java Conductor workflow example demonstrating Account Deletion. Uses [Conductor](https://github.co... |
| [bulk-user-import](user-mgmt/bulk-user-import/) | A Java Conductor workflow example demonstrating Bulk User Import. Uses [Conductor](https://github.co... |
| [data-export-request](user-mgmt/data-export-request/) | A Java Conductor workflow example demonstrating Data Export Request. Uses [Conductor](https://github... |
| [email-verification](user-mgmt/email-verification/) | A Java Conductor workflow example demonstrating Email Verification. Uses [Conductor](https://github.... |
| [gdpr-consent](user-mgmt/gdpr-consent/) | A Java Conductor workflow example demonstrating GDPR Consent. Uses [Conductor](https://github.com/co... |
| [multi-factor-auth](user-mgmt/multi-factor-auth/) | A Java Conductor workflow example demonstrating Multi Factor Auth. Uses [Conductor](https://github.c... |
| [notification-preferences](user-mgmt/notification-preferences/) | A Java Conductor workflow example demonstrating Notification Preferences. Uses [Conductor](https://g... |
| [nps-scoring](user-mgmt/nps-scoring/) | A Java Conductor workflow example demonstrating NPS Scoring. Uses [Conductor](https://github.com/con... |
| [password-reset](user-mgmt/password-reset/) | User clicks "Reset Password." The email takes eight minutes because your SMTP relay is backed up. Th... |
| [permission-sync](user-mgmt/permission-sync/) | A Java Conductor workflow example demonstrating Permission Sync. Uses [Conductor](https://github.com... |
| [profile-update](user-mgmt/profile-update/) | A Java Conductor workflow example demonstrating Profile Update. Uses [Conductor](https://github.com/... |
| [role-management](user-mgmt/role-management/) | A Java Conductor workflow example demonstrating Role Management. Uses [Conductor](https://github.com... |
| [session-management](user-mgmt/session-management/) | A Java Conductor workflow example demonstrating Session Management. Uses [Conductor](https://github.... |
| [social-login](user-mgmt/social-login/) | A Java Conductor workflow example demonstrating Social Login. Uses [Conductor](https://github.com/co... |
| [user-analytics](user-mgmt/user-analytics/) | A Java Conductor workflow example demonstrating User Analytics. Uses [Conductor](https://github.com/... |
| [user-feedback](user-mgmt/user-feedback/) | A Java Conductor workflow example for processing user feedback .  ingesting submissions from any cha... |
| [user-migration](user-mgmt/user-migration/) | A Java Conductor workflow example for migrating user records between databases .  extracting from a ... |
| [user-onboarding](user-mgmt/user-onboarding/) | A Java Conductor workflow example that onboards a new user end-to-end: creates an account with a det... |
| [user-registration](user-mgmt/user-registration/) | A Java Conductor workflow example for user registration .  validating username and email format, cre... |
| [user-survey](user-mgmt/user-survey/) | A Java Conductor workflow example for running user satisfaction surveys end-to-end .  creating a sur... |

---

## Example Structure

Every example follows the same layout:

```
examples/<category>/<example>/
├── pom.xml                    # Standalone Maven build (Java 21)
├── run.sh                     # Launcher script
├── README.md                  # Full documentation
├── src/main/java/             # Main class + workers
├── src/main/resources/        # workflow.json
└── src/test/java/             # Unit tests

```

All examples support `--workers` flag for worker-only mode.

## Legacy Examples

The original Gradle-based examples are preserved in [old/](old/).
