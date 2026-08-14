package com.radarfinanceiro.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.radarfinanceiro.data.dao.CategoryDao
import com.radarfinanceiro.data.dao.MerchantMappingDao
import com.radarfinanceiro.data.dao.TransactionDao
import com.radarfinanceiro.data.entity.Category
import com.radarfinanceiro.data.entity.MerchantMapping
import com.radarfinanceiro.data.entity.Transaction
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [Transaction::class, MerchantMapping::class, Category::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun transactionDao(): TransactionDao
    abstract fun merchantMappingDao(): MerchantMappingDao
    abstract fun categoryDao(): CategoryDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "radar_financeiro.db"
                )
                    .addCallback(object : Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            CoroutineScope(Dispatchers.IO).launch {
                                getInstance(context).categoryDao().insertAll(defaultCategories())
                                getInstance(context).merchantMappingDao().let { dao ->
                                    defaultMerchantMappings().forEach { dao.insert(it) }
                                }
                            }
                        }
                    })
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private fun defaultCategories(): List<Category> = listOf(
            Category("Alimentacao", "ShoppingCart", 0xFF4CAF50, 1),
            Category("Combustivel", "LocalGasStation", 0xFFFF9800, 2),
            Category("Saude", "LocalHospital", 0xFFF44336, 3),
            Category("Restaurante", "Restaurant", 0xFFE91E63, 4),
            Category("Vestuario", "Checkroom", 0xFF9C27B0, 5),
            Category("Educacao", "School", 0xFF3F51B5, 6),
            Category("Casa", "Home", 0xFF795548, 7),
            Category("Transporte", "DirectionsCar", 0xFF607D8B, 8),
            Category("Viagem", "Flight", 0xFF00BCD4, 9),
            Category("Servicos", "Build", 0xFF009688, 10),
            Category("Lazer", "SportsEsports", 0xFFFF5722, 11),
            Category("Compras Online", "ShoppingBag", 0xFF673AB7, 12),
            Category("Luciana", "Person", 0xFFEC407A, 13),
            Category("PIX", "SwapHoriz", 0xFF2196F3, 14),
            Category("Outros", "MoreHoriz", 0xFF9E9E9E, 15)
        )

        private fun defaultMerchantMappings(): List<MerchantMapping> = listOf(
            // Alimentacao / Supermercado
            MerchantMapping(merchantPattern = "PADARIA", category = "Alimentacao"),
            MerchantMapping(merchantPattern = "SUPERMERCADO", category = "Alimentacao"),
            MerchantMapping(merchantPattern = "SUPER SO", category = "Alimentacao"),
            MerchantMapping(merchantPattern = "SACOLAO", category = "Alimentacao"),
            MerchantMapping(merchantPattern = "HORTIFRUTI", category = "Alimentacao"),
            MerchantMapping(merchantPattern = "ACOUGUE", category = "Alimentacao"),
            MerchantMapping(merchantPattern = "QUITANDA", category = "Alimentacao"),
            MerchantMapping(merchantPattern = "PIT STOP MERC", category = "Alimentacao"),
            MerchantMapping(merchantPattern = "PRODUTOS CASEIROS", category = "Alimentacao"),

            // Combustivel
            MerchantMapping(merchantPattern = "POSTO", category = "Combustivel"),
            MerchantMapping(merchantPattern = "AutoPosto", category = "Combustivel"),
            MerchantMapping(merchantPattern = "WR GAS", category = "Combustivel"),

            // Saude
            MerchantMapping(merchantPattern = "DROGASIL", category = "Saude"),
            MerchantMapping(merchantPattern = "DROGA", category = "Saude"),
            MerchantMapping(merchantPattern = "FARMACIA", category = "Saude"),
            MerchantMapping(merchantPattern = "SOURIR", category = "Saude"),

            // Restaurante
            MerchantMapping(merchantPattern = "PIZZA", category = "Restaurante"),
            MerchantMapping(merchantPattern = "HOT DOG", category = "Restaurante"),
            MerchantMapping(merchantPattern = "FAST FOOD", category = "Restaurante"),
            MerchantMapping(merchantPattern = "LANCHE", category = "Restaurante"),
            MerchantMapping(merchantPattern = "BAR ", category = "Restaurante"),
            MerchantMapping(merchantPattern = "ACAI", category = "Restaurante"),
            MerchantMapping(merchantPattern = "FORNERIA", category = "Restaurante"),
            MerchantMapping(merchantPattern = "PEIXE DOURADO", category = "Restaurante"),
            MerchantMapping(merchantPattern = "PAYGO", category = "Restaurante"),
            MerchantMapping(merchantPattern = "COFFE MIX", category = "Restaurante"),
            MerchantMapping(merchantPattern = "EMPADAO", category = "Restaurante"),

            // Vestuario
            MerchantMapping(merchantPattern = "AREZZO", category = "Vestuario"),
            MerchantMapping(merchantPattern = "NJACK", category = "Vestuario"),
            MerchantMapping(merchantPattern = "N JACK", category = "Vestuario"),
            MerchantMapping(merchantPattern = "LUMAVIL", category = "Vestuario"),
            MerchantMapping(merchantPattern = "MODA PRAI", category = "Vestuario"),
            MerchantMapping(merchantPattern = "MEIAS", category = "Vestuario"),
            MerchantMapping(merchantPattern = "CEA", category = "Vestuario"),
            MerchantMapping(merchantPattern = "LUNDGRE", category = "Vestuario"),

            // Educacao
            MerchantMapping(merchantPattern = "CNA", category = "Educacao"),
            MerchantMapping(merchantPattern = "PAPELARIA", category = "Educacao"),
            MerchantMapping(merchantPattern = "OAB", category = "Educacao"),

            // Viagem
            MerchantMapping(merchantPattern = "AZUL", category = "Viagem"),
            MerchantMapping(merchantPattern = "azulvia", category = "Viagem"),

            // Servicos
            MerchantMapping(merchantPattern = "VIVO", category = "Servicos"),
            MerchantMapping(merchantPattern = "BARBEARIA", category = "Servicos"),
            MerchantMapping(merchantPattern = "FLORICULTURA", category = "Servicos"),

            // Transporte
            MerchantMapping(merchantPattern = "MOTORTEC", category = "Transporte"),
            MerchantMapping(merchantPattern = "CENTER BIKE", category = "Transporte"),
            MerchantMapping(merchantPattern = "AutoParts", category = "Transporte"),
            MerchantMapping(merchantPattern = "BIKE", category = "Transporte"),

            // Casa
            MerchantMapping(merchantPattern = "MADEIRA", category = "Casa"),
            MerchantMapping(merchantPattern = "MAGAZINE", category = "Casa"),
            MerchantMapping(merchantPattern = "UTILIDADES", category = "Casa"),
            MerchantMapping(merchantPattern = "2000Tintas", category = "Casa"),
            MerchantMapping(merchantPattern = "LISTA MATERIAL", category = "Casa"),

            // Compras Online
            MerchantMapping(merchantPattern = "AMAZON", category = "Compras Online"),
            MerchantMapping(merchantPattern = "MERCADO LIVRE", category = "Compras Online"),
            MerchantMapping(merchantPattern = "ESFERA", category = "Compras Online"),

            // Lazer
            MerchantMapping(merchantPattern = "TATAMED", category = "Lazer"),
            MerchantMapping(merchantPattern = "NR ACA", category = "Lazer"),
        )
    }
}
