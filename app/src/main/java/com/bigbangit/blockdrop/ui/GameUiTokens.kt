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
    val BackgroundDark = Color(0xFF071225)
    val BackgroundCenter = Color(0xFF13315C)
    val BackgroundEdge = Color(0xFF06101F)
    val BackgroundNebula = Color(0xFF245A96)
    val BackgroundCoolBloom = Color(0xFF69BFFF)
    val SurfaceGlow = Color(0xFF6CB8FF)

    // ── Playfield frame ─────────────────────────────────────────
    val FrameGlow = Color(0xFF62B8FF)
    const val FrameGlowAlpha = 0.22f
    const val FrameGlowRadius = 14f
    val FrameStroke = Color(0xFF78BCFF)
    const val FrameStrokeAlpha = 0.24f
    const val FrameStrokeWidth = 1.3f
    val FrameCornerRadius = 18f
    val FrameElectricGlow = Color(0xFF8ED4FF)
    const val FrameElectricGlowAlpha = 0.42f
    const val FrameElectricGlowRadius = 18f
    const val FrameElectricStrokeWidth = 2.2f

    // ── Grid ────────────────────────────────────────────────────
    val GridLine = Color(0xFFA2CAFF)
    const val GridLineAlpha = 0.055f

    // ── Block glow strengths ────────────────────────────────────
    const val ActiveBlockGlowAlpha = 0.62f
    const val ActiveBlockBlurRadius = 0.22f
    const val SettledBlockGlowAlpha = 0.32f
    const val SettledBlockBlurRadius = 0.14f

    // ── Ghost piece ─────────────────────────────────────────────
    val GhostStroke = Color(0xFFB8D8FF)
    const val GhostStrokeAlpha = 0.36f
    const val GhostStrokeWidth = 1.35f
    const val GhostFillAlpha = 0.018f

    // ── Embedded preview labels ─────────────────────────────────
    val PreviewLabelColor = Color(0xFFC7DCFF)
    const val PreviewLabelAlpha = 0.52f
    val PreviewBorderColor = Color(0xFFC4DEFF)
    const val PreviewBorderAlpha = 0.10f
    const val PreviewBgAlpha = 0.025f

    // ── Control pad ─────────────────────────────────────────────
    val ControlBg = Color(0xFFB9D6FF)
    const val ControlBgAlpha = 0.032f
    val ControlBorder = Color(0xFF79BAFF)
    const val ControlBorderAlpha = 0.11f
    val ControlIcon = Color(0xFFEAF4FF)
    const val ControlIconAlpha = 0.72f
    val ControlGlow = Color(0xFF6FB8FF)
    const val ControlGlowAlpha = 0.07f
    const val ControlPressedScale = 0.95f
    const val ControlPressedBgAlpha = 0.08f
    const val ControlPressedBorderAlpha = 0.24f
    val ControlCornerRadius: Dp = 24.dp
    val ControlButtonSize: Dp = 74.dp
    val ControlButtonSpacing: Dp = 16.dp
    val ControlHorizontalInset: Dp = 8.dp

    // ── Top HUD ─────────────────────────────────────────────────
    val HudIconColor = Color(0xFFD7E8FF)
    const val HudIconAlpha = 0.78f
    val HudScoreColor = Color(0xFFF5F9FF)
    val HudSecondaryColor = Color(0xFF8EACD3)
    const val HudSecondaryAlpha = 0.92f
    val HudDivider = Color(0xFF7FB8FF)
    const val HudDividerAlpha = 0.26f

    // ── Text ────────────────────────────────────────────────────
    val TextPrimary = Color(0xFFF5F7FF)
    val TextSecondary = Color(0xFF93B4DC)

    // ── Spacing ─────────────────────────────────────────────────
    val ScreenPaddingHorizontal: Dp = 14.dp
    val ScreenPaddingVertical: Dp = 10.dp
    val HudHeight: Dp = 68.dp
    val ControlPadHeight: Dp = 156.dp
    val ControlPadBottomPadding: Dp = 18.dp
    val PlayfieldHorizontalInset: Dp = 10.dp
    val PlayfieldTopInset: Dp = 10.dp
}
