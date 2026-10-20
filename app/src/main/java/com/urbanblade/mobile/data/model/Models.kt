package com.urbanblade.mobile.data.model

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.annotations.SerializedName

// `plataforma` le dice al backend que este token es de la app móvil: tiene su propia vigencia
// (config/auth.php de barber: 180 días con ventana deslizante, la web usa 30). Sin ella el
// backend la infiere del device_name, pero declararla evita depender del texto del nombre.
data class LoginRequest(
    val email: String,
    val password: String,
    @SerializedName("device_name") val deviceName: String = "Android UrbanBlade",
    val plataforma: String = "movil"
)

data class RegisterRequest(
    val name: String,
    val email: String,
    val password: String,
    @SerializedName("password_confirmation") val passwordConfirmation: String,
    @SerializedName("device_name") val deviceName: String = "Android UrbanBlade",
    val plataforma: String = "movil"
)

data class ForgotPasswordRequest(val email: String)
data class GoogleLoginRequest(@SerializedName("id_token") val idToken: String)

data class LoginResponse(
    val message: String? = null,
    @SerializedName("token_type") val tokenType: String? = null,
    val token: String,
    val user: AuthUser
)

data class MeResponse(
    val user: AuthUser,
    /** La sesión se abrió con Google: para eliminar la cuenta se confirma con ELIMINAR, no con contraseña. */
    @SerializedName("sesion_con_google") val sesionConGoogle: Boolean = false
)

data class AuthUser(
    val id: String? = null,
    val name: String,
    val email: String,
    @SerializedName("avatar_url") val avatarUrl: String? = null,
    val roles: List<String> = emptyList(),
    @SerializedName("profile_complete") val profileComplete: Boolean = true,
    @SerializedName("profile_missing") val profileMissing: List<String> = emptyList(),
    @SerializedName("client_id") val clientId: String? = null,
    @SerializedName("barber_id") val barberId: String? = null,
    val client: ClientInfo? = null
)

data class ClientInfo(
    val telefono: String? = null,
    @SerializedName("fecha_nacimiento") val fechaNacimiento: String? = null,
    val sexo: String? = null
)

data class MessageResponse(
    val message: String? = null,
    /** Solo en PATCH /appointments/{code}/status al pasar a «completada»: el ticket del servicio. */
    val ticket: ServiceTicket? = null
)

/** Ticket del servicio terminado (GET /appointments/{code}/ticket y la respuesta de «completada»). */
data class ServiceTicket(
    val folio: String = "",
    val cita: String? = null,
    val fecha: String? = null,
    val cliente: String? = null,
    val barbero: String? = null,
    val servicio: String? = null,
    @SerializedName("duracion_min") val duracionMin: Int = 0,
    @SerializedName("minutos_extra") val minutosExtra: Int = 0,
    @SerializedName("metodo_pago") val metodoPago: String? = null,
    @SerializedName("precio_servicio") val precioServicio: Double = 0.0,
    val descuentos: Double = 0.0,
    @SerializedName("deposito_aplicado") val depositoAplicado: Double = 0.0,
    val monto: Double = 0.0,
    val propina: Double = 0.0,
    @SerializedName("total_pagado") val totalPagado: Double = 0.0,
    @SerializedName("comprobante_url") val comprobanteUrl: String? = null
)

data class ServiceTicketResponse(val data: ServiceTicket? = null)

/** Cargo por inasistencia (GET /no-show-fees): el cliente ve los suyos; recepción, los pendientes. */
data class NoShowFeeCita(
    val code: String? = null,
    val fecha: String? = null,
    val servicio: String? = null,
    val cliente: String? = null
)

data class NoShowFeeItem(
    val id: String = "",
    @SerializedName("appointment_id") val appointmentId: String = "",
    val cita: NoShowFeeCita? = null,
    val monto: Double = 0.0,
    @SerializedName("monto_base") val montoBase: Double = 0.0,
    @SerializedName("credito_anticipo") val creditoAnticipo: Double = 0.0,
    val porcentaje: Int = 0,
    val estado: String = "pendiente",
    @SerializedName("metodo_cobro") val metodoCobro: String? = null
)

data class NoShowFeesResponse(
    val data: List<NoShowFeeItem> = emptyList(),
    /** Solo para el cliente: lo que debe en total. */
    @SerializedName("adeudo_total") val adeudoTotal: Double? = null
)

/** POST /appointments (201): trae la cita creada para poder ofrecer "Pagar ahora" sin buscarla en la lista. */
data class CreatedAppointmentData(val id: String? = null, val code: String? = null)
data class CreateAppointmentResponse(
    val message: String? = null,
    val data: CreatedAppointmentData? = null,
    /** Si los productos no se pudieron agregar (p. ej. sin stock); la cita igual queda reservada. */
    @SerializedName("productos_error") val productosError: String? = null
)

/** POST /profile/push-token: token de Firebase Cloud Messaging del dispositivo (T142). */
data class PushTokenRequest(val token: String, val provider: String = "fcm")

data class ApiList<T>(val data: List<T> = emptyList())

data class ServiceItem(
    val id: String,
    val nombre: String,
    val precio: Double = 0.0,
    @SerializedName("duracion_min") val duracionMin: Int = 0,
    val descripcion: String? = null,
    val imagen: String? = null
)

data class BarberUser(val id: String? = null, val name: String? = null)

data class BarberItem(
    val id: String,
    val slug: String? = null,
    val user: BarberUser? = null,
    val descripcion: String? = null,
    val foto: String? = null,
    val especialidades: String? = null,
    @SerializedName("es_favorito") val esFavorito: Boolean = false,
    @SerializedName("avg_rating") val avgRating: Double? = null,
    @SerializedName("total_reviews") val totalReviews: Int = 0
)

/** GET /barbers/{slug}: ficha pública del barbero con portafolio y reseñas. */
data class BarberProfileResponse(
    val barber: BarberItem,
    val works: List<BarberWork> = emptyList(),
    val reviews: List<BarberReviewItem> = emptyList(),
    @SerializedName("avg_rating") val avgRating: Double? = null,
    @SerializedName("total_reviews") val totalReviews: Int = 0,
    @SerializedName("citas_completadas") val citasCompletadas: Int = 0,
    @SerializedName("can_review") val canReview: Boolean = false,
    @SerializedName("already_reviewed") val alreadyReviewed: Boolean = false,
    /** Servicios que el cliente recibió con este barbero y puede calificar. */
    @SerializedName("reviewable_services") val reviewableServices: List<ReviewableService> = emptyList()
)

data class BarberWork(val id: String, val title: String? = null, val description: String? = null, val images: List<String> = emptyList())
data class ReviewAuthor(val user: BarberUser? = null)
data class BarberReviewItem(
    val id: String,
    val rating: Int = 0,
    val comment: String? = null,
    @SerializedName("created_at") val createdAt: String? = null,
    val client: ReviewAuthor? = null,
    /** Nombre del servicio calificado, si la reseña es de un servicio. */
    val service: String? = null
)
data class ReviewableService(val id: String, val nombre: String)
data class BarberReviewRequest(
    val rating: Int,
    val comment: String? = null,
    @SerializedName("service_id") val serviceId: String? = null
)

