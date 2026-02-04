<!--
Sync Impact Report:
- Version change: New -> 1.0.0
- Added sections: Operational Excellence, Contribution & Workflow
- Modified principles: N/A (Standard template + Feature-Centric addition)
- Templates requiring updates:
  - /.specify/templates/plan-template.md (✅ updated)
  - /.specify/templates/tasks-template.md (✅ updated)
  - /.specify/templates/spec-template.md (✅ checked, no changes needed)
-->
# PlantUML Server Constitution

## Core Principles

### I. Library-First Design
Every significant feature MUST begin as a cohesive, reusable library (module/package).
Libraries MUST:
- Be self-contained and independently testable.
- Have a single, clearly documented responsibility.
- Avoid being mere organizational/wrapper shells without real behavior.

Rationale: encourages reuse, clear boundaries, and easier testing.

### II. CLI & Text I/O Interface
Each library SHOULD expose a command-line interface (CLI) for core operations.
CLIs MUST:
- Accept input via stdin/arguments/files using plain text or JSON.
- Write normal results to stdout and errors to stderr.
- Prefer JSON for machine consumption and human-readable text for operators.

Rationale: standardizes integration, observability, and automation.

### III. Test-First Development
Implementation MUST follow a Test-Driven Development style for core logic:
- Write or update tests BEFORE implementing new behavior.
- Ensure tests FAIL first (Red), then implement to make them PASS (Green).
- Refactor only with all tests passing (Refactor).

At minimum:
- Pure functions/utilities MUST have unit tests.
- Critical flows MUST have automated regression coverage.

Rationale: reduces regressions and clarifies intent.

### IV. Integration & Contract Testing
Integration/contract tests SHOULD cover:
- Cross-service communication and external APIs.
- Shared schemas or data contracts.
- Critical end-to-end user journeys.

When real dependencies are hard to run locally, abstract them behind interfaces
and document follow-up contract tests in the plan/tasks.

Rationale: validates real-world behavior beyond unit tests.

### V. Observability, Versioning & Simplicity
All components MUST be observable and versioned:
- Use structured logs for important events and errors.
- Prefer semantic versioning (MAJOR.MINOR.PATCH).
- Document any breaking changes and migration notes.
- Keep designs as simple as possible; avoid speculative features (YAGNI).

Rationale: makes systems debuggable, upgradable, and maintainable.

### VI. Continuous Integration & Quality Gates
Changes MUST be safe to merge:
- Linting, formatting, and basic tests MUST pass in CI.
- A minimal smoke test or example run SHOULD be provided for new features.
- New behavior MUST be reflected in specs/plan/tasks/docs where applicable.

Rationale: ensures consistent quality and predictable releases.

### VII. Feature-Centric Development
Feature 是项目的长期核心框架：
- Feature 列表必须保持为项目的“单一事实来源”。
- 在 spec → plan → tasks → implement 的每个阶段都必须复核 Feature 的新增/合并/拆分/删除。
- Feature 变更必须可追溯到相应的 spec/plan 依据，并记录在 Feature 详情中。

Rationale: 让项目演进以 Feature 为中心，确保长期一致性与可维护性。

## Operational Excellence

- **Security First**: All features MUST respect `PLANTUML_SECURITY_PROFILE`. Default profile is INTERNET. Do not expose local files or network access without explicit configuration.
- **Container Compatibility**: Changes MUST support both Jetty and Tomcat environments.
- **Backward Compatibility**: Maintain support for JDK 11+ and Maven 3.0.2+.

## Contribution & Workflow

- **Documentation**: Updates to the WebUI or core features MUST include corresponding docs in `docs/`.
- **License Compliance**: All contributions MUST be compatible with GNU GPL v3.

## Governance

- The Constitution supersedes conflicting guidelines in other documentation.
- Amendments require a version bump (Major/Minor/Patch) and maintainer review.
- All Pull Requests MUST be checked against the Core Principles.

**Version**: 1.0.0 | **Ratified**: 2026-02-03 | **Last Amended**: 2026-02-03