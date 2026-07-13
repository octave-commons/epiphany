#!/usr/bin/env bash
# PreToolUse hook: block direct Edit/Write of `status: done` into
# docs/kanban/stories|epics|chores/*.md frontmatter.
#
# The kanban_update_status MCP tool (see kanban-mcp-status-gate.sh) is the
# only path that runs bin/kanban-done-gate. An agent using the Edit or Write
# tool directly on a card's markdown file bypasses that gate entirely (and
# bypasses Rheos's own FSM/ledger too, if Rheos is meant to be the system of
# record). This hook does not block editing kanban files in general -- only
# a direct frontmatter change that sets status to done, which must go
# through kanban_update_status instead.
#
# This is a floor, not a lock: an agent could still write status: done via a
# Bash `sed`/`echo` command. It only closes the Edit/Write tool path, plus
# it's a strong nudge -- the point is making the enforced path the path of
# least resistance, not building a tamper-proof wall.

set -euo pipefail

input="$(cat)"

file_path="$(echo "$input" | python3 -c 'import json,sys; print(json.load(sys.stdin).get("tool_input",{}).get("file_path",""))')"

case "$file_path" in
  */docs/kanban/stories/*.md|*/docs/kanban/epics/*.md|*/docs/kanban/chores/*.md) ;;
  *)
    echo '{"continue": true}'
    exit 0
    ;;
esac

# Look at whatever content this Edit/Write call would introduce -- Edit's
# new_string, or Write's content -- for a frontmatter status: done line.
new_content="$(echo "$input" | python3 -c '
import json, sys
d = json.load(sys.stdin).get("tool_input", {})
print(d.get("new_string", "") or d.get("content", ""))
')"

if echo "$new_content" | grep -qE '^status:\s*"?done"?\s*$'; then
  echo '{"continue": false, "stopReason": "Direct edit sets status: done", "hookSpecificOutput": {"hookEventName": "PreToolUse", "permissionDecision": "deny", "permissionDecisionReason": "This edits a kanban card frontmatter to status: done directly, bypassing bin/kanban-done-gate and Rheos'\''s FSM/ledger. Use the kanban_update_status MCP tool (project: epiphany) instead, which the kanban-mcp-status-gate hook checks."}}'
  exit 0
fi

echo '{"continue": true}'
exit 0