data class SlotItem(
    val time: String,
    val label: String,
    @SerializedName("end_time") val endTime: String? = null,
    @SerializedName("end_label") val endLabel: String? = null
)

data class SlotsResponse(val slots: List<SlotItem> = emptyList())

data class AppointmentBarber(
    val id: String? = null,
    val slug: String? = null,
    val user: BarberUser? = null,
    @SerializedName("foto_url") val fotoUrl: String? = null
)

data class AppointmentService(
    val id: String? = null,
    val nombre: String? = null,
    val precio: Double? = null,
    @SerializedName("duracion_min") val duracionMin: Int? = null
)

data class AppointmentClientUser(val name: String? = null, @SerializedName("avatar_url") val avatarUrl: String? = null)
data class AppointmentClientRef(val id: String? = null, val user: AppointmentClientUser? = null)
data class AppointmentRow(
    val id: String,
    val code: String? = null,
    val fecha: String,
    @SerializedName("hora_inicio") val horaInicio: String,
    @SerializedName("hora_fin") val horaFin: String? = null,
    val estado: String,
    val notas: String? = null,
    @SerializedName("precio_cobrado") val precioCobrado: Double? = null,
    @SerializedName("has_payment") val hasPayment: Boolean = false,
    @SerializedName("is_chargeable") val isChargeable: Boolean = false,
    @SerializedName("reminder_24h_sent") val reminder24hSent: Boolean = false,
    @SerializedName("reminder_2h_sent") val reminder2hSent: Boolean = false,
    /** Flujo V2 (solo citas confirmadas): si el pago ya está resuelto y si se puede iniciar el servicio, con el motivo. */
    @SerializedName("pago_resuelto") val pagoResuelto: Boolean? = null,
    @SerializedName("puede_iniciar") val puedeIniciar: Boolean? = null,
    @SerializedName("motivo_no_iniciar") val motivoNoIniciar: String? = null,
    /** Solo en servicio en proceso: fin estimado (ISO-8601, con el tiempo extra) y minutos agregados. */
    @SerializedName("fin_estimado") val finEstimado: String? = null,
    @SerializedName("minutos_extra") val minutosExtra: Int? = null,
    val client: AppointmentClientRef? = null,
    val barber: AppointmentBarber? = null,
    val service: AppointmentService? = null
)

data class AppointmentStats(
    val total: Int = 0,
    val proximas: Int = 0,
    val completadas: Int = 0,
    val canceladas: Int = 0
)

/** Servicio del catálogo tal como lo ve el administrador (GET /services/manage). */
data class ServiceAdminItem(
    val id: String = "",
    val slug: String = "",
    val nombre: String = "",
    val categoria: String = "",
    val precio: Double = 0.0,
    @SerializedName("duracion_min") val duracionMin: Int = 0,
    val descripcion: String? = null,
    val activo: Boolean = true,
    @SerializedName("imagen_url") val imagenUrl: String? = null
)

data class ServicesAdminMeta(
    @SerializedName("current_page") val currentPage: Int = 1,
    @SerializedName("last_page") val lastPage: Int = 1,
    val total: Int = 0
)

data class ServicesAdminResponse(
    val data: List<ServiceAdminItem> = emptyList(),
    val meta: ServicesAdminMeta = ServicesAdminMeta(),
    val categories: List<String> = emptyList()
)

/** Cuerpo de crear/editar servicio; el servidor valida precio >= 0 y duración de 5 a 600 min. */
data class ServiceUpsertRequest(
    val nombre: String,
    val categoria: String,
    val precio: Double,
    @SerializedName("duracion_min") val duracionMin: Int,
    val descripcion: String?,
    val activo: Boolean
)

data class BarberAdminUser(val id: String? = null, val name: String = "", val email: String = "")

/** Barbero tal como lo ve el administrador (GET /barbers/manage). */
data class BarberAdminItem(
    val id: String = "",
    val slug: String = "",
    val especialidades: String? = null,
    val descripcion: String? = null,
    val foto: String? = null,
    @SerializedName("foto_url") val fotoUrl: String? = null,
    val activo: Boolean = true,
    @SerializedName("comision_pct") val comisionPct: Double = 0.0,
    val user: BarberAdminUser = BarberAdminUser()
)

data class BarbersAdminResponse(
    val data: List<BarberAdminItem> = emptyList(),
    val meta: ServicesAdminMeta = ServicesAdminMeta()
)

/**
 * El servidor reescribe todo el perfil en cada PUT (lo que falte queda en null/false), por eso
 * siempre se manda el perfil completo, incluida la foto que hoy no se edita desde la app.
 */
data class BarberUpsertRequest(
    val name: String,
    val email: String,
    val especialidades: String?,
    val descripcion: String?,
    val foto: String?,
    val activo: Boolean,
    @SerializedName("comision_pct") val comisionPct: Double
)

data class BarberPerformance(
    val appointmentsThisMonth: Int = 0,
    val appointmentsLastMonth: Int = 0,
    val growth: Int = 0,
    val averageRating: Double = 0.0,
    val totalClients: Int = 0
)

data class BarberPerformanceResponse(val data: BarberPerformance = BarberPerformance())

/** Plan de membresía recurrente (GET /admin/membership-plans). */
data class MembershipPlanItem(
    val id: String = "",
    val nombre: String = "",
    val descripcion: String? = null,
    @SerializedName("precio_mensual") val precioMensual: Double = 0.0,
    @SerializedName("descuento_pct") val descuentoPct: Int = 0,
    val activo: Boolean = true
)

data class AdminMembershipPlansResponse(val data: List<MembershipPlanItem> = emptyList())

/** Crear o cambiar el precio de un plan crea un Price nuevo en Stripe; activar/desactivar no. */
data class MembershipPlanRequest(
    val nombre: String,
    val descripcion: String?,
    @SerializedName("precio_mensual") val precioMensual: Double,
    @SerializedName("descuento_pct") val descuentoPct: Int,
    val activo: Boolean
)

/** Paquete prepagado de usos de un servicio (GET /admin/service-packages). */
data class ServicePackageItem(
    val id: String = "",
    val nombre: String = "",
    val service: PackageServiceRef = PackageServiceRef(),
    @SerializedName("cantidad_usos") val cantidadUsos: Int = 0,
    val precio: Double = 0.0,
    @SerializedName("vigencia_dias") val vigenciaDias: Int? = null,
    val activo: Boolean = true
)

data class ServicePackagesResponse(val data: List<ServicePackageItem> = emptyList())

data class ServicePackageRequest(
    val nombre: String,
    @SerializedName("service_id") val serviceId: String,
    @SerializedName("cantidad_usos") val cantidadUsos: Int,
    val precio: Double,
    @SerializedName("vigencia_dias") val vigenciaDias: Int?,
    val activo: Boolean
)

