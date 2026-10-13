package com.urbanblade.mobile.core.network

import com.google.gson.JsonObject
import com.urbanblade.mobile.data.model.*
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.*

interface UrbanBladeApi {
    @PUT("profile/favorite-barber")
    suspend fun favoriteBarber(@Body body: FavoriteBarberRequest): FavoriteBarberResponse

    @Streaming
    @GET("dashboard/membership/card")
    suspend fun membershipCard(): okhttp3.ResponseBody

    @POST("packages/stripe-intent")
    suspend fun packageStripeIntent(@Body body: PackagePurchaseRequest): StripeIntentResponse

    @POST("gift-cards/stripe-intent")
    suspend fun giftCardStripeIntent(@Body body: CashGiftCardRequest): StripeIntentResponse
    @POST("auth/login") suspend fun login(@Body body: LoginRequest): LoginResponse
    @POST("auth/register") suspend fun register(@Body body: RegisterRequest): LoginResponse
    @POST("auth/forgot-password") suspend fun forgotPassword(@Body body: ForgotPasswordRequest): MessageResponse
    @POST("auth/google/token") suspend fun googleLogin(@Body body: GoogleLoginRequest): LoginResponse
    @GET("auth/me") suspend fun me(): MeResponse
    /** Elimina la cuenta propia: {"password": …} o, en sesión de Google, {"confirmacion": "ELIMINAR"}. */
    @HTTP(method = "DELETE", path = "profile", hasBody = true) suspend fun deleteAccount(@Body body: Map<String, String>): MessageResponse
    @POST("auth/logout") suspend fun logout(): MessageResponse?
    @POST("auth/refresh-token") suspend fun refreshToken(): LoginResponse

    @GET("dashboard") suspend fun dashboard(): DashboardResponse
    @GET("analytics") suspend fun analytics(): AnalyticsResponse

    @GET("barbershop") suspend fun barbershop(): JsonObject
    @GET("services") suspend fun services(): ApiList<ServiceItem>
    @GET("barbers") suspend fun barbers(): ApiList<BarberItem>
    @GET("products") suspend fun products(@Query("q") q: String? = null, @Query("categoria") categoria: String? = null): ApiList<ProductItem>
    @GET("barbers/{barber}") suspend fun barberDetail(@Path("barber") barber: String): JsonObject
    @GET("barbers/{barber}") suspend fun barberProfile(@Path("barber") barber: String): BarberProfileResponse
    @POST("barbers/{barber}/review") suspend fun reviewBarber(@Path("barber") barber: String, @Body body: BarberReviewRequest): MessageResponse

    @GET("availability/slots")
    suspend fun slots(@Query("barber_id") barberId: String, @Query("service_id") serviceId: String, @Query("date") date: String): SlotsResponse

    @GET("appointments") suspend fun appointments(
        @Query("page") page: Int? = null,
        @Query("per_page") perPage: Int? = null,
        @Query("desde") desde: String? = null,
        @Query("hasta") hasta: String? = null
    ): AppointmentsResponse
    @POST("appointments") suspend fun createAppointment(@Body body: AppointmentRequest): CreateAppointmentResponse
    @PUT("appointments/{code}") suspend fun updateAppointment(@Path("code") code: String, @Body body: AppointmentRequest): MessageResponse
    @PATCH("appointments/{code}/status") suspend fun updateAppointmentStatus(@Path("code") code: String, @Body body: JsonObject): MessageResponse
    @DELETE("appointments/{code}") suspend fun cancelAppointment(@Path("code") code: String): MessageResponse

    /** Agregar tiempo al servicio en curso (flujo V2): `{minutos, forzar}`. 422 con `puede_forzar` si choca con la siguiente cita. */
    @POST("appointments/{code}/extend") suspend fun extendAppointment(@Path("code") code: String, @Body body: JsonObject): MessageResponse
    /** Ticket del servicio terminado (cliente dueño, barbero de la cita o personal). */
    @GET("appointments/{code}/ticket") suspend fun appointmentTicket(@Path("code") code: String): ServiceTicketResponse

