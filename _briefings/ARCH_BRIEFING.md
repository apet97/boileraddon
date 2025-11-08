Solution Architect Briefing — Clockify Add-on Boilerplate
Repo commit: 239a31a40da23bfaa7eaf8720120d19723058eb4

Scope for this role:

Validate module boundaries between application code and inline SDK runtime.

Ensure deployment model assumptions (single add-on per JVM, context path isolation) are upheld in target environments.

Govern manifest, lifecycle, and webhook registration flows for new add-ons.

Advise on environment claim handling and token persistence strategy.

Primary artifacts in repo:

Architecture Overview

AutoTagAssistantApp bootstrap logic

TokenStore reference implementation

How to do your job:

Review module responsibilities (app vs inline SDK) to design compliant extensions or integrations.

Confirm AddonServlet/EmbeddedServer usage when planning deployment topologies; ensure context paths stay unique.

Verify manifest builder flow in AutoTagAssistantApp when introducing additional components or scopes.

Enforce token persistence design—swap demo TokenStore for DatabaseTokenStore in multi-node deployments.

Ensure lifecycle/webhook registration keeps manifest synchronized via ClockifyAddon helpers in any custom modules.

Validate environment claims extraction through JwtTokenDecoder before routing traffic to regional APIs.

Align deployment architecture with Production guide’s security, scaling, and monitoring sections when specifying hosting patterns.

Critical decisions already made:

Each add-on runs best as a dedicated process; shared servlet containers require additional isolation work.

Runtime manifest is constructed programmatically and must remain in sync with registered endpoints.

TokenStore currently defaults to in-memory storage; production deployments should replace it with persistent storage per guide recommendations.

Open questions and risks:

Owner	Source	Link
Engineering Lead	Template lifecycle handlers still log instead of persisting credentials; formalize storage contract for new add-ons.	https://github.com/apet97/boileraddon/blob/239a31a40da23bfaa7eaf8720120d19723058eb4/addons/_template-addon/src/main/java/com/example/templateaddon/LifecycleHandlers.java#L14-L55
Engineering Lead	RateLimiter defaults to IP-based throttling; confirm production profile for workspace-level limiting and distributed caches.	https://github.com/apet97/boileraddon/blob/239a31a40da23bfaa7eaf8720120d19723058eb4/addons/addon-sdk/src/main/java/com/clockify/addon/sdk/middleware/RateLimiter.java#L18-L188
Commands or APIs you will call (if any):

ITokenStore tokenStore = DatabaseTokenStore.fromEnvironment();

References:

Architecture overview.

AutoTagAssistantApp manifest bootstrap.

Production deployment guide for security and scaling.