/** Bloque `meta` que el servidor agrega solo cuando se pide `page`. */
data class PageMeta(
    val page: Int = 1,
    @SerializedName("per_page") val perPage: Int = 0,
    val total: Int = 0,
    @SerializedName("has_more") val hasMore: Boolean = false
)

data class AppointmentsResponse(
    val data: List<AppointmentRow> = emptyList(),
    val meta: PageMeta? = null,
    val stats: AppointmentStats = AppointmentStats(),
    val next: AppointmentRow? = null,
    @SerializedName("cancellation_policy_hours") val cancellationPolicyHours: Int = 24
)

data class AppointmentRequest(
    @SerializedName("barber_id") val barberId: String,
    @SerializedName("service_id") val serviceId: String,
    val fecha: String,
    @SerializedName("hora_inicio") val horaInicio: String,
    val notas: String? = null,
    /** Pagar todo al reservar: barber fija el monto (con descuento) como cobro de la cita pendiente. */
    @SerializedName("pagar_ahora") val pagarAhora: Boolean? = null,
    @SerializedName("propina_sugerida") val propinaSugerida: Double? = null,
    /** El cliente acepta el cargo por inasistencia en el último paso de la reserva. */
    @SerializedName("acepta_cargo_inasistencia") val aceptaCargoInasistencia: Boolean? = null,
    /** Productos opcionales de la visita: barber crea un pedido ligado a la cita que se paga en el salón. */
    val productos: List<OrderItemRequest>? = null
)

/** POST /appointments/{code}/deposit/stripe-intent: cobro de "pagar ahora" (o depósito anti-no-show). */
data class DepositIntentRequest(
    @SerializedName("guardar_tarjeta") val guardarTarjeta: Boolean? = null,
    @SerializedName("tarjeta_guardada") val tarjetaGuardada: Boolean? = null
)

data class DashboardResponse(
    val role: String? = null,
    val data: JsonObject = JsonObject()
)

data class ProfileResponse(val user: ProfileUser)

data class ProfileUser(
    val name: String,
    val email: String,
    @SerializedName("avatar_url") val avatarUrl: String? = null,
    val roles: List<String> = emptyList(),
    val client: ClientInfo? = null
)

data class UpdateProfileRequest(
    val name: String,
    val email: String,
    val telefono: String? = null,
    @SerializedName("fecha_nacimiento") val fechaNacimiento: String? = null,
    val sexo: String? = null
)

data class UpdateProfileResponse(
    val message: String? = null,
    val user: AuthUser? = null
)

data class ChangePasswordRequest(
    @SerializedName("current_password") val currentPassword: String,
    val password: String,
    @SerializedName("password_confirmation") val passwordConfirmation: String
)

data class NotificationItem(
    val id: String,
    val title: String? = null,
    val message: String? = null,
    @SerializedName("read_at") val readAt: String? = null,
    @SerializedName("created_at") val createdAt: String? = null
)

data class ProductItem(
    val id: String,
    val nombre: String,
    val categoria: String? = null,
    val descripcion: String? = null,
    @SerializedName("precio_venta") val precioVenta: Double = 0.0,
    @SerializedName("stock_actual") val stockActual: Int = 0,
    val imagen: String? = null
)

data class OrderItemRequest(
    @SerializedName("product_id") val productId: String,
    val cantidad: Int
)
data class OrderRequest(val items: List<OrderItemRequest>)
data class DeliverOrderRequest(@SerializedName("metodo_pago") val metodoPago: String)
data class OrderLine(
    @SerializedName("product_id") val productId: String? = null,
    val nombre: String? = null,
    val precio: Double = 0.0,
    val cantidad: Int = 0,
    val subtotal: Double = 0.0
)
data class OrderClient(val id: String? = null, val name: String? = null)
data class OrderRow(
    val id: String,
    val folio: String? = null,
    val estado: String = "pendiente",
    val tipo: String? = null,
    val total: Double = 0.0,
    @SerializedName("metodo_pago") val metodoPago: String? = null,
    @SerializedName("created_at") val createdAt: String? = null,
    @SerializedName("entregado_en") val entregadoEn: String? = null,
    val items: List<OrderLine> = emptyList(),
    val client: OrderClient? = null
)
/** GET orders/{id}/receipt-link: liga firmada al comprobante PDF de un pedido entregado. */
data class OrderReceiptData(@SerializedName("order_id") val orderId: String? = null, @SerializedName("receipt_url") val receiptUrl: String? = null)
data class OrderReceiptResponse(val data: OrderReceiptData? = null)
/** Totales que el servidor calcula solo para el personal. */
data class OrderStats(
    val pendientes: Int = 0,
    val entregados: Int = 0,
    @SerializedName("por_cobrar") val porCobrar: Double = 0.0
)
data class OrderMeta(
    @SerializedName("current_page") val currentPage: Int = 1,
    @SerializedName("last_page") val lastPage: Int = 1,
    val total: Int = 0,
    val stats: OrderStats? = null
)
data class OrdersResponse(val data: List<OrderRow> = emptyList(), val meta: OrderMeta? = null)
data class OrderMutationResponse(val message: String? = null, val data: OrderRow? = null)

data class PaymentAppointment(
    val id: String? = null,
    val fecha: String? = null,
    @SerializedName("hora_inicio") val horaInicio: String? = null,
    val service: String? = null,
    val client: String? = null,
    val barber: String? = null
)
data class PaymentRow(
    val id: String,
    val monto: Double = 0.0,
    @SerializedName("metodo_pago") val metodoPago: String? = null,
    val propina: Double = 0.0,
    @SerializedName("receipt_url") val receiptUrl: String? = null,
    @SerializedName("created_at") val createdAt: String? = null,
    val appointment: PaymentAppointment? = null,
    /** verificado | pendiente_verificacion | rechazado | reembolsado (null en pagos antiguos = cobrado). */
    val estado: String? = null,
    /** Pagado al reservar (anticipo o pago completo por adelantado). */
    @SerializedName("es_deposito") val esDeposito: Boolean = false
) {
    /** Dinero que de verdad se quedó en la barbería: solo estos llevan comprobante. */
    val collected: Boolean get() = estado == null || estado == "verificado"
}
/** Estadísticas globales que el servidor calcula para el personal (no dependen de la página cargada). */
data class PaymentsStats(
    @SerializedName("total_hoy") val totalHoy: Double = 0.0,
    @SerializedName("total_mes") val totalMes: Double = 0.0,
    val count: Int = 0,
    val metodos: Map<String, Int> = emptyMap()
)
/** `meta` del listado: el personal recibe paginación y estadísticas; el cliente, sus totales. */
data class PaymentsMeta(
    @SerializedName("current_page") val currentPage: Int = 1,
    @SerializedName("last_page") val lastPage: Int = 1,
    val total: Int = 0,
    val stats: PaymentsStats? = null,
    @SerializedName("pending_count") val pendingCount: Int = 0,
    @SerializedName("total_pagado") val totalPagado: Double = 0.0,
    @SerializedName("total_citas") val totalCitas: Int = 0
)
data class PaymentsResponse(val data: List<PaymentRow> = emptyList(), val meta: PaymentsMeta? = null)
data class ReceiptData(@SerializedName("payment_id") val paymentId: String? = null, @SerializedName("receipt_url") val receiptUrl: String? = null)
data class ReceiptResponse(val data: ReceiptData? = null)
data class PendingAppointment(
    val id: String? = null,
    val client: String? = null,
    val service: String? = null,
    @SerializedName("service_price") val servicePrice: Double? = null
)
data class PendingPaymentRow(
    val id: String,
    val monto: Double = 0.0,
    @SerializedName("created_at") val createdAt: String? = null,
    @SerializedName("comprobante_url") val comprobanteUrl: String? = null,
    @SerializedName("ocr_texto") val ocrTexto: String? = null,
    @SerializedName("ocr_monto_detectado") val ocrMontoDetectado: Double? = null,
    val appointment: PendingAppointment? = null
)
/**
 * Cobro manual del personal. `monto` es solo informativo: PaymentService relee el precio del servicio
 * en el servidor y calcula descuentos y puntos (nunca se confía en el monto del cliente).
 */
