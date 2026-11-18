package com.loypos.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.withTransaction
import com.loypos.app.core.DiscountInput
import com.loypos.app.core.LowStockWorkScheduler
import com.loypos.app.core.SaleTotals
import com.loypos.app.core.computeTotals
import com.loypos.app.core.formatQty
import com.loypos.app.core.newId
import com.loypos.app.core.nowMs
import com.loypos.app.core.pointsForTotal
import com.loypos.app.data.LoyPosRepository
import com.loypos.app.data.local.CartLine
import com.loypos.app.data.local.CustomerEntity
import com.loypos.app.data.local.LoyPosDatabase
import com.loypos.app.data.local.ProductEntity
import com.loypos.app.data.local.SaleEntity
import com.loypos.app.data.local.SaleItemEntity
import com.loypos.app.data.local.SaleWithItems
import com.loypos.app.data.local.SettingEntity
import com.loypos.app.data.local.SettingsModel
import com.loypos.app.data.local.StockMovementEntity
import com.loypos.app.data.local.TicketWithLines
import com.loypos.app.data.local.UserEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CartSessionState(
    val items: List<CartLine> = emptyList(),
    val discount: DiscountInput? = null,
    val customerId: String? = null,
    val note: String = "",
    val openTicketId: String? = null,
    val rentalDays: Int = 1,
    val rentalStartMs: Long = nowMs(),
    val rentalEndMs: Long = rentalStartMs + 86_400_000L,
    val depositCents: Long = 0L,
    val eventName: String = "",
    val eventVenue: String = "",
)

class TransactionViewModel(application: Application) : AndroidViewModel(application) {
    private val db = LoyPosDatabase.getInstance(application.applicationContext)
    private val repo = LoyPosRepository(application.applicationContext)
    private val saleDao = db.saleDao()
    private val saleItemDao = db.saleItemDao()
    private val productDao = db.productDao()
    private val coreDao = db.dao()

    private val _cartSession = MutableStateFlow(CartSessionState())
    val cartSession: StateFlow<CartSessionState> = _cartSession.asStateFlow()

