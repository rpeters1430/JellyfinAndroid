# 🤖 Gemini AI-Powered Issue Management

This repository uses Google's Gemini AI to automate issue triaging, fix planning, and implementation.

## ✨ What This Does

When you open an issue, Gemini automatically:
1. 🏷️ Analyzes and applies relevant labels
2. 📋 Generates fix plans on demand (`/fix` command)
3. 🔧 Creates PRs with fixes (`/approve` command)
4. ✅ Merges approved PRs

## 🎯 Quick Start

### For Issue Reporters
1. Open an issue with a clear title and description
2. Labels will be automatically applied within 1-2 minutes
3. Wait for a maintainer to comment `/fix` if they want an AI fix plan

### For Maintainers
1. Review auto-applied labels on new issues
2. Comment `/fix` on an issue to get an AI-generated fix plan
3. Review the plan, then:
   - Comment `/approve` to create a PR with the fix
   - Comment `/deny` to close the issue
4. Review the PR, then:
   - Comment `/approve` to merge
   - Comment `/deny` to close without merging

## 📚 Documentation

- **[Full Workflow Documentation](GEMINI_WORKFLOWS.md)** - Complete guide with examples and troubleshooting
- **[Quick Reference](GEMINI_WORKFLOWS_QUICK_REF.md)** - Commands and labels cheat sheet

## 🔐 Setup (For Repository Admins)

1. Get a Gemini API key from [Google AI Studio](https://aistudio.google.com/app/apikey)
2. Add as repository secret: `GEMINI_API_KEY`
3. Set repository variable: `GEMINI_MODEL` = `gemini-2.0-flash-exp`
4. Workflows are in `.github/workflows/gemini-*.yml`

## 🎨 Workflow Files

| File | Purpose | Trigger |
|------|---------|---------|
| `gemini-triage.yml` | Auto-label issues | Issue opened/reopened |
| `gemini-fix-plan.yml` | Generate fix plan | `/fix` comment on issue |
| `gemini-apply-fix.yml` | Create PR with fix | `/approve` comment on issue |
| `gemini-merge.yml` | Merge approved PR | `/approve` comment on PR |
| `gemini-deny.yml` | Cancel and close | `/deny` comment on issue/PR |

## 🎓 Example Flow

```
┌─────────────────┐
│  Issue Opened   │
└────────┬────────┘
         │ (automatic, ~1-2 min)
         ▼
┌─────────────────┐
│ Labels Applied  │  🏷️ type:bug, area:ui, priority:high
└────────┬────────┘
         │ (maintainer comments "/fix")
         ▼
┌─────────────────┐
│  Fix Plan Posted│  📋 Detailed steps and approach
└────────┬────────┘
         │ (maintainer comments "/approve")
         ▼
┌─────────────────┐
│   PR Created    │  🔧 Branch + code changes
└────────┬────────┘
         │ (maintainer reviews & comments "/approve")
         ▼
┌─────────────────┐
│   PR Merged     │  ✅ Changes integrated, issue closed
└─────────────────┘
```

## 💡 Tips

- **Be specific** in issue descriptions for better AI analysis
- **Review carefully** before approving AI-generated fixes
- **Test locally** if you want to be extra sure (the workflow runs tests automatically)
- **Use `/deny`** to cancel at any stage without losing work

## 🚨 Security

- Only repository OWNER, MEMBER, and COLLABORATOR can use commands
- All changes go through PR review before merging
- Tests run automatically (best effort) before creating PR
- API key is securely stored as a repository secret

## 🤝 Contributing

To improve the workflows, edit the files in `.github/workflows/` and test in a fork first.

---

**Questions?** Open an issue with the `area:build` label or see the [full documentation](GEMINI_WORKFLOWS.md).
