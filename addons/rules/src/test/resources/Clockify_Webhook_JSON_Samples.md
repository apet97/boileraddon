# Clockify Webhook JSON Samples

## NEW_TIME_ENTRY
```json
{
  "workspaceId": "workspace123",
  "id": "entry456",
  "description": "Meeting with client",
  "userId": "user789",
  "projectId": "project123",
  "taskId": "task456",
  "billable": true,
  "project": {
    "id": "project123",
    "name": "Client Project",
    "clientId": "client123",
    "clientName": "Acme Corp"
  },
  "task": {
    "id": "task456",
    "name": "Design Review"
  },
  "user": {
    "id": "user789",
    "name": "John Doe"
  }
}
```

## TIME_ENTRY_UPDATED
```json
{
  "workspaceId": "workspace123",
  "id": "entry456",
  "description": "Updated meeting description",
  "userId": "user789",
  "projectId": "project123",
  "billable": false
}
```

## PROJECT_CREATED
```json
{
  "workspaceId": "workspace123",
  "id": "project123",
  "name": "New Project",
  "clientId": "client123",
  "clientName": "Acme Corp",
  "billable": true
}
```

## CLIENT_CREATED
```json
{
  "workspaceId": "workspace123",
  "id": "client123",
  "name": "Acme Corp",
  "archived": false
}
```

## TAG_CREATED
```json
{
  "workspaceId": "workspace123",
  "id": "tag123",
  "name": "urgent",
  "archived": false
}
```

## TASK_CREATED
```json
{
  "workspaceId": "workspace123",
  "id": "task123",
  "name": "Design Task",
  "projectId": "project123",
  "assigneeId": "user789",
  "assigneeIds": ["user789"]
}
```

## USER_CREATED
```json
{
  "workspaceId": "workspace123",
  "id": "user789",
  "email": "john@example.com",
  "name": "John Doe"
}
```

## TIMER_STOPPED
```json
{
  "workspaceId": "workspace123",
  "id": "timer123",
  "description": "Timer stopped",
  "userId": "user789"
}
```