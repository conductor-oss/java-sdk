# Structured Customer Email Analysis: Prompt, Generate, Parse, Validate

A customer sends an email about a product. The system needs to classify the intent (inquiry/complaint/return/purchase), detect sentiment (positive/neutral/negative), suggest products from the catalog, and draft a reply -- all as structured JSON. If the LLM hallucinates a product ID that isn't in the catalog or produces a reply shorter than 50 characters, the validation step catches it.

## Workflow

```
customerEmail, productCatalog
       │
       ▼
┌──────────────────┐
│ chain_prompt     │  Build few-shot prompt with expected JSON format
└────────┬─────────┘
         │  formattedPrompt, expectedFormat
         ▼
┌──────────────────┐
│ chain_generate   │  Call OpenAI gpt-4o-mini
└────────┬─────────┘
         │  rawText (JSON string), tokenUsage
         ▼
┌──────────────────┐
│ chain_parse      │  Parse JSON string into Map
└────────┬─────────┘
         │  parsedData
         ▼
┌──────────────────┐
│ chain_validate   │  Run 4 business rule checks
└──────────────────┘
         │
         ▼
   validatedResult, checks, allChecksPassed
```

## Workers

**ChainPromptWorker** (`chain_prompt`) -- Builds a structured prompt with the role `"customer service AI assistant"`, includes the `productCatalog` string, two few-shot examples (a positive inquiry and a neutral return), and the expected JSON format with fields `intent`, `sentiment`, `suggestedProducts`, `draftReply`. Appends `"Customer email:\n" + customerEmail` at the end.

**ChainGenerateWorker** (`chain_generate`) -- Requires `CONDUCTOR_OPENAI_API_KEY`. Calls OpenAI Chat Completions with `gpt-4o-mini`, `temperature: 0.2`, `max_tokens: 1024`. Extracts the response via Jackson's `root.at("/choices/0/message/content")` and token usage from `/usage/prompt_tokens`, `/usage/completion_tokens`, `/usage/total_tokens`.

**ChainParseWorker** (`chain_parse`) -- Attempts `MAPPER.readValue(rawText, Map.class)` to parse the LLM's JSON output into a `Map<String, Object>`. Returns `FAILED` if the JSON is malformed, with the parse exception message in `error`.

**ChainValidateWorker** (`chain_validate`) -- Runs 4 validation checks against `parsedData`: (1) `valid_intent` -- intent must be in `Set.of("inquiry", "complaint", "return", "purchase")`, (2) `valid_sentiment` -- must be in `Set.of("positive", "neutral", "negative")`, (3) `products_in_catalog` -- splits `productCatalog` on commas, trims, and checks that `suggestedProducts` are a subset via `catalogProducts.containsAll(suggestedProducts)`, (4) `reply_length` -- `draftReply.length()` must be >50 and <2000. Returns `allChecksPassed` as a boolean.

## Tests

19 tests cover prompt construction, API generation, JSON parsing with invalid input, and all 4 validation rules.

## Further Reading

- [RUNNING.md](../../RUNNING.md) -- how to build and run this example
