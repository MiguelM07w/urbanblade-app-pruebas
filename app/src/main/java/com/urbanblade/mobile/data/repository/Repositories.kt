package com.urbanblade.mobile.data.repository

import android.content.Context
import android.net.Uri
import com.urbanblade.mobile.core.media.MediaUploadHelper
import com.urbanblade.mobile.core.media.UploadPolicy
import com.urbanblade.mobile.core.network.UrbanBladeApi
import com.urbanblade.mobile.core.session.SessionManager
import com.urbanblade.mobile.data.model.*
import okhttp3.MultipartBody

class AuthRepository(
    private val api: UrbanBladeApi,
    private val session: SessionManager
) {
    suspend fun login(email: String, password: String): AuthUser {
        val response = api.login(LoginRequest(email, password))
        session.saveToken(response.token)
        return response.user
    }

    suspend fun register(name: String, email: String, password: String): AuthUser {
        val response = api.register(
            RegisterRequest(name, email, password, password)
        )
        session.saveToken(response.token)
        return response.user
    }

    suspend fun googleLogin(idToken: String): AuthUser {
        val response = api.googleLogin(GoogleLoginRequest(idToken))
        session.saveToken(response.token)
        return response.user
    }

    suspend fun forgotPassword(email: String): String {
        return api.forgotPassword(ForgotPasswordRequest(email)).message
            ?: "Si el correo existe, recibirás instrucciones."
    }

    /** Vuelve a leer el usuario de la sesión (tras cambiar nombre, correo o foto en Mi cuenta). */
    suspend fun currentUser(): AuthUser = api.me().user.also { session.saveUser(it) }

    /**
     * Arranque de sesión. `fromCache` es verdadero cuando se entró con el resumen guardado porque
     * no se pudo confirmar con el servidor (sin red o error del servidor): la sesión sigue viva y
     * la app lo avisa, en vez de mostrar la pantalla de acceso como si se hubiera cerrado.
     */
    data class RestoredSession(val user: AuthUser?, val fromCache: Boolean = false)

    suspend fun restoreSession(): RestoredSession {
        if (session.currentToken().isNullOrBlank()) return RestoredSession(null)
        return try {
            val user = api.me().user
            session.saveUser(user)
            RestoredSession(user)
        } catch (e: retrofit2.HttpException) {
            // 401: el servidor rechazó la sesión; ahí sí se limpia y se pide acceso otra vez.
            if (e.code() == 401) {
                session.clear()
                RestoredSession(null)
            } else {
                session.cachedUser()?.let { RestoredSession(it, fromCache = true) } ?: RestoredSession(null)
            }
        } catch (_: Exception) {
            // Sin red (o tiempo agotado): se entra con el resumen guardado.
            session.cachedUser()?.let { RestoredSession(it, fromCache = true) } ?: RestoredSession(null)
        }
    }

    suspend fun logout() {
        try { api.logout() } catch (_: Exception) { }
        session.clear()
    }

    /** Borra solo el token local, sin llamar al servidor (que ya rechazó esa sesión). */
    suspend fun clearLocalSession() {
        session.clear()
    }
}

class UrbanRepository(private val api: UrbanBladeApi) {
    suspend fun dashboard() = api.dashboard()
    suspend fun services() = api.services().data
    suspend fun barbers() = api.barbers().data
    suspend fun barberProfile(slug: String) = api.barberProfile(slug)
    suspend fun reviewBarber(slug: String, body: BarberReviewRequest) = api.reviewBarber(slug, body)
    suspend fun slots(barberId: String, serviceId: String, date: String) =
        api.slots(barberId, serviceId, date).slots

    suspend fun appointments(page: Int? = null, perPage: Int? = null, desde: String? = null, hasta: String? = null) =
        api.appointments(page, perPage, desde, hasta)
    suspend fun createAppointment(body: AppointmentRequest) = api.createAppointment(body)
    suspend fun updateAppointment(code: String, body: AppointmentRequest) = api.updateAppointment(code, body)
    suspend fun cancelAppointment(code: String) = api.cancelAppointment(code)
    suspend fun createPayment(request: CreatePaymentRequest) = api.createPayment(request)