data class CreatePaymentRequest(
    @SerializedName("appointment_id") val appointmentId: String,
    val monto: Double,
    @SerializedName("metodo_pago") val metodoPago: String,
    val propina: Double = 0.0
)
data class PendingPaymentsResponse(val data: List<PendingPaymentRow> = emptyList())
data class RejectPaymentRequest(@SerializedName("motivo_rechazo") val motivo: String)
data class StripeIntentRequest(
    @SerializedName("appointment_id") val appointmentId: String,
    @SerializedName("puntos_canjeados") val puntos: Int = 0,
    @SerializedName("codigo_gift_card") val codigoGiftCard: String? = null,
    val propina: Double = 0.0,
    /** Tarjeta nueva que el cliente quiere guardar para la próxima vez. */
    @SerializedName("guardar_tarjeta") val guardarTarjeta: Boolean? = null,
    /** El cliente paga con una tarjeta que ya tenía guardada. */
    @SerializedName("tarjeta_guardada") val tarjetaGuardada: Boolean? = null
)

/** GET /payments/transfer-info: datos para transferir que configura el administrador. */
data class TransferInfo(
    val configurado: Boolean = false,
    val banco: String? = null,
    val beneficiario: String? = null,
    val clabe: String? = null,
    val concepto: String? = null
)
data class TransferInfoResponse(val data: TransferInfo = TransferInfo())

/** GET /payments/cards: tarjeta guardada en Stripe (solo datos seguros de mostrar). */
data class SavedCard(
    val id: String,
    val brand: String = "card",
    val last4: String = "",
    @SerializedName("exp_month") val expMonth: Int = 0,
    @SerializedName("exp_year") val expYear: Int = 0,
    /** Nombre del titular tal como se guardó en Stripe; null en tarjetas guardadas antes de pedirlo. */
    val holder: String? = null
)
data class SavedCardsResponse(val data: List<SavedCard> = emptyList())
data class StripeIntentResponseData(
    @SerializedName("client_secret") val clientSecret: String,
    @SerializedName("payment_intent_id") val paymentIntentId: String
)
data class StripeIntentResponse(val data: StripeIntentResponseData)
data class UploadPaymentReceiptResponse(val message: String? = null, val data: UploadPaymentReceiptData? = null)
data class UploadPaymentReceiptData(val id: String, val estado: String)

// ── Horario de barbero ──────────────────────────────────────────────────
data class BarberScheduleDay(
    @SerializedName("day_of_week") val dayOfWeek: String,
    @SerializedName("start_time") val startTime: String,
    @SerializedName("end_time") val endTime: String,
    @SerializedName("is_active") val isActive: Boolean = true
)
data class BarberScheduleResponse(val schedules: List<BarberScheduleDay> = emptyList())
data class UpdateBarberScheduleRequest(val schedules: List<BarberScheduleDay>)

// ── Corte de caja ────────────────────────────────────────────────────────
data class CashCloseRecord(
    val id: String? = null,
    @SerializedName("efectivo_contado") val efectivoContado: Double = 0.0,
    val diferencia: Double = 0.0,
    val notas: String? = null,
    @SerializedName("cerrado_por_nombre") val cerradoPorNombre: String? = null,
    @SerializedName("created_at") val createdAt: String? = null
)
data class CashClosePreview(
    val fecha: String? = null,
    val esperado: Map<String, Double> = emptyMap(),
    @SerializedName("esperado_total") val esperadoTotal: Double = 0.0,
    @SerializedName("efectivo_esperado") val efectivoEsperado: Double = 0.0,
    val propinas: Double = 0.0,
    val pagos: Int = 0,
    val pedidos: Int = 0,
    val paquetes: Int = 0,
    @SerializedName("gift_cards") val giftCards: Int = 0,
    val membresias: Int = 0,
    val cierre: CashCloseRecord? = null
)
data class CashClosePreviewResponse(val data: CashClosePreview = CashClosePreview())
data class RegisterCashCloseRequest(
    @SerializedName("efectivo_contado") val efectivoContado: Double,
    val notas: String? = null
)

// ── Métricas de admin ────────────────────────────────────────────────────
data class AdminMetrics(
    val totalClients: Int = 0,
    val activeBarbers: Int = 0,
    val cancellationRate: Double = 0.0,
    val averageRevenuePerAppointment: Double = 0.0,
    val totalRevenue: Double = 0.0
)
data class AdminMetricsResponse(val metrics: AdminMetrics = AdminMetrics())

// ── Estado del sistema ───────────────────────────────────────────────────
data class SystemAppInfo(
    val name: String? = null,
    val env: String? = null,
    @SerializedName("laravel_version") val laravelVersion: String? = null,
    @SerializedName("php_version") val phpVersion: String? = null
)
data class SystemServiceStatus(
    val status: String? = null,
    @SerializedName("latency_ms") val latencyMs: Int? = null,
    val error: String? = null
)
data class SystemQueueStatus(
    val connection: String? = null,
    val pending: Int? = null,
    val failed: Int? = null
)
data class SystemScheduledTask(
    val name: String? = null,
    val expression: String? = null,
    val status: String? = null,
    @SerializedName("ran_at") val ranAt: String? = null,
    @SerializedName("runtime_ms") val runtimeMs: Int? = null,
    val error: String? = null
)
data class SystemStatusResponse(
    val app: SystemAppInfo = SystemAppInfo(),
    val database: SystemServiceStatus = SystemServiceStatus(),
    val redis: SystemServiceStatus = SystemServiceStatus(),
    val queue: SystemQueueStatus = SystemQueueStatus(),
    @SerializedName("scheduled_tasks") val scheduledTasks: List<SystemScheduledTask> = emptyList()
)

