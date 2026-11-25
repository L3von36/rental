package com.loypos.app.data

import android.content.Context
import androidx.room.withTransaction
import com.loypos.app.core.AuthCrypto
import com.loypos.app.core.CsvImportCommitResult
import com.loypos.app.core.CsvImportPreview
import com.loypos.app.core.DemoCatalog
import com.loypos.app.core.DiscountInput
import com.loypos.app.core.InventoryCsvExportResult
import com.loypos.app.core.InventoryCsvManager
import com.loypos.app.core.ParsedCsvProductRow
import com.loypos.app.core.SalesCsvExporter
import com.loypos.app.core.SalesCsvReportResult
import com.loypos.app.core.SalesReportPeriod
import com.loypos.app.core.computeTotals
import com.loypos.app.core.newId
import com.loypos.app.core.nowMs
import com.loypos.app.core.pointsForTotal
import com.loypos.app.core.startOfTodayMs
import com.loypos.app.core.ParsedQrCode
import com.loypos.app.core.QrCodeUtils
import com.loypos.app.core.RentalAgreementEngine
import com.loypos.app.data.local.AgreementTemplateEntity
import com.loypos.app.data.local.BackOfficeUserEntity
import com.loypos.app.data.local.BranchEntity
import com.loypos.app.data.local.CartLine
import com.loypos.app.data.local.CashMovementEntity
import com.loypos.app.data.local.CategoryEntity
import com.loypos.app.data.local.CustomerEntity
import com.loypos.app.data.local.DailyRevenuePoint
import com.loypos.app.core.ItemPhotos
import com.loypos.app.data.local.DamageReportEntity
import com.loypos.app.data.local.DeliveryStopEntity
import com.loypos.app.data.local.MaintenanceScheduleEntity
import com.loypos.app.data.local.DbStats
import com.loypos.app.data.local.EquipmentScanLogEntity
import com.loypos.app.data.local.EquipmentUnitEntity
import com.loypos.app.data.local.EquipmentUtilizationPoint
import com.loypos.app.data.local.EventKitEntity
import com.loypos.app.data.local.EventKitItemEntity
import com.loypos.app.data.local.EventKitWithItems
import com.loypos.app.data.local.ItemEntity
import com.loypos.app.data.local.LoyPosDatabase
import com.loypos.app.data.local.MonthlyIncomePoint
import com.loypos.app.data.local.ProductEntity
import com.loypos.app.data.local.ReceiptEntity
import com.loypos.app.data.local.ReceiptLineEntity
import com.loypos.app.data.local.ReceiptWithLines
import com.loypos.app.data.local.SaleEntity
import com.loypos.app.data.local.SettingEntity
import com.loypos.app.data.local.SettingsModel
import com.loypos.app.data.local.ShiftEntity
import com.loypos.app.data.local.ShiftStats
import com.loypos.app.data.local.StockMovementEntity
import com.loypos.app.data.local.TicketEntity
import com.loypos.app.data.local.TicketLineEntity
import com.loypos.app.data.local.TicketWithLines
import com.loypos.app.data.local.UserEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Calendar
import java.util.Locale

data class PleskSyncCounts(
    val pushedRecords: Int = 0,
    val pulledRecords: Int = 0,
)

class LoyPosRepository(private val context: Context) {
    private val db = LoyPosDatabase.getInstance(context)
    val dao = db.dao()
    val productDao = db.productDao()
    val categoryDao = db.categoryDao()
    val saleDao = db.saleDao()
    val saleItemDao = db.saleItemDao()
    val backOfficeUserDao = db.backOfficeUserDao()
    private val seedMutex = Mutex()

    suspend fun ensureInitialized() = seedMutex.withLock {
        val existingSettings = dao.getAllSettings()
        if (existingSettings.none { it.key == "store_name" }) {
            LoyPosDatabase.seedDefaults(db)
        } else {
            LoyPosDatabase.seedDefaultUnitsAndTemplates(db)
        }
    }

    val settingsFlow: Flow<SettingsModel> = dao.observeSettings().map { list ->
        val map = list.associate { it.key to it.value }
        SettingsModel(
            storeName = map["store_name"] ?: "EventGear Rentals",
            currencySymbol = map["currency_symbol"] ?: "Br",
            taxRatePercent = map["tax_rate"]?.toDoubleOrNull() ?: 0.0,
            taxIncluded = map["tax_included"] == "true",
            receiptFooter = map["receipt_footer"] ?: "Thank you for renting with us! Inspect equipment on return.",
            nextReceiptNumber = map["next_receipt_number"]?.toIntOrNull() ?: 1,
            lowStockAlertsEnabled = map["low_stock_alerts_enabled"] == "true",
            lowStockThreshold = (map["low_stock_threshold"]?.toDoubleOrNull() ?: 3.0).coerceAtLeast(0.0),
            lowStockIntervalMinutes = (map["low_stock_interval_minutes"]?.toLongOrNull() ?: 15L).coerceAtLeast(15L),
            lowStockLastCheckAt = map["low_stock_last_check_at"]?.toLongOrNull() ?: 0L,
            lowStockLastCount = map["low_stock_last_count"]?.toIntOrNull() ?: 0,
            returnReminderAlertsEnabled = map["return_reminder_alerts_enabled"] != "false",
            returnReminderLastCheckAt = map["return_reminder_last_check_at"]?.toLongOrNull() ?: 0L,
            returnReminderLastCount = map["return_reminder_last_count"]?.toIntOrNull() ?: 0,
            dailyRevenueTargetCents = (map["daily_revenue_target_cents"]?.toLongOrNull() ?: 0L).coerceAtLeast(0L),
            hasSeenWalkthrough = map["has_seen_walkthrough"] == "true",
        )
    }

    val rawSettingsFlow: Flow<Map<String, String>> =
        dao.observeSettings().map { list -> list.associate { it.key to it.value } }

    val branchesFlow: Flow<List<BranchEntity>> = dao.observeActiveBranches()
    val categoriesFlow: Flow<List<CategoryEntity>> = categoryDao.observeCategories()
    val itemsFlow: Flow<List<ItemEntity>> = productDao.observeProducts()

    fun observeProductsFiltered(query: String): Flow<List<ItemEntity>> {
        val trimmed = query.trim()
        return if (trimmed.isEmpty()) productDao.observeProducts() else productDao.searchProducts(trimmed)
    }
    val customersFlow: Flow<List<CustomerEntity>> = dao.observeCustomers()
    val staffFlow: Flow<List<UserEntity>> = dao.observeStaff()
    val backOfficeUsersFlow: Flow<List<BackOfficeUserEntity>> = backOfficeUserDao.observeBackOfficeUsers()
    val stockMovementsFlow: Flow<List<StockMovementEntity>> = dao.observeRecentStockMovements()
    val shiftsFlow: Flow<List<ShiftEntity>> = dao.observeShifts()
    val cashMovementsFlow: Flow<List<CashMovementEntity>> = dao.observeCashMovements()
    val equipmentUnitsFlow: Flow<List<EquipmentUnitEntity>> = dao.observeEquipmentUnits()
    val equipmentScanLogsFlow: Flow<List<EquipmentScanLogEntity>> = dao.observeEquipmentScanLogs()
    val agreementTemplatesFlow: Flow<List<AgreementTemplateEntity>> = dao.observeAgreementTemplates()
    val eventKitsFlow: Flow<List<EventKitWithItems>> = dao.observeEventKitsWithItems()
    val damageReportsFlow: Flow<List<DamageReportEntity>> = dao.observeDamageReports()
    val maintenanceSchedulesFlow: Flow<List<MaintenanceScheduleEntity>> = dao.observeMaintenanceSchedules()
    val deliveryStopsFlow: Flow<List<DeliveryStopEntity>> = dao.observeDeliveryStops()

    val receiptsWithLinesFlow: Flow<List<ReceiptWithLines>> = saleDao.observeSalesWithItems()

    fun observeCompletedSalesSince(sinceMs: Long): Flow<List<SaleEntity>> =
        saleDao.observeCompletedSalesSince(sinceMs)

    suspend fun buildSalesCsvReport(
        period: SalesReportPeriod,
        year: Int,
        monthZeroBased: Int,
        dayOfMonth: Int,
        branchFilterId: String?,
    ): SalesCsvReportResult = withContext(Dispatchers.IO) {
        val (startMs, endMs) = when (period) {
            SalesReportPeriod.DAILY -> SalesCsvExporter.dateRangeForDaily(year, monthZeroBased, dayOfMonth)
            SalesReportPeriod.MONTHLY -> SalesCsvExporter.dateRangeForMonthly(year, monthZeroBased)
        }
        val sales = saleDao.getSalesWithItemsInRange(startMs, endMs)
        val branches = dao.getAllBranches()
        val settings = settingsFlow.first()
        SalesCsvExporter.buildCsvReport(
            period = period,
            year = year,
            monthZeroBased = monthZeroBased,
            dayOfMonth = dayOfMonth,
            sales = sales,
            branches = branches,
            settings = settings,
            branchFilterId = branchFilterId,
        )
    }

    fun computeDailyRevenueTrend(
        sales: List<SaleEntity>,
        days: Int,
        branchId: String?,
    ): List<DailyRevenuePoint> {
        val clampedDays = days.coerceIn(1, 90)
        val todayStart = startOfTodayMs()
        val cal = Calendar.getInstance()
        val dayLabels = arrayOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
        val monthNames = arrayOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
        val filtered = sales.filter { sale ->
            sale.isCompleted && (branchId == null || sale.branchId == null || sale.branchId == branchId)
        }
        return ((clampedDays - 1) downTo 0).map { offset ->
            val start = todayStart - offset * 86_400_000L
            val end = start + 86_400_000L
            cal.timeInMillis = start
            val label = if (clampedDays <= 7) {
                dayLabels[cal.get(Calendar.DAY_OF_WEEK) - 1]
            } else {
                "${monthNames[cal.get(Calendar.MONTH)]} ${cal.get(Calendar.DAY_OF_MONTH)}"
            }
            val daySales = filtered.filter { it.createdAt in start until end }
            DailyRevenuePoint(
                dayStartMs = start,
                label = label,
                totalCents = daySales.sumOf { it.totalCents },
                salesCount = daySales.size,
            )
        }
    }

    fun computeMonthlyRentalIncome(
        receipts: List<ReceiptWithLines>,
        months: Int,
        branchId: String?,
    ): List<MonthlyIncomePoint> {
        val clampedMonths = months.coerceIn(1, 24)
        val zone = ZoneId.systemDefault()
        val currentMonth = YearMonth.now(zone)
        val filtered = receipts.filter { entry ->
            entry.receipt.isCompleted &&
                (branchId == null || entry.receipt.branchId == null || entry.receipt.branchId == branchId)
        }
        val grouped = filtered.groupBy { entry ->
            YearMonth.from(Instant.ofEpochMilli(entry.receipt.createdAt).atZone(zone))
        }
        return ((clampedMonths - 1) downTo 0).map { offset ->
            val ym = currentMonth.minusMonths(offset.toLong())
            val monthOrders = grouped[ym].orEmpty()
            val monthLabel = ym.month.getDisplayName(TextStyle.SHORT, Locale.getDefault())
            val label = if (clampedMonths > 6 && ym.monthValue == 1) {
                "$monthLabel '${(ym.year % 100).toString().padStart(2, '0')}"
            } else {
                monthLabel
            }
            val unitDays = monthOrders.sumOf { entry ->
                val days = entry.receipt.rentalDays.coerceAtLeast(1)
                entry.lines.sumOf { it.qty * days }
            }
            MonthlyIncomePoint(
                yearMonthKey = ym.toString(),
                label = label,
                totalCents = monthOrders.sumOf { it.receipt.totalCents },
                ordersCount = monthOrders.size,
                rentedUnitDays = unitDays,
            )
        }
    }

