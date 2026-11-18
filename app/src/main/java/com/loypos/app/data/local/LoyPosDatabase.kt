package com.loypos.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.loypos.app.core.DemoCatalog
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
    ],
    version = 6,
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

        fun getInstance(context: Context): LoyPosDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    LoyPosDatabase::class.java,
                    DB_NAME,
                )
                    .addMigrations(MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6)
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
        }
    }
}
