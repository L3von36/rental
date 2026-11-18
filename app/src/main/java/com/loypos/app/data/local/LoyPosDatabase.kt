package com.loypos.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.loypos.app.core.DemoCatalog
import com.loypos.app.core.QrCodeUtils
import com.loypos.app.core.RentalAgreementEngine
import com.loypos.app.core.nowMs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        UserEntity::class,
        BackOfficeUserEntity::class,
        BranchEntity::class,
        CategoryEntity::class,
        ProductEntity::class,
        CustomerEntity::class,
        SaleEntity::class,
        SaleItemEntity::class,
        TicketEntity::class,
        TicketLineEntity::class,
        StockMovementEntity::class,
        ShiftEntity::class,
        CashMovementEntity::class,
        SettingEntity::class,
        EquipmentUnitEntity::class,
        EquipmentScanLogEntity::class,
        AgreementTemplateEntity::class,
        EventKitEntity::class,
        EventKitItemEntity::class,
        DamageReportEntity::class,
        MaintenanceScheduleEntity::class,
    ],
    version = 10,
    exportSchema = false,
)
abstract class LoyPosDatabase : RoomDatabase() {
    abstract fun dao(): LoyPosDao
    abstract fun productDao(): ProductDao
    abstract fun categoryDao(): CategoryDao
    abstract fun saleDao(): SaleDao
    abstract fun saleItemDao(): SaleItemDao
    abstract fun backOfficeUserDao(): BackOfficeUserDao