    val cartItems: StateFlow<List<CartLine>> = _cartSession
        .map { it.items }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList(),
        )

    val cartTotals: StateFlow<SaleTotals> = combine(
        _cartSession,
        repo.settingsFlow,
    ) { session, settings ->
        computeTotals(
            lineTotalsCents = session.items.map { it.lineTotalCents },
            discount = session.discount,
            taxRatePercent = settings.taxRatePercent,
            taxIncluded = settings.taxIncluded,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SaleTotals(0L, 0L, 0L, 0L),
    )

    val recentSales: StateFlow<List<SaleWithItems>> = saleDao.observeSalesWithItems()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList(),
        )

    private val _lastCompletedSale = MutableStateFlow<SaleWithItems?>(null)
    val lastCompletedSale: StateFlow<SaleWithItems?> = _lastCompletedSale.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val transactionMessage: StateFlow<String?> = _message.asStateFlow()

    fun consumeMessage() {
        _message.value = null
    }

    fun addProductToCart(product: ProductEntity, qty: Double = 1.0) {
        if (product.trackStock && product.stock <= 0.0) {
            _message.value = "${product.name} is completely rented out"
            return
        }
        val currentQty = _cartSession.value.items.firstOrNull { it.itemId == product.id }?.qty ?: 0.0
        if (product.trackStock && currentQty + qty > product.stock) {
            _message.value = "Only ${formatQty(product.stock)} ${product.name} available to rent"
            return
        }
        _cartSession.update { state ->
            val days = state.rentalDays.coerceAtLeast(1)
            val updated = state.items.toMutableList()
            val idx = updated.indexOfFirst { it.itemId == product.id }
            if (idx >= 0) {
                val existing = updated[idx]
                updated[idx] = existing.copy(qty = existing.qty + qty, rentalDays = days)
            } else {
                updated.add(CartLine(product.id, product.name, product.priceCents, qty, days))
            }
            state.copy(items = updated)
        }
    }

    fun updateCartItemQty(productId: String, newQty: Double) {
        _cartSession.update { state ->
            val days = state.rentalDays.coerceAtLeast(1)
            val updated = if (newQty <= 0.0) {
                state.items.filterNot { it.itemId == productId }
            } else {
                state.items.map {
                    if (it.itemId == productId) it.copy(qty = newQty, rentalDays = days) else it
                }
            }
            state.copy(items = updated)
        }
    }

    fun removeFromCart(productId: String) {
        _cartSession.update { state ->
            state.copy(items = state.items.filterNot { it.itemId == productId })
        }
    }

    fun setRentalPeriod(startMs: Long, days: Int) {
        val clamped = days.coerceIn(1, 365)
        val safeStart = if (startMs > 0L) startMs else nowMs()
        val end = safeStart + clamped * 86_400_000L
        _cartSession.update { state ->
            state.copy(
                rentalDays = clamped,
                rentalStartMs = safeStart,
                rentalEndMs = end,
                items = state.items.map { it.copy(rentalDays = clamped) },
            )
        }
    }

    fun setRentalDays(days: Int) {
        val currentStart = _cartSession.value.rentalStartMs
        setRentalPeriod(if (currentStart > 0L) currentStart else nowMs(), days)
    }

    fun setEventDetails(eventName: String, eventVenue: String) {
        _cartSession.update {
            it.copy(
                eventName = eventName.trim(),
                eventVenue = eventVenue.trim(),
            )
        }
    }

    fun setDepositCents(depositCents: Long) {
        _cartSession.update { it.copy(depositCents = depositCents.coerceAtLeast(0L)) }
    }

    fun setDiscount(discount: DiscountInput?) {
        _cartSession.update { it.copy(discount = discount) }
    }

    fun setCustomer(customerId: String?) {
        _cartSession.update { it.copy(customerId = customerId) }
    }

    fun setCartNote(note: String) {
        _cartSession.update { it.copy(note = note) }
    }

    fun clearCart() {
        val start = nowMs()
        _cartSession.value = CartSessionState(
            rentalStartMs = start,
            rentalEndMs = start + 86_400_000L,
        )
    }

    fun loadTicket(ticketWithLines: TicketWithLines) {
        val t = ticketWithLines.ticket
        val days = t.rentalDays.coerceAtLeast(1)
        val start = if (t.rentalStartMs > 0L) t.rentalStartMs else nowMs()
        val end = if (t.rentalEndMs > start) t.rentalEndMs else start + days * 86_400_000L
        _cartSession.value = CartSessionState(
            items = ticketWithLines.toCartLines(),
            discount = ticketWithLines.discount,
            customerId = t.customerId,
            note = t.note,
            openTicketId = t.id,
            rentalDays = days,
            rentalStartMs = start,
            rentalEndMs = end,
            depositCents = t.depositCents,
            eventName = t.eventName,
            eventVenue = t.eventVenue,
        )
    }

    fun saveCurrentTicket(name: String, customer: CustomerEntity?, branchId: String?) {
        val session = _cartSession.value
        if (session.items.isEmpty()) return
        viewModelScope.launch {
            repo.saveTicket(
                ticketId = session.openTicketId,
                name = name.trim().ifEmpty { session.eventName.ifEmpty { "Event Quote" } },
                lines = session.items,
                discount = session.discount,
                customer = customer,
                note = session.note,
                branchId = branchId,
                rentalDays = session.rentalDays,
                rentalStartMs = session.rentalStartMs,
                rentalEndMs = session.rentalEndMs,
                depositCents = session.depositCents,
                eventName = session.eventName,
                eventVenue = session.eventVenue,
            )
            clearCart()
            _message.value = "Rental quote saved"
        }
    }

    fun deleteTicket(ticketId: String) {
        viewModelScope.launch {
            repo.deleteTicket(ticketId)
            if (_cartSession.value.openTicketId == ticketId) {
                clearCart()
            }
        }
    }

    fun finalizeSale(
        paymentType: String,
        paidCents: Long,
        settings: SettingsModel,
        customer: CustomerEntity?,
        branchId: String?,
        cashier: UserEntity?,
        onCompleted: (SaleWithItems) -> Unit = {},
    ) {
        val session = _cartSession.value
        if (session.items.isEmpty()) return

        val days = session.rentalDays.coerceAtLeast(1)
        val normalizedLines = session.items.map { it.copy(rentalDays = days) }
        val totals = computeTotals(
            lineTotalsCents = normalizedLines.map { it.lineTotalCents },
            discount = session.discount,
            taxRatePercent = settings.taxRatePercent,
            taxIncluded = settings.taxIncluded,
        )
        val changeCents = (paidCents - totals.totalCents).coerceAtLeast(0L)
        val now = nowMs()
        val startMs = if (session.rentalStartMs > 0L) session.rentalStartMs else now
        val endMs = startMs + days * 86_400_000L
        val saleId = newId()

        viewModelScope.launch {
            val completedSale = db.withTransaction {
                val number = (coreDao.getSetting("next_receipt_number")?.toIntOrNull()
                    ?: settings.nextReceiptNumber)
                    .coerceAtLeast(1)
                coreDao.upsertSetting(SettingEntity("next_receipt_number", (number + 1).toString()))

                val sale = SaleEntity(
                    id = saleId,
                    number = number,
                    createdAt = now,
                    subtotalCents = totals.subtotalCents,
                    discountCents = totals.discountCents,
                    discountType = session.discount?.type,
                    discountValue = session.discount?.value,
                    taxCents = totals.taxCents,
                    totalCents = totals.totalCents,
                    paidCents = paidCents,
                    changeCents = changeCents,
                    paymentType = paymentType,
                    customerId = customer?.id,
                    customerName = customer?.name,
                    status = "completed",
                    note = session.note,
                    branchId = branchId,
                    cashierId = cashier?.id,
                    cashierName = cashier?.ownerName?.ifEmpty { cashier.email },
                    updatedAt = now,
                    rentalDays = days,
                    rentalStartMs = startMs,
                    rentalEndMs = endMs,
                    depositCents = session.depositCents.coerceAtLeast(0L),
                    rentalStatus = "out",
                    returnedAtMs = null,
                    eventName = session.eventName,
                    eventVenue = session.eventVenue,
                )

                val saleItems = normalizedLines.map { line ->
                    SaleItemEntity(
                        id = newId(),
                        receiptId = saleId,
                        itemId = line.itemId,
                        name = line.name,
                        priceCents = line.priceCents,
                        qty = line.qty,
                        lineTotalCents = line.lineTotalCents,
                    )
                }

                saleDao.insertSale(sale)
                saleItemDao.insertSaleItems(saleItems)

                for (line in normalizedLines) {
                    productDao.adjustProductStock(line.itemId, -line.qty, now)
                    coreDao.insertStockMovement(
                        StockMovementEntity(
                            id = newId(),
                            itemId = line.itemId,
                            qty = -line.qty,
                            type = "rental",
                            createdAt = now,
                            note = "Rental #${number.toString().padStart(4, '0')} (${days}d)",
                            receiptId = saleId,
                        )
                    )
                }

                if (customer != null) {
                    val pts = pointsForTotal(totals.totalCents)
                    if (pts > 0) coreDao.addCustomerPoints(customer.id, pts, now)
                }

                if (!session.openTicketId.isNullOrEmpty()) {
                    coreDao.deleteTicketLines(session.openTicketId)
                    coreDao.deleteTicketById(session.openTicketId)
                }

                SaleWithItems(sale, saleItems)
            }

            _lastCompletedSale.value = completedSale
            clearCart()
            if (settings.lowStockAlertsEnabled) {
                LowStockWorkScheduler.runImmediateCheck(getApplication())
            }
            onCompleted(completedSale)
        }
    }

    fun markRentalReturned(saleId: String) {
        viewModelScope.launch {
            val ok = repo.markRentalReturned(saleId)
            _message.value = if (ok) {
                "Equipment marked returned and stock restored"
            } else {
                "Rental was already returned or refunded"
            }
        }
    }

    fun refundSale(saleId: String) {
        viewModelScope.launch {
            repo.refundReceipt(saleId)
            _message.value = "Rental refunded and equipment availability restored"
        }
    }
}
