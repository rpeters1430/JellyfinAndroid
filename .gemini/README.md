# Gemini CLI Configuration Directory

This directory is used by the Gemini CLI GitHub Actions workflows for runtime configuration and telemetry.

## Contents

- **commands/**: Contains TOML command definitions for Gemini CLI workflows
  - `gemini-fix.toml`: Command configuration for fix workflow
  - `gemini-review.toml`: Command configuration for code review workflow
  - `gemini-invoke.toml`: Command configuration for generic invoke workflow
  - `gemini-triage.toml`: Command configuration for issue triage workflow

## Runtime Files (Git-ignored)

The following files are created at runtime by the Gemini CLI and are ignored by git:

- `settings.json`: Runtime configuration generated from workflow settings
- `telemetry.log`: Telemetry data from Gemini CLI executions
- Other temporary files created during workflow execution

## Configuration

Gemini CLI workflows are configured in `.github/workflows/gemini-*.yml` files. Each workflow provides its settings inline via the `settings:` parameter, which may be written to `settings.json` during execution.

## Note

All files in this directory except for `commands/` and this README are git-ignored to prevent committing runtime artifacts.