    fun computeEquipmentUtilization(
        items: List<ItemEntity>,
        categories: List<CategoryEntity>,
        receipts: List<ReceiptWithLines>,
        branchId: String?,
        byCategory: Boolean,
        liveOutMode: Boolean,
    ): List<EquipmentUtilizationPoint> {
        val tracked = items.filter { it.trackStock && !it.archived && it.availableInBranch(branchId) }
        if (tracked.isEmpty()) return emptyList()

        val now = nowMs()
        val windowDays = 30L
        val windowStart = now - windowDays * 86_400_000L
        val branchCompleted = receipts.filter { entry ->
            entry.receipt.isCompleted &&
                (branchId == null || entry.receipt.branchId == null || entry.receipt.branchId == branchId)
        }

        val activeOutByItem = mutableMapOf<String, Double>()
        val unitDays30dByItem = mutableMapOf<String, Double>()
        val revenueByItem = mutableMapOf<String, Long>()

        for (entry in branchCompleted) {
            val r = entry.receipt
            val rStart = if (r.rentalStartMs > 0L) r.rentalStartMs else r.createdAt
            val rEnd = if (r.rentalEndMs > rStart) r.rentalEndMs else rStart + r.rentalDays.coerceAtLeast(1) * 86_400_000L
            val overlapMs = (minOf(rEnd, now) - maxOf(rStart, windowStart)).coerceAtLeast(0L)
            val overlapDays = if (overlapMs > 0L) (overlapMs / 86_400_000.0).coerceAtLeast(0.25) else 0.0

            for (line in entry.lines) {
                revenueByItem[line.itemId] = (revenueByItem[line.itemId] ?: 0L) + line.lineTotalCents
                if (r.isRentalOut) {
                    activeOutByItem[line.itemId] = (activeOutByItem[line.itemId] ?: 0.0) + line.qty
                }
                if (overlapDays > 0.0) {
                    unitDays30dByItem[line.itemId] = (unitDays30dByItem[line.itemId] ?: 0.0) + (line.qty * overlapDays)
                }
            }
        }

        val categoryNameById = categories.associate { it.id to it.name }

        return if (byCategory) {
            tracked
                .groupBy { it.categoryId ?: "uncategorized" }
                .map { (catId, group) ->
                    val label = categoryNameById[catId] ?: "Other"
                    val shortLabel = label
                        .substringBefore("&")
                        .trim()
                        .split(" ")
                        .firstOrNull()
                        .orEmpty()
                        .ifEmpty { label }
                        .take(8)
                    val activeOut = group.sumOf { activeOutByItem[it.id] ?: 0.0 }
                    val totalCapacity = group.sumOf { (it.stock + (activeOutByItem[it.id] ?: 0.0)).coerceAtLeast(0.0) }
                    val unitDays30 = group.sumOf { unitDays30dByItem[it.id] ?: 0.0 }
                    val revCents = group.sumOf { revenueByItem[it.id] ?: 0L }
                    val pct = if (totalCapacity <= 0.0) {
                        0.0
                    } else if (liveOutMode) {
                        (activeOut / totalCapacity * 100.0).coerceIn(0.0, 100.0)
                    } else {
                        (unitDays30 / (totalCapacity * windowDays.toDouble()) * 100.0).coerceIn(0.0, 100.0)
                    }
                    EquipmentUtilizationPoint(
                        id = catId,
                        label = label,
                        shortLabel = shortLabel,
                        utilizationPercent = pct,
                        activeOutUnits = activeOut,
                        totalCapacityUnits = totalCapacity,
                        revenueCents = revCents,
                    )
                }
                .sortedWith(
                    compareByDescending<EquipmentUtilizationPoint> { it.utilizationPercent }
                        .thenByDescending { it.revenueCents }
                        .thenByDescending { it.totalCapacityUnits }
                )
        } else {
            tracked
                .map { item ->
                    val activeOut = activeOutByItem[item.id] ?: 0.0
                    val totalCapacity = (item.stock + activeOut).coerceAtLeast(0.0)
                    val unitDays30 = unitDays30dByItem[item.id] ?: 0.0
                    val revCents = revenueByItem[item.id] ?: 0L
                    val pct = if (totalCapacity <= 0.0) {
                        0.0
                    } else if (liveOutMode) {
                        (activeOut / totalCapacity * 100.0).coerceIn(0.0, 100.0)
                    } else {
                        (unitDays30 / (totalCapacity * windowDays.toDouble()) * 100.0).coerceIn(0.0, 100.0)
                    }
                    val shortLabel = item.sku.takeIf { it.isNotBlank() }?.take(8)
                        ?: item.name.take(8).trim()
                    EquipmentUtilizationPoint(
                        id = item.id,
                        label = item.name,
                        shortLabel = shortLabel,
                        utilizationPercent = pct,
                        activeOutUnits = activeOut,
                        totalCapacityUnits = totalCapacity,
                        revenueCents = revCents,
                    )
                }
                .sortedWith(
                    compareByDescending<EquipmentUtilizationPoint> { it.utilizationPercent }
                        .thenByDescending { it.activeOutUnits }
                        .thenByDescending { it.revenueCents }
                )
                .take(6)
        }
    }

    val ticketsWithLinesFlow: Flow<List<TicketWithLines>> =
        combine(dao.observeTickets(), dao.observeAllTicketLines()) { tickets, lines ->
            val byTicket = lines.groupBy { it.ticketId }
            tickets.map { t -> TicketWithLines(t, byTicket[t.id].orEmpty()) }
        }

    // --- Settings ---
    suspend fun loadRawSettings(): Map<String, String> =
        dao.getAllSettings().associate { it.key to it.value }

    suspend fun saveSetting(key: String, value: String) {
        dao.upsertSetting(SettingEntity(key, value))
    }

    suspend fun saveSettings(
        storeName: String? = null,
        currencySymbol: String? = null,
        taxRatePercent: Double? = null,
        taxIncluded: Boolean? = null,
        receiptFooter: String? = null,
    ) {
        val now = nowMs()
        if (storeName != null) saveSetting("store_name", storeName)
        if (currencySymbol != null) saveSetting("currency_symbol", currencySymbol)
        if (taxRatePercent != null) saveSetting("tax_rate", taxRatePercent.toString())
        if (taxIncluded != null) saveSetting("tax_included", taxIncluded.toString())
        if (receiptFooter != null) saveSetting("receipt_footer", receiptFooter)
        saveSetting("cloud_settings_ts", now.toString())
    }

    suspend fun saveLowStockAlertSettings(
        enabled: Boolean,
        threshold: Double,
        intervalMinutes: Long = 15L,
    ) {
        val cleanThreshold = threshold.coerceAtLeast(0.0)
        val cleanInterval = intervalMinutes.coerceAtLeast(15L)
        saveSetting("low_stock_alerts_enabled", enabled.toString())
        saveSetting("low_stock_threshold", cleanThreshold.toString())
        saveSetting("low_stock_interval_minutes", cleanInterval.toString())
    }

    suspend fun saveLowStockCheckResult(checkedAt: Long, lowCount: Int) {
        saveSetting("low_stock_last_check_at", checkedAt.toString())
        saveSetting("low_stock_last_count", lowCount.coerceAtLeast(0).toString())
    }

    suspend fun saveReturnReminderSettings(enabled: Boolean) {
        saveSetting("return_reminder_alerts_enabled", enabled.toString())
    }

    suspend fun saveReturnReminderCheckResult(checkedAt: Long, dueCount: Int) {
        saveSetting("return_reminder_last_check_at", checkedAt.toString())
        saveSetting("return_reminder_last_count", dueCount.coerceAtLeast(0).toString())
    }

    suspend fun saveReturnReminderNotifiedTokens(encoded: String) {
        saveSetting("return_reminder_notified_tokens", encoded)
    }

    suspend fun saveDailyRevenueTarget(targetCents: Long) {
        saveSetting("daily_revenue_target_cents", targetCents.coerceAtLeast(0L).toString())
    }

    suspend fun setWalkthroughCompleted(completed: Boolean) {
        saveSetting("has_seen_walkthrough", completed.toString())
    }

    suspend fun getLowStockProducts(threshold: Double): List<ItemEntity> =
        productDao.getLowStockProducts(threshold.coerceAtLeast(0.0))

    // --- Auth ---
    suspend fun currentSessionUser(): UserEntity? {
        val id = dao.getSetting("session_user") ?: return null
        if (id.isEmpty()) return null
        val user = dao.getUserById(id)
        if (user == null || !user.active) {
            dao.upsertSetting(SettingEntity("session_user", ""))
            return null
        }
        return user
    }

    suspend fun anyUsersExist(): Boolean = dao.countUsers() > 0

    suspend fun signUpOwner(
        emailRaw: String,
        password: String,
        shopName: String,
        ownerName: String,
        branchName: String,
    ): Result<UserEntity> {
        val email = AuthCrypto.normalizeEmail(emailRaw)
        if (!AuthCrypto.isValidEmail(email)) {
            return Result.failure(IllegalArgumentException("Enter a valid email address"))
        }
        if (password.length < 8) {
            return Result.failure(IllegalArgumentException("Password must be at least 8 characters"))
        }
        if (dao.getUserByEmail(email) != null) {
            return Result.failure(IllegalArgumentException("An account with this email already exists"))
        }
        val salt = AuthCrypto.newSalt()
        val hash = AuthCrypto.hashPassword(password, salt)
        val now = nowMs()
        val user = UserEntity(
            id = newId(),
            email = email,
            shopName = shopName.trim(),
            ownerName = ownerName.trim(),
            passwordHash = hash,
            salt = salt,
            createdAt = now,
            role = UserEntity.ROLE_OWNER,
            branchId = null,
            active = true,
            updatedAt = now,
        )
        db.withTransaction {
            dao.upsertUser(user)
            dao.upsertSetting(SettingEntity("session_user", user.id))
            if (shopName.trim().isNotEmpty()) {
                dao.upsertSetting(SettingEntity("store_name", shopName.trim()))
            }
            val cleanBranch = branchName.trim().ifEmpty { "Main branch" }
            val branches = dao.getActiveBranches()
            if (branches.isNotEmpty()) {
                val first = branches.first()
                dao.upsertBranch(first.copy(name = cleanBranch, updatedAt = now))
                dao.upsertSetting(SettingEntity("active_branch", first.id))
            } else {
                val b = BranchEntity(id = newId(), name = cleanBranch, createdAt = now, updatedAt = now)
                dao.upsertBranch(b)
                dao.upsertSetting(SettingEntity("active_branch", b.id))
            }
        }
        return Result.success(user)
    }