    /** Cargos por inasistencia: el cliente los suyos (con `adeudo_total`); recepción/admin, por estado (pendiente por defecto). */
    @GET("no-show-fees") suspend fun noShowFees(@Query("estado") estado: String? = null): NoShowFeesResponse
    @POST("no-show-fees/{id}/pay") suspend fun payNoShowFee(@Path("id") id: String, @Body body: JsonObject): MessageResponse
    @POST("no-show-fees/{id}/waive") suspend fun waiveNoShowFee(@Path("id") id: String, @Body body: JsonObject): MessageResponse

    @POST("waitlist") suspend fun joinWaitlist(@Body body: WaitlistRequest): WaitlistJoinResponse
    @GET("waitlist") suspend fun waitlist(): WaitlistResponse
    @GET("waitlist") suspend fun waitlistForStaff(@Query("estado") estado: String? = null): WaitlistResponse
    @DELETE("waitlist/{id}") suspend fun leaveWaitlist(@Path("id") id: String): MessageResponse

    // ── Wallet: autoservicio del cliente, sin cambios de contrato en barber ──
    @GET("memberships/plans") suspend fun membershipPlans(): MembershipPlansResponse
    @GET("memberships/mine") suspend fun myMembership(): MyMembershipResponse
    @POST("memberships/cancel") suspend fun cancelMembership(): MessageResponse
    @POST("memberships/subscribe") suspend fun subscribeMembership(@Body body: SubscribeMembershipRequest): SubscribeMembershipResponse
    @GET("memberships/invoices") suspend fun membershipInvoices(): MembershipInvoicesResponse
    @GET("memberships/invoices/{id}/receipt-link") suspend fun membershipReceiptLink(@Path("id") id: String): MembershipReceiptResponse
    @GET("packages") suspend fun myPackages(): MyPackagesResponse
    @GET("packages/catalog") suspend fun packageCatalog(): PackageCatalogResponse
    @GET("gift-cards/mine") suspend fun myGiftCards(): GiftCardsResponse
    @POST("gift-cards") suspend fun sellGiftCardCash(@Body body: CashGiftCardRequest): GiftCardSaleResponse
    @GET("gift-cards/{code}") suspend fun giftCardByCode(@Path("code") code: String): GiftCardLookupResponse
    @GET("referrals/mine") suspend fun myReferrals(): ReferralInfoResponse

    @GET("orders") suspend fun orders(
        @Query("page") page: Int = 1,
        @Query("estado") estado: String? = null,
        @Query("q") q: String? = null
    ): OrdersResponse
    @POST("orders") suspend fun createOrder(@Body body: OrderRequest): OrderMutationResponse
    @PATCH("orders/{id}/cancel") suspend fun cancelOrder(@Path("id") id: String): OrderMutationResponse
    @GET("orders/{id}/receipt-link") suspend fun orderReceiptLink(@Path("id") id: String): OrderReceiptResponse
    @PATCH("orders/{id}/deliver") suspend fun deliverOrder(@Path("id") id: String, @Body body: DeliverOrderRequest): OrderMutationResponse

    @GET("payments") suspend fun payments(): PaymentsResponse
    @GET("payments") suspend fun paymentsForStaff(
        @Query("page") page: Int = 1,
        @Query("q") q: String? = null,
        @Query("metodo_pago") metodoPago: String? = null,
        @Query("fecha_desde") fechaDesde: String? = null,
        @Query("fecha_hasta") fechaHasta: String? = null
    ): PaymentsResponse
    @GET("payments/{id}/receipt") suspend fun paymentReceipt(@Path("id") id: String): ReceiptResponse
    @GET("payments/pending") suspend fun pendingPayments(): PendingPaymentsResponse
    @POST("payments") suspend fun createPayment(@Body body: CreatePaymentRequest): JsonObject

