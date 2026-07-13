# Stitch Design Export

UI designs exported from Google Stitch project [`9776402998627439360`](https://stitch.withgoogle.com/projects/9776402998627439360)
via the `@_davideast/stitch-mcp` CLI.

- `html/` — raw HTML/CSS for each screen (Tailwind via CDN, dark theme).
- `images/` — hi-res PNG mockups of each screen.
- `manifest.json` — screenId → title → file mapping.

## Screens → app modules

| # | Screen | Likely target module |
|---|--------|----------------------|
| 03 | Secure PIN Setup | `:feature:feature-auth` |
| 08 | PIN Unlock (biometric/keypad) | `:feature:feature-auth` |
| 07 | Smart Finance Assistant (home) | `:feature:feature-dashboard` |
| 01 | Transaction Detail | `:feature:feature-transactions` |
| 04 | Add Expense | `:feature:feature-transactions` |
| 09 | Full History | `:feature:feature-transactions` |
| 02 | Voice Capture | `:feature:feature-voice` |
| 10 | Voice Review | `:feature:feature-voice` |
| 11 | Intelligent Voice Finance | `:feature:feature-voice` |
| 06 | Settings | `:feature:feature-settings` |
| 05 | Untitled (empty/errored screen in Stitch) | — |

## Re-exporting

The `stitch` MCP server is registered in Claude Code (`npx @_davideast/stitch-mcp proxy`).
To refresh assets:

```bash
export STITCH_API_KEY="<your key>"
npx @_davideast/stitch-mcp screens -p 9776402998627439360        # browse
npx @_davideast/stitch-mcp tool get_screen_code  -d '{"projectId":"9776402998627439360","screenId":"<id>"}'
npx @_davideast/stitch-mcp tool get_screen_image -d '{"projectId":"9776402998627439360","screenId":"<id>"}'
```
