package com.urbanblade.mobile.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.urbanblade.mobile.data.model.AuthUser
import com.urbanblade.mobile.data.model.CreateProductRequest
import com.urbanblade.mobile.data.model.InventoryProductRow
import com.urbanblade.mobile.ui.components.*
import com.urbanblade.mobile.ui.components.UrbanFormat
import com.urbanblade.mobile.ui.theme.UrbanColors
import com.urbanblade.mobile.ui.viewmodel.InventoryViewModel

private val TIPO_LABEL = mapOf("venta_cliente" to "Venta a cliente", "insumo_trabajo" to "Insumo de trabajo")

@Composable
fun InventoryListScreen(user: AuthUser, onBack: () -> Unit, onHistory: () -> Unit = {}, vm: InventoryViewModel = viewModel()) {
    val response by vm.products.collectAsState()
    val busy by vm.busy.collectAsState()
    val saving by vm.saving.collectAsState()
    val message by vm.message.collectAsState()
    val error by vm.error.collectAsState()
    val isAdmin = user.roles.contains("administrador")

    val context = LocalContext.current
    var categoryFilter by remember { mutableStateOf<String?>(null) }
    var showMovement by remember { mutableStateOf<InventoryProductRow?>(null) }
    var showCreate by remember { mutableStateOf(false) }
    var showEdit by remember { mutableStateOf<InventoryProductRow?>(null) }
    var showDeleteConfirm by remember { mutableStateOf<InventoryProductRow?>(null) }

    LaunchedEffect(Unit) { vm.load() }

    val visibleProducts = remember(response, categoryFilter) {
        response.data
            .filter { categoryFilter == null || it.categoria == categoryFilter }
            .sortedWith(
                compareByDescending<InventoryProductRow> { it.lowStock }
                    .thenByDescending { it.pendingRestock }
                    .thenBy { it.nombre.lowercase() }
            )
    }
    val attentionProducts = remember(visibleProducts) { visibleProducts.filter { it.lowStock || it.pendingRestock } }
    val regularProducts = remember(visibleProducts, attentionProducts) { visibleProducts.filterNot { it in attentionProducts } }

    Scaffold(
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
        topBar = {
            UrbanTopBar("", onBack) {
                if (isAdmin) {
                    IconButton(onClick = { showCreate = true }) { Icon(Icons.Default.Add, "Agregar producto") }
                }
                IconButton(onClick = onHistory) { Icon(Icons.Default.History, "Historial de movimientos") }
                IconButton(onClick = { vm.load() }) { Icon(Icons.Default.Refresh, "Actualizar") }
            }
        }
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                val subtitle = when {
                    response.meta.stats.bajoStock > 0 -> UrbanFormat.count(response.meta.stats.bajoStock, "producto requiere reposición", "productos requieren reposición")
                    else -> "Existencias de la tienda al día."
                }
                UrbanPageHeader(title = "Inventario", subtitle = subtitle, eyebrow = "OPERACIÓN")
            }
            if (busy) item { LinearProgressIndicator(Modifier.fillMaxWidth(), color = UrbanColors.Gold) }
            error?.let { item { UrbanErrorBanner(it) } }
            message?.let { item { UrbanInfoBanner(it, Icons.Default.CheckCircle) } }

            item {
                UrbanPremiumCard(Modifier.fillMaxWidth()) {
                    Text("RESUMEN DE EXISTENCIAS", style = MaterialTheme.typography.labelLarge, color = UrbanColors.Gold)
                    Spacer(Modifier.height(8.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        InventorySummaryValue("Productos", response.meta.stats.total.toString(), Modifier.weight(1f))
                        InventorySummaryValue("Por reponer", response.meta.stats.bajoStock.toString(), Modifier.weight(1f), response.meta.stats.bajoStock > 0)
                        InventorySummaryValue("Valor venta", "${UrbanFormat.moneyShort(response.meta.stats.valorTotal)}", Modifier.weight(1f))
                    }
                }
            }

            if (response.meta.categorias.isNotEmpty()) {
                item {
                    Row(
                        Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(selected = categoryFilter == null, onClick = { categoryFilter = null }, label = { Text("Todas") })
                        response.meta.categorias.forEach { cat ->
                            FilterChip(selected = categoryFilter == cat, onClick = { categoryFilter = if (categoryFilter == cat) null else cat }, label = { Text(cat) })
                        }
                    }
                }
            }

            if (visibleProducts.isEmpty() && !busy) {
                item { UrbanMascotState(UrbanStateKind.EMPTY, "Sin productos", "No hay productos en esta categoría.") }
            }

            if (attentionProducts.isNotEmpty()) {
                item {
                    UrbanSectionTitle(
                        "Requiere atención",
                        UrbanFormat.count(attentionProducts.size, "producto necesita revisión", "productos necesitan revisión")
                    )
                }
                items(attentionProducts, key = { it.id }) { product ->
                    InventoryProductCard(product, isAdmin, { showMovement = product }, { showEdit = product }, { showDeleteConfirm = product })
                }
            }

            if (regularProducts.isNotEmpty()) {
                item { UrbanSectionTitle("Existencias", UrbanFormat.count(regularProducts.size, "producto disponible", "productos disponibles")) }
                items(regularProducts, key = { it.id }) { product ->
                    InventoryProductCard(product, isAdmin, { showMovement = product }, { showEdit = product }, { showDeleteConfirm = product })
                }
            }
        }
    }

    showMovement?.let { product ->
        MovementDialog(
            product = product,
            allowEntrada = isAdmin,
            saving = saving,
            onDismiss = { showMovement = null },
            onConfirm = { tipo, cantidad, motivo ->
                vm.registerMovement(product.id, tipo, cantidad, motivo) { showMovement = null }
            }
        )
    }

    if (showCreate) {
        ProductFormDialog(
            title = "Nuevo producto",
            initial = null,
            saving = saving,
            onDismiss = { showCreate = false },
            onSubmit = { req, imageUri -> vm.createProduct(context, req, imageUri) { showCreate = false } }
        )
    }

    showEdit?.let { product ->
        ProductFormDialog(
            title = "Editar producto",
            initial = product,
            saving = saving,
            onDismiss = { showEdit = null },
            onSubmit = { req, imageUri -> vm.updateProduct(context, product.id, req, imageUri) { showEdit = null } }
        )
    }

    showDeleteConfirm?.let { product ->
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = null },
            title = { Text("¿Eliminar ${product.nombre}?") },
            text = { Text("Esta acción no se puede deshacer.") },
            confirmButton = {
                TextButton(onClick = { vm.deleteProduct(product.id); showDeleteConfirm = null }) { Text("Sí, eliminar") }
            },
            dismissButton = { TextButton(onClick = { showDeleteConfirm = null }) { Text("Cancelar") } }
        )
    }
}

