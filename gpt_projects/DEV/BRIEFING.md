# Addon Engineer (Feature Dev) Briefing — Clockify Add-on Boilerplate
- Repo commit: a487d16c75425f6c14d1c3195459a52bc0991f88
- Scope for this role:
  - Define addon MVPs and success metrics ["https://github.com/apet97/boileraddon/blob/a487d16c75425f6c14d1c3195459a52bc0991f88/README.md#L1-L20"].
  - Align supported modules and templates ["https://github.com/apet97/boileraddon/blob/a487d16c75425f6c14d1c3195459a52bc0991f88/pom.xml#L19-L27"].
  - Track build/run workflows for feasibility ["https://github.com/apet97/boileraddon/blob/a487d16c75425f6c14d1c3195459a52bc0991f88/Makefile#L8-L29"].
  - Confirm Java 17 + Maven stack ["https://github.com/apet97/boileraddon/blob/a487d16c75425f6c14d1c3195459a52bc0991f88/pom.xml#L13-L17"].

- Primary artifacts in repo:
  - Project README https://github.com/apet97/boileraddon/blob/a487d16c75425f6c14d1c3195459a52bc0991f88/README.md#L1-L40
  - Maven modules https://github.com/apet97/boileraddon/blob/a487d16c75425f6c14d1c3195459a52bc0991f88/pom.xml#L19-L27
  - Make targets https://github.com/apet97/boileraddon/blob/a487d16c75425f6c14d1c3195459a52bc0991f88/Makefile#L1-L30

- How to do your job:

- Scaffold and build locally: make build-template (65–69) then run demo (88–102) [Makefile]({base}/Makefile#L65-L69) [Makefile]({base}/Makefile#L88-L102)
- Register manifest + lifecycle: TemplateAddonApp lines 45–52 [TemplateAddonApp.java]({base}/addons/_template-addon/src/main/java/com/example/templateaddon/TemplateAddonApp.java#L45-L52)
- Implement lifecycle/webhook TODOs: LifecycleHandlers lines 16–60 [LifecycleHandlers.java]({base}/addons/_template-addon/src/main/java/com/example/templateaddon/LifecycleHandlers.java#L16-L60)
- Validate manifests before install: Makefile lines 48–51 [Makefile]({base}/Makefile#L48-L51)
  - Use provided quick start to assess dev effort https://github.com/apet97/boileraddon/blob/a487d16c75425f6c14d1c3195459a52bc0991f88/Makefile#L23-L31
  - Plan around two addons: template and auto-tag assistant https://github.com/apet97/boileraddon/blob/a487d16c75425f6c14d1c3195459a52bc0991f88/pom.xml#L22-L27
  - Consider local run via `run-auto-tag-assistant` https://github.com/apet97/boileraddon/blob/a487d16c75425f6c14d1c3195459a52bc0991f88/Makefile#L20-L26
  - Validate manifests before delivery https://github.com/apet97/boileraddon/blob/a487d16c75425f6c14d1c3195459a52bc0991f88/Makefile#L41-L45
  - Note dependency policy: Maven Central only https://github.com/apet97/boileraddon/blob/a487d16c75425f6c14d1c3195459a52bc0991f88/Makefile#L30-L31

- Critical decisions already made:
  - Language/runtime: Java 17 and Maven https://github.com/apet97/boileraddon/blob/a487d16c75425f6c14d1c3195459a52bc0991f88/pom.xml#L13-L17
  - Packaging via shaded JARs https://github.com/apet97/boileraddon/blob/a487d16c75425f6c14d1c3195459a52bc0991f88/Makefile#L54-L56

- Open questions and risks:
  - Owner | Issue | Link
  - PM | Marketplace submission steps — not documented | https://github.com/apet97/boileraddon/blob/a487d16c75425f6c14d1c3195459a52bc0991f88/README.md#L1-L1

- Commands or APIs you will call (if any):
```
make build
make run-auto-tag-assistant
```
  - Source: https://github.com/apet97/boileraddon/blob/a487d16c75425f6c14d1c3195459a52bc0991f88/Makefile#L47-L56 https://github.com/apet97/boileraddon/blob/a487d16c75425f6c14d1c3195459a52bc0991f88/Makefile#L20-L21

- References:
  - README https://github.com/apet97/boileraddon/blob/a487d16c75425f6c14d1c3195459a52bc0991f88/README.md#L1-L80
  - Makefile https://github.com/apet97/boileraddon/blob/a487d16c75425f6c14d1c3195459a52bc0991f88/Makefile#L1-L80
  - POM https://github.com/apet97/boileraddon/blob/a487d16c75425f6c14d1c3195459a52bc0991f88/pom.xml#L1-L29
