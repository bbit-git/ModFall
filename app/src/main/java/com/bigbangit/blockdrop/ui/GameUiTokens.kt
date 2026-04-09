package com.bigbangit.blockdrop.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Centralized design tokens for the gameplay UI.
 * Tuning any visual aspect of the dark-neon theme should start here.
 */
object GameUiTokens {

    // ── Background ──────────────────────────────────────────────
    val BackgroundDark = Color(0xFF070E18)
    val BackgroundCenter = Color(0xFF0D1B2A)
    val BackgroundEdge = Color(0xFF060C16)

    // ── Playfield frame ─────────────────────────────────────────
    val FrameGlow = Color(0xFF3A7BD5)
    const val FrameGlowAlpha = 0.28f
    const val FrameGlowRadius = 18f
    val FrameStroke = Color(0xFF2A5A9A)
    const val FrameStrokeAlpha = 0.55f
    const val FrameStrokeWidth = 2.5f
    val FrameCornerRadius = 10f

    // ── Grid ────────────────────────────────────────────────────
    val GridLine = Color(0xFF1A2D44)
    const val GridLineAlpha = 0.32f

    // ── Block glow strengths ────────────────────────────────────
    const val ActiveBlockGlowAlpha = 0.62f
    const val ActiveBlockBlurRadius = 0.25f   // fraction of cell width
    const val SettledBlockGlowAlpha = 0.22f
    const val SettledBlockBlurRadius = 0.12f  // fraction of cell width

    // ── Ghost piece ─────────────────────────────────────────────
    val GhostStroke = Color.White
    const val GhostStrokeAlpha = 0.22f
    const val GhostStrokeWidth = 1.5f
    const val GhostFillAlpha = 0.04f

    // ── Embedded preview labels ─────────────────────────────────
    val PreviewLabelColor = Color.White
    const val PreviewLabelAlpha = 0.38f
    val PreviewBorderColor = Color.White
    const val PreviewBorderAlpha = 0.08f
    const val PreviewBgAlpha = 0.06f

    // ── Control pad ─────────────────────────────────────────────
    val ControlBg = Color.White
    const val ControlBgAlpha = 0.07f
    val ControlBorder = Color(0xFF3A7BD5)
    const val ControlBorderAlpha = 0.25f
    val ControlIcon = Color.White
    const val ControlIconAlpha = 0.72f
    val ControlGlow = Color(0xFF3A7BD5)
    const val ControlGlowAlpha = 0.12f
    const val ControlPressedScale = 0.92f
    const val ControlPressedBgAlpha = 0.14f
    const val ControlPressedBorderAlpha = 0.45f
    val ControlCornerRadius: Dp = 14.dp
    val ControlButtonSize: Dp = 52.dp
    val ControlButtonSpacing: Dp = 12.dp

    // ── Top HUD ─────────────────────────────────────────────────
    val HudIconColor = Color.White
    const val HudIconAlpha = 0.65f
    val HudScoreColor = Color.White
    val HudSecondaryColor = Color(0xFF8AB4FF)
    const val HudSecondaryAlpha = 0.6f

    // ── Text ────────────────────────────────────────────────────
    val TextPrimary = Color(0xFFF5F7FF)
    val TextSecondary = Color(0xFF8AB4FF)

    // ── Spacing ─────────────────────────────────────────────────
    val ScreenPaddingHorizontal: Dp = 12.dp
    val ScreenPaddingVertical: Dp = 8.dp
    val HudHeight: Dp = 56.dp
    val ControlPadHeight: Dp = 130.dp
    val ControlPadBottomPadding: Dp = 16.dp
}
