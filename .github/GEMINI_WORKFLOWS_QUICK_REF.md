# 🚀 Quick Reference: Gemini Workflows

## Commands

| Command | Where to Use | What It Does |
|---------|--------------|--------------|
| `/fix` | Issue | Generates a fix plan |
| `/approve` | Issue (after `/fix`) | Creates a PR with the fix |
| `/approve` | PR | Merges the PR |
| `/deny` | Issue or PR | Cancels and closes |

## Labels

### Auto-Applied by Triage
- **Type:** `type:bug`, `type:feature`, `type:enhancement`, `type:chore`, `type:question`, `type:documentation`
- **Area:** `area:android`, `area:ui`, `area:media`, `area:network`, `area:auth`, `area:build`, `area:docs`, `area:testing`
- **Priority:** `priority:critical`, `priority:high`, `priority:medium`, `priority:low`

### Workflow State Labels
- `gemini:awaiting-approval` - Issue has a fix plan, waiting for `/approve` or `/deny`
- `gemini:awaiting-merge` - PR is ready to merge with `/approve`

## Typical Workflow

```
1. Open Issue
   ↓ (automatic)
2. Labels Applied
   ↓ (comment /fix)
3. Fix Plan Posted
   ↓ (comment /approve)
4. PR Created
   ↓ (review + comment /approve on PR)
5. PR Merged & Issue Closed ✅
```

## Requirements

### Repository Settings
- **Secret:** `GEMINI_API_KEY` - Get from [Google AI Studio](https://aistudio.google.com/app/apikey)
- **Variable:** `GEMINI_MODEL` - e.g., `gemini-2.0-flash-exp`

### Permissions
Only users with these roles can use commands:
- OWNER
- MEMBER
- COLLABORATOR

## Common Issues

| Problem | Solution |
|---------|----------|
| Command doesn't work | Make sure comment is exactly `/fix`, `/approve`, or `/deny` |
| Permission denied | Only repo members can use commands |
| Triage fails | Check GEMINI_API_KEY is set correctly |
| Apply fix fails | Issue description might need more detail |

## Examples

### Simple Bug Fix
```
Issue: "App crashes when loading library"
1. Comment: /fix
2. Review plan
3. Comment: /approve
4. Review PR
5. Comment: /approve (on PR)
✅ Done!
```

### Cancel a Fix
```
Issue: Has fix plan
1. Comment: /deny
✅ Issue closed
```

---

📖 **Full Documentation:** See [GEMINI_WORKFLOWS.md](GEMINI_WORKFLOWS.md)