    @GET("admin/membership-plans") suspend fun adminMembershipPlans(): AdminMembershipPlansResponse
    @POST("admin/membership-plans") suspend fun createMembershipPlan(@Body body: MembershipPlanRequest): JsonObject
    @PUT("admin/membership-plans/{id}") suspend fun updateMembershipPlan(@Path("id") id: String, @Body body: MembershipPlanRequest): JsonObject

    @GET("admin/service-packages") suspend fun servicePackages(): ServicePackagesResponse
    @POST("admin/service-packages") suspend fun createServicePackage(@Body body: ServicePackageRequest): JsonObject
    @PUT("admin/service-packages/{id}") suspend fun updateServicePackage(@Path("id") id: String, @Body body: ServicePackageRequest): JsonObject

    @GET("barbers/manage") suspend fun barbersAdmin(
        @Query("page") page: Int = 1,
        @Query("q") q: String? = null,
        @Query("activo") activo: String? = null
    ): BarbersAdminResponse
    @PUT("barbers/manage/{slug}") suspend fun updateBarber(@Path("slug") slug: String, @Body body: BarberUpsertRequest): JsonObject
    @GET("admin/barbers/{slug}/performance") suspend fun barberPerformance(@Path("slug") slug: String): BarberPerformanceResponse

    @GET("services/manage") suspend fun servicesAdmin(
        @Query("page") page: Int = 1,
        @Query("q") q: String? = null,
        @Query("activo") activo: String? = null,
        @Query("categoria") categoria: String? = null
    ): ServicesAdminResponse
    @POST("services/manage") suspend fun createService(@Body body: ServiceUpsertRequest): JsonObject
    @PUT("services/manage/{slug}") suspend fun updateService(@Path("slug") slug: String, @Body body: ServiceUpsertRequest): JsonObject
    @DELETE("services/manage/{slug}") suspend fun deleteService(@Path("slug") slug: String): JsonObject
    @POST("payments/{id}/approve") suspend fun approvePayment(@Path("id") id: String): MessageResponse
    @POST("payments/{id}/reject") suspend fun rejectPayment(@Path("id") id: String, @Body body: RejectPaymentRequest): MessageResponse
    // Anticipos: comprobantes de "pagar ahora" por transferencia y depósitos anti-no-show (misma forma que payments/pending).
    @GET("deposits/pending") suspend fun pendingDeposits(): PendingPaymentsResponse
    @POST("deposits/{id}/approve") suspend fun approveDeposit(@Path("id") id: String): MessageResponse
    @POST("deposits/{id}/reject") suspend fun rejectDeposit(@Path("id") id: String, @Body body: RejectPaymentRequest): MessageResponse
    @POST("payments/stripe-intent") suspend fun stripeIntent(@Body body: StripeIntentRequest): StripeIntentResponse
    @GET("payments/transfer-info") suspend fun transferInfo(): TransferInfoResponse
    @POST("appointments/{code}/deposit/stripe-intent")
    suspend fun depositStripeIntent(@Path("code") code: String, @Body body: DepositIntentRequest): StripeIntentResponse
    @Multipart
    @POST("appointments/{code}/deposit/receipt")
    suspend fun uploadDepositReceipt(@Path("code") code: String, @Part comprobante: MultipartBody.Part): UploadPaymentReceiptResponse
    @GET("payments/cards") suspend fun savedCards(): SavedCardsResponse
    @POST("profile/push-token") suspend fun savePushToken(@Body body: PushTokenRequest): MessageResponse
    /** T061: el servidor manda un push de prueba solo a este teléfono. */
    @POST("profile/push-test") suspend fun sendTestPush(): MessageResponse
    @Multipart
    @POST("appointments/{code}/payment/receipt")
    suspend fun uploadPaymentReceipt(
        @Path("code") code: String,
        @Part comprobante: MultipartBody.Part,
        @Part propina: MultipartBody.Part?
    ): UploadPaymentReceiptResponse