// ── Clientes (admin/clients) ─────────────────────────────────────────────
data class ClientRow(
    val id: String,
    val slug: String? = null,
    val name: String? = null,
    @SerializedName("avatar_url") val avatarUrl: String? = null,
    val email: String? = null,
    val telefono: String? = null,
    val segment: String? = null,
    val totalAppointments: Int = 0,
    val totalSpent: Double = 0.0,
    val lastAppointment: String? = null,
    val joinedAt: String? = null
)
data class ClientsResponse(
    val success: Boolean = true,
    val data: List<ClientRow> = emptyList(),
    val total: Int? = null,
    @SerializedName("current_page") val currentPage: Int? = null,
    @SerializedName("last_page") val lastPage: Int? = null
)
data class ClientAppointmentRow(
    val id: String,
    val code: String? = null,
    val fecha: String? = null,
    @SerializedName("hora_inicio") val horaInicio: String? = null,
    val barber: String? = null,
    val service: String? = null,
    val precio: Double? = null,
    val estado: String? = null
)
data class ClientDetail(
    val id: String,
    val slug: String? = null,
    val name: String? = null,
    @SerializedName("avatar_url") val avatarUrl: String? = null,
    val email: String? = null,
    val telefono: String? = null,
    val segment: String? = null,
    val nivel: String? = null,
    val puntos: Int = 0,
    val notas: String? = null,
    val joinedAt: String? = null,
    val totalAppointments: Int = 0,
    val totalSpent: Double = 0.0,
    val averageSpent: Double = 0.0,
    val lastAppointment: String? = null,
    val daysSinceLastAppointment: Int? = null,
    val preferredBarber: String? = null,
    val appointments: List<ClientAppointmentRow> = emptyList()
)
data class ClientDetailResponse(val success: Boolean = true, val data: ClientDetail? = null)
data class ClientMutationResponse(val success: Boolean = true, val message: String? = null, val data: ClientRow? = null)
data class CreateClientRequest(val name: String, val email: String, val telefono: String?, val password: String)
data class UpdateClientRequest(val name: String?, val email: String?, val telefono: String?, val notas: String?)

// ── Inventario (inventory/products, staff: admin + recepcionista) ────────
data class InventoryProductRow(
    val id: String,
    val nombre: String,
    val categoria: String? = null,
    val descripcion: String? = null,
    val tipo: String? = null,
    @SerializedName("stock_actual") val stockActual: Int = 0,
    @SerializedName("stock_minimo") val stockMinimo: Int = 0,
    @SerializedName("precio_compra") val precioCompra: Double = 0.0,
    @SerializedName("precio_venta") val precioVenta: Double = 0.0,
    val activo: Boolean = true,
    @SerializedName("low_stock") val lowStock: Boolean = false,
    @SerializedName("pending_restock") val pendingRestock: Boolean = false,
    @SerializedName("imagen_url") val imagenUrl: String? = null
)
data class InventoryStats(val total: Int = 0, @SerializedName("bajo_stock") val bajoStock: Int = 0, @SerializedName("valor_total") val valorTotal: Double = 0.0)
data class InventoryMeta(
    @SerializedName("current_page") val currentPage: Int? = null,
    @SerializedName("last_page") val lastPage: Int? = null,
    val total: Int? = null,
    val stats: InventoryStats = InventoryStats(),
    val categorias: List<String> = emptyList(),
    val tipos: List<String> = emptyList()
)
data class InventoryResponse(val data: List<InventoryProductRow> = emptyList(), val meta: InventoryMeta = InventoryMeta())
data class CreateProductRequest(
    val nombre: String,
    val categoria: String,
    val descripcion: String?,
    @SerializedName("precio_compra") val precioCompra: Double,
    @SerializedName("precio_venta") val precioVenta: Double,
    @SerializedName("stock_actual") val stockActual: Int,
    @SerializedName("stock_minimo") val stockMinimo: Int,
    val tipo: String,
    val activo: Boolean = true
)
data class RegisterMovementRequest(
    @SerializedName("product_id") val productId: String,
    val tipo: String,
    val cantidad: Int,
    val motivo: String?
)

// ── Reseñas (reviews, admin-only) ────────────────────────────────────────
data class ReviewBarberRef(val id: String? = null, val name: String? = null)
data class ReviewClientRef(val id: String? = null, val name: String? = null)
data class ReviewRow(
    val id: String,
    val rating: Int = 0,
    val comment: String? = null,
    @SerializedName("created_at") val createdAt: String? = null,
    val barber: ReviewBarberRef? = null,
    val client: ReviewClientRef? = null
)
data class ReviewStats(val total: Int = 0, val promedio: Double = 0.0, val bajas: Int = 0)
data class ReviewsResponse(
    val data: List<ReviewRow> = emptyList(),
    val meta: JsonObject? = null,
    val barbers: List<ReviewBarberRef> = emptyList(),
    val stats: ReviewStats = ReviewStats()
)

// ── Logs de auditoría (admin + ingeniero, solo lectura) ──────────────────
data class LogCauser(val id: String? = null, val name: String? = null, val email: String? = null)
data class LogRow(
    val id: String,
    @SerializedName("log_name") val logName: String? = null,
    val description: String? = null,
    val event: String? = null,
    @SerializedName("subject_type") val subjectType: String? = null,
    @SerializedName("created_at") val createdAt: String? = null,
    val causer: LogCauser? = null
)
data class LogStats(val total: Int = 0, val hoy: Int = 0, val creates: Int = 0, val updates: Int = 0, val deletes: Int = 0)
data class LogsResponse(
    val data: List<LogRow> = emptyList(),
    val meta: JsonObject? = null,
    @SerializedName("log_names") val logNames: List<String> = emptyList(),
    val events: List<String> = emptyList(),
    val stats: LogStats = LogStats()
)

// ── Usuarios del sistema (admin-only) ────────────────────────────────────
data class SystemUserRow(
    val id: String,
    val name: String,
    val email: String,
    @SerializedName("avatar_url") val avatarUrl: String? = null,
    @SerializedName("email_verified_at") val emailVerifiedAt: String? = null,
    @SerializedName("created_at") val createdAt: String? = null,
    val roles: List<String> = emptyList()
)
data class UsersMeta(
    @SerializedName("current_page") val currentPage: Int = 1,
    @SerializedName("last_page") val lastPage: Int = 1,
    val total: Int = 0
)
data class SystemUsersResponse(
    val data: List<SystemUserRow> = emptyList(),
    val meta: UsersMeta? = null,
    val roles: List<String> = emptyList()
)
data class SystemUserMutationResponse(val message: String? = null)
data class CreateUserRequest(val name: String, val email: String, val password: String, @SerializedName("password_confirmation") val passwordConfirmation: String, val role: String)
data class UpdateUserRequest(val name: String, val email: String, val password: String?, @SerializedName("password_confirmation") val passwordConfirmation: String?, val role: String)