    suspend fun signIn(emailRaw: String, password: String): Result<UserEntity> {
        val email = AuthCrypto.normalizeEmail(emailRaw)
        if (email.isEmpty() || password.isEmpty()) {
            return Result.failure(IllegalArgumentException("Enter email and password"))
        }
        val user = dao.getUserByEmail(email)
            ?: return Result.failure(IllegalArgumentException("Incorrect email or password"))
        val computed = AuthCrypto.hashPassword(password, user.salt)
        if (!AuthCrypto.safeEquals(computed, user.passwordHash)) {
            return Result.failure(IllegalArgumentException("Incorrect email or password"))
        }
        if (!user.active) {
            return Result.failure(IllegalArgumentException("This account has been deactivated by the owner"))
        }
        dao.upsertSetting(SettingEntity("session_user", user.id))
        if (user.isCashier && !user.branchId.isNullOrEmpty()) {
            dao.upsertSetting(SettingEntity("active_branch", user.branchId))
        }
        return Result.success(user)
    }

    suspend fun signOut() {
        dao.upsertSetting(SettingEntity("session_user", ""))
    }

    suspend fun addCashier(
        name: String,
        emailRaw: String,
        password: String,
        branchId: String,
        shopName: String,
    ): Result<UserEntity> {
        val cleanName = name.trim()
        val email = AuthCrypto.normalizeEmail(emailRaw)
        if (cleanName.isEmpty()) {
            return Result.failure(IllegalArgumentException("Seller name is required"))
        }
        if (!AuthCrypto.isValidEmail(email)) {
            return Result.failure(IllegalArgumentException("Enter a valid email or username@shop format"))
        }
        if (password.length < 6) {
            return Result.failure(IllegalArgumentException("PIN/password must be at least 6 characters"))
        }
        if (dao.getUserByEmail(email) != null) {
            return Result.failure(IllegalArgumentException("A user with this email already exists"))
        }
        val salt = AuthCrypto.newSalt()
        val now = nowMs()
        val user = UserEntity(
            id = newId(),
            email = email,
            shopName = shopName,
            ownerName = cleanName,
            passwordHash = AuthCrypto.hashPassword(password, salt),
            salt = salt,
            createdAt = now,
            role = UserEntity.ROLE_CASHIER,
            branchId = branchId,
            active = true,
            updatedAt = now,
        )
        dao.upsertUser(user)
        return Result.success(user)
    }

    suspend fun setStaffActive(id: String, active: Boolean) {
        dao.setUserActive(id, active, nowMs())
    }

    suspend fun deleteStaff(id: String) {
        dao.deleteUserById(id)
    }

    // --- Back Office Auth (isolated from POS Register sessions) ---
    suspend fun currentBackOfficeSessionUser(): BackOfficeUserEntity? {
        val id = dao.getSetting("backoffice_session_user") ?: return null
        if (id.isEmpty()) return null
        val user = backOfficeUserDao.getBackOfficeUserById(id)
        if (user == null || !user.active) {
            dao.upsertSetting(SettingEntity("backoffice_session_user", ""))
            return null
        }
        return user
    }

    suspend fun anyBackOfficeUsersExist(): Boolean = backOfficeUserDao.countBackOfficeUsers() > 0

    suspend fun signUpBackOfficeAdmin(
        emailRaw: String,
        password: String,
        fullName: String,
        shopName: String,
    ): Result<BackOfficeUserEntity> {
        val email = AuthCrypto.normalizeEmail(emailRaw)
        if (!AuthCrypto.isValidEmail(email)) {
            return Result.failure(IllegalArgumentException("Enter a valid Back Office email address"))
        }
        if (password.length < 8) {
            return Result.failure(IllegalArgumentException("Password must be at least 8 characters"))
        }
        if (backOfficeUserDao.getBackOfficeUserByEmail(email) != null) {
            return Result.failure(IllegalArgumentException("A Back Office account with this email already exists"))
        }
        val cleanName = fullName.trim()
        val cleanShop = shopName.trim()
        val salt = AuthCrypto.newSalt()
        val hash = AuthCrypto.hashPassword(password, salt)
        val now = nowMs()
        val user = BackOfficeUserEntity(
            id = newId(),
            email = email,
            fullName = cleanName,
            shopName = cleanShop,
            passwordHash = hash,
            salt = salt,
            role = BackOfficeUserEntity.ROLE_ADMIN,
            active = true,
            createdAt = now,
            updatedAt = now,
        )
        db.withTransaction {
            backOfficeUserDao.upsertBackOfficeUser(user)
            dao.upsertSetting(SettingEntity("backoffice_session_user", user.id))
            if (cleanShop.isNotEmpty()) {
                dao.upsertSetting(SettingEntity("store_name", cleanShop))
            }
        }
        return Result.success(user)
    }

    suspend fun signInBackOffice(emailRaw: String, password: String): Result<BackOfficeUserEntity> {
        val email = AuthCrypto.normalizeEmail(emailRaw)
        if (email.isEmpty() || password.isEmpty()) {
            return Result.failure(IllegalArgumentException("Enter Back Office email and password"))
        }
        val user = backOfficeUserDao.getBackOfficeUserByEmail(email)
            ?: return Result.failure(IllegalArgumentException("Incorrect Back Office email or password"))
        val computed = AuthCrypto.hashPassword(password, user.salt)
        if (!AuthCrypto.safeEquals(computed, user.passwordHash)) {
            return Result.failure(IllegalArgumentException("Incorrect Back Office email or password"))
        }
        if (!user.active) {
            return Result.failure(IllegalArgumentException("This Back Office account has been deactivated"))
        }
        dao.upsertSetting(SettingEntity("backoffice_session_user", user.id))
        return Result.success(user)
    }

    suspend fun signOutBackOffice() {
        dao.upsertSetting(SettingEntity("backoffice_session_user", ""))
    }

    suspend fun addBackOfficeAdmin(
        fullName: String,
        emailRaw: String,
        password: String,
        role: String,
        shopName: String,
    ): Result<BackOfficeUserEntity> {
        val cleanName = fullName.trim()
        val email = AuthCrypto.normalizeEmail(emailRaw)
        if (cleanName.isEmpty()) {
            return Result.failure(IllegalArgumentException("Admin name is required"))
        }
        if (!AuthCrypto.isValidEmail(email)) {
            return Result.failure(IllegalArgumentException("Enter a valid email address"))
        }
        if (password.length < 8) {
            return Result.failure(IllegalArgumentException("Password must be at least 8 characters"))
        }
        if (backOfficeUserDao.getBackOfficeUserByEmail(email) != null) {
            return Result.failure(IllegalArgumentException("A Back Office account with this email already exists"))
        }
        val salt = AuthCrypto.newSalt()
        val now = nowMs()
        val user = BackOfficeUserEntity(
            id = newId(),
            email = email,
            fullName = cleanName,
            shopName = shopName,
            passwordHash = AuthCrypto.hashPassword(password, salt),
            salt = salt,
            role = role,
            active = true,
            createdAt = now,
            updatedAt = now,
        )
        backOfficeUserDao.upsertBackOfficeUser(user)
        return Result.success(user)
    }

    suspend fun setBackOfficeUserActive(id: String, active: Boolean) {
        backOfficeUserDao.setBackOfficeUserActive(id, active, nowMs())
    }

    suspend fun deleteBackOfficeUser(id: String) {
        backOfficeUserDao.deleteBackOfficeUserById(id)
    }

    // --- Branches ---
    suspend fun saveBranch(branch: BranchEntity) {
        dao.upsertBranch(branch.copy(updatedAt = nowMs()))
    }

    suspend fun archiveBranch(id: String): Result<Unit> {
        val active = dao.getActiveBranches()
        if (active.size <= 1) {
            return Result.failure(IllegalStateException("Keep at least one active branch"))
        }
        val now = nowMs()
        dao.archiveBranch(id, now)
        val remaining = active.firstOrNull { it.id != id }
        if (remaining != null) {
            val currentActive = dao.getSetting("active_branch")
            if (currentActive == id) {
                saveSetting("active_branch", remaining.id)
            }
        }
        return Result.success(Unit)
    }

    // --- Categories ---
    suspend fun saveCategory(category: CategoryEntity) {
        dao.upsertCategory(category.copy(updatedAt = nowMs()))
    }

    suspend fun deleteCategory(id: String) {
        db.withTransaction {
            dao.clearCategoryOnItems(id, nowMs())
            dao.deleteCategoryById(id)
        }
    }

    // --- Items & Stock (via ProductDao CRUD) ---
    suspend fun saveItem(item: ItemEntity) {
        productDao.upsertProduct(item.copy(updatedAt = nowMs()))
    }

    suspend fun updateItemSafetyThreshold(itemId: String, safetyThreshold: Double?) {
        productDao.updateSafetyThreshold(itemId, safetyThreshold?.coerceAtLeast(0.0), nowMs())
    }

    suspend fun deleteItem(id: String) {
        productDao.archiveProduct(id, nowMs())
    }

    suspend fun adjustStock(itemId: String, newStock: Double, type: String = "adjust", note: String = "") {
        val current = productDao.getProductById(itemId) ?: return
        val delta = newStock - current.stock
        val now = nowMs()
        db.withTransaction {
            productDao.updateProductStock(itemId, newStock, now)
            dao.insertStockMovement(
                StockMovementEntity(
                    id = newId(),
                    itemId = itemId,
                    qty = delta,
                    type = type,
                    createdAt = now,
                    note = note,
                )
            )
        }
    }

    suspend fun receiveStock(itemId: String, addedQty: Double, note: String = "") {
        val current = productDao.getProductById(itemId) ?: return
        val now = nowMs()
        db.withTransaction {
            productDao.updateProductStock(itemId, current.stock + addedQty, now)
            dao.insertStockMovement(
                StockMovementEntity(
                    id = newId(),
                    itemId = itemId,
                    qty = addedQty,
                    type = "receive",
                    createdAt = now,
                    note = note,
                )
            )
        }
    }

    suspend fun batchRestockToTarget(
        itemIds: List<String>,
        targetStock: Double,
        note: String = "Batch restock",
    ): Int {
        if (itemIds.isEmpty() || targetStock <= 0.0) return 0
        val now = nowMs()
        var updatedCount = 0
        db.withTransaction {
            for (id in itemIds) {
                val product = productDao.getProductById(id) ?: continue
                if (!product.trackStock) continue
                val delta = targetStock - product.stock
                if (delta > 0.0) {
                    productDao.updateProductStock(id, targetStock, now)
                    dao.insertStockMovement(
                        StockMovementEntity(
                            id = newId(),
                            itemId = id,
                            qty = delta,
                            type = "receive",
                            createdAt = now,
                            note = note,
                        )
                    )
                    updatedCount++
                }
            }
        }
        return updatedCount
    }

    // --- Inventory CSV Export & Bulk Import ---
    suspend fun buildInventoryCsvExport(includeArchived: Boolean = false): InventoryCsvExportResult =
        withContext(Dispatchers.IO) {
            val products = productDao.getAllProducts()
            val categories = categoryDao.getAllCategories()
            InventoryCsvManager.buildInventoryCsv(
                products = products,
                categories = categories,
                includeArchived = includeArchived,
            )
        }

