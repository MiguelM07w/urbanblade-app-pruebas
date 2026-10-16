package com.urbanblade.mobile.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import org.junit.Assert.assertTrue
import org.junit.Test

class ThemeContrastTest {
    private fun contrast(a: Color, b: Color): Float {
        val x = a.luminance(); val y = b.luminance()
        return (maxOf(x, y) + .05f) / (minOf(x, y) + .05f)
    }

    @Test fun `texto y acento legibles sobre superficies de los cuatro temas`() {
        try {
            UrbanTheme.entries.forEach { theme ->
                UrbanColors.applyTheme(theme)
                listOf(UrbanColors.Background, UrbanColors.Card, UrbanColors.CardAlt).forEach { bg ->
                    listOf(UrbanColors.Ink, UrbanColors.Muted, UrbanColors.GoldText).forEach { fg ->
                        assertTrue("${theme.key}: contraste ${contrast(fg, bg)}", contrast(fg, bg) >= 4.5f)
                    }
                }
            }
        } finally { UrbanColors.applyTheme(UrbanTheme.NOIR) }
    }

    @Test fun `estados del tema claro legibles sobre tarjetas`() {
        try {
            UrbanColors.applyTheme(UrbanTheme.LIBRETA)
            listOf(UrbanColors.Success, UrbanColors.Warning, UrbanColors.Danger, UrbanColors.Info).forEach { fg ->
                assertTrue(contrast(fg, UrbanColors.CardAlt) >= 4.5f)
            }
            assertTrue(contrast(UrbanColors.OnGold, UrbanColors.Gold) >= 4.5f)
        } finally { UrbanColors.applyTheme(UrbanTheme.NOIR) }
    }
}