    suspend fun adminMembershipPlans() = api.adminMembershipPlans().data
    suspend fun createMembershipPlan(body: MembershipPlanRequest) = api.createMembershipPlan(body)
    suspend fun updateMembershipPlan(id: String, body: MembershipPlanRequest) = api.updateMembershipPlan(id, body)
    suspend fun servicePackages() = api.servicePackages().data
    suspend fun createServicePackage(body: ServicePackageRequest) = api.createServicePackage(body)
    suspend fun updateServicePackage(id: String, body: ServicePackageRequest) = api.updateServicePackage(id, body)

    suspend fun barbersAdmin(page: Int, q: String?, activo: String?) = api.barbersAdmin(page, q, activo)
    suspend fun updateBarber(slug: String, body: BarberUpsertRequest) = api.updateBarber(slug, body)
    suspend fun barberPerformance(slug: String) = api.barberPerformance(slug).data

    suspend fun servicesAdmin(page: Int, q: String?, activo: String?, categoria: String?) =
        api.servicesAdmin(page, q, activo, categoria)
    suspend fun createService(body: ServiceUpsertRequest) = api.createService(body)
    suspend fun updateService(slug: String, body: ServiceUpsertRequest) = api.updateService(slug, body)
    suspend fun deleteService(slug: String) = api.deleteService(slug)

    suspend fun stripeIntent(body: StripeIntentRequest) = api.stripeIntent(body).data
    suspend fun transferInfo() = api.transferInfo().data
    suspend fun depositStripeIntent(code: String, body: DepositIntentRequest) = api.depositStripeIntent(code, body).data

    suspend fun uploadDepositReceipt(context: Context, code: String, receiptUri: Uri): UploadPaymentReceiptResponse {
        val comprobante = MediaUploadHelper.uriToPart(context, receiptUri, "comprobante")
            ?: error("No se pudo leer el comprobante seleccionado.")
        return api.uploadDepositReceipt(code, comprobante)
    }
    suspend fun savedCards() = api.savedCards().data
    suspend fun savePushToken(token: String) = api.savePushToken(PushTokenRequest(token))
    suspend fun sendTestPush() = api.sendTestPush()
    suspend fun sessionOpenedWithGoogle() = api.me().sesionConGoogle
    suspend fun deleteAccount(body: Map<String, String>) = api.deleteAccount(body)

    suspend fun uploadPaymentReceipt(context: Context, code: String, propina: Double, receiptUri: Uri): UploadPaymentReceiptResponse {
        val comprobante = MediaUploadHelper.uriToPart(context, receiptUri, "comprobante")
            ?: error("No se pudo leer el comprobante seleccionado.")
        val propinaPart = if (propina > 0) MultipartBody.Part.createFormData("propina", propina.toString()) else null
        return api.uploadPaymentReceipt(code, comprobante, propinaPart)
    }

    suspend fun joinWaitlist(barberId: String, serviceId: String, fecha: String) =
        api.joinWaitlist(WaitlistRequest(barberId, serviceId, fecha))
    suspend fun waitlist() = api.waitlist().data
    suspend fun waitlistForStaff(estado: String?) = api.waitlistForStaff(estado).data
    suspend fun inventoryMovementsPage(page: Int, tipo: String?, q: String?) = api.inventoryMovementsPage(page, tipo, q)
    suspend fun leaveWaitlist(id: String) = api.leaveWaitlist(id)

    suspend fun membershipPlans() = api.membershipPlans().data
    suspend fun myMembership() = api.myMembership().data
    suspend fun cancelMembership() = api.cancelMembership()
    suspend fun subscribeMembership(planId: String) = api.subscribeMembership(SubscribeMembershipRequest(planId)).data
    suspend fun membershipInvoices() = api.membershipInvoices()
    suspend fun membershipReceiptUrl(id: String) = api.membershipReceiptLink(id).data?.receiptUrl
    suspend fun myPackages() = api.myPackages().data
    suspend fun packageCatalog() = api.packageCatalog().data
    suspend fun myGiftCards() = api.myGiftCards().data
    suspend fun sellGiftCardCash(body: CashGiftCardRequest) = api.sellGiftCardCash(body).data
    suspend fun giftCardByCode(code: String) = api.giftCardByCode(code).data
    suspend fun myReferrals() = api.myReferrals().data

