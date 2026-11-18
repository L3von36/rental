package com.loypos.app.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductDao {
    // Read
    @Query("SELECT * FROM products WHERE archived = 0 ORDER BY name ASC")
    fun observeProducts(): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE archived = 0 AND category_id = :categoryId ORDER BY name ASC")
    fun observeProductsByCategory(categoryId: String): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE archived = 0 AND (name LIKE '%' || :query || '%' OR sku LIKE '%' || :query || '%' OR barcode LIKE '%' || :query || '%') ORDER BY name ASC")
    fun searchProducts(query: String): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products ORDER BY name ASC")
    suspend fun getAllProducts(): List<ProductEntity>

    @Query("SELECT * FROM products WHERE id = :id LIMIT 1")
    suspend fun getProductById(id: String): ProductEntity?

    @Query("SELECT * FROM products WHERE archived = 0 AND sku = :sku LIMIT 1")
    suspend fun getProductBySku(sku: String): ProductEntity?

    @Query(
        "SELECT * FROM products WHERE archived = 0 AND track_stock = 1 " +
            "AND stock < COALESCE(safety_threshold, :threshold) " +
            "ORDER BY (stock - COALESCE(safety_threshold, :threshold)) ASC, stock ASC, name ASC"
    )
    suspend fun getLowStockProducts(threshold: Double): List<ProductEntity>

    // Create / Upsert
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProduct(product: ProductEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProduct(product: ProductEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProducts(products: List<ProductEntity>)

    // Update
    @Update
    suspend fun updateProduct(product: ProductEntity)

    @Query("UPDATE products SET stock = :stock, updated_at = :updatedAt WHERE id = :id")
    suspend fun updateProductStock(id: String, stock: Double, updatedAt: Long)

    @Query("UPDATE products SET safety_threshold = :safetyThreshold, updated_at = :updatedAt WHERE id = :id")
    suspend fun updateSafetyThreshold(id: String, safetyThreshold: Double?, updatedAt: Long)

    @Query("UPDATE products SET stock = stock + :delta, updated_at = :updatedAt WHERE id = :id AND track_stock = 1")
    suspend fun adjustProductStock(id: String, delta: Double, updatedAt: Long)

    // Delete / Archive
    @Delete
    suspend fun deleteProduct(product: ProductEntity)

    @Query("UPDATE products SET archived = 1, updated_at = :updatedAt WHERE id = :id")
    suspend fun archiveProduct(id: String, updatedAt: Long)

    @Query("DELETE FROM products WHERE id = :id")
    suspend fun deleteProductById(id: String)

    @Query("DELETE FROM products")
    suspend fun deleteAllProducts()
}

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories ORDER BY position ASC, name ASC")
    fun observeCategories(): Flow<List<CategoryEntity>>

    @Transaction
    @Query("SELECT * FROM categories ORDER BY position ASC, name ASC")
    fun observeCategoriesWithProducts(): Flow<List<CategoryWithProducts>>

    @Query("SELECT * FROM categories ORDER BY position ASC, name ASC")
    suspend fun getAllCategories(): List<CategoryEntity>

    @Query("SELECT * FROM categories WHERE id = :id LIMIT 1")
    suspend fun getCategoryById(id: String): CategoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: CategoryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCategory(category: CategoryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCategories(categories: List<CategoryEntity>)

    @Update
    suspend fun updateCategory(category: CategoryEntity)

    @Delete
    suspend fun deleteCategory(category: CategoryEntity)

    @Query("DELETE FROM categories WHERE id = :id")
    suspend fun deleteCategoryById(id: String)
}

@Dao
interface SaleDao {
    @Query("SELECT * FROM sales ORDER BY created_at DESC")
    fun observeSales(): Flow<List<SaleEntity>>

    @Query("SELECT * FROM sales WHERE status = 'completed' AND created_at >= :sinceMs ORDER BY created_at ASC")
    fun observeCompletedSalesSince(sinceMs: Long): Flow<List<SaleEntity>>

    @Query("SELECT * FROM sale_items")
    fun observeSaleItems(): Flow<List<SaleItemEntity>>

    @Transaction
    @Query("SELECT * FROM sales ORDER BY created_at DESC")
    fun observeSalesWithItems(): Flow<List<SaleWithItems>>

    @Transaction
    @Query("SELECT * FROM sales WHERE id = :saleId LIMIT 1")
    suspend fun getSaleWithItems(saleId: String): SaleWithItems?

    @Transaction
    @Query("SELECT * FROM sales WHERE created_at >= :startMs AND created_at < :endMs ORDER BY created_at ASC")
    suspend fun getSalesWithItemsInRange(startMs: Long, endMs: Long): List<SaleWithItems>

    @Query("SELECT * FROM sales ORDER BY created_at DESC")
    suspend fun getAllSales(): List<SaleEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSale(sale: SaleEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSale(sale: SaleEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSaleItems(items: List<SaleItemEntity>)

    @Query("UPDATE sales SET status = 'refunded', rental_status = 'refunded', updated_at = :updatedAt WHERE id = :id")
    suspend fun markSaleRefunded(id: String, updatedAt: Long)

    @Query("UPDATE sales SET rental_status = 'returned', returned_at_ms = :returnedAtMs, updated_at = :updatedAt WHERE id = :id")
    suspend fun markSaleReturned(id: String, returnedAtMs: Long, updatedAt: Long)

    @Query("DELETE FROM sales WHERE id = :id")
    suspend fun deleteSaleById(id: String)
}

@Dao
interface SaleItemDao {
    @Query("SELECT * FROM sale_items")
    fun observeSaleItems(): Flow<List<SaleItemEntity>>

    @Query("SELECT * FROM sale_items WHERE receipt_id = :saleId")
    suspend fun getSaleItemsForSale(saleId: String): List<SaleItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSaleItem(item: SaleItemEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSaleItems(items: List<SaleItemEntity>)

    @Query("DELETE FROM sale_items WHERE receipt_id = :saleId")
    suspend fun deleteSaleItemsForSale(saleId: String)
}

@Dao
interface BackOfficeUserDao {
    @Query("SELECT * FROM backoffice_users ORDER BY created_at DESC")
    fun observeBackOfficeUsers(): Flow<List<BackOfficeUserEntity>>

    @Query("SELECT * FROM backoffice_users WHERE id = :id LIMIT 1")
    suspend fun getBackOfficeUserById(id: String): BackOfficeUserEntity?

    @Query("SELECT * FROM backoffice_users WHERE email = :email LIMIT 1")
    suspend fun getBackOfficeUserByEmail(email: String): BackOfficeUserEntity?

    @Query("SELECT COUNT(*) FROM backoffice_users")
    suspend fun countBackOfficeUsers(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertBackOfficeUser(user: BackOfficeUserEntity)

    @Query("UPDATE backoffice_users SET active = :active, updated_at = :updatedAt WHERE id = :id")
    suspend fun setBackOfficeUserActive(id: String, active: Boolean, updatedAt: Long)

    @Query("DELETE FROM backoffice_users WHERE id = :id")
    suspend fun deleteBackOfficeUserById(id: String)
}

@Dao
interface LoyPosDao {
    // Settings
    @Query("SELECT * FROM settings")
    fun observeSettings(): Flow<List<SettingEntity>>

    @Query("SELECT * FROM settings")
    suspend fun getAllSettings(): List<SettingEntity>

    @Query("SELECT value FROM settings WHERE key = :key LIMIT 1")
    suspend fun getSetting(key: String): String?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSetting(setting: SettingEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSettings(settings: List<SettingEntity>)

    // Users
    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    suspend fun getUserById(id: String): UserEntity?

    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    suspend fun getUserByEmail(email: String): UserEntity?

    @Query("SELECT * FROM users WHERE role = 'cashier' ORDER BY created_at DESC")
    fun observeStaff(): Flow<List<UserEntity>>

    @Query("SELECT COUNT(*) FROM users")
    suspend fun countUsers(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertUser(user: UserEntity)

    @Query("UPDATE users SET active = :active, updated_at = :updatedAt WHERE id = :id")
    suspend fun setUserActive(id: String, active: Boolean, updatedAt: Long)

    @Query("DELETE FROM users WHERE id = :id")
    suspend fun deleteUserById(id: String)

    // Branches
    @Query("SELECT * FROM branches WHERE archived = 0 ORDER BY created_at ASC")
    fun observeActiveBranches(): Flow<List<BranchEntity>>

    @Query("SELECT * FROM branches WHERE archived = 0 ORDER BY created_at ASC")
    suspend fun getActiveBranches(): List<BranchEntity>

    @Query("SELECT * FROM branches ORDER BY created_at ASC")
    suspend fun getAllBranches(): List<BranchEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertBranch(branch: BranchEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertBranches(branches: List<BranchEntity>)

    @Query("UPDATE branches SET archived = 1, updated_at = :updatedAt WHERE id = :id")
    suspend fun archiveBranch(id: String, updatedAt: Long)

    // Categories
    @Query("SELECT * FROM categories ORDER BY position ASC, name ASC")
    fun observeCategories(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories ORDER BY position ASC, name ASC")
    suspend fun getAllCategories(): List<CategoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCategory(category: CategoryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCategories(categories: List<CategoryEntity>)

    @Query("UPDATE products SET category_id = NULL, updated_at = :updatedAt WHERE category_id = :catId")
    suspend fun clearCategoryOnItems(catId: String, updatedAt: Long)

    @Query("DELETE FROM categories WHERE id = :id")
    suspend fun deleteCategoryById(id: String)

    // Products
    @Query("SELECT * FROM products WHERE archived = 0 ORDER BY name ASC")
    fun observeActiveItems(): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products ORDER BY name ASC")
    suspend fun getAllItems(): List<ProductEntity>

    @Query("SELECT * FROM products WHERE id = :id LIMIT 1")
    suspend fun getItemById(id: String): ProductEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertItem(item: ProductEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertItems(items: List<ProductEntity>)

    @Query("UPDATE products SET stock = stock + :delta, updated_at = :updatedAt WHERE id = :id AND track_stock = 1")
    suspend fun addItemStock(id: String, delta: Double, updatedAt: Long)

    @Query("UPDATE products SET stock = :stock, updated_at = :updatedAt WHERE id = :id")
    suspend fun setItemStock(id: String, stock: Double, updatedAt: Long)

    @Query("UPDATE products SET archived = 1, updated_at = :updatedAt WHERE id = :id")
    suspend fun archiveItem(id: String, updatedAt: Long)

    // Customers
    @Query("SELECT * FROM customers ORDER BY name ASC")
    fun observeCustomers(): Flow<List<CustomerEntity>>

    @Query("SELECT * FROM customers ORDER BY name ASC")
    suspend fun getAllCustomers(): List<CustomerEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCustomer(customer: CustomerEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCustomers(customers: List<CustomerEntity>)

    @Query("UPDATE customers SET points = points + :delta, updated_at = :updatedAt WHERE id = :id")
    suspend fun addCustomerPoints(id: String, delta: Int, updatedAt: Long)

    @Query("DELETE FROM customers WHERE id = :id")
    suspend fun deleteCustomerById(id: String)

    // Sales & Sale Items
    @Query("SELECT * FROM sales ORDER BY created_at DESC")
    fun observeReceipts(): Flow<List<SaleEntity>>

    @Query("SELECT * FROM sale_items")
    fun observeAllReceiptLines(): Flow<List<SaleItemEntity>>

    @Query("SELECT * FROM sales ORDER BY created_at DESC")
    suspend fun getAllReceipts(): List<SaleEntity>

    @Query("SELECT * FROM sale_items WHERE receipt_id = :receiptId")
    suspend fun getReceiptLines(receiptId: String): List<SaleItemEntity>

    @Query("SELECT * FROM sale_items")
    suspend fun getAllReceiptLines(): List<SaleItemEntity>

    @Query("SELECT * FROM sales WHERE id = :id LIMIT 1")
    suspend fun getReceiptById(id: String): SaleEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertReceipt(receipt: SaleEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertReceipts(receipts: List<SaleEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReceiptLines(lines: List<SaleItemEntity>)

    @Query("UPDATE sales SET status = 'refunded', rental_status = 'refunded', updated_at = :updatedAt WHERE id = :id")
    suspend fun markReceiptRefunded(id: String, updatedAt: Long)

    @Query("UPDATE sales SET rental_status = 'returned', returned_at_ms = :returnedAtMs, updated_at = :updatedAt WHERE id = :id")
    suspend fun markReceiptReturned(id: String, returnedAtMs: Long, updatedAt: Long)

    // Tickets & Ticket Lines
    @Query("SELECT * FROM tickets ORDER BY created_at DESC")
    fun observeTickets(): Flow<List<TicketEntity>>

    @Query("SELECT * FROM ticket_lines")
    fun observeAllTicketLines(): Flow<List<TicketLineEntity>>

    @Query("SELECT * FROM tickets ORDER BY created_at DESC")
    suspend fun getAllTickets(): List<TicketEntity>

    @Query("SELECT * FROM ticket_lines")
    suspend fun getAllTicketLines(): List<TicketLineEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTicket(ticket: TicketEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTickets(tickets: List<TicketEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTicketLines(lines: List<TicketLineEntity>)

    @Query("DELETE FROM ticket_lines WHERE ticket_id = :ticketId")
    suspend fun deleteTicketLines(ticketId: String)

    @Query("DELETE FROM tickets WHERE id = :ticketId")
    suspend fun deleteTicketById(ticketId: String)

    // Stock Movements
    @Query("SELECT * FROM stock_movements ORDER BY created_at DESC LIMIT 80")
    fun observeRecentStockMovements(): Flow<List<StockMovementEntity>>

    @Query("SELECT * FROM stock_movements ORDER BY created_at DESC")
    suspend fun getAllStockMovements(): List<StockMovementEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStockMovement(movement: StockMovementEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStockMovements(movements: List<StockMovementEntity>)

    // Shifts & Cash Movements
    @Query("SELECT * FROM shifts ORDER BY opened_at DESC")
    fun observeShifts(): Flow<List<ShiftEntity>>

    @Query("SELECT * FROM shifts ORDER BY opened_at DESC")
    suspend fun getAllShifts(): List<ShiftEntity>

    @Query("SELECT * FROM cash_movements ORDER BY created_at DESC")
    fun observeCashMovements(): Flow<List<CashMovementEntity>>

    @Query("SELECT * FROM cash_movements ORDER BY created_at DESC")
    suspend fun getAllCashMovements(): List<CashMovementEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertShift(shift: ShiftEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertShifts(shifts: List<ShiftEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCashMovement(movement: CashMovementEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCashMovements(movements: List<CashMovementEntity>)

    @Query("UPDATE shifts SET closed_at = :closedAt, counted_cents = :countedCents, expected_cents = :expectedCents WHERE id = :id")
    suspend fun closeShift(id: String, closedAt: Long, countedCents: Long, expectedCents: Long)

    // Cleanup queries for import/reset
    @Query("DELETE FROM branches") suspend fun deleteAllBranches()
    @Query("DELETE FROM categories") suspend fun deleteAllCategories()
    @Query("DELETE FROM products") suspend fun deleteAllItems()
    @Query("DELETE FROM customers") suspend fun deleteAllCustomers()
    @Query("DELETE FROM sale_items") suspend fun deleteAllReceiptLines()
    @Query("DELETE FROM sales") suspend fun deleteAllReceipts()
    @Query("DELETE FROM ticket_lines") suspend fun deleteAllTicketLines()
    @Query("DELETE FROM tickets") suspend fun deleteAllTickets()
    @Query("DELETE FROM stock_movements") suspend fun deleteAllStockMovements()
    @Query("DELETE FROM cash_movements") suspend fun deleteAllCashMovements()
    @Query("DELETE FROM shifts") suspend fun deleteAllShifts()
    @Query("DELETE FROM settings WHERE key != 'session_user'") suspend fun deleteShopSettings()

    // Equipment Units & QR Scan Logs
    @Query("SELECT * FROM equipment_units ORDER BY item_name ASC, unit_code ASC")
    fun observeEquipmentUnits(): Flow<List<EquipmentUnitEntity>>

    @Query("SELECT * FROM equipment_units ORDER BY item_name ASC, unit_code ASC")
    suspend fun getAllEquipmentUnits(): List<EquipmentUnitEntity>

    @Query("SELECT * FROM equipment_units WHERE item_id = :itemId ORDER BY unit_code ASC")
    suspend fun getEquipmentUnitsForItem(itemId: String): List<EquipmentUnitEntity>

    @Query("SELECT * FROM equipment_units WHERE unit_code = :unitCode LIMIT 1")
    suspend fun getEquipmentUnitByCode(unitCode: String): EquipmentUnitEntity?

    @Query("SELECT * FROM equipment_units WHERE id = :id LIMIT 1")
    suspend fun getEquipmentUnitById(id: String): EquipmentUnitEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertEquipmentUnit(unit: EquipmentUnitEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertEquipmentUnits(units: List<EquipmentUnitEntity>)

    @Query("DELETE FROM equipment_units WHERE id = :id")
    suspend fun deleteEquipmentUnitById(id: String)

    @Query("SELECT * FROM equipment_scan_logs ORDER BY created_at DESC LIMIT 120")
    fun observeEquipmentScanLogs(): Flow<List<EquipmentScanLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEquipmentScanLog(log: EquipmentScanLogEntity)

    // Digital Rental Agreement Templates
    @Query("SELECT * FROM agreement_templates ORDER BY is_default DESC, updated_at DESC, title ASC")
    fun observeAgreementTemplates(): Flow<List<AgreementTemplateEntity>>

    @Query("SELECT * FROM agreement_templates ORDER BY is_default DESC, updated_at DESC, title ASC")
    suspend fun getAllAgreementTemplates(): List<AgreementTemplateEntity>

    @Query("SELECT COUNT(*) FROM agreement_templates")
    suspend fun countAgreementTemplates(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAgreementTemplate(template: AgreementTemplateEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAgreementTemplates(templates: List<AgreementTemplateEntity>)

    @Query("UPDATE agreement_templates SET is_default = CASE WHEN id = :id THEN 1 ELSE 0 END")
    suspend fun setDefaultAgreementTemplate(id: String)

    @Query("DELETE FROM agreement_templates WHERE id = :id")
    suspend fun deleteAgreementTemplateById(id: String)

    // Event Kits / Bundles
    @Transaction
    @Query("SELECT * FROM event_kits ORDER BY updated_at DESC, name ASC")
    fun observeEventKitsWithItems(): Flow<List<EventKitWithItems>>

    @Transaction
    @Query("SELECT * FROM event_kits ORDER BY updated_at DESC, name ASC")
    suspend fun getAllEventKitsWithItems(): List<EventKitWithItems>

    @Query("SELECT COUNT(*) FROM event_kits")
    suspend fun countEventKits(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertEventKit(kit: EventKitEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEventKitItems(items: List<EventKitItemEntity>)

    @Query("DELETE FROM event_kit_items WHERE kit_id = :kitId")
    suspend fun deleteEventKitItemsForKit(kitId: String)

    @Query("DELETE FROM event_kits WHERE id = :kitId")
    suspend fun deleteEventKitById(kitId: String)

    // Equipment Damage Reports
    @Query("SELECT * FROM damage_reports ORDER BY created_at DESC")
    fun observeDamageReports(): Flow<List<DamageReportEntity>>

    @Query("SELECT * FROM damage_reports ORDER BY created_at DESC")
    suspend fun getAllDamageReports(): List<DamageReportEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertDamageReport(report: DamageReportEntity)

    @Query("UPDATE damage_reports SET status = :status, resolved_at = :resolvedAt WHERE id = :id")
    suspend fun updateDamageReportStatus(id: String, status: String, resolvedAt: Long?)

    @Query("DELETE FROM damage_reports WHERE id = :id")
    suspend fun deleteDamageReportById(id: String)

    // Equipment Maintenance Schedules
    @Query("SELECT * FROM maintenance_schedules ORDER BY next_due_ms ASC, created_at DESC")
    fun observeMaintenanceSchedules(): Flow<List<MaintenanceScheduleEntity>>

    @Query("SELECT * FROM maintenance_schedules ORDER BY next_due_ms ASC, created_at DESC")
    suspend fun getAllMaintenanceSchedules(): List<MaintenanceScheduleEntity>

    @Query("SELECT * FROM maintenance_schedules WHERE item_id = :itemId ORDER BY next_due_ms ASC")
    suspend fun getMaintenanceSchedulesForItem(itemId: String): List<MaintenanceScheduleEntity>

    @Query("SELECT COUNT(*) FROM maintenance_schedules")
    suspend fun countMaintenanceSchedules(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMaintenanceSchedule(schedule: MaintenanceScheduleEntity)

    @Query("UPDATE maintenance_schedules SET status = :status, last_serviced_ms = :lastServicedMs, next_due_ms = :nextDueMs WHERE id = :id")
    suspend fun updateMaintenanceScheduleStatus(id: String, status: String, lastServicedMs: Long, nextDueMs: Long)

    @Query("DELETE FROM maintenance_schedules WHERE id = :id")
    suspend fun deleteMaintenanceScheduleById(id: String)

    @Query("SELECT COUNT(DISTINCT sales.id) FROM sale_items JOIN sales ON sale_items.receipt_id = sales.id WHERE sale_items.item_id = :itemId AND sales.status = 'completed'")
    suspend fun getRentalCountForItem(itemId: String): Int

    // Counts for DB Health
    @Query("SELECT COUNT(*) FROM branches WHERE archived = 0") suspend fun countBranches(): Int
    @Query("SELECT COUNT(*) FROM categories") suspend fun countCategories(): Int
    @Query("SELECT COUNT(*) FROM products WHERE archived = 0") suspend fun countItems(): Int
    @Query("SELECT COUNT(*) FROM customers") suspend fun countCustomers(): Int
    @Query("SELECT COUNT(*) FROM sales") suspend fun countReceipts(): Int
    @Query("SELECT COUNT(*) FROM tickets") suspend fun countTickets(): Int
    @Query("SELECT COUNT(*) FROM stock_movements") suspend fun countMovements(): Int
    @Query("SELECT COUNT(*) FROM shifts") suspend fun countShifts(): Int
}