    suspend fun previewInventoryCsvImport(
        rawCsv: String,
        filenameLabel: String,
    ): CsvImportPreview = withContext(Dispatchers.IO) {
        val products = productDao.getAllProducts()
        val categories = categoryDao.getAllCategories()
        InventoryCsvManager.parseInventoryCsv(
            rawCsv = rawCsv,
            filenameLabel = filenameLabel,
            existingProducts = products,
            existingCategories = categories,
        )
    }

    suspend fun commitInventoryCsvImport(
        rows: List<ParsedCsvProductRow>,
        skipExisting: Boolean,
    ): CsvImportCommitResult = withContext(Dispatchers.IO) {
        if (rows.isEmpty()) {
            return@withContext CsvImportCommitResult(0, 0, 0, 0)
        }
        val now = nowMs()
        var insertedCount = 0
        var updatedCount = 0
        var skippedCount = 0
        var createdCategoriesCount = 0

        db.withTransaction {
            val categories = categoryDao.getAllCategories().toMutableList()
            val categoryByLower = categories.associateBy { it.name.trim().lowercase(Locale.ROOT) }.toMutableMap()

            val existingProducts = productDao.getAllProducts().toMutableList()
            val byId = existingProducts.associateBy { it.id }.toMutableMap()
            val bySku = existingProducts
                .filter { it.sku.isNotBlank() }
                .associateBy { it.sku.trim().lowercase(Locale.ROOT) }
                .toMutableMap()
            val byBarcode = existingProducts
                .filter { it.barcode.isNotBlank() }
                .associateBy { it.barcode.trim().lowercase(Locale.ROOT) }
                .toMutableMap()
            val byName = existingProducts
                .associateBy { it.name.trim().lowercase(Locale.ROOT) }
                .toMutableMap()

            val productsToUpsert = mutableListOf<ProductEntity>()
            val newCategories = mutableListOf<CategoryEntity>()
            val movements = mutableListOf<StockMovementEntity>()

            for (row in rows) {
                val existing = row.matchedExistingId?.let { byId[it] }
                    ?: row.sku.takeIf { it.isNotEmpty() }?.let { bySku[it.lowercase(Locale.ROOT)] }
                    ?: row.barcode.takeIf { it.isNotEmpty() }?.let { byBarcode[it.lowercase(Locale.ROOT)] }
                    ?: byName[row.name.lowercase(Locale.ROOT)]

                if (existing != null && skipExisting) {
                    skippedCount++
                    continue
                }

                val resolvedCategoryId: String? = if (row.categoryName.isNotEmpty()) {
                    val lower = row.categoryName.lowercase(Locale.ROOT)
                    val found = categoryByLower[lower]
                    if (found != null) {
                        found.id
                    } else {
                        val created = CategoryEntity(
                            id = newId(),
                            name = row.categoryName,
                            position = categories.size,
                            updatedAt = now,
                        )
                        categories.add(created)
                        categoryByLower[lower] = created
                        newCategories.add(created)
                        createdCategoriesCount++
                        created.id
                    }
                } else {
                    existing?.categoryId
                }

                if (existing != null) {
                    val updated = existing.copy(
                        categoryId = resolvedCategoryId,
                        name = row.name,
                        priceCents = row.priceCents,
                        costCents = if (row.costCents > 0L) row.costCents else existing.costCents,
                        sku = row.sku.ifEmpty { existing.sku },
                        barcode = row.barcode.ifEmpty { existing.barcode },
                        trackStock = row.trackStock,
                        stock = row.stock,
                        archived = row.archived,
                        updatedAt = now,
                    )
                    productsToUpsert.add(updated)
                    byId[updated.id] = updated
                    if (updated.sku.isNotBlank()) bySku[updated.sku.trim().lowercase(Locale.ROOT)] = updated
                    if (updated.barcode.isNotBlank()) byBarcode[updated.barcode.trim().lowercase(Locale.ROOT)] = updated
                    byName[updated.name.trim().lowercase(Locale.ROOT)] = updated
                    if (updated.trackStock && updated.stock != existing.stock) {
                        movements.add(
                            StockMovementEntity(
                                id = newId(),
                                itemId = updated.id,
                                qty = updated.stock - existing.stock,
                                type = "adjust",
                                createdAt = now,
                                note = "CSV import update",
                            )
                        )
                    }
                    updatedCount++
                } else {
                    val created = ProductEntity(
                        id = newId(),
                        categoryId = resolvedCategoryId,
                        name = row.name,
                        priceCents = row.priceCents,
                        costCents = row.costCents,
                        sku = row.sku,
                        barcode = row.barcode,
                        trackStock = row.trackStock,
                        stock = row.stock,
                        archived = row.archived,
                        updatedAt = now,
                    )
                    productsToUpsert.add(created)
                    byId[created.id] = created
                    if (created.sku.isNotBlank()) bySku[created.sku.trim().lowercase(Locale.ROOT)] = created
                    if (created.barcode.isNotBlank()) byBarcode[created.barcode.trim().lowercase(Locale.ROOT)] = created
                    byName[created.name.trim().lowercase(Locale.ROOT)] = created
                    if (created.trackStock && created.stock > 0.0) {
                        movements.add(
                            StockMovementEntity(
                                id = newId(),
                                itemId = created.id,
                                qty = created.stock,
                                type = "receive",
                                createdAt = now,
                                note = "CSV import",
                            )
                        )
                    }
                    insertedCount++
                }
            }

            if (newCategories.isNotEmpty()) categoryDao.upsertCategories(newCategories)
            if (productsToUpsert.isNotEmpty()) productDao.upsertProducts(productsToUpsert)
            if (movements.isNotEmpty()) dao.insertStockMovements(movements)
        }

        CsvImportCommitResult(
            insertedCount = insertedCount,
            updatedCount = updatedCount,
            skippedCount = skippedCount,
            createdCategoriesCount = createdCategoriesCount,
        )
    }

    // --- Customers ---
    suspend fun saveCustomer(customer: CustomerEntity) {
        dao.upsertCustomer(customer.copy(updatedAt = nowMs()))
    }

    suspend fun deleteCustomer(id: String) {
        dao.deleteCustomerById(id)
    }

    // --- Tickets ---
    suspend fun saveTicket(
        ticketId: String?,
        name: String,
        lines: List<CartLine>,
        discount: DiscountInput?,
        customer: CustomerEntity?,
        note: String,
        branchId: String?,
        rentalDays: Int = 1,
        rentalStartMs: Long = nowMs(),
        rentalEndMs: Long = rentalStartMs + rentalDays.coerceAtLeast(1) * 86_400_000L,
        depositCents: Long = 0L,
        eventName: String = "",
        eventVenue: String = "",
    ): TicketEntity {
        val id = ticketId ?: newId()
        val now = nowMs()
        val days = rentalDays.coerceAtLeast(1)
        val ticket = TicketEntity(
            id = id,
            createdAt = now,
            name = name,
            discountType = discount?.type,
            discountValue = discount?.value,
            customerId = customer?.id,
            customerName = customer?.name,
            note = note,
            branchId = branchId,
            rentalDays = days,
            rentalStartMs = rentalStartMs,
            rentalEndMs = rentalEndMs,
            depositCents = depositCents.coerceAtLeast(0L),
            eventName = eventName.trim(),
            eventVenue = eventVenue.trim(),
        )
        val ticketLines = lines.map {
            TicketLineEntity(
                id = newId(),
                ticketId = id,
                itemId = it.itemId,
                name = it.name,
                priceCents = it.priceCents,
                qty = it.qty,
            )
        }
        db.withTransaction {
            dao.upsertTicket(ticket)
            dao.deleteTicketLines(id)
            dao.insertTicketLines(ticketLines)
        }
        return ticket
    }

    suspend fun deleteTicket(ticketId: String) {
        db.withTransaction {
            dao.deleteTicketLines(ticketId)
            dao.deleteTicketById(ticketId)
        }
    }

    // --- Complete Rental Checkout, Return Equipment & Refund ---
    suspend fun completeSale(
        lines: List<CartLine>,
        discount: DiscountInput?,
        customer: CustomerEntity?,
        note: String,
        paymentType: String,
        paidCents: Long,
        settings: SettingsModel,
        branchId: String?,
        cashier: UserEntity?,
        openTicketId: String?,
        rentalDays: Int = 1,
        rentalStartMs: Long = nowMs(),
        rentalEndMs: Long = rentalStartMs + rentalDays.coerceAtLeast(1) * 86_400_000L,
        depositCents: Long = 0L,
        eventName: String = "",
        eventVenue: String = "",
    ): ReceiptWithLines {
        val days = rentalDays.coerceAtLeast(1)
        val normalizedLines = lines.map { it.copy(rentalDays = days) }
        val totals = computeTotals(
            lineTotalsCents = normalizedLines.map { it.lineTotalCents },
            discount = discount,
            taxRatePercent = settings.taxRatePercent,
            taxIncluded = settings.taxIncluded,
        )
        val changeCents = (paidCents - totals.totalCents).coerceAtLeast(0L)
        val now = nowMs()
        val receiptId = newId()

        return db.withTransaction {
            val number = (dao.getSetting("next_receipt_number")?.toIntOrNull() ?: settings.nextReceiptNumber)
                .coerceAtLeast(1)
            dao.upsertSetting(SettingEntity("next_receipt_number", (number + 1).toString()))

            val receipt = ReceiptEntity(
                id = receiptId,
                number = number,
                createdAt = now,
                subtotalCents = totals.subtotalCents,
                discountCents = totals.discountCents,
                discountType = discount?.type,
                discountValue = discount?.value,
                taxCents = totals.taxCents,
                totalCents = totals.totalCents,
                paidCents = paidCents,
                changeCents = changeCents,
                paymentType = paymentType,
                customerId = customer?.id,
                customerName = customer?.name,
                status = "completed",
                note = note,
                branchId = branchId,
                cashierId = cashier?.id,
                cashierName = cashier?.ownerName?.ifEmpty { cashier.email },
                updatedAt = now,
                rentalDays = days,
                rentalStartMs = rentalStartMs,
                rentalEndMs = rentalEndMs,
                depositCents = depositCents.coerceAtLeast(0L),
                rentalStatus = "out",
                returnedAtMs = null,
                eventName = eventName.trim(),
                eventVenue = eventVenue.trim(),
            )
            val receiptLines = normalizedLines.map {
                ReceiptLineEntity(
                    id = newId(),
                    receiptId = receiptId,
                    itemId = it.itemId,
                    name = it.name,
                    priceCents = it.priceCents,
                    qty = it.qty,
                    lineTotalCents = it.lineTotalCents,
                )
            }
            saleDao.insertSale(receipt)
            saleItemDao.insertSaleItems(receiptLines)

            for (l in normalizedLines) {
                productDao.adjustProductStock(l.itemId, -l.qty, now)
                dao.insertStockMovement(
                    StockMovementEntity(
                        id = newId(),
                        itemId = l.itemId,
                        qty = -l.qty,
                        type = "rental",
                        createdAt = now,
                        note = "Rental #${number.toString().padStart(4, '0')} (${days}d)",
                        receiptId = receiptId,
                    )
                )
            }

            if (customer != null) {
                val pts = pointsForTotal(totals.totalCents)
                if (pts > 0) dao.addCustomerPoints(customer.id, pts, now)
            }

            if (!openTicketId.isNullOrEmpty()) {
                dao.deleteTicketLines(openTicketId)
                dao.deleteTicketById(openTicketId)
            }

            ReceiptWithLines(receipt, receiptLines)
        }
    }