// ── Analítica (analytics) ────────────────────────────────────────────────
data class AnalyticsGraph(val tipo: String? = null, val labels: List<String> = emptyList(), val valores: List<JsonElement> = emptyList())
data class AnalyticsKpi(
    val label: String? = null,
    val value: String? = null,
    val detail: String? = null,
    val tone: String? = null,
    val type: String? = null,
    val graph: AnalyticsGraph? = null,
    val message: String? = null
)
data class AnalyticsInsightItem(
    val tipo: String? = null,
    val titulo: String? = null,
    val mensaje: String? = null,
    @SerializedName("valor_destacado") val valorDestacado: String? = null,
    val color: String? = null,
    val grafica: AnalyticsGraph? = null,
    @SerializedName("visual_type") val visualType: String? = null,
    @SerializedName("has_renderable_visual") val hasRenderableVisual: Boolean = false,
    @SerializedName("generado_en") val generadoEn: String? = null
)
data class AnalyticsSection(
    val titulo: String? = null,
    val subtitulo: String? = null,
    val intro: String? = null,
    val acento: String? = null,
    val insights: List<AnalyticsInsightItem> = emptyList()
)
data class AnalyticsSecciones(
    val resumen: AnalyticsSection = AnalyticsSection(),
    val operacion: AnalyticsSection = AnalyticsSection(),
    val clientes: AnalyticsSection = AnalyticsSection(),
    val prediccion: AnalyticsSection = AnalyticsSection()
)
data class OperationalKpi(val label: String? = null, val value: Double = 0.0, val detail: String? = null)
data class OperationalAction(val label: String? = null, val detail: String? = null)
data class OperationalSummary(val kpis: List<OperationalKpi> = emptyList(), val actions: List<OperationalAction> = emptyList())
data class SparkFlowStep(
    val titulo: String? = null,
    val descripcion: String? = null,
    val color: String? = null,
    val count: Int = 0,
    val total: Int = 1,
    val progress: Double = 0.0
)
data class AnalyticsResponse(
    @SerializedName("rol_label") val rolLabel: String? = null,
    val operational: OperationalSummary? = null,
    val kpis: List<AnalyticsKpi> = emptyList(),
    @SerializedName("ultima_actualizacion") val ultimaActualizacion: String? = null,
    val secciones: AnalyticsSecciones = AnalyticsSecciones(),
    @SerializedName("diagnostico_insights") val diagnosticoInsights: List<AnalyticsInsightItem> = emptyList(),
    @SerializedName("spark_flow") val sparkFlow: List<SparkFlowStep> = emptyList()
)

// ── Reportes (reports, admin + ingeniero solo lectura) ───────────────────
data class ReportManifest(val types: List<String> = emptyList(), val formats: List<String> = emptyList())
/** Gráfica que el servidor calcula para cada reporte (misma que lleva el PDF). */
data class ReportChart(
    val title: String? = null,
    val labels: List<String> = emptyList(),
    val values: List<Double> = emptyList(),
    val unit: String? = null
)
data class ReportData(
    val title: String? = null,
    val headings: List<String> = emptyList(),
    val keys: List<String> = emptyList(),
    val rows: List<Map<String, JsonElement>> = emptyList(),
    val chart: ReportChart? = null
)

// ── Configuración de la barbería (settings, admin-only) ──────────────────
data class RedesSociales(val instagram: String? = null, val facebook: String? = null, val tiktok: String? = null)
data class DatosBancarios(val clabe: String? = null, val banco: String? = null, val beneficiario: String? = null, val concepto: String? = null)
data class BarbershopSetting(
    val id: String? = null,
    val nombre: String = "",
    val direccion: String? = null,
    val telefono: String? = null,
    @SerializedName("horario_apertura") val horarioApertura: String? = null,
    @SerializedName("horario_cierre") val horarioCierre: String? = null,
    @SerializedName("politica_cancelacion") val politicaCancelacion: Int = 24,
    @SerializedName("deposito_no_show_umbral") val depositoNoShowUmbral: Int = 2,
    @SerializedName("deposito_no_show_porcentaje") val depositoNoShowPorcentaje: Int = 50,
    @SerializedName("maintenance_mode") val maintenanceMode: Boolean = false,
    @SerializedName("redes_sociales") val redesSociales: RedesSociales = RedesSociales(),
    @SerializedName("datos_bancarios") val datosBancarios: DatosBancarios = DatosBancarios()
)
data class BarbershopSettingResponse(val data: BarbershopSetting = BarbershopSetting())
data class UpdateSettingRequest(
    val nombre: String,
    val direccion: String?,
    val telefono: String?,
    @SerializedName("horario_apertura") val horarioApertura: String?,
    @SerializedName("horario_cierre") val horarioCierre: String?,
    @SerializedName("politica_cancelacion") val politicaCancelacion: Int,
    @SerializedName("deposito_no_show_umbral") val depositoNoShowUmbral: Int?,
    @SerializedName("deposito_no_show_porcentaje") val depositoNoShowPorcentaje: Int?,
    val instagram: String?,
    val facebook: String?,
    val tiktok: String?,
    val clabe: String?,
    val banco: String?,
    val beneficiario: String?,
    val concepto: String?
)
data class MaintenanceToggleResponse(val message: String? = null, val data: MaintenanceStatus = MaintenanceStatus())
data class MaintenanceStatus(@SerializedName("maintenance_mode") val maintenanceMode: Boolean = false)

// ── Agenda de barbero (barber/agenda) ────────────────────────────────────
data class AgendaRange(val start: String? = null, val end: String? = null, val label: String? = null)
data class AgendaStats(
    @SerializedName("completed_count") val completedCount: Int = 0,
    @SerializedName("income_total") val incomeTotal: Double = 0.0,
    val productivity: Int = 0,
    @SerializedName("total_period") val totalPeriod: Int = 0,
    @SerializedName("pending_period") val pendingPeriod: Int = 0,
    @SerializedName("confirmed_period") val confirmedPeriod: Int = 0,
    @SerializedName("in_process_period") val inProcessPeriod: Int = 0,
    @SerializedName("completed_period") val completedPeriod: Int = 0,
    @SerializedName("cancelled_period") val cancelledPeriod: Int = 0,
    @SerializedName("no_show_period") val noShowPeriod: Int = 0
)
data class BarberAgendaResponse(
    val data: List<AppointmentRow> = emptyList(),
    val period: String = "day",
    val estado: String? = null,
    val offset: Int = 0,
    val range: AgendaRange = AgendaRange(),
    val stats: AgendaStats = AgendaStats()
)

// ── Campañas (campaigns, admin-only) ─────────────────────────────────────
data class CampaignRow(
    val id: String,
    val titulo: String? = null,
    val cuerpo: String? = null,
    @SerializedName("cta_label") val ctaLabel: String? = null,
    @SerializedName("cta_url") val ctaUrl: String? = null,
    val segmento: String? = null,
    val destinatarios: Int = 0,
    val estado: String? = null,
    @SerializedName("programada_para") val programadaPara: String? = null,
    @SerializedName("enviada_en") val enviadaEn: String? = null,
    @SerializedName("created_at") val createdAt: String? = null,
    val opens: Int = 0,
    val clicks: Int = 0,
    @SerializedName("open_rate") val openRate: Double = 0.0,
    @SerializedName("click_rate") val clickRate: Double = 0.0
)
data class CampaignsResponse(
    val levels: Map<String, String> = emptyMap(),
    @SerializedName("segment_counts") val segmentCounts: Map<String, Int> = emptyMap(),
    val data: List<CampaignRow> = emptyList()
)
data class CreateCampaignRequest(
    val titulo: String,
    val cuerpo: String,
    @SerializedName("cta_label") val ctaLabel: String?,
    @SerializedName("cta_url") val ctaUrl: String?,
    val segmento: String,
    val modo: String,
    @SerializedName("programada_para") val programadaPara: String?
)

