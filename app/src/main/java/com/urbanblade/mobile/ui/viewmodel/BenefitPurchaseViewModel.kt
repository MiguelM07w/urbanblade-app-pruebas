package com.urbanblade.mobile.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.urbanblade.mobile.core.media.MembershipCardFiles
import com.urbanblade.mobile.core.network.AppContainer
import com.urbanblade.mobile.data.model.PackageCatalogItem
import com.urbanblade.mobile.data.repository.UrbanRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

class BenefitPurchaseViewModel @JvmOverloads constructor(
    private val repo: UrbanRepository = AppContainer.urbanRepository
) : ViewModel() {
    private val _catalog = MutableStateFlow<List<PackageCatalogItem>>(emptyList())
    val catalog = _catalog.asStateFlow()
    private val _busy = MutableStateFlow(false)
    val busy = _busy.asStateFlow()
    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()
    private val _message = MutableStateFlow<String?>(null)
    val message = _message.asStateFlow()
    private val _secret = MutableStateFlow<String?>(null)
    val secret = _secret.asStateFlow()
    private val _document = MutableStateFlow<File?>(null)
    val document = _document.asStateFlow()

    fun clearError() { _error.value = null }

    fun loadCatalog() {
        if (_busy.value) return
        _busy.value = true
        viewModelScope.launch {
            _error.value = null
            try { _catalog.value = repo.packageCatalog() }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) { _error.value = e.toFriendlyMessage("No pudimos cargar los paquetes.") }
            finally { _busy.value = false }
        }
    }

    fun buyPackage(id: String) = purchase { repo.buyPackage(id).clientSecret }
    fun buyGiftCard(amount: Double, recipient: String?) = purchase { repo.buyGiftCard(amount, recipient).clientSecret }

    private fun purchase(createIntent: suspend () -> String) {
        // Se bloquea antes de lanzar la corrutina: un doble toque no crea dos intentos.
        if (_busy.value) return
        _busy.value = true
        _error.value = null; _message.value = null
        viewModelScope.launch {
            try {
                val secret = createIntent()
                require(secret.isNotBlank()) { "No pudimos iniciar el pago." }
                _secret.value = secret
            } catch (e: CancellationException) { _busy.value = false; throw e }
            catch (e: Exception) {
                _error.value = e.toFriendlyMessage("No pudimos iniciar el pago. Intenta de nuevo.")
                _busy.value = false
            }
        }
    }

    fun consumeSecret() { _secret.value = null }
    fun paymentResult(completed: Boolean, canceled: Boolean) {
        _secret.value = null; _busy.value = false
        when {
            completed -> _message.value = "Pago enviado. Tu compra aparecerá en Beneficios cuando el servidor confirme el cobro."
            canceled -> _message.value = "Cancelaste el pago. Puedes volver a intentarlo."
            else -> _error.value = "No pudimos completar el pago. Revisa tu tarjeta o inténtalo de nuevo."
        }
    }

    fun downloadCard(context: Context) {
        if (_busy.value) return
        _busy.value = true; _error.value = null
        viewModelScope.launch {
            try { _document.value = MembershipCardFiles.save(context.applicationContext, repo.membershipCard()) }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) { _error.value = e.toFriendlyMessage("No pudimos descargar tu tarjeta.") }
            finally { _busy.value = false }
        }
    }

    fun documentOpened(opened: Boolean) {
        _document.value = null
        if (!opened) _error.value = "Necesitas una aplicación para abrir PDF. Instálala y vuelve a descargar tu tarjeta."
    }
}