    @GET("profile") suspend fun profile(): AccountProfileResponse
    @PUT("profile") suspend fun updateProfile(@Body body: UpdateProfileRequest): UpdateProfileResponse
    @PUT("profile/password") suspend fun updatePassword(@Body body: ChangePasswordRequest): MessageResponse
    @Multipart
    @POST("profile/avatar") suspend fun updateAvatar(@Part avatar: MultipartBody.Part): AvatarResponse
    @GET("notifications/preferences") suspend fun notificationPreferences(): NotificationPreferencesResponse
    @PATCH("notifications/preferences")
    suspend fun updateNotificationPreferences(@Body body: Map<String, Boolean>): NotificationPreferencesResponse

    @GET("notifications") suspend fun notifications(): JsonObject
    @POST("notifications/read-all") suspend fun markNotificationsRead(): MessageResponse
    @POST("notifications/{id}/read") suspend fun markNotificationRead(@Path("id") id: String): MessageResponse
    @DELETE("notifications/{id}") suspend fun deleteNotification(@Path("id") id: String): MessageResponse

    @GET("social/feed") suspend fun socialFeed(): SocialFeedResponse
    @POST("social/work/{id}/react") suspend fun reactWork(@Path("id") id: String): SocialActionResponse
    @POST("social/work/{id}/save") suspend fun saveWork(@Path("id") id: String): SocialActionResponse
    @POST("social/work/{id}/comment") suspend fun commentWork(@Path("id") id: String, @Body body: PostCommentRequest): PostCommentResponse

    @POST("chatbot/query") suspend fun chatbot(@Body body: JsonObject): JsonObject
    @GET("ai/briefing") suspend fun aiBriefing(): AiBriefingResponse
    @GET("chatbot/history") suspend fun chatbotHistory(): ChatHistoryResponse
    @POST("chatbot/feedback") suspend fun chatbotFeedback(@Body body: ChatFeedbackRequest): MessageResponse
    @POST("chatbot/clear-history") suspend fun clearChatbot(): MessageResponse

    @GET("barber/agenda") suspend fun barberAgenda(@QueryMap query: Map<String, String> = emptyMap()): BarberAgendaResponse
    @GET("barber/me") suspend fun barberMe(): JsonObject
    @GET("barber/portfolio") suspend fun barberPortfolio(): PortfolioResponse
    @Multipart
    @POST("barber/works")
    suspend fun createWork(
        @Part("title") title: RequestBody,
        @Part("description") description: RequestBody?,
        @Part media: List<MultipartBody.Part>
    ): CreateWorkResponse
    @DELETE("barber/works/{id}") suspend fun deleteWork(@Path("id") id: String): MessageResponse
    @GET("barber/schedule") suspend fun barberSchedule(): BarberScheduleResponse
    @PUT("barber/schedule") suspend fun updateBarberSchedule(@Body body: UpdateBarberScheduleRequest): BarberScheduleResponse

    @GET("clients") suspend fun clients(): JsonObject
    @GET("inventory/products") suspend fun inventoryProducts(@Query("page") page: Int = 1): InventoryResponse
    // Multipart siempre (no solo cuando hay imagen): Laravel valida los
    // campos de texto igual venga el request como JSON o multipart/form-data,
    // así que no hace falta duplicar el endpoint -- un solo camino para
    // crear/editar con o sin imagen.
    @Multipart
    @POST("inventory/products")
    suspend fun createProduct(@PartMap fields: Map<String, @JvmSuppressWildcards RequestBody>, @Part imagen: MultipartBody.Part?): JsonObject
    @Multipart
    @POST("inventory/products/{id}") // con el campo _method=PUT (ver UrbanRepository.updateProduct)
    suspend fun updateProduct(@Path("id") id: String, @PartMap fields: Map<String, @JvmSuppressWildcards RequestBody>, @Part imagen: MultipartBody.Part?): JsonObject
    @DELETE("inventory/products/{id}") suspend fun deleteProduct(@Path("id") id: String): MessageResponse
    @POST("inventory/movements") suspend fun registerMovement(@Body body: RegisterMovementRequest): JsonObject
    @GET("inventory/low-stock") suspend fun lowStock(): JsonObject
    @GET("inventory/movements") suspend fun inventoryMovements(): JsonObject
    @GET("inventory/movements") suspend fun inventoryMovementsPage(
        @Query("page") page: Int = 1,
        @Query("tipo") tipo: String? = null,
        @Query("q") q: String? = null
    ): InventoryMovementsResponse
    @GET("cash-closes") suspend fun cashCloses(): JsonObject
    @GET("cash-closes/preview") suspend fun cashClosePreview(): CashClosePreviewResponse
    @POST("cash-closes") suspend fun registerCashClose(@Body body: RegisterCashCloseRequest): JsonObject

