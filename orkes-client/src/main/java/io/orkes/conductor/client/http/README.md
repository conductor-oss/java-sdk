## TODO
* I didn't check workflow yet.

## NOT covered by sdk

* Admin Resource
* Environment Resource
* Event Execution
* health check resource
* Incomming Webhook resource
* Limits resource
* Queue Admin Resource
* Schema Resource
* Version Resource
* Webhooks Config Resource

EventResource:
* /event/handler GET
* /event/handler/{name} GET

Group resource:
* /api/groups/{groupId}/users DELETE - I assume not needed
* /api/groups/{groupId}/users POST - I assume not needed

Secret resource:
* /api/secrets POST - I assume not needed



## Differences between clusters

Not exist in SM cluster
* context controller
* event-execution resource
* event-message resource
* human task
* human task resource
* LLM
* scheduler bulk resource
* Service Registry Resource
* Sharing resource
* User form
* user form template

Similar by names, but different endpoints:
* metrics token resource vs metrics resource

Integrations:
* /api/integrations/def/register POST - absent in SM
* /api/integrations/def/{name}   GET - absent in SM
* /api/integrations/provider/{name}/integration/{integration_name}/metrics GET  ++ absent in development  + exists in SDK
* /api/integrations/provider/{name}/integration/{integration_name}/metrics POST ++ absent in development  - doesn't exist in SDK
* /api/integrations/provider/{name}/metrics ++ absent in development + exists in SDK

Metadata:
* /api/metadata/workflow-importer/import-bpm  POST - absent in SM

Prompts:
* /api/prompts/{name} POST  - params diferent
* /api/prompts/{name} PUT  - absent in SM
* /api/prompts/{name}/versions GET  - absent in SM
* /api/prompts/{name}/versions/{version} DELETE  - absent in SM

Schema:
* /api/schema GET  - absent in SM

Tasks:
* /api/tasks/{workflowId}/{status}/signal  POST  - absent in SM
* /api/tasks/{workflowId}/{status}/signal/sync POST  - absent in SM
* /api/tasks/update-v2 POST  - absent in SM




## Broken Swagger (could be more, I checked only some of them):
// full event controller. e.g.
* https://developer.orkescloud.com/swagger-ui/index.html#/event-resource/addEventHandler
* https://developer.orkescloud.com/swagger-ui/index.html#/event-resource/updateEventHandler
* https://developer.orkescloud.com/swagger-ui/index.html#/event-resource/getEventHandlersForEvent


## Other notes:

* Prompt api doesn't exist in conductor oss, but exists in conductor-oss client
* .path("/tasks/search-v2") doesn't exist in orkes but exists in conductor oss