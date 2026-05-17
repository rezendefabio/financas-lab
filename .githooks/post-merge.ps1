$ErrorActionPreference = "Stop"
$repoRoot = git rev-parse --show-toplevel

. "$repoRoot/.claude/hooks/universal/npm-install-on-package-change.ps1"