    @GET("reports") suspend fun reports(): ReportManifest
    @GET("reports/{type}/{format}") suspend fun exportReport(
        @Path("type") type: String,
        @Path("format") format: String,
        @QueryMap query: Map<String, String> = emptyMap()
    ): ReportData
    @GET("logs") suspend fun logs(@QueryMap query: Map<String, String> = emptyMap()): LogsResponse
    @GET("campaigns") suspend fun campaigns(): CampaignsResponse
    @POST("campaigns") suspend fun createCampaign(@Body body: CreateCampaignRequest): MessageResponse
    @GET("raffles") suspend fun raffles(): RaffleResponse
    @GET("reviews") suspend fun reviews(@QueryMap query: Map<String, String> = emptyMap()): ReviewsResponse
    @GET("users") suspend fun users(@QueryMap query: Map<String, String> = emptyMap()): SystemUsersResponse
    @POST("users") suspend fun createUser(@Body body: CreateUserRequest): SystemUserMutationResponse
    @PUT("users/{id}") suspend fun updateUser(@Path("id") id: String, @Body body: UpdateUserRequest): SystemUserMutationResponse
    @DELETE("users/{id}") suspend fun deleteUser(@Path("id") id: String): MessageResponse
    @GET("settings") suspend fun settings(): BarbershopSettingResponse
    @PUT("settings") suspend fun updateSettings(@Body body: UpdateSettingRequest): BarbershopSettingResponse
    @POST("settings/maintenance") suspend fun toggleMaintenance(): MaintenanceToggleResponse

    @GET("admin/dashboard/stats") suspend fun adminStats(): JsonObject
    @GET("admin/dashboard/appointments") suspend fun adminAppointments(): JsonObject
    @GET("admin/dashboard/revenue") suspend fun adminRevenue(): JsonObject
    @GET("admin/dashboard/alerts") suspend fun adminAlerts(): JsonObject
    @GET("admin/dashboard/metrics") suspend fun adminMetrics(): AdminMetricsResponse
    @GET("admin/predictions/insights") suspend fun predictionInsights(): InsightsResponse
    @GET("admin/system/status") suspend fun systemStatus(): SystemStatusResponse
    @GET("admin/clients") suspend fun adminClients(@QueryMap query: Map<String, String> = emptyMap()): ClientsResponse
    @GET("admin/clients/{id}") suspend fun adminClientDetail(@Path("id") id: String): ClientDetailResponse
    @POST("admin/clients") suspend fun createClient(@Body body: CreateClientRequest): ClientMutationResponse
    @PUT("admin/clients/{id}") suspend fun updateClient(@Path("id") id: String, @Body body: UpdateClientRequest): ClientMutationResponse
    @DELETE("admin/clients/{id}") suspend fun deleteClient(@Path("id") id: String): MessageResponse
    @GET("admin/inventory/summary") suspend fun adminInventorySummary(): JsonObject

    @GET suspend fun genericGet(@Url url: String, @QueryMap query: Map<String, String> = emptyMap()): JsonObject
    @POST suspend fun genericPost(@Url url: String, @Body body: JsonObject = JsonObject()): JsonObject
    @PUT suspend fun genericPut(@Url url: String, @Body body: JsonObject): JsonObject
    @PATCH suspend fun genericPatch(@Url url: String, @Body body: JsonObject): JsonObject
    @DELETE suspend fun genericDelete(@Url url: String): JsonObject
}