@Composable
private fun InventorySummaryValue(label: String, value: String, modifier: Modifier = Modifier, alert: Boolean = false) {
    Column(modifier) {
        Text(label.uppercase(), style = MaterialTheme.typography.labelSmall, color = UrbanColors.Muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(value, style = MaterialTheme.typography.titleMedium, color = if (alert) UrbanColors.Warning else UrbanColors.Ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun InventoryProductCard(
    product: InventoryProductRow,
    isAdmin: Boolean,
    onMovement: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    UrbanCard(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
            product.imagenUrl?.let { url ->
                AsyncImage(model = url, contentDescription = null, modifier = Modifier.size(56.dp).clip(RoundedCornerShape(10.dp)))
                Spacer(Modifier.width(10.dp))
            }
            Column(Modifier.weight(1f)) {
                product.categoria?.let { Text(it.uppercase(), style = MaterialTheme.typography.labelMedium, color = UrbanColors.Gold) }
                Text(product.nombre, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    "${product.stockActual} disponibles · mínimo ${product.stockMinimo}",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (product.lowStock) UrbanColors.Danger else UrbanColors.Muted
                )
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("${UrbanFormat.money(product.precioVenta)}", style = MaterialTheme.typography.titleMedium, color = UrbanColors.Gold)
                when {
                    product.lowStock -> SimpleStatusPill("stock bajo", UrbanColors.Danger)
                    product.pendingRestock -> SimpleStatusPill("reposición pendiente", UrbanColors.Warning)
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        UrbanStockBar(product.stockActual, product.stockMinimo)
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            UrbanOutlineButton(text = "Registrar movimiento", onClick = onMovement, icon = Icons.Default.SwapVert, modifier = Modifier.weight(1f))
            if (isAdmin) {
                IconButton(onClick = onEdit) { Icon(Icons.Default.Edit, "Editar ${product.nombre}") }
                IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, "Eliminar ${product.nombre}", tint = UrbanColors.Danger) }
            }
        }
    }
}

@Composable
private fun MovementDialog(
    product: InventoryProductRow,
    allowEntrada: Boolean,
    saving: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (String, Int, String?) -> Unit
) {
    // La recepción solo puede registrar salidas (consumo) -- misma regla
    // que ya valida InventoryController::storeMovement() en barber, no se
    // inventa aquí, solo se refleja.
    var tipo by remember { mutableStateOf("salida") }
    var cantidad by remember { mutableStateOf("1") }
    var motivo by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Movimiento: ${product.nombre}") },
        text = {
            UrbanFormScroll(
                modifier = Modifier.heightIn(max = 420.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (allowEntrada) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected = tipo == "entrada", onClick = { tipo = "entrada" }, label = { Text("Entrada") })
                        FilterChip(selected = tipo == "salida", onClick = { tipo = "salida" }, label = { Text("Salida") })
                    }
                } else {
                    Text("Tipo: Salida (consumo)", style = MaterialTheme.typography.bodySmall, color = UrbanColors.Muted)
                }
                UrbanTextField(cantidad, { cantidad = it.filter(Char::isDigit) }, "Cantidad", Modifier.fillMaxWidth(), keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                UrbanTextField(motivo, { motivo = it }, "Motivo (opcional)", Modifier.fillMaxWidth(), capitalization = androidx.compose.ui.text.input.KeyboardCapitalization.Sentences, imeAction = androidx.compose.ui.text.input.ImeAction.Default, minLines = 2)
            }
        },
        confirmButton = {
            TextButton(
                enabled = !saving && (cantidad.toIntOrNull() ?: 0) > 0,
                onClick = { onConfirm(tipo, cantidad.toInt(), motivo.takeIf { it.isNotBlank() }) }
            ) { Text(if (saving) "Guardando…" else "Registrar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

@Composable
private fun ProductFormDialog(
    title: String,
    initial: InventoryProductRow?,
    saving: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (CreateProductRequest, android.net.Uri?) -> Unit
) {
    var nombre by remember { mutableStateOf(initial?.nombre.orEmpty()) }
    var categoria by remember { mutableStateOf(initial?.categoria.orEmpty()) }
    var descripcion by remember { mutableStateOf(initial?.descripcion.orEmpty()) }
    var precioCompra by remember { mutableStateOf(initial?.precioCompra?.toString().orEmpty()) }
    var precioVenta by remember { mutableStateOf(initial?.precioVenta?.toString().orEmpty()) }
    var stockActual by remember { mutableStateOf(initial?.stockActual?.toString().orEmpty()) }
    var stockMinimo by remember { mutableStateOf(initial?.stockMinimo?.toString().orEmpty()) }
    var tipo by remember { mutableStateOf(initial?.tipo ?: "venta_cliente") }
    var imageUri by remember { mutableStateOf<android.net.Uri?>(null) }
    val pickImage = rememberSingleImagePicker { imageUri = it }

    val valid = nombre.isNotBlank() && categoria.isNotBlank() &&
        precioCompra.toDoubleOrNull() != null && precioVenta.toDoubleOrNull() != null &&
        stockActual.toIntOrNull() != null && stockMinimo.toIntOrNull() != null

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            UrbanFormScroll(
                modifier = Modifier.heightIn(max = 480.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    val previewModel = imageUri ?: initial?.imagenUrl
                    if (previewModel != null) {
                        AsyncImage(model = previewModel, contentDescription = null, modifier = Modifier.size(56.dp).clip(RoundedCornerShape(10.dp)))
                    }
                    UrbanOutlineButton(text = if (previewModel != null) "Cambiar foto" else "Agregar foto", onClick = pickImage, icon = Icons.Default.Image)
                }
                UrbanTextField(nombre, { nombre = it }, "Nombre", Modifier.fillMaxWidth(), capitalization = androidx.compose.ui.text.input.KeyboardCapitalization.Words)
                UrbanTextField(categoria, { categoria = it }, "Categoría", Modifier.fillMaxWidth(), capitalization = androidx.compose.ui.text.input.KeyboardCapitalization.Words)
                UrbanTextField(descripcion, { descripcion = it }, "Descripción (opcional)", Modifier.fillMaxWidth(), capitalization = androidx.compose.ui.text.input.KeyboardCapitalization.Sentences, imeAction = androidx.compose.ui.text.input.ImeAction.Default, minLines = 2)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    UrbanTextField(precioCompra, { precioCompra = it }, "Precio compra", Modifier.weight(1f), keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal)
                    UrbanTextField(precioVenta, { precioVenta = it }, "Precio venta", Modifier.weight(1f), keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    UrbanTextField(stockActual, { stockActual = it.filter(Char::isDigit) }, "Stock actual", Modifier.weight(1f), keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                    UrbanTextField(stockMinimo, { stockMinimo = it.filter(Char::isDigit) }, "Stock mínimo", Modifier.weight(1f), keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TIPO_LABEL.forEach { (key, label) ->
                        FilterChip(selected = tipo == key, onClick = { tipo = key }, label = { Text(label) })
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = !saving && valid,
                onClick = {
                    onSubmit(
                        CreateProductRequest(
                            nombre = nombre,
                            categoria = categoria,
                            descripcion = descripcion.takeIf { it.isNotBlank() },
                            precioCompra = precioCompra.toDouble(),
                            precioVenta = precioVenta.toDouble(),
                            stockActual = stockActual.toInt(),
                            stockMinimo = stockMinimo.toInt(),
                            tipo = tipo
                        ),
                        imageUri
                    )
                }
            ) { Text(if (saving) "Guardando…" else "Guardar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}