    suspend fun markRentalReturned(receiptId: String): Boolean {
        val now = nowMs()
        var restored = false
        db.withTransaction {
            val saleWithItems = saleDao.getSaleWithItems(receiptId) ?: return@withTransaction
            val receipt = saleWithItems.receipt
            if (!receipt.isCompleted || receipt.isReturned) return@withTransaction
            val lines = saleItemDao.getSaleItemsForSale(receiptId)
            saleDao.markSaleReturned(receiptId, now, now)
            for (l in lines) {
                productDao.adjustProductStock(l.itemId, l.qty, now)
                dao.insertStockMovement(
                    StockMovementEntity(
                        id = newId(),
                        itemId = l.itemId,
                        qty = l.qty,
                        type = "return",
                        createdAt = now,
                        note = "Return #${receipt.number.toString().padStart(4, '0')}",
                        receiptId = receiptId,
                    )
                )
            }
            val checkedOutUnits = dao.getAllEquipmentUnits().filter { it.currentReceiptId == receiptId }
            checkedOutUnits.forEach { unit ->
                dao.upsertEquipmentUnit(
                    unit.copy(
                        status = EquipmentUnitEntity.STATUS_CHECKED_IN,
                        currentReceiptId = null,
                        currentEventName = "",
                        currentCustomerName = "",
                        lastScanAt = now,
                        updatedAt = now,
                    )
                )
                dao.insertEquipmentScanLog(
                    EquipmentScanLogEntity(
                        id = newId(),
                        unitId = unit.id,
                        unitCode = unit.unitCode,
                        itemId = unit.itemId,
                        itemName = unit.itemName,
                        action = "check_in",
                        receiptId = receiptId,
                        eventName = receipt.eventName,
                        customerName = receipt.customerName ?: "",
                        note = "Returned with order #${receipt.number.toString().padStart(4, '0')}",
                        createdAt = now,
                    )
                )
            }
            restored = true
        }
        return restored
    }

    suspend fun refundReceipt(receiptId: String) {
        val now = nowMs()
        db.withTransaction {
            val saleWithItems = saleDao.getSaleWithItems(receiptId) ?: return@withTransaction
            val receipt = saleWithItems.receipt
            if (!receipt.isCompleted) return@withTransaction
            val lines = saleItemDao.getSaleItemsForSale(receiptId)
            val shouldRestoreStock = !receipt.isReturned
            saleDao.markSaleRefunded(receiptId, now)
            if (shouldRestoreStock) {
                for (l in lines) {
                    productDao.adjustProductStock(l.itemId, l.qty, now)
                    dao.insertStockMovement(
                        StockMovementEntity(
                            id = newId(),
                            itemId = l.itemId,
                            qty = l.qty,
                            type = "refund",
                            createdAt = now,
                            note = "Refund #${receipt.number.toString().padStart(4, '0')}",
                            receiptId = receiptId,
                        )
                    )
                }
            }
        }
    }

    // --- Shifts ---
    suspend fun openShift(openingFloatCents: Long, branchId: String?): ShiftEntity {
        val shift = ShiftEntity(
            id = newId(),
            openedAt = nowMs(),
            openingFloatCents = openingFloatCents,
            branchId = branchId,
        )
        dao.upsertShift(shift)
        return shift
    }

    suspend fun addCashMovement(shiftId: String, type: String, amountCents: Long, note: String) {
        dao.insertCashMovement(
            CashMovementEntity(
                id = newId(),
                shiftId = shiftId,
                type = type,
                amountCents = amountCents,
                note = note.trim(),
                createdAt = nowMs(),
            )
        )
    }

    suspend fun closeShift(shiftId: String, countedCents: Long, expectedCents: Long) {
        dao.closeShift(shiftId, nowMs(), countedCents, expectedCents)
    }

    fun computeShiftStats(
        shift: ShiftEntity,
        receipts: List<ReceiptEntity>,
        movements: List<CashMovementEntity>,
    ): ShiftStats {
        val endMs = shift.closedAt ?: Long.MAX_VALUE
        val branchReceipts = receipts.filter { r ->
            r.createdAt >= shift.openedAt && r.createdAt <= endMs &&
                (shift.branchId == null || r.branchId == null || r.branchId == shift.branchId)
        }
        var cashSales = 0L
        var cardSales = 0L
        var salesCount = 0
        var refundsCount = 0
        for (r in branchReceipts) {
            if (r.isCompleted) {
                salesCount++
                if (r.paymentType == "card") cardSales += r.totalCents
                else cashSales += r.totalCents
            } else {
                refundsCount++
            }
        }
        val shiftMoves = movements.filter { it.shiftId == shift.id }
        val cashIn = shiftMoves.filter { it.isCashIn }.sumOf { it.amountCents }
        val cashOut = shiftMoves.filter { !it.isCashIn }.sumOf { it.amountCents }
        return ShiftStats(
            cashSalesCents = cashSales,
            cardSalesCents = cardSales,
            salesCount = salesCount,
            refundsCount = refundsCount,
            cashInCents = cashIn,
            cashOutCents = cashOut,
            movements = shiftMoves,
        )
    }

    // --- QR Code Unit Tracking (Check-In / Check-Out) ---
    data class ResolvedQrTarget(
        val item: ProductEntity,
        val unit: EquipmentUnitEntity?,
        val matchedCode: String,
    )

    suspend fun resolveQrTarget(rawCode: String): ResolvedQrTarget? {
        val code = rawCode.trim()
        if (code.isEmpty()) return null
        val parsed = QrCodeUtils.parsePayload(code)
        val allProducts = productDao.getAllProducts().filter { !it.archived }
        val allUnits = dao.getAllEquipmentUnits()

        return when (parsed) {
            is ParsedQrCode.UnitTag -> {
                val unit = allUnits.firstOrNull {
                    it.unitCode.equals(parsed.unitCode, ignoreCase = true) ||
                        it.qrPayload.equals(code, ignoreCase = true)
                }
                val item = (unit?.itemId ?: parsed.itemId)?.let { id ->
                    allProducts.firstOrNull { it.id == id }
                } ?: unit?.let { u -> allProducts.firstOrNull { it.id == u.itemId } }
                if (item != null) ResolvedQrTarget(item, unit, parsed.unitCode) else null
            }
            is ParsedQrCode.EquipmentTag -> {
                val item = parsed.itemId?.let { id -> allProducts.firstOrNull { it.id == id } }
                    ?: allProducts.firstOrNull {
                        it.sku.equals(parsed.skuOrCode, ignoreCase = true) ||
                            it.barcode.equals(parsed.skuOrCode, ignoreCase = true)
                    }
                if (item != null) ResolvedQrTarget(item, null, parsed.skuOrCode) else null
            }
            is ParsedQrCode.RawCode -> {
                val unit = allUnits.firstOrNull {
                    it.unitCode.equals(parsed.code, ignoreCase = true) ||
                        it.qrPayload.equals(parsed.code, ignoreCase = true) ||
                        it.id == parsed.code
                }
                if (unit != null) {
                    val item = allProducts.firstOrNull { it.id == unit.itemId }
                    if (item != null) return ResolvedQrTarget(item, unit, unit.unitCode)
                }
                val item = allProducts.firstOrNull {
                    it.id == parsed.code ||
                        it.sku.equals(parsed.code, ignoreCase = true) ||
                        it.barcode.equals(parsed.code, ignoreCase = true) ||
                        it.name.equals(parsed.code, ignoreCase = true)
                }
                if (item != null) ResolvedQrTarget(item, null, parsed.code) else null
            }
        }
    }

    suspend fun addEquipmentUnit(
        itemId: String,
        customUnitCode: String = "",
        conditionNote: String = "",
    ): Result<EquipmentUnitEntity> {
        val item = productDao.getProductById(itemId)
            ?: return Result.failure(IllegalArgumentException("Equipment item not found"))
        val existing = dao.getEquipmentUnitsForItem(itemId)
        val unitCode = customUnitCode.trim().ifEmpty {
            var idx = existing.size + 1
            var candidate = QrCodeUtils.buildDefaultUnitCode(item.sku, item.id, idx)
            val usedCodes = dao.getAllEquipmentUnits().map { it.unitCode.lowercase() }.toSet()
            while (candidate.lowercase() in usedCodes) {
                idx++
                candidate = QrCodeUtils.buildDefaultUnitCode(item.sku, item.id, idx)
            }
            candidate
        }
        val conflict = dao.getEquipmentUnitByCode(unitCode)
        if (conflict != null) {
            return Result.failure(IllegalStateException("Unit code $unitCode already exists"))
        }
        val now = nowMs()
        val unit = EquipmentUnitEntity(
            id = newId(),
            itemId = item.id,
            itemName = item.name,
            unitCode = unitCode,
            qrPayload = QrCodeUtils.buildUnitPayload(item.id, unitCode),
            status = EquipmentUnitEntity.STATUS_CHECKED_IN,
            conditionNote = conditionNote.trim(),
            updatedAt = now,
        )
        dao.upsertEquipmentUnit(unit)
        return Result.success(unit)
    }

    suspend fun deleteEquipmentUnit(unitId: String) {
        dao.deleteEquipmentUnitById(unitId)
    }