// ── Portafolio de barbero (barber/portfolio, barber/works) ───────────────
data class WorkMediaItem(val id: String? = null, val url: String? = null, val type: String? = null)
data class WorkRow(
    val id: String,
    val title: String? = null,
    val description: String? = null,
    val media: List<WorkMediaItem> = emptyList(),
    @SerializedName("reactions_count") val reactionsCount: Int = 0,
    @SerializedName("comments_count") val commentsCount: Int = 0,
    @SerializedName("created_at") val createdAt: String? = null
)
data class PortfolioStats(
    @SerializedName("total_works") val totalWorks: Int = 0,
    @SerializedName("total_reactions") val totalReactions: Int = 0,
    @SerializedName("total_comments") val totalComments: Int = 0,
    @SerializedName("total_saves") val totalSaves: Int = 0
)
data class PortfolioResponse(val works: List<WorkRow> = emptyList(), val stats: PortfolioStats = PortfolioStats())
data class CreateWorkResponse(val message: String? = null, val work: WorkRow? = null)

// ── Insights IA (admin/predictions/insights) ─────────────────────────────
// generateInsights() en barber es cómputo local sobre datos ya cacheados
// (PredictionService::historicalData) -- a diferencia de sus endpoints
// hermanos (income/appointments/services), NO llama a Ollama, así que
// responde rápido pese al set_time_limit(120) defensivo del controlador.
data class RevenueInsight(val status: String? = null, val message: String? = null, @SerializedName("avg_daily") val avgDaily: Double? = null)
data class AppointmentsInsight(
    val status: String? = null,
    val message: String? = null,
    @SerializedName("avg_daily") val avgDaily: Double? = null,
    @SerializedName("trend_7d") val trend7d: Double? = null
)
data class ServiceConcentrationInsight(
    val status: String? = null,
    val message: String? = null,
    @SerializedName("top_service") val topService: String? = null,
    val percentage: Double? = null
)
data class InsightsData(
    val revenue: RevenueInsight? = null,
    val appointments: AppointmentsInsight? = null,
    @SerializedName("service_concentration") val serviceConcentration: ServiceConcentrationInsight? = null
)
data class InsightsResponse(val type: String? = null, val data: InsightsData = InsightsData())

// ── Muro social (social/feed) ────────────────────────────────────────────
data class SocialBarber(val id: String? = null, val name: String? = null, val slug: String? = null, val foto: String? = null)
data class SocialMedia(val url: String? = null, val type: String? = null)
data class SocialCommentUser(val name: String? = null)
data class SocialComment(
    val id: String? = null,
    val comment: String? = null,
    val user: SocialCommentUser? = null,
    @SerializedName("is_mine") val isMine: Boolean = false,
    @SerializedName("created_at") val createdAt: String? = null
)
data class SocialWork(
    val id: String,
    val title: String? = null,
    val description: String? = null,
    @SerializedName("work_date") val workDate: String? = null,
    val barber: SocialBarber? = null,
    val media: List<SocialMedia> = emptyList(),
    @SerializedName("reactions_count") val reactionsCount: Int = 0,
    @SerializedName("comments_count") val commentsCount: Int = 0,
    @SerializedName("saved_count") val savedCount: Int = 0,
    @SerializedName("is_reacted") val isReacted: Boolean = false,
    @SerializedName("is_saved") val isSaved: Boolean = false,
    val comments: List<SocialComment> = emptyList()
)
data class SocialFeedResponse(val data: List<SocialWork> = emptyList(), val meta: JsonObject? = null)
data class SocialActionResponse(val status: String? = null, val count: Int? = null)
data class PostCommentRequest(val comment: String)
data class ReportCommentRequest(val reason: String, val details: String? = null)
data class SocialModerationResponse(val message: String? = null)
data class PostCommentResponse(val message: String? = null, val data: SocialComment? = null)

// ── Sorteos ──────────────────────────────────────────────────────────────
data class RaffleClientUser(val name: String? = null)
data class RaffleClient(val id: String? = null, val user: RaffleClientUser? = null)
data class RaffleRow(
    val id: String,
    val mes: String? = null,
    val premio: String? = null,
    @SerializedName("nivel_ganador") val nivelGanador: String? = null,
    @SerializedName("vence_en") val venceEn: String? = null,
    @SerializedName("reclamado_en") val reclamadoEn: String? = null,
    val client: RaffleClient? = null,
    @SerializedName("is_claimed") val isClaimed: Boolean = false,
    @SerializedName("is_expired") val isExpired: Boolean = false
)
data class RaffleStats(val total: Int = 0, val reclamados: Int = 0, val vigentes: Int = 0)
data class RaffleResponse(
    val data: List<RaffleRow> = emptyList(),
    val meta: JsonObject? = null,
    val stats: RaffleStats = RaffleStats()
)

// ── Lista de espera (waitlist) ───────────────────────────────────────────
data class WaitlistRef(val id: String? = null, val name: String? = null)
data class WaitlistServiceRef(val id: String? = null, val nombre: String? = null)
data class WaitlistEntry(
    val id: String,
    val estado: String,
    val fecha: String? = null,
    @SerializedName("notificado_en") val notificadoEn: String? = null,
    val client: WaitlistRef? = null,
    val barber: WaitlistRef? = null,
    val service: WaitlistServiceRef? = null
)
data class WaitlistResponse(val data: List<WaitlistEntry> = emptyList())

// ── Historial de movimientos de inventario (GET /inventory/movements, personal) ──
data class MovementProduct(val id: String? = null, val nombre: String? = null)
data class MovementUser(val id: String? = null, val name: String? = null)
data class MovementAppointment(val id: String? = null, val fecha: String? = null, val client: String? = null)
data class InventoryMovementRow(
    val id: String,
    val tipo: String = "",
    val cantidad: Int = 0,
    val motivo: String? = null,
    val fecha: String? = null,
    val product: MovementProduct? = null,
    val user: MovementUser? = null,
    val appointment: MovementAppointment? = null
)
data class InventoryMovementStats(val total: Int = 0, val entradas: Int = 0, val salidas: Int = 0, val hoy: Int = 0)
data class InventoryMovementsMeta(
    @SerializedName("current_page") val currentPage: Int = 1,
    @SerializedName("last_page") val lastPage: Int = 1,
    val total: Int = 0,
    val stats: InventoryMovementStats = InventoryMovementStats()
)
data class InventoryMovementsResponse(
    val data: List<InventoryMovementRow> = emptyList(),
    val meta: InventoryMovementsMeta = InventoryMovementsMeta()
)
data class WaitlistJoinResponse(val message: String? = null, val data: WaitlistEntry? = null)
data class WaitlistRequest(
    @SerializedName("barber_id") val barberId: String,
    @SerializedName("service_id") val serviceId: String,
    val fecha: String
)