    // El backend no expone un endpoint propio de lealtad -- viene embebido en
    // GET dashboard (rama "cliente"), que hoy solo se tipa como JsonObject
    // crudo. Se extrae aquí sin tocar el resto de DashboardResponse.
    suspend fun clientLoyalty(): ClientLoyalty? {
        val loyaltyJson = api.dashboard().data.getAsJsonObject("loyalty") ?: return null
        return com.google.gson.Gson().fromJson(loyaltyJson, ClientLoyalty::class.java)
    }
    suspend fun profile() = api.profile().user
    suspend fun updateProfile(body: UpdateProfileRequest) = api.updateProfile(body)
    suspend fun updatePassword(current: String, password: String, confirmation: String) =
        api.updatePassword(ChangePasswordRequest(current, password, confirmation))
    suspend fun updateAvatar(context: Context, uri: Uri): AvatarResponse {
        val avatar = MediaUploadHelper.uriToPart(context, uri, "avatar", UploadPolicy.AVATAR)
            ?: error("No se pudo leer la foto seleccionada.")
        return api.updateAvatar(avatar)
    }
    suspend fun notificationPreferences() = api.notificationPreferences().data
    suspend fun updateNotificationPreference(key: String, enabled: Boolean) =
        api.updateNotificationPreferences(mapOf(key to enabled)).data
    suspend fun products(query: String? = null) = api.products(query).data
    suspend fun orders(page: Int = 1, estado: String? = null, q: String? = null) = api.orders(page, estado, q)
    suspend fun createOrder(body: OrderRequest) = api.createOrder(body)
    suspend fun cancelOrder(id: String) = api.cancelOrder(id)
    suspend fun deliverOrder(id: String, method: String) = api.deliverOrder(id, DeliverOrderRequest(method))
    suspend fun payments() = api.payments()
    suspend fun paymentsForStaff(page: Int, q: String?, metodo: String?, desde: String?, hasta: String?) =
        api.paymentsForStaff(page, q, metodo, desde, hasta)
    suspend fun paymentReceiptUrl(id: String) = api.paymentReceipt(id).data?.receiptUrl
    suspend fun orderReceiptUrl(id: String) = api.orderReceiptLink(id).data?.receiptUrl
    suspend fun pendingPayments() = api.pendingPayments()
    suspend fun approvePayment(id: String) = api.approvePayment(id)
    suspend fun rejectPayment(id: String, reason: String) = api.rejectPayment(id, RejectPaymentRequest(reason))
    suspend fun pendingDeposits() = api.pendingDeposits()
    suspend fun approveDeposit(id: String) = api.approveDeposit(id)
    suspend fun rejectDeposit(id: String, reason: String) = api.rejectDeposit(id, RejectPaymentRequest(reason))
    suspend fun notifications() = api.notifications()
    suspend fun markAllNotificationsRead() = api.markNotificationsRead()
    suspend fun markNotificationRead(id: String) = api.markNotificationRead(id)
    suspend fun analytics() = api.analytics()
    suspend fun socialFeed() = api.socialFeed()
    suspend fun reactWork(id: String) = api.reactWork(id)
    suspend fun saveWork(id: String) = api.saveWork(id)
    suspend fun commentWork(id: String, comment: String) = api.commentWork(id, PostCommentRequest(comment))
    suspend fun chatbot(message: String) = api.chatbot(com.google.gson.JsonObject().apply { addProperty("message", message) })
    suspend fun chatbotHistory() = api.chatbotHistory().history
    suspend fun aiBriefing() = api.aiBriefing().data
    suspend fun clearChatbot() = api.clearChatbot()
    suspend fun chatbotFeedback(message: String, response: String, helpful: Boolean) =
        api.chatbotFeedback(ChatFeedbackRequest(message, response, helpful))