    suspend fun processEquipmentUnitScan(
        unitId: String?,
        itemId: String,
        action: String, // "check_out" | "check_in" | "maintenance" | "inspect"
        receipt: ReceiptEntity? = null,
        note: String = "",
        adjustStock: Boolean = true,
        operatorName: String = "",
    ): Result<EquipmentUnitEntity> {
        val item = productDao.getProductById(itemId)
            ?: return Result.failure(IllegalArgumentException("Equipment not found"))
        val now = nowMs()
        val targetUnit: EquipmentUnitEntity = db.withTransaction {
            val existingUnit = when {
                unitId != null -> dao.getEquipmentUnitById(unitId)
                action == "check_out" -> dao.getEquipmentUnitsForItem(itemId)
                    .firstOrNull { it.status == EquipmentUnitEntity.STATUS_CHECKED_IN }
                action == "check_in" -> dao.getEquipmentUnitsForItem(itemId)
                    .firstOrNull { it.status == EquipmentUnitEntity.STATUS_CHECKED_OUT }
                    ?: dao.getEquipmentUnitsForItem(itemId).firstOrNull()
                else -> dao.getEquipmentUnitsForItem(itemId).firstOrNull()
            }

            val resolvedUnit = existingUnit ?: run {
                val existingList = dao.getEquipmentUnitsForItem(itemId)
                val code = QrCodeUtils.buildDefaultUnitCode(item.sku, item.id, existingList.size + 1)
                EquipmentUnitEntity(
                    id = newId(),
                    itemId = item.id,
                    itemName = item.name,
                    unitCode = code,
                    qrPayload = QrCodeUtils.buildUnitPayload(item.id, code),
                    status = EquipmentUnitEntity.STATUS_CHECKED_IN,
                    updatedAt = now,
                )
            }

            val wasCheckedOut = resolvedUnit.status == EquipmentUnitEntity.STATUS_CHECKED_OUT
            val nextStatus = when (action) {
                "check_out" -> EquipmentUnitEntity.STATUS_CHECKED_OUT
                "check_in" -> EquipmentUnitEntity.STATUS_CHECKED_IN
                "maintenance" -> EquipmentUnitEntity.STATUS_MAINTENANCE
                else -> resolvedUnit.status
            }

            val updatedUnit = resolvedUnit.copy(
                itemName = item.name,
                status = nextStatus,
                currentReceiptId = if (action == "check_out") receipt?.id else if (action == "check_in" || action == "maintenance") null else resolvedUnit.currentReceiptId,
                currentEventName = if (action == "check_out") (receipt?.eventName ?: "") else if (action == "check_in" || action == "maintenance") "" else resolvedUnit.currentEventName,
                currentCustomerName = if (action == "check_out") (receipt?.customerName ?: "") else if (action == "check_in" || action == "maintenance") "" else resolvedUnit.currentCustomerName,
                conditionNote = note.trim().ifEmpty { resolvedUnit.conditionNote },
                lastScanAt = now,
                updatedAt = now,
            )
            dao.upsertEquipmentUnit(updatedUnit)

            if (adjustStock && item.trackStock) {
                if (action == "check_out" && !wasCheckedOut && receipt == null && item.stock > 0.0) {
                    productDao.adjustProductStock(item.id, -1.0, now)
                    dao.insertStockMovement(
                        StockMovementEntity(
                            id = newId(),
                            itemId = item.id,
                            qty = -1.0,
                            type = "rental",
                            createdAt = now,
                            note = "QR Check-Out (${updatedUnit.unitCode})",
                        )
                    )
                } else if (action == "check_in" && wasCheckedOut && resolvedUnit.currentReceiptId == null) {
                    productDao.adjustProductStock(item.id, 1.0, now)
                    dao.insertStockMovement(
                        StockMovementEntity(
                            id = newId(),
                            itemId = item.id,
                            qty = 1.0,
                            type = "return",
                            createdAt = now,
                            note = "QR Check-In (${updatedUnit.unitCode})",
                        )
                    )
                }
            }

            dao.insertEquipmentScanLog(
                EquipmentScanLogEntity(
                    id = newId(),
                    unitId = updatedUnit.id,
                    unitCode = updatedUnit.unitCode,
                    itemId = item.id,
                    itemName = item.name,
                    action = action,
                    receiptId = receipt?.id ?: resolvedUnit.currentReceiptId,
                    eventName = receipt?.eventName?.ifEmpty { resolvedUnit.currentEventName } ?: resolvedUnit.currentEventName,
                    customerName = receipt?.customerName ?: resolvedUnit.currentCustomerName,
                    operatorName = operatorName,
                    note = note.trim(),
                    createdAt = now,
                )
            )
            updatedUnit
        }
        return Result.success(targetUnit)
    }

    // --- Digital Rental Agreement Templates ---
    suspend fun saveAgreementTemplate(template: AgreementTemplateEntity) {
        val now = nowMs()
        db.withTransaction {
            val clean = template.copy(
                title = template.title.trim().ifEmpty { "Untitled Rental Agreement" },
                category = template.category.trim().ifEmpty { "General Event" },
                createdAt = if (template.createdAt == 0L) now else template.createdAt,
                updatedAt = now,
            )
            dao.upsertAgreementTemplate(clean)
            if (clean.isDefault) {
                dao.setDefaultAgreementTemplate(clean.id)
            }
        }
    }

    suspend fun setDefaultAgreementTemplate(templateId: String) {
        dao.setDefaultAgreementTemplate(templateId)
    }

    suspend fun deleteAgreementTemplate(templateId: String) {
        db.withTransaction {
            dao.deleteAgreementTemplateById(templateId)
            val remaining = dao.getAllAgreementTemplates()
            if (remaining.isNotEmpty() && remaining.none { it.isDefault }) {
                dao.setDefaultAgreementTemplate(remaining.first().id)
            }
        }
    }

    // --- Event Kits / Bundles ---
    suspend fun saveEventKit(
        kitId: String?,
        name: String,
        description: String,
        category: String,
        discountPercent: Double,
        lines: List<Triple<String, String, Double>>, // (itemId, itemName, qty)
    ): EventKitWithItems {
        val now = nowMs()
        val id = kitId?.takeIf { it.isNotBlank() } ?: newId()
        val existingCreatedAt = if (kitId != null) {
            dao.getAllEventKitsWithItems().firstOrNull { it.kit.id == id }?.kit?.createdAt ?: now
        } else {
            now
        }
        val kit = EventKitEntity(
            id = id,
            name = name.trim().ifEmpty { "Event Kit" },
            description = description.trim(),
            category = category.trim().ifEmpty { "Event Package" },
            discountPercent = discountPercent.coerceIn(0.0, 90.0),
            createdAt = existingCreatedAt,
            updatedAt = now,
        )
        val kitItems = lines
            .filter { it.first.isNotBlank() && it.third > 0.0 }
            .groupBy { it.first }
            .values
            .map { group ->
                val first = group.first()
                EventKitItemEntity(
                    id = newId(),
                    kitId = id,
                    itemId = first.first,
                    itemName = first.second,
                    qty = group.sumOf { it.third },
                )
            }
        db.withTransaction {
            dao.upsertEventKit(kit)
            dao.deleteEventKitItemsForKit(id)
            if (kitItems.isNotEmpty()) {
                dao.insertEventKitItems(kitItems)
            }
        }
        return EventKitWithItems(kit, kitItems)
    }

    suspend fun deleteEventKit(kitId: String) {
        db.withTransaction {
            dao.deleteEventKitItemsForKit(kitId)
            dao.deleteEventKitById(kitId)
        }
    }

    // --- Equipment Damage Report Log ---
    suspend fun saveDamageReport(
        reportId: String?,
        itemId: String,
        itemSku: String,
        itemName: String,
        unitCode: String,
        receiptId: String?,
        receiptNumber: Int?,
        customerName: String,
        severity: String,
        status: String,
        repairCostCents: Long,
        notes: String,
        photoPaths: List<String>,
        reportedBy: String,
        markUnitMaintenance: Boolean,
    ): DamageReportEntity {
        val now = nowMs()
        val id = reportId?.takeIf { it.isNotBlank() } ?: newId()
        val cleanStatus = status.ifBlank { DamageReportEntity.STATUS_OPEN }
        val resolvedAt = if (cleanStatus == DamageReportEntity.STATUS_RESOLVED) now else null
        val report = DamageReportEntity(
            id = id,
            itemId = itemId.trim(),
            itemSku = itemSku.trim(),
            itemName = itemName.trim().ifEmpty { itemId.trim() },
            unitCode = unitCode.trim(),
            receiptId = receiptId?.takeIf { it.isNotBlank() },
            receiptNumber = receiptNumber,
            customerName = customerName.trim(),
            severity = severity.ifBlank { DamageReportEntity.SEVERITY_MINOR },
            status = cleanStatus,
            repairCostCents = repairCostCents.coerceAtLeast(0L),
            notes = notes.trim(),
            photoPaths = DamageReportEntity.encodePhotoPaths(photoPaths),
            reportedBy = reportedBy.trim(),
            createdAt = if (reportId != null) {
                dao.getAllDamageReports().firstOrNull { it.id == id }?.createdAt ?: now
            } else {
                now
            },
            resolvedAt = resolvedAt,
        )
        db.withTransaction {
            dao.upsertDamageReport(report)
            if (markUnitMaintenance && unitCode.isNotBlank()) {
                val unit = dao.getEquipmentUnitByCode(unitCode.trim())
                if (unit != null) {
                    val noteSummary = notes.trim().take(120).ifEmpty { "Damage logged (${report.severity})" }
                    dao.upsertEquipmentUnit(
                        unit.copy(
                            status = EquipmentUnitEntity.STATUS_MAINTENANCE,
                            conditionNote = noteSummary,
                            lastScanAt = now,
                            updatedAt = now,
                        )
                    )
                }
            }
        }
        return report
    }

    suspend fun updateDamageReportStatus(id: String, status: String) {
        val now = nowMs()
        val resolvedAt = if (status == DamageReportEntity.STATUS_RESOLVED) now else null
        dao.updateDamageReportStatus(id, status, resolvedAt)
    }

    suspend fun deleteDamageReport(id: String, photoPaths: List<String>) {
        dao.deleteDamageReportById(id)
        withContext(Dispatchers.IO) {
            photoPaths.forEach { ItemPhotos.deleteFile(it) }
        }
    }

    // --- Storage Health, Import Replace, Reset ---
    suspend fun getDbStats(): DbStats = withContext(Dispatchers.IO) {
        val dbFile = context.getDatabasePath(LoyPosDatabase.DB_NAME)
        val sizeBytes = if (dbFile.exists()) dbFile.length() else 0L
        DbStats(
            branches = dao.countBranches(),
            categories = dao.countCategories(),
            items = dao.countItems(),
            customers = dao.countCustomers(),
            receipts = dao.countReceipts(),
            tickets = dao.countTickets(),
            movements = dao.countMovements(),
            shifts = dao.countShifts(),
            fileBytes = sizeBytes,
        )
    }

