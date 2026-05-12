
## Design System

The canonical design reference is `DESIGN.md` in the project root.

Key decisions:
- **Colors:** Token system in `Color.kt` — never hardcode colors in composables. See `DESIGN.md § Color Tokens`.
- **Typography:** DM Serif Display (display/headers/empty states) + DM Mono (timestamps/metadata). No Instrument Serif, no italic for headers.
- **Album art:** Always `RoundedCornerShape` — never `CircleShape`.
- **Mini player:** Floating pill above nav bar, not a full-width bottom bar.
- **Accent color:** `LocalAccentColor` CompositionLocal — replaced at runtime by Palette API from album art.
- **Preview:** `design-preview.html` in project root — open in browser to see all screens.

## Skill routing

When the user's request matches an available skill, invoke it via the Skill tool. When in doubt, invoke the skill.

Key routing rules:
- Product ideas/brainstorming → invoke /office-hours
- Strategy/scope → invoke /plan-ceo-review
- Architecture → invoke /plan-eng-review
- Design system/plan review → invoke /design-consultation or /plan-design-review
- Full review pipeline → invoke /autoplan
- Bugs/errors → invoke /investigate
- QA/testing site behavior → invoke /qa or /qa-only
- Code review/diff check → invoke /review
- Visual polish → invoke /design-review
- Ship/deploy/PR → invoke /ship or /land-and-deploy
- Save progress → invoke /context-save
- Resume context → invoke /context-restore