    suspend fun barberSchedule() = api.barberSchedule().schedules
    suspend fun updateBarberSchedule(schedules: List<BarberScheduleDay>) =
        api.updateBarberSchedule(UpdateBarberScheduleRequest(schedules)).schedules

    suspend fun cashClosePreview() = api.cashClosePreview().data
    suspend fun registerCashClose(efectivoContado: Double, notas: String?) =
        api.registerCashClose(RegisterCashCloseRequest(efectivoContado, notas))

    suspend fun adminMetrics() = api.adminMetrics().metrics
    suspend fun adminStats() = api.adminStats()
    suspend fun systemStatus() = api.systemStatus()
    suspend fun raffles() = api.raffles()

    suspend fun clients(search: String? = null, segment: String? = null, page: Int = 1): ClientsResponse {
        val query = buildMap {
            put("page", page.toString())
            if (!search.isNullOrBlank()) put("search", search)
            if (!segment.isNullOrBlank()) put("segment", segment)
        }
        return api.adminClients(query)
    }
    suspend fun clientDetail(id: String) = api.adminClientDetail(id).data
    suspend fun createClient(name: String, email: String, telefono: String?, password: String) =
        api.createClient(CreateClientRequest(name, email, telefono, password))
    suspend fun updateClient(id: String, name: String?, email: String?, telefono: String?, notas: String?) =
        api.updateClient(id, UpdateClientRequest(name, email, telefono, notas))
    suspend fun deleteClient(id: String) = api.deleteClient(id)

    suspend fun inventoryProducts() = api.inventoryProducts()

    private fun productFields(
        nombre: String, categoria: String, descripcion: String?,
        precioCompra: Double, precioVenta: Double, stockActual: Int, stockMinimo: Int,
        tipo: String, activo: Boolean
    ) = buildMap {
        put("nombre", MediaUploadHelper.textPart(nombre))
        put("categoria", MediaUploadHelper.textPart(categoria))
        descripcion?.let { put("descripcion", MediaUploadHelper.textPart(it)) }
        put("precio_compra", MediaUploadHelper.textPart(precioCompra.toString()))
        put("precio_venta", MediaUploadHelper.textPart(precioVenta.toString()))
        put("stock_actual", MediaUploadHelper.textPart(stockActual.toString()))
        put("stock_minimo", MediaUploadHelper.textPart(stockMinimo.toString()))
        put("tipo", MediaUploadHelper.textPart(tipo))
        put("activo", MediaUploadHelper.textPart(if (activo) "1" else "0"))
    }

    suspend fun createProduct(context: Context, req: CreateProductRequest, imageUri: Uri?) = api.createProduct(
        productFields(req.nombre, req.categoria, req.descripcion, req.precioCompra, req.precioVenta, req.stockActual, req.stockMinimo, req.tipo, req.activo),
        imageUri?.let { MediaUploadHelper.uriToPart(context, it, "imagen") }
    )

    // Va como POST con _method=PUT: PHP no lee los campos ni el archivo de un PUT multipart, así
    // que un PUT llegaba vacío al backend y la edición (con o sin foto) no guardaba nada.
    suspend fun updateProduct(context: Context, id: String, req: CreateProductRequest, imageUri: Uri?) = api.updateProduct(
        id,
        productFields(req.nombre, req.categoria, req.descripcion, req.precioCompra, req.precioVenta, req.stockActual, req.stockMinimo, req.tipo, req.activo) +
            ("_method" to MediaUploadHelper.textPart("PUT")),
        imageUri?.let { MediaUploadHelper.uriToPart(context, it, "imagen") }
    )

