package com.radarfinanceiro.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "merchant_mappings")
data class MerchantMapping(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val merchantPattern: String,     // Padrao do nome (ex: "PADARIA SANTA CRUZ")
    val category: String,            // Categoria aprendida
    val customName: String = "",     // Nome amigavel dado pelo usuario
    val timesUsed: Int = 1,          // Quantas vezes foi classificado assim
    val lastUsed: Long = System.currentTimeMillis()
)
