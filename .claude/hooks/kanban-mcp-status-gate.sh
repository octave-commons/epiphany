#!/usr/bin/env bash
# PreToolUse hook: gate the Rheos MCP kanban_update_status tool.
#
# Rheos (rheos.service) already FSM/ledger-enforces status moves made through
# its own CLI/HTTP/MCP surfaces -- but its shared promethean-fsm has no
# per-card evidence check, only fixed build/lint/test commands wired to
# specific edges. This hook adds that missing per-card check on THIS
# repo's side, without touching Rheos or any other project it serves:
# before letting a kanban_update_status call through with status "done",
# it runs bin/kanban-done-gate against the target uuid and blocks the tool
# call if the gate fails.
#
# Any other status (or a different MCP server/tool) passes through untouched.
#
# See docs/process/review-and-acceptance.md and docs/kanban/AGENTS.md hard
# rule 7 for what this is a floor for, and its limits (it cannot verify the
# reviewer was independent/authorized -- only that *a* disposition exists).

set -euo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
input="$(cat)"

tool_name="$(echo "$input" | python3 -c 'import json,sys; print(json.load(sys.stdin).get("tool_name",""))')"

# Only act on the Rheos kanban status-update tool. Claude Code names MCP
# tools mcp__<server>__<tool>; match on tool suffix so this survives a
# server rename in .mcp.json.
case "$tool_name" in
  *kanban_update_status|*kanban-status-update) ;;
  *)
    echo '{"continue": true}'
    exit 0
    ;;
esac

status="$(echo "$input" | python3 -c 'import json,sys; print(json.load(sys.stdin).get("tool_input",{}).get("status",""))')"
uuid="$(echo "$input" | python3 -c 'import json,sys; print(json.load(sys.stdin).get("tool_input",{}).get("uuid",""))')"

case "$status" in
  done) ;;
  *)
    echo '{"continue": true}'
    exit 0
    ;;
esac

if [ -z "$uuid" ]; then
  echo '{"continue": true}'
  exit 0
fi

if gate_output="$("$repo_root/bin/kanban-done-gate" "$uuid" 2>&1)"; then
  echo '{"continue": true}'
  exit 0
else
  reason="$(printf '%s' "$gate_output" | python3 -c 'import json,sys; print(json.dumps(sys.stdin.read()))')"
  printf '{"continue": false, "stopReason": "kanban-done-gate blocked this transition", "hookSpecificOutput": {"hookEventName": "PreToolUse", "permissionDecision": "deny", "permissionDecisionReason": %s}}\n' "$reason"
  exit 0
fi