    suspend fun checkIntegrity(): String = withContext(Dispatchers.IO) {
        val writable = db.openHelper.writableDatabase
        writable.query("PRAGMA integrity_check").use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(0) ?: "unknown" else "unknown"
        }
    }

    suspend fun optimiseDatabase() = withContext(Dispatchers.IO) {
        val writable = db.openHelper.writableDatabase
        writable.execSQL("VACUUM")
        writable.execSQL("ANALYZE")
    }

    suspend fun importReplace(
        settings: Map<String, String>,
        branches: List<BranchEntity>,
        categories: List<CategoryEntity>,
        items: List<ItemEntity>,
        customers: List<CustomerEntity>,
        receipts: List<ReceiptEntity>,
        receiptLines: List<ReceiptLineEntity>,
        tickets: List<TicketEntity>,
        ticketLines: List<TicketLineEntity>,
        movements: List<StockMovementEntity>,
    ) {
        db.withTransaction {
            dao.deleteAllTicketLines()
            dao.deleteAllTickets()
            dao.deleteAllReceiptLines()
            dao.deleteAllReceipts()
            dao.deleteAllStockMovements()
            dao.deleteAllItems()
            dao.deleteAllCategories()
            dao.deleteAllCustomers()
            dao.deleteAllBranches()
            dao.deleteShopSettings()

            val skipKeys = setOf("session_user", "backoffice_session_user", "cloud_team_id")
            val settingEntities = settings.entries
                .filter { it.key !in skipKeys }
                .map { SettingEntity(it.key, it.value) }
            if (settingEntities.isNotEmpty()) dao.upsertSettings(settingEntities)

            if (branches.isNotEmpty()) {
                dao.upsertBranches(branches)
            } else {
                dao.upsertBranch(
                    BranchEntity(id = "branch-main", name = "Main branch", createdAt = nowMs())
                )
            }
            val validCategoryIds = categories.map { it.id }.toSet()
            val sanitizedItems = items.map { item ->
                if (item.categoryId != null && item.categoryId !in validCategoryIds) {
                    item.copy(categoryId = null)
                } else {
                    item
                }
            }
            val validCustomerIds = customers.map { it.id }.toSet()
            val sanitizedReceipts = receipts.map { receipt ->
                if (receipt.customerId != null && receipt.customerId !in validCustomerIds) {
                    receipt.copy(customerId = null)
                } else {
                    receipt
                }
            }

            if (categories.isNotEmpty()) categoryDao.upsertCategories(categories)
            if (sanitizedItems.isNotEmpty()) productDao.upsertProducts(sanitizedItems)
            if (customers.isNotEmpty()) dao.upsertCustomers(customers)
            if (sanitizedReceipts.isNotEmpty()) dao.upsertReceipts(sanitizedReceipts)
            if (receiptLines.isNotEmpty()) saleItemDao.insertSaleItems(receiptLines)
            if (tickets.isNotEmpty()) dao.upsertTickets(tickets)
            if (ticketLines.isNotEmpty()) dao.insertTicketLines(ticketLines)
            if (movements.isNotEmpty()) dao.insertStockMovements(movements)
        }
    }

    suspend fun resetAllShopData() {
        db.withTransaction {
            dao.deleteAllTicketLines()
            dao.deleteAllTickets()
            dao.deleteAllReceiptLines()
            dao.deleteAllReceipts()
            dao.deleteAllStockMovements()
            dao.deleteAllCashMovements()
            dao.deleteAllShifts()
            dao.deleteAllItems()
            dao.deleteAllCategories()
            dao.deleteAllCustomers()
            dao.deleteAllBranches()
            dao.deleteShopSettings()
            LoyPosDatabase.seedDefaults(db)
        }
    }

    // --- Plesk MySQL Sync Snapshot & Merge ---
    suspend fun exportPleskSyncPayload(sinceMs: Long): Pair<JSONObject, Int> = withContext(Dispatchers.IO) {
        val incremental = sinceMs > 0L
        val branches = dao.getAllBranches().let { list ->
            if (incremental) list.filter { it.updatedAt >= sinceMs || it.createdAt >= sinceMs } else list
        }
        val categories = categoryDao.getAllCategories().let { list ->
            if (incremental) list.filter { it.updatedAt >= sinceMs } else list
        }
        val products = productDao.getAllProducts().let { list ->
            if (incremental) list.filter { it.updatedAt >= sinceMs } else list
        }
        val customers = dao.getAllCustomers().let { list ->
            if (incremental) list.filter { it.updatedAt >= sinceMs || it.createdAt >= sinceMs } else list
        }
        val sales = saleDao.getAllSales().let { list ->
            if (incremental) list.filter { it.updatedAt >= sinceMs || it.createdAt >= sinceMs } else list
        }
        val saleIds = sales.map { it.id }.toSet()
        val allSaleItems = dao.getAllReceiptLines()
        val saleItems = if (incremental) allSaleItems.filter { it.receiptId in saleIds } else allSaleItems
        val movements = dao.getAllStockMovements().let { list ->
            if (incremental) list.filter { it.createdAt >= sinceMs } else list
        }
        val shifts = dao.getAllShifts().let { list ->
            if (incremental) list.filter { it.openedAt >= sinceMs || (it.closedAt ?: 0L) >= sinceMs } else list
        }
        val cashMoves = dao.getAllCashMovements().let { list ->
            if (incremental) list.filter { it.createdAt >= sinceMs } else list
        }
        val syncableSettingKeys = setOf(
            "store_name",
            "currency_symbol",
            "tax_rate",
            "tax_included",
            "receipt_footer",
            "next_receipt_number",
            "low_stock_alerts_enabled",
            "low_stock_threshold",
            "low_stock_interval_minutes",
            "return_reminder_alerts_enabled",
            "daily_revenue_target_cents",
            "active_branch",
        )
        val settings = dao.getAllSettings().filter { it.key in syncableSettingKeys }

        val root = JSONObject().apply {
            put("since_ms", sinceMs)
            put("client_time_ms", nowMs())
            put("branches", JSONArray().apply {
                branches.forEach { b ->
                    put(JSONObject().apply {
                        put("id", b.id)
                        put("name", b.name)
                        put("address", b.address)
                        put("phone", b.phone)
                        put("created_at", b.createdAt)
                        put("archived", if (b.archived) 1 else 0)
                        put("updated_at", b.updatedAt)
                    })
                }
            })
            put("categories", JSONArray().apply {
                categories.forEach { c ->
                    put(JSONObject().apply {
                        put("id", c.id)
                        put("name", c.name)
                        put("position", c.position)
                        put("updated_at", c.updatedAt)
                    })
                }
            })
            put("products", JSONArray().apply {
                products.forEach { p ->
                    put(JSONObject().apply {
                        put("id", p.id)
                        put("category_id", p.categoryId ?: JSONObject.NULL)
                        put("name", p.name)
                        put("price_cents", p.priceCents)
                        put("cost_cents", p.costCents)
                        put("sku", p.sku)
                        put("barcode", p.barcode)
                        put("track_stock", if (p.trackStock) 1 else 0)
                        put("stock", p.stock)
                        put("archived", if (p.archived) 1 else 0)
                        put("branch_ids", p.branchIds)
                        put("image_path", p.imagePath)
                        put("safety_threshold", p.safetyThreshold ?: JSONObject.NULL)
                        put("updated_at", p.updatedAt)
                    })
                }
            })
            put("customers", JSONArray().apply {
                customers.forEach { c ->
                    put(JSONObject().apply {
                        put("id", c.id)
                        put("name", c.name)
                        put("phone", c.phone)
                        put("email", c.email)
                        put("note", c.note)
                        put("points", c.points)
                        put("created_at", c.createdAt)
                        put("updated_at", c.updatedAt)
                    })
                }
            })
            put("sales", JSONArray().apply {
                sales.forEach { s ->
                    put(JSONObject().apply {
                        put("id", s.id)
                        put("number", s.number)
                        put("created_at", s.createdAt)
                        put("subtotal_cents", s.subtotalCents)
                        put("discount_cents", s.discountCents)
                        put("discount_type", s.discountType ?: JSONObject.NULL)
                        put("discount_value", s.discountValue ?: JSONObject.NULL)
                        put("tax_cents", s.taxCents)
                        put("total_cents", s.totalCents)
                        put("paid_cents", s.paidCents)
                        put("change_cents", s.changeCents)
                        put("payment_type", s.paymentType)
                        put("customer_id", s.customerId ?: JSONObject.NULL)
                        put("customer_name", s.customerName ?: JSONObject.NULL)
                        put("status", s.status)
                        put("note", s.note)
                        put("branch_id", s.branchId ?: JSONObject.NULL)
                        put("cashier_id", s.cashierId ?: JSONObject.NULL)
                        put("cashier_name", s.cashierName ?: JSONObject.NULL)
                        put("updated_at", s.updatedAt)
                        put("rental_days", s.rentalDays)
                        put("rental_start_ms", s.rentalStartMs)
                        put("rental_end_ms", s.rentalEndMs)
                        put("deposit_cents", s.depositCents)
                        put("rental_status", s.rentalStatus)
                        put("returned_at_ms", s.returnedAtMs ?: JSONObject.NULL)
                        put("event_name", s.eventName)
                        put("event_venue", s.eventVenue)
                    })
                }
            })
            put("sale_items", JSONArray().apply {
                saleItems.forEach { li ->
                    put(JSONObject().apply {
                        put("id", li.id)
                        put("receipt_id", li.receiptId)
                        put("item_id", li.itemId)
                        put("name", li.name)
                        put("price_cents", li.priceCents)
                        put("qty", li.qty)
                        put("line_total_cents", li.lineTotalCents)
                    })
                }
            })
            put("stock_movements", JSONArray().apply {
                movements.forEach { m ->
                    put(JSONObject().apply {
                        put("id", m.id)
                        put("item_id", m.itemId)
                        put("qty", m.qty)
                        put("type", m.type)
                        put("created_at", m.createdAt)
                        put("note", m.note)
                        put("receipt_id", m.receiptId ?: JSONObject.NULL)
                    })
                }
            })
            put("shifts", JSONArray().apply {
                shifts.forEach { sh ->
                    put(JSONObject().apply {
                        put("id", sh.id)
                        put("opened_at", sh.openedAt)
                        put("opening_float_cents", sh.openingFloatCents)
                        put("branch_id", sh.branchId ?: JSONObject.NULL)
                        put("closed_at", sh.closedAt ?: JSONObject.NULL)
                        put("counted_cents", sh.countedCents ?: JSONObject.NULL)
                        put("expected_cents", sh.expectedCents ?: JSONObject.NULL)
                    })
                }
            })
            put("cash_movements", JSONArray().apply {
                cashMoves.forEach { cm ->
                    put(JSONObject().apply {
                        put("id", cm.id)
                        put("shift_id", cm.shiftId)
                        put("type", cm.type)
                        put("amount_cents", cm.amountCents)
                        put("note", cm.note)
                        put("created_at", cm.createdAt)
                    })
                }
            })
            put("settings", JSONArray().apply {
                settings.forEach { st ->
                    put(JSONObject().apply {
                        put("setting_key", st.key)
                        put("setting_value", st.value)
                    })
                }
            })
        }

        val count = branches.size + categories.size + products.size + customers.size +
            sales.size + saleItems.size + movements.size + shifts.size + cashMoves.size
        root to count
    }

    suspend fun applyPleskPullPayload(payload: JSONObject): Int = withContext(Dispatchers.IO) {
        var applied = 0
        db.withTransaction {
            val branchesArr = payload.optJSONArray("branches") ?: JSONArray()
            val localBranches = dao.getAllBranches().associateBy { it.id }
            val branchesToUpsert = mutableListOf<BranchEntity>()
            for (i in 0 until branchesArr.length()) {
                val o = branchesArr.getJSONObject(i)
                val id = o.getString("id")
                val remoteUpdated = o.optLong("updated_at", 0L)
                val existing = localBranches[id]
                if (existing == null || remoteUpdated >= existing.updatedAt) {
                    branchesToUpsert += BranchEntity(
                        id = id,
                        name = o.getString("name"),
                        address = o.optString("address", ""),
                        phone = o.optString("phone", ""),
                        createdAt = o.optLong("created_at", nowMs()),
                        archived = o.optInt("archived", 0) == 1 || o.optBoolean("archived", false),
                        updatedAt = remoteUpdated,
                    )
                }
            }
            if (branchesToUpsert.isNotEmpty()) {
                dao.upsertBranches(branchesToUpsert)
                applied += branchesToUpsert.size
            }

            val catsArr = payload.optJSONArray("categories") ?: JSONArray()
            val localCats = categoryDao.getAllCategories().associateBy { it.id }
            val catsToUpsert = mutableListOf<CategoryEntity>()
            for (i in 0 until catsArr.length()) {
                val o = catsArr.getJSONObject(i)
                val id = o.getString("id")
                val remoteUpdated = o.optLong("updated_at", 0L)
                val existing = localCats[id]
                if (existing == null || remoteUpdated >= existing.updatedAt) {
                    catsToUpsert += CategoryEntity(
                        id = id,
                        name = o.getString("name"),
                        position = o.optInt("position", 0),
                        updatedAt = remoteUpdated,
                    )
                }
            }
            if (catsToUpsert.isNotEmpty()) {
                categoryDao.upsertCategories(catsToUpsert)
                applied += catsToUpsert.size
            }

            val validCatIds = categoryDao.getAllCategories().map { it.id }.toSet()
            val prodsArr = payload.optJSONArray("products") ?: JSONArray()
            val localProds = productDao.getAllProducts().associateBy { it.id }
            val prodsToUpsert = mutableListOf<ProductEntity>()
            for (i in 0 until prodsArr.length()) {
                val o = prodsArr.getJSONObject(i)
                val id = o.getString("id")
                val remoteUpdated = o.optLong("updated_at", 0L)
                val existing = localProds[id]
                if (existing == null || remoteUpdated >= existing.updatedAt) {
                    val catId = o.optString("category_id", "").takeIf { it.isNotEmpty() && it in validCatIds }
                    prodsToUpsert += ProductEntity(
                        id = id,
                        categoryId = catId,
                        name = o.getString("name"),
                        priceCents = o.optLong("price_cents", 0L),
                        costCents = o.optLong("cost_cents", 0L),
                        sku = o.optString("sku", ""),
                        barcode = o.optString("barcode", ""),
                        trackStock = o.optInt("track_stock", 0) == 1 || o.optBoolean("track_stock", false),
                        stock = o.optDouble("stock", 0.0),
                        archived = o.optInt("archived", 0) == 1 || o.optBoolean("archived", false),
                        branchIds = o.optString("branch_ids", ""),
                        imagePath = existing?.imagePath ?: o.optString("image_path", ""),
                        safetyThreshold = if (o.has("safety_threshold") && !o.isNull("safety_threshold")) {
                            o.optDouble("safety_threshold").coerceAtLeast(0.0)
                        } else {
                            existing?.safetyThreshold
                        },
                        updatedAt = remoteUpdated,
                    )
                }
            }
            if (prodsToUpsert.isNotEmpty()) {
                productDao.upsertProducts(prodsToUpsert)
                applied += prodsToUpsert.size
            }

            val custArr = payload.optJSONArray("customers") ?: JSONArray()
            val localCusts = dao.getAllCustomers().associateBy { it.id }
            val custsToUpsert = mutableListOf<CustomerEntity>()
            for (i in 0 until custArr.length()) {
                val o = custArr.getJSONObject(i)
                val id = o.getString("id")
                val remoteUpdated = o.optLong("updated_at", 0L)
                val existing = localCusts[id]
                if (existing == null || remoteUpdated >= existing.updatedAt) {
                    custsToUpsert += CustomerEntity(
                        id = id,
                        name = o.getString("name"),
                        phone = o.optString("phone", ""),
                        email = o.optString("email", ""),
                        note = o.optString("note", ""),
                        points = o.optInt("points", 0),
                        createdAt = o.optLong("created_at", nowMs()),
                        updatedAt = remoteUpdated,
                    )
                }
            }
            if (custsToUpsert.isNotEmpty()) {
                dao.upsertCustomers(custsToUpsert)
                applied += custsToUpsert.size
            }

            val validCustIds = dao.getAllCustomers().map { it.id }.toSet()
            val salesArr = payload.optJSONArray("sales") ?: JSONArray()
            val localSales = saleDao.getAllSales().associateBy { it.id }
            val salesToUpsert = mutableListOf<ReceiptEntity>()
            for (i in 0 until salesArr.length()) {
                val o = salesArr.getJSONObject(i)
                val id = o.getString("id")
                val remoteUpdated = o.optLong("updated_at", 0L)
                val existing = localSales[id]
                if (existing == null || remoteUpdated >= existing.updatedAt) {
                    val custId = o.optString("customer_id", "").takeIf { it.isNotEmpty() && it in validCustIds }
                    val createdAt = o.optLong("created_at", nowMs())
                    val rDays = o.optInt("rental_days", 1).coerceAtLeast(1)
                    val rStart = o.optLong("rental_start_ms", createdAt).let { if (it <= 0L) createdAt else it }
                    val rEnd = o.optLong("rental_end_ms", rStart + rDays * 86_400_000L).let { if (it <= 0L) rStart + rDays * 86_400_000L else it }
                    val statusVal = o.optString("status", "completed")
                    salesToUpsert += ReceiptEntity(
                        id = id,
                        number = o.optInt("number", 1),
                        createdAt = createdAt,
                        subtotalCents = o.optLong("subtotal_cents", 0L),
                        discountCents = o.optLong("discount_cents", 0L),
                        discountType = o.optString("discount_type", "").takeIf { it.isNotEmpty() },
                        discountValue = if (o.isNull("discount_value")) null else o.optDouble("discount_value"),
                        taxCents = o.optLong("tax_cents", 0L),
                        totalCents = o.optLong("total_cents", 0L),
                        paidCents = o.optLong("paid_cents", 0L),
                        changeCents = o.optLong("change_cents", 0L),
                        paymentType = o.optString("payment_type", "cash"),
                        customerId = custId,
                        customerName = o.optString("customer_name", "").takeIf { it.isNotEmpty() },
                        status = statusVal,
                        note = o.optString("note", ""),
                        branchId = o.optString("branch_id", "").takeIf { it.isNotEmpty() },
                        cashierId = o.optString("cashier_id", "").takeIf { it.isNotEmpty() },
                        cashierName = o.optString("cashier_name", "").takeIf { it.isNotEmpty() },
                        updatedAt = remoteUpdated,
                        rentalDays = rDays,
                        rentalStartMs = rStart,
                        rentalEndMs = rEnd,
                        depositCents = o.optLong("deposit_cents", 0L),
                        rentalStatus = o.optString("rental_status", if (statusVal == "refunded") "refunded" else "out"),
                        returnedAtMs = if (o.isNull("returned_at_ms")) null else o.optLong("returned_at_ms"),
                        eventName = o.optString("event_name", ""),
                        eventVenue = o.optString("event_venue", ""),
                    )
                }
            }
            if (salesToUpsert.isNotEmpty()) {
                dao.upsertReceipts(salesToUpsert)
                applied += salesToUpsert.size
            }

            val validSaleIds = saleDao.getAllSales().map { it.id }.toSet()
            val saleItemsArr = payload.optJSONArray("sale_items") ?: JSONArray()
            val linesToInsert = mutableListOf<ReceiptLineEntity>()
            for (i in 0 until saleItemsArr.length()) {
                val o = saleItemsArr.getJSONObject(i)
                val receiptId = o.getString("receipt_id")
                if (receiptId in validSaleIds) {
                    linesToInsert += ReceiptLineEntity(
                        id = o.getString("id"),
                        receiptId = receiptId,
                        itemId = o.getString("item_id"),
                        name = o.getString("name"),
                        priceCents = o.optLong("price_cents", 0L),
                        qty = o.optDouble("qty", 1.0),
                        lineTotalCents = o.optLong("line_total_cents", 0L),
                    )
                }
            }
            if (linesToInsert.isNotEmpty()) {
                saleItemDao.insertSaleItems(linesToInsert)
                applied += linesToInsert.size
            }

            val movesArr = payload.optJSONArray("stock_movements") ?: JSONArray()
            val movesToInsert = mutableListOf<StockMovementEntity>()
            for (i in 0 until movesArr.length()) {
                val o = movesArr.getJSONObject(i)
                movesToInsert += StockMovementEntity(
                    id = o.getString("id"),
                    itemId = o.getString("item_id"),
                    qty = o.optDouble("qty", 0.0),
                    type = o.optString("type", "adjust"),
                    createdAt = o.optLong("created_at", nowMs()),
                    note = o.optString("note", ""),
                    receiptId = o.optString("receipt_id", "").takeIf { it.isNotEmpty() },
                )
            }
            if (movesToInsert.isNotEmpty()) {
                dao.insertStockMovements(movesToInsert)
                applied += movesToInsert.size
            }

            val shiftsArr = payload.optJSONArray("shifts") ?: JSONArray()
            val shiftsToUpsert = mutableListOf<ShiftEntity>()
            for (i in 0 until shiftsArr.length()) {
                val o = shiftsArr.getJSONObject(i)
                shiftsToUpsert += ShiftEntity(
                    id = o.getString("id"),
                    openedAt = o.optLong("opened_at", nowMs()),
                    openingFloatCents = o.optLong("opening_float_cents", 0L),
                    branchId = o.optString("branch_id", "").takeIf { it.isNotEmpty() },
                    closedAt = if (o.isNull("closed_at")) null else o.optLong("closed_at"),
                    countedCents = if (o.isNull("counted_cents")) null else o.optLong("counted_cents"),
                    expectedCents = if (o.isNull("expected_cents")) null else o.optLong("expected_cents"),
                )
            }
            if (shiftsToUpsert.isNotEmpty()) {
                dao.upsertShifts(shiftsToUpsert)
                applied += shiftsToUpsert.size
            }

            val cashArr = payload.optJSONArray("cash_movements") ?: JSONArray()
            val cashToInsert = mutableListOf<CashMovementEntity>()
            for (i in 0 until cashArr.length()) {
                val o = cashArr.getJSONObject(i)
                cashToInsert += CashMovementEntity(
                    id = o.getString("id"),
                    shiftId = o.getString("shift_id"),
                    type = o.optString("type", "in"),
                    amountCents = o.optLong("amount_cents", 0L),
                    note = o.optString("note", ""),
                    createdAt = o.optLong("created_at", nowMs()),
                )
            }
            if (cashToInsert.isNotEmpty()) {
                dao.insertCashMovements(cashToInsert)
                applied += cashToInsert.size
            }
        }
        applied
    }

    suspend fun saveMaintenanceSchedule(schedule: MaintenanceScheduleEntity) {
        dao.upsertMaintenanceSchedule(schedule)
    }

    suspend fun deleteMaintenanceSchedule(id: String) {
        dao.deleteMaintenanceScheduleById(id)
    }

    suspend fun updateMaintenanceStatus(id: String, status: String, lastServicedMs: Long, nextDueMs: Long) {
        dao.updateMaintenanceScheduleStatus(id, status, lastServicedMs, nextDueMs)
    }

    suspend fun completeMaintenance(id: String) {
        val list = dao.getAllMaintenanceSchedules()
        val item = list.firstOrNull { it.id == id } ?: return
        val now = nowMs()
        val nextDue = now + item.intervalDays.toLong().coerceAtLeast(1L) * 86_400_000L
        dao.updateMaintenanceScheduleStatus(id, MaintenanceScheduleEntity.STATUS_COMPLETED, now, nextDue)
    }

    suspend fun getItemRentalCount(itemId: String): Int = dao.getRentalCountForItem(itemId)

    suspend fun saveDeliveryStop(stop: DeliveryStopEntity) {
        dao.upsertDeliveryStop(stop)
    }

    suspend fun saveDeliveryStops(stops: List<DeliveryStopEntity>) {
        if (stops.isNotEmpty()) dao.upsertDeliveryStops(stops)
    }

    suspend fun deleteDeliveryStop(id: String) {
        dao.deleteDeliveryStopById(id)
    }

    suspend fun getDeliveryStopsForReceipt(receiptId: String): List<DeliveryStopEntity> =
        dao.getDeliveryStopsForReceipt(receiptId)

    suspend fun updateDeliveryStopStatus(id: String, status: String) {
        val completedAt = if (status == DeliveryStopEntity.STATUS_DONE || status == DeliveryStopEntity.STATUS_FAILED) nowMs() else null
        dao.updateDeliveryStopStatus(id, status, completedAt)
    }
}

