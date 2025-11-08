# Clockify Webhook Automation Flows (IFTTT Style)

This document maps Clockify webhook triggers (If…) to native Clockify API actions (Then…), using only official endpoints. It's suitable as a basis for a Figma automation canvas or an integration runbook. Field hints show required path/body parameters.

Note: All endpoints are relative to the Clockify API base (e.g., `https://api.clockify.me/api/v1`). Preload entity caches in the Rules add‑on to map human names to IDs (projects, tags, clients, users, tasks).

## IFTTT Builder Integration

The Rules add-on now includes an IFTTT builder at `/rules/ifttt` that:
- Automatically loads webhook triggers from `Clockify_Webhook_JSON_Samples.md`
- Dynamically populates available actions from the Clockify OpenAPI spec (`dev-docs-marketplace-cake-snapshot/extras/clockify-openapi.json`)
- Supports placeholder templating with `{{field.path}}` syntax for dynamic values
- Generates live HTTP previews of composed actions

**Catalog Endpoints:**
- `GET /rules/api/catalog/triggers` — returns all webhook events with descriptions and sample fields
- `GET /rules/api/catalog/actions` — returns all OpenAPI endpoints grouped by tag, with parameter schemas

## Time Entry Events

| Webhook Trigger | Suggested Action(s) | API Endpoint(s) |
|---|---|---|
| NEW_TIME_ENTRY (time entry created) | Update the new entry (set billable, description, tags). Stop any other running timer for the same user. | PUT `/v1/workspaces/{workspaceId}/time-entries/{timeEntryId}` (body: e.g. `{ "billable":true, "description":"…", "projectId":"…", "tagIds":[…] }`) and GET `/v1/workspaces/{workspaceId}/time-entries/status/in-progress`, then PUT `/v1/workspaces/{workspaceId}/time-entries/{otherId}` with `{ "end": "<ISO datetime>" }` |
| TIME_ENTRY_UPDATED | Re-apply rules if needed (fix billable, sync tags) | PUT `/v1/workspaces/{workspaceId}/time-entries/{timeEntryId}` |
| TIME_ENTRY_DELETED | No direct action (entry removed). Optionally log or re-create. | N/A |
| TIME_ENTRY_SPLIT | Reconcile splits (custom). | Use GET/PUT/POST as needed (e.g., duplicate, update endpoints). |
| NEW_TIMER_STARTED | Stop any other in‑progress timers for user. | GET `/v1/workspaces/{workspaceId}/time-entries/status/in-progress`, then PUT to set `end`. |
| TIMER_STOPPED | Optionally update just-stopped entry (e.g., set billable/tags). | PUT `/v1/workspaces/{workspaceId}/time-entries/{timeEntryId}` |

## Project, Task & Client Events

| Webhook Trigger | Suggested Action(s) | API Endpoint(s) |
|---|---|---|
| NEW_PROJECT | Assign default users/groups; create template tasks. | POST `/v1/workspaces/{workspaceId}/projects/{projectId}/memberships` (body: `{ "userIds":["<userId>"] , "remove": false }`); POST/PUT task endpoints as needed. |
| PROJECT_UPDATED | Update project fields. | PATCH `/v1/workspaces/{workspaceId}/projects/{projectId}` (e.g., `{ "name": "…", "billable": false }`). |
| PROJECT_DELETED | Clean-up/notify. | DELETE `/v1/workspaces/{workspaceId}/projects/{projectId}`. |
| NEW_TASK | Assign defaults; add to templates. | PUT `/v1/workspaces/{workspaceId}/projects/{projectId}/tasks/{taskId}` (e.g., `{ "assigneeId": "<userId>" }`). |
| TASK_UPDATED | Update task fields. | PUT `/v1/workspaces/{workspaceId}/projects/{projectId}/tasks/{taskId}`. |
| TASK_DELETED | Archive/remove in templates. | DELETE `/v1/workspaces/{workspaceId}/projects/{projectId}/tasks/{taskId}`. |
| NEW_CLIENT | Create default project. | POST `/v1/workspaces/{workspaceId}/clients` (body: `{ "name": "Client" }`). |
| CLIENT_UPDATED | Update client fields. | PUT `/v1/workspaces/{workspaceId}/clients/{id}`. |
| CLIENT_DELETED | Archive related projects/tasks. | DELETE `/v1/workspaces/{workspaceId}/clients/{id}`. |

## Tag Events

| Webhook Trigger | Suggested Action(s) | API Endpoint(s) |
|---|---|---|
| NEW_TAG | Seed templates or backfill entries. | POST `/v1/workspaces/{workspaceId}/tags` (body: `{ "name": "<tagName>" }`). |
| TAG_UPDATED | Rename/adjust. | PUT `/v1/workspaces/{workspaceId}/tags/{id}`. |
| TAG_DELETED | Remove tag from entries. | DELETE `/v1/workspaces/{workspaceId}/tags/{id}`. |

## User & Assignment Events

| Webhook Trigger | Suggested Action(s) | API Endpoint(s) |
|---|---|---|
| USER_JOINED_WORKSPACE | Add user to default projects; set groups. | POST `/v1/workspaces/{workspaceId}/projects/{projectId}/memberships` (body: `{ "userIds":["<newUserId>"] }`). |
| USER_DELETED_FROM_WORKSPACE | Remove from memberships. | POST `/v1/workspaces/{workspaceId}/projects/{projectId}/memberships` (body: `{ "userIds":["<userId>"] , "remove": true }`). |
| USER_DEACTIVATED_ON_WORKSPACE | Mark inactive. | PUT `/v1/workspaces/{workspaceId}/users/{userId}` (body: `{ "status": "INACTIVE" }`). |
| USER_ACTIVATED_ON_WORKSPACE | Mark active. | PUT `/v1/workspaces/{workspaceId}/users/{userId}` (body: `{ "status": "ACTIVE" }`). |
| ASSIGNMENT_CREATED/UPDATED | Publish/copy assignments. | PUT `/v1/workspaces/{workspaceId}/scheduling/assignments/publish` (body as needed). |
| ASSIGNMENT_DELETED/PUBLISHED | No action or fetch for sync. | GET scheduling endpoints as needed. |

## Invoice and Approval Events

| Webhook Trigger | Suggested Action(s) | API Endpoint(s) |
|---|---|---|
| NEW_INVOICE | Link invoice to client/project (external). | POST `/v1/workspaces/{workspaceId}/invoices` (CreateInvoiceRequest). |
| INVOICE_UPDATED | Mark PAID. | PATCH `/v1/workspaces/{workspaceId}/invoices/{invoiceId}/status` (body: `{ "status": "PAID" }`). |
| NEW_APPROVAL_REQUEST | Auto-approve/route. | PATCH `/v1/workspaces/{workspaceId}/approval-requests/{approvalRequestId}` (body: `{ "status": "APPROVED" }`). |
| APPROVAL_REQUEST_STATUS_UPDATED | If approved, adjust time entries (billable). | PATCH approval status and/or PUT time entries. |

## Naming, Filters & Logic

- Flow names: “On [Event] → [Action]”, e.g., “On New Time Entry → Mark Billable”.
- Filters: Limit by billable flag, project, client, tags. Combine with local rules (conditions) before calling actions.
- Logic blocks: Avoid duplicate updates (idempotence). For timers, stop all other in‑progress entries for the same user.
- Field requirements: Always include required path params (`workspaceId`, `…Id`) and request body per schema when updating.
- Error handling: Check response codes; handle 404/409/429 with retries/backoff where safe.

Sources: Clockify’s OpenAPI spec and webhook examples.
