package com.shuckler.app.ui.theme

import androidx.compose.ui.graphics.Color

// ── Background / Surface ─────────────────────────────────────────────────────
val Base            = Color(0xFF0C0A07)   // App background — warm near-black
val Surface         = Color(0xFF141210)   // Cards, bottom sheets
val SurfaceElevated = Color(0xFF1C1A17)   // Elevated cards, inputs
val SurfaceHigh     = Color(0xFF252220)   // Highest elevation (pill, context menus)

// ── Borders ───────────────────────────────────────────────────────────────────
val Border          = Color(0xFF2A2722)   // Standard dividers and card borders
val BorderSubtle    = Color(0xFF1E1C19)   // Track row separators

// ── Text ─────────────────────────────────────────────────────────────────────
val Text1           = Color(0xFFF5F2EC)   // Primary — warm off-white
val Text2           = Color(0xFF8C8880)   // Secondary — muted warm grey
val Text3           = Color(0xFF4A4843)   // Tertiary — timestamps, labels

// ── Accent ───────────────────────────────────────────────────────────────────
val Amber           = Color(0xFFE8A850)   // Static accent; replaced at runtime by Palette
val Green           = Color(0xFF6AB187)   // Success / download complete
val Red             = Color(0xFFC0635A)   // Error / destructive

// ── Legacy aliases (kept for backwards compat while migrating) ─────────────
val ShucklerBlack           = Base
val ShucklerBlackElevated   = SurfaceElevated
val ShucklerNeonYellow      = Amber
val ShucklerBrightYellow    = Amber
val ShucklerYellowOnBlack   = Amber
val ShucklerSurfaceDark     = SurfaceHigh
val ShucklerOnSurface       = Text1
val ShucklerOnSurfaceVariant = Text2