// ── Wallet: membresías (memberships/*, autoservicio del cliente) ─────────
data class MembershipPlan(
    val id: String,
    val nombre: String,
    val descripcion: String? = null,
    @SerializedName("precio_mensual") val precioMensual: Double = 0.0,
    @SerializedName("descuento_pct") val descuentoPct: Double = 0.0
)
data class MembershipPlansResponse(val data: List<MembershipPlan> = emptyList())
data class MyMembership(
    val id: String,
    val estado: String,
    @SerializedName("cancelar_al_finalizar") val cancelarAlFinalizar: Boolean = false,
    @SerializedName("periodo_actual_fin") val periodoActualFin: String? = null,
    val plan: MembershipPlan? = null
)
data class MyMembershipResponse(val data: MyMembership? = null)
/** GET ai/briefing: resumen del día para recepción y administración; source = "ia" | "reglas". */
data class AiBriefing(val text: String = "", val source: String = "reglas", @SerializedName("generated_at") val generatedAt: String? = null)
data class AiBriefingResponse(val data: AiBriefing? = null)
/** Servicio que Bladebot recomendó (campo suggested_service de chatbot/query), para reservarlo directo. */
data class SuggestedService(val id: String, val nombre: String, val precio: Double = 0.0, @SerializedName("duracion_min") val duracionMin: Int = 0)
/** POST memberships/subscribe: el client_secret confirma el primer cobro con Stripe. */
data class SubscribeMembershipRequest(@SerializedName("membership_plan_id") val membershipPlanId: String)
data class SubscribeMembershipData(@SerializedName("client_secret") val clientSecret: String? = null, val membership: MyMembership? = null)
data class SubscribeMembershipResponse(val data: SubscribeMembershipData? = null)
/** GET memberships/invoices: cobros mensuales de la membresía, para Mis facturas. */
data class MembershipInvoiceRow(
    val id: String,
    val plan: String? = null,
    val monto: Double = 0.0,
    @SerializedName("pagado_en") val pagadoEn: String? = null
)
data class MembershipInvoicesMeta(@SerializedName("total_pagado") val totalPagado: Double = 0.0)
data class MembershipInvoicesResponse(val data: List<MembershipInvoiceRow> = emptyList(), val meta: MembershipInvoicesMeta? = null)
data class MembershipReceiptData(@SerializedName("receipt_url") val receiptUrl: String? = null)
data class MembershipReceiptResponse(val data: MembershipReceiptData? = null)

// ── Wallet: paquetes (packages, packages/catalog) ─────────────────────────
data class PackageServiceRef(val id: String? = null, val nombre: String? = null)
data class MyPackage(
    val id: String,
    val nombre: String? = null,
    val service: PackageServiceRef? = null,
    @SerializedName("usos_totales") val usosTotales: Int = 0,
    @SerializedName("usos_restantes") val usosRestantes: Int = 0,
    @SerializedName("precio_pagado") val precioPagado: Double = 0.0,
    @SerializedName("metodo_pago") val metodoPago: String? = null,
    @SerializedName("comprado_en") val compradoEn: String? = null,
    @SerializedName("expira_en") val expiraEn: String? = null,
    val estado: String? = null
)
data class MyPackagesResponse(val data: List<MyPackage> = emptyList())
data class PackageCatalogItem(
    val id: String,
    val nombre: String,
    val service: PackageServiceRef? = null,
    @SerializedName("cantidad_usos") val cantidadUsos: Int = 0,
    val precio: Double = 0.0,
    @SerializedName("vigencia_dias") val vigenciaDias: Int = 0
)
data class PackageCatalogResponse(val data: List<PackageCatalogItem> = emptyList())

// ── Wallet: gift cards (gift-cards/mine) ──────────────────────────────────
data class GiftCard(
    val code: String,
    val saldo: Double = 0.0,
    @SerializedName("monto_inicial") val montoInicial: Double = 0.0,
    val estado: String? = null,
    @SerializedName("comprado_en") val compradoEn: String? = null
)
data class GiftCardsResponse(val data: List<GiftCard> = emptyList())

/** Venta de una gift card en efectivo por el personal (POST /gift-cards). */
data class CashGiftCardRequest(
    val monto: Double,
    @SerializedName("comprador_nombre") val compradorNombre: String? = null,
    @SerializedName("destinatario_email") val destinatarioEmail: String? = null
)
data class GiftCardSold(val code: String, val saldo: Double = 0.0)
data class GiftCardSaleResponse(val message: String? = null, val data: GiftCardSold? = null)
data class GiftCardLookupResponse(val data: GiftCard? = null)

// ── Wallet: referidos (referrals/mine) ────────────────────────────────────
data class ReferralEntry(
    val referido: String? = null,
    val estado: String? = null,
    @SerializedName("recompensa_otorgada_en") val recompensaOtorgadaEn: String? = null
)
data class ReferralInfo(
    @SerializedName("codigo_referido") val codigoReferido: String? = null,
    @SerializedName("puntos_por_referido") val puntosPorReferido: Int = 0,
    val referidos: List<ReferralEntry> = emptyList(),
    val completados: Int = 0
)
data class ReferralInfoResponse(val data: ReferralInfo = ReferralInfo())

// ── Wallet: lealtad (embebida en GET dashboard, rol cliente -- ver
// DashboardService::clientMetrics(); no existe endpoint propio) ───────────
data class LoyaltyTransaction(val descripcion: String? = null, val puntos: Int = 0)
data class ClientLoyalty(
    val nivel: String? = null,
    val nivelLabel: String? = null,
    val puntos: Int = 0,
    val discountPct: Double = 0.0,
    val nextNivel: String? = null,
    val nextNivelLabel: String? = null,
    val citasFaltan: Int = 0,
    val progressPct: Double = 0.0,
    val recentTransactions: List<LoyaltyTransaction> = emptyList(),
    // Aditivos (barber, DashboardController::clientPayload): null o vacíos hasta que el backend
    // desplegado los mande; la Wallet oculta lo que no llegue.
    val nextDiscountPct: Int? = null,
    val levels: List<LoyaltyLevel> = emptyList(),
    val earnRules: List<LoyaltyEarnRule> = emptyList(),
    val maxRedeemPct: Int? = null,
    val wonRaffle: WonRaffle? = null
)
data class LoyaltyLevel(val nivel: String, val label: String, val citas: Int = 0, val discountPct: Int = 0)
data class LoyaltyEarnRule(val descripcion: String, val puntos: Int = 0)
data class WonRaffle(val mes: String? = null, val premio: String? = null, val isExpired: Boolean = false, val venceEn: String? = null)
