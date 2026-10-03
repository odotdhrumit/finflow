package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// ====================================================
// ORYVO Official Color Palette
// ====================================================

// Primary Purple (#6F2DBD): primary buttons, selected navigation, important actions, main branding
val OryvoPrimaryPurple = Color(0xFF6F2DBD)

// Purple / Secondary (#A663CC): secondary actions and selected highlights
val OryvoSecondaryPurple = Color(0xFFA663CC)

// Soft Purple (#B298DC): subtle cards, backgrounds and supporting UI
val OryvoSoftPurple = Color(0xFFB298DC)

// Soft Blue (#B8D0EB): subtle cards, borders, supporting UI
val OryvoSoftBlue = Color(0xFFB8D0EB)

// Light Cyan (#B9FAF8): very lightly for soft highlights and positive visual areas
val OryvoLightCyan = Color(0xFFB9FAF8)

// Clean, very light main app background & surfaces
val OryvoBackground = Color(0xFFFAF9FE)
val OryvoSurface = Color(0xFFFFFFFF)
val OryvoSurfaceVariant = Color(0xFFF3EFFB) // very subtle purple-tinted surface
val OryvoBorder = Color(0xFFE4DFEE)          // soft subtle lavender/blue border

// Clean high-contrast typography
val OryvoTextPrimary = Color(0xFF1E1430)
val OryvoTextSecondary = Color(0xFF5D546D)
val OryvoTextMuted = Color(0xFF8B829C)

// Semantic Financial Colors (Muted & Professional - NEVER neon or gaming-style)
val OryvoIncome = Color(0xFF248255)
val OryvoIncomeLight = Color(0xFFEAF8F1)
val OryvoExpense = Color(0xFFC74343)
val OryvoExpenseLight = Color(0xFFFDF0F0)

// Aliases for compatibility
val FinFlowPrimaryPurple = OryvoPrimaryPurple
val FinFlowSecondaryPurple = OryvoSecondaryPurple
val FinFlowSoftPurple = OryvoSoftPurple
val FinFlowSoftBlue = OryvoSoftBlue
val FinFlowLightCyan = OryvoLightCyan
val FinFlowBackground = OryvoBackground
val FinFlowSurface = OryvoSurface
val FinFlowSurfaceVariant = OryvoSurfaceVariant
val FinFlowBorder = OryvoBorder
val FinFlowTextPrimary = OryvoTextPrimary
val FinFlowTextSecondary = OryvoTextSecondary
val FinFlowTextMuted = OryvoTextMuted
val FinFlowIncome = OryvoIncome
val FinFlowIncomeLight = OryvoIncomeLight
val FinFlowExpense = OryvoExpense
val FinFlowExpenseLight = OryvoExpenseLight

// Aliases for compatibility across existing screens
val DeepNavy = FinFlowPrimaryPurple
val DeepNavyDark = Color(0xFF541C94)
val DeepNavyLight = Color(0xFFF0E5FA)

val PositiveGreen = FinFlowIncome
val PositiveGreenLight = FinFlowIncomeLight
val NegativeRed = FinFlowExpense
val NegativeRedLight = FinFlowExpenseLight
val WarningAmber = FinFlowSecondaryPurple
val WarningAmberLight = Color(0xFFF6EEFB)

val IncomeGreen = FinFlowIncome
val ExpenseRed = FinFlowExpense
val LoanAmber = FinFlowPrimaryPurple
val SavingsTeal = FinFlowSecondaryPurple
val InvestmentIndigo = FinFlowPrimaryPurple
val TransferPurple = FinFlowSecondaryPurple

val EmeraldPrimary = FinFlowPrimaryPurple
val EmeraldLight = FinFlowSoftPurple
val EmeraldDark = Color(0xFF541C94)

// Dark Theme Palette (Refined Deep Slate & Purple)
val DarkBackground = Color(0xFF130E1E)
val DarkSurface = Color(0xFF1C162B)
val DarkSurfaceVariant = Color(0xFF28203B)
val DarkTextPrimary = Color(0xFFF5F3FA)
val DarkTextSecondary = Color(0xFFB6ADC7)
val DarkBorder = Color(0xFF3B3054)
val DarkPrimary = FinFlowSoftPurple
val DarkPositiveGreen = Color(0xFF4EBA86)
val DarkNegativeRed = Color(0xFFE67373)

// Backward-compatible light theme aliases
val LightBackground = FinFlowBackground
val LightSurface = FinFlowSurface
val LightSurfaceVariant = FinFlowSurfaceVariant
val LightTextPrimary = FinFlowTextPrimary
val LightTextSecondary = FinFlowTextSecondary
val LightBorder = FinFlowBorder