    companion object {
        const val DB_NAME = "loypos.db"

        @Volatile
        private var INSTANCE: LoyPosDatabase? = null

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE INDEX IF NOT EXISTS index_products_sku ON products(sku)")
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS backoffice_users (
                        id TEXT NOT NULL PRIMARY KEY,
                        email TEXT NOT NULL,
                        full_name TEXT NOT NULL,
                        shop_name TEXT NOT NULL,
                        password_hash TEXT NOT NULL,
                        salt TEXT NOT NULL,
                        role TEXT NOT NULL,
                        active INTEGER NOT NULL,
                        created_at INTEGER NOT NULL,
                        updated_at INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_backoffice_users_email ON backoffice_users(email)")
            }
        }

        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE sales ADD COLUMN rental_days INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE sales ADD COLUMN rental_start_ms INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE sales ADD COLUMN rental_end_ms INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE sales ADD COLUMN deposit_cents INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE sales ADD COLUMN rental_status TEXT NOT NULL DEFAULT 'out'")
                db.execSQL("ALTER TABLE sales ADD COLUMN returned_at_ms INTEGER")
                db.execSQL("ALTER TABLE sales ADD COLUMN event_name TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE sales ADD COLUMN event_venue TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE tickets ADD COLUMN rental_days INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE tickets ADD COLUMN rental_start_ms INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE tickets ADD COLUMN rental_end_ms INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE tickets ADD COLUMN deposit_cents INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE tickets ADD COLUMN event_name TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE tickets ADD COLUMN event_venue TEXT NOT NULL DEFAULT ''")
            }
        }

        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS equipment_units (
                        id TEXT NOT NULL PRIMARY KEY,
                        item_id TEXT NOT NULL,
                        item_name TEXT NOT NULL,
                        unit_code TEXT NOT NULL,
                        qr_payload TEXT NOT NULL,
                        status TEXT NOT NULL,
                        current_receipt_id TEXT,
                        current_event_name TEXT NOT NULL,
                        current_customer_name TEXT NOT NULL,
                        condition_note TEXT NOT NULL,
                        last_scan_at INTEGER NOT NULL,
                        updated_at INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_equipment_units_item_id ON equipment_units(item_id)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_equipment_units_unit_code ON equipment_units(unit_code)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS equipment_scan_logs (
                        id TEXT NOT NULL PRIMARY KEY,
                        unit_id TEXT,
                        unit_code TEXT NOT NULL,
                        item_id TEXT NOT NULL,
                        item_name TEXT NOT NULL,
                        action TEXT NOT NULL,
                        receipt_id TEXT,
                        event_name TEXT NOT NULL,
                        customer_name TEXT NOT NULL,
                        operator_name TEXT NOT NULL,
                        note TEXT NOT NULL,
                        created_at INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_equipment_scan_logs_unit_id ON equipment_scan_logs(unit_id)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_equipment_scan_logs_item_id ON equipment_scan_logs(item_id)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_equipment_scan_logs_created_at ON equipment_scan_logs(created_at)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS agreement_templates (
                        id TEXT NOT NULL PRIMARY KEY,
                        title TEXT NOT NULL,
                        category TEXT NOT NULL,
                        body TEXT NOT NULL,
                        is_default INTEGER NOT NULL,
                        created_at INTEGER NOT NULL,
                        updated_at INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        private val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE products ADD COLUMN safety_threshold REAL")
            }
        }

        private val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS event_kits (
                        id TEXT NOT NULL PRIMARY KEY,
                        name TEXT NOT NULL,
                        category TEXT NOT NULL,
                        description TEXT NOT NULL,
                        discount_percent REAL NOT NULL,
                        created_at INTEGER NOT NULL,
                        updated_at INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS event_kit_items (
                        id TEXT NOT NULL PRIMARY KEY,
                        kit_id TEXT NOT NULL,
                        item_id TEXT NOT NULL,
                        item_name TEXT NOT NULL,
                        qty REAL NOT NULL,
                        FOREIGN KEY(kit_id) REFERENCES event_kits(id) ON DELETE CASCADE ON UPDATE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_event_kit_items_kit_id ON event_kit_items(kit_id)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_event_kit_items_item_id ON event_kit_items(item_id)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS damage_reports (
                        id TEXT NOT NULL PRIMARY KEY,
                        item_id TEXT NOT NULL,
                        item_sku TEXT NOT NULL,
                        item_name TEXT NOT NULL,
                        unit_code TEXT NOT NULL,
                        receipt_id TEXT,
                        receipt_number INTEGER,
                        customer_name TEXT NOT NULL,
                        severity TEXT NOT NULL,
                        status TEXT NOT NULL,
                        repair_cost_cents INTEGER NOT NULL,
                        notes TEXT NOT NULL,
                        photo_paths TEXT NOT NULL,
                        reported_by TEXT NOT NULL,
                        created_at INTEGER NOT NULL,
                        resolved_at INTEGER
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_damage_reports_item_id ON damage_reports(item_id)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_damage_reports_receipt_id ON damage_reports(receipt_id)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_damage_reports_created_at ON damage_reports(created_at)")
            }
        }

        private val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS maintenance_schedules (
                        id TEXT NOT NULL PRIMARY KEY,
                        item_id TEXT NOT NULL,
                        item_name TEXT NOT NULL,
                        service_title TEXT NOT NULL,
                        interval_days INTEGER NOT NULL,
                        interval_rentals INTEGER NOT NULL,
                        last_serviced_ms INTEGER NOT NULL,
                        next_due_ms INTEGER NOT NULL,
                        status TEXT NOT NULL,
                        priority TEXT NOT NULL,
                        technician_name TEXT NOT NULL,
                        notes TEXT NOT NULL,
                        created_at INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_maintenance_schedules_item_id ON maintenance_schedules(item_id)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_maintenance_schedules_created_at ON maintenance_schedules(created_at)")
            }
        }

        fun getInstance(context: Context): LoyPosDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    LoyPosDatabase::class.java,
                    DB_NAME,
                )
                    .addMigrations(MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10)
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .addCallback(object : Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            CoroutineScope(Dispatchers.IO).launch {
                                seedDefaults(getInstance(context))
                            }
                        }
                    })
                    .build()
                    .also { INSTANCE = it }
            }
        }

        suspend fun seedDefaults(db: LoyPosDatabase) {
            val dao = db.dao()
            val now = nowMs()
            val defaults = SettingsModel()
            dao.upsertSettings(
                listOf(
                    SettingEntity("store_name", defaults.storeName),
                    SettingEntity("currency_symbol", defaults.currencySymbol),
                    SettingEntity("tax_rate", defaults.taxRatePercent.toString()),
                    SettingEntity("tax_included", defaults.taxIncluded.toString()),
                    SettingEntity("receipt_footer", defaults.receiptFooter),
                    SettingEntity("next_receipt_number", defaults.nextReceiptNumber.toString()),
                    SettingEntity("low_stock_alerts_enabled", defaults.lowStockAlertsEnabled.toString()),
                    SettingEntity("low_stock_threshold", defaults.lowStockThreshold.toString()),
                    SettingEntity("active_branch", "branch-main"),
                )
            )
            dao.upsertBranch(
                BranchEntity(
                    id = "branch-main",
                    name = "Main branch",
                    createdAt = now,
                    archived = false,
                    updatedAt = 0L,
                )
            )
            db.categoryDao().upsertCategories(DemoCatalog.categories)
            db.productDao().upsertProducts(DemoCatalog.items)
            dao.upsertCustomer(DemoCatalog.defaultCustomer(now))
            seedDefaultUnitsAndTemplates(db, now)
        }

        suspend fun seedDefaultUnitsAndTemplates(db: LoyPosDatabase, now: Long = nowMs()) {
            val dao = db.dao()
            if (dao.countAgreementTemplates() == 0) {
                dao.upsertAgreementTemplates(RentalAgreementEngine.defaultTemplates(now))
            }
            if (dao.countEventKits() == 0) {
                DemoCatalog.defaultEventKits(now).forEach { entry ->
                    dao.upsertEventKit(entry.kit)
                    dao.insertEventKitItems(entry.items)
                }
            }
            if (dao.countMaintenanceSchedules() == 0) {
                DemoCatalog.defaultMaintenanceSchedules(now).forEach { schedule ->
                    dao.upsertMaintenanceSchedule(schedule)
                }
            }
            if (dao.getAllEquipmentUnits().isEmpty()) {
                val items = db.productDao().getAllProducts().filter { !it.archived }
                val initialUnits = items.flatMap { item ->
                    val count = item.stock.toInt().coerceIn(1, 3)
                    (1..count).map { idx ->
                        val unitCode = QrCodeUtils.buildDefaultUnitCode(item.sku, item.id, idx)
                        EquipmentUnitEntity(
                            id = "unit-${item.id}-$idx",
                            itemId = item.id,
                            itemName = item.name,
                            unitCode = unitCode,
                            qrPayload = QrCodeUtils.buildUnitPayload(item.id, unitCode),
                            status = EquipmentUnitEntity.STATUS_CHECKED_IN,
                            updatedAt = now,
                        )
                    }
                }
                if (initialUnits.isNotEmpty()) {
                    dao.upsertEquipmentUnits(initialUnits)
                }
            }
        }
    }
}
