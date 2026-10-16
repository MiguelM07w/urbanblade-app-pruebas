package com.urbanblade.mobile.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.urbanblade.mobile.ui.theme.UrbanColors

/**
 * Documentos legales de UrbanBlade. Viven en la web (una sola versión vigente para web y app) y se
 * abren en el navegador. Si cambia el dominio, se cambia aquí.
 */
object UrbanLegal {
    const val PRIVACY_URL = "https://urbanblade.com.mx/privacidad"
    const val TERMS_URL = "https://urbanblade.com.mx/terminos"
    const val CONTACT_EMAIL = "al222310427@gmail.com"
}

/** Texto con los enlaces a los Términos y al Aviso de Privacidad (se abren en el navegador). */
@Composable
fun LegalLinksText(prefix: String, modifier: Modifier = Modifier) {
    val linkStyle = TextLinkStyles(SpanStyle(color = UrbanColors.GoldText, fontWeight = FontWeight.SemiBold))
    val text = buildAnnotatedString {
        withStyle(SpanStyle(color = UrbanColors.Muted)) { append(prefix) }
        withLink(LinkAnnotation.Url(UrbanLegal.TERMS_URL, linkStyle)) { append("Términos y Condiciones") }
        withStyle(SpanStyle(color = UrbanColors.Muted)) { append(" y el ") }
        withLink(LinkAnnotation.Url(UrbanLegal.PRIVACY_URL, linkStyle)) { append("Aviso de Privacidad") }
        withStyle(SpanStyle(color = UrbanColors.Muted)) { append(" de UrbanBlade.") }
    }
    Text(text, style = MaterialTheme.typography.bodySmall, modifier = modifier)
}

/** Casilla obligatoria del registro, igual que en la web: sin aceptarla no se crea la cuenta. */
@Composable
fun LegalConsentCheckbox(checked: Boolean, onCheckedChange: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier
            .fillMaxWidth()
            .toggleable(value = checked, role = Role.Checkbox, onValueChange = onCheckedChange),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = null,
            colors = CheckboxDefaults.colors(checkedColor = UrbanColors.Gold, checkmarkColor = UrbanColors.OnGold, uncheckedColor = UrbanColors.Muted)
        )
        Spacer(Modifier.width(8.dp))
        LegalLinksText("Soy mayor de edad y acepto los ")
    }
}

/** Botón de texto para abrir un documento legal desde Cuenta. */
@Composable
fun rememberOpenLegal(): (String) -> Unit {
    val uriHandler = LocalUriHandler.current
    return { url -> runCatching { uriHandler.openUri(url) } }
}
