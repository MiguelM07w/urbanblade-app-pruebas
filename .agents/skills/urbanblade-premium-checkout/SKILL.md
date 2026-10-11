---
name: UrbanBlade Premium Checkout Flow
description: Guía de implementación para el flujo de selección de tarjetas 3D y animaciones de recibo (estilo Fintech) para plataformas móvil y web.
---

# UrbanBlade Premium Checkout Flow

Esta skill define los patrones de UI/UX y los requisitos técnicos para implementar una experiencia de pago premium (inspirada en Fintech) en el ecosistema UrbanBlade.

## 🎯 Objetivo General
Replicar la sensación premium de una "impresión de recibo" y una "selección espacial de tarjetas de crédito" fluida, minimizando la carga cognitiva y maximizando la satisfacción (delight) del usuario.

## 📱 Fase 1: Aplicación Móvil (Android / Jetpack Compose)

### 1. Componente: Carrusel Espacial de Tarjetas (`UrbanCreditCardCarousel.kt`)
*   **Base Técnica:** Utilizar `HorizontalPager` de `androidx.compose.foundation.pager`.
*   **Animación 3D (Efecto Profundidad):** 
    *   Utilizar `Modifier.graphicsLayer` para escalar las tarjetas que no están en el foco (`scaleX` y `scaleY` a `0.85f`).
    *   Modificar la opacidad (`alpha` a `0.6f`) de las tarjetas laterales.
    *   (Opcional) Aplicar un ligero `rotationY` basado en el `pagerState.currentPageOffsetFraction` para un efecto de cubierta 3D.
*   **Identidad Visual (Mapeo de Marcas a UrbanColors):**
    *   `Visa`: Tarjeta con fondo plata/gris usando gradientes sutiles basados en `UrbanColors.Line` / grises.
    *   `Mastercard`: Tarjeta dorada utilizando `UrbanColors.Gold`.
    *   `American Express / Premium`: Tarjeta oscura (Black Card) usando `UrbanColors.Ink` con tipografía dorada.
*   **Interacciones (Haptics):**
    *   Inyectar `LocalHapticFeedback.current.performHapticFeedback(HapticFeedbackType.TextHandleMove)` cuando el `pagerState.currentPage` cambie, para dar la sensación física de que las tarjetas hacen "click" al girar.

### 2. Componente: Recibo Animado (`UrbanReceiptAnimation.kt`)
*   *Nota:* Este componente ya fue implementado previamente. Se encarga de mostrar un ticket deslizándose desde una ranura de impresora cuando el pago es exitoso (`justPaid == true`). 
*   **Regla de Integración:** Asegurarse de que cualquier nuevo flujo de checkout (ej. Membresías en la Wallet) oculte el formulario y monte este componente antes de cerrar el BottomSheet.

## 💻 Fase 2: Plataforma Web (React.js + Tailwind CSS)

Si UrbanBlade se expande a una interfaz de cliente web (ej. panel de reservas web):

### 1. Animaciones (Framer Motion)
*   **Carrusel de Tarjetas:** Usar `framer-motion` para el comportamiento de arrastre (drag).
    ```jsx
    <motion.div 
      drag="x" 
      dragConstraints={{ left: -150, right: 150 }} 
      whileTap={{ scale: 0.95 }}
      animate={{ scale: isCenter ? 1 : 0.85, opacity: isCenter ? 1 : 0.5 }}
    >
      {/* Visual de Tarjeta */}
    </motion.div>
    ```
### 2. Recibo Animado (Web)
*   Utilizar `clip-path: polygon(...)` en CSS para simular los dientes (zigzag) de la parte inferior del ticket de papel.
*   Usar `transform: translateY(...)` dentro de un contenedor con `overflow: hidden` para simular la salida de la impresora.

## ⚠️ Reglas Estrictas (Constraint)
*   **Sin sobrecargar la UI:** Solo debe haber un modal/bottom-sheet. El carrusel de tarjetas vive *adentro* del `CheckoutSheet`, reemplazando los simples Radio Buttons por tarjetas visuales.
*   **Contrato Backend:** No modificar los Modelos (DTOs) ni llamadas de Retrofit actuales. El UI debe adaptarse y parsear la data (como los últimos 4 dígitos) con lo que ya existe o inyectando mock visual hasta que el endpoint esté listo.