    suspend fun portfolio() = api.barberPortfolio()
    suspend fun createWork(context: Context, title: String, description: String?, mediaUris: List<Uri>): CreateWorkResponse {
        val parts = mediaUris.mapNotNull { MediaUploadHelper.uriToPart(context, it, "media[]") }
        return api.createWork(
            MediaUploadHelper.textPart(title),
            description?.let { MediaUploadHelper.textPart(it) },
            parts
        )
    }
    suspend fun deleteWork(id: String) = api.deleteWork(id)
    suspend fun deleteProduct(id: String) = api.deleteProduct(id)
    suspend fun registerMovement(productId: String, tipo: String, cantidad: Int, motivo: String?) =
        api.registerMovement(RegisterMovementRequest(productId, tipo, cantidad, motivo))

    suspend fun reviews(barberId: String? = null, rating: Int? = null): ReviewsResponse {
        val query = buildMap {
            if (!barberId.isNullOrBlank()) put("barber_id", barberId)
            if (rating != null) put("rating", rating.toString())
        }
        return api.reviews(query)
    }

    suspend fun logs(search: String? = null, logName: String? = null, event: String? = null): LogsResponse {
        val query = buildMap {
            if (!search.isNullOrBlank()) put("q", search)
            if (!logName.isNullOrBlank()) put("log_name", logName)
            if (!event.isNullOrBlank()) put("event", event)
        }
        return api.logs(query)
    }

    suspend fun systemUsers(search: String? = null, role: String? = null, page: Int = 1): SystemUsersResponse {
        val query = buildMap {
            put("page", page.toString())
            if (!search.isNullOrBlank()) put("q", search)
            if (!role.isNullOrBlank()) put("role", role)
        }
        return api.users(query)
    }
    suspend fun createUser(name: String, email: String, password: String, role: String) =
        api.createUser(CreateUserRequest(name, email, password, password, role))
    suspend fun updateUser(id: String, name: String, email: String, password: String?, role: String) =
        api.updateUser(id, UpdateUserRequest(name, email, password?.takeIf { it.isNotBlank() }, password?.takeIf { it.isNotBlank() }, role))
    suspend fun deleteUser(id: String) = api.deleteUser(id)

    suspend fun reportManifest() = api.reports()
    suspend fun exportReport(type: String, format: String, startDate: String? = null, endDate: String? = null): ReportData {
        val query = buildMap {
            if (!startDate.isNullOrBlank()) put("start_date", startDate)
            if (!endDate.isNullOrBlank()) put("end_date", endDate)
        }
        return api.exportReport(type, format, query)
    }

    suspend fun settings() = api.settings().data
    suspend fun updateSettings(body: UpdateSettingRequest) = api.updateSettings(body).data
    suspend fun toggleMaintenance() = api.toggleMaintenance()

    suspend fun barberAgenda(period: String, estado: String?, offset: Int): BarberAgendaResponse {
        val query = buildMap {
            put("period", period)
            put("offset", offset.toString())
            if (!estado.isNullOrBlank()) put("estado", estado)
        }
        return api.barberAgenda(query)
    }
    suspend fun updateAppointmentStatus(code: String, estado: String) =
        api.updateAppointmentStatus(code, com.google.gson.JsonObject().apply { addProperty("estado", estado) })

    suspend fun extendAppointment(code: String, minutos: Int, forzar: Boolean = false) =
        api.extendAppointment(code, com.google.gson.JsonObject().apply { addProperty("minutos", minutos); addProperty("forzar", forzar) })
    suspend fun appointmentTicket(code: String) = api.appointmentTicket(code).data

    suspend fun noShowFees(estado: String? = null) = api.noShowFees(estado)
    suspend fun payNoShowFee(id: String, metodo: String) =
        api.payNoShowFee(id, com.google.gson.JsonObject().apply { addProperty("metodo", metodo) })
    suspend fun waiveNoShowFee(id: String, motivo: String) =
        api.waiveNoShowFee(id, com.google.gson.JsonObject().apply { addProperty("motivo", motivo) })

    suspend fun campaigns() = api.campaigns()
    suspend fun createCampaign(body: CreateCampaignRequest) = api.createCampaign(body)

    suspend fun predictionInsights() = api.predictionInsights().data
}
