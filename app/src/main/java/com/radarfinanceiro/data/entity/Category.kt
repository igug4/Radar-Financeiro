package com.radarfinanceiro.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "categories")
data class Category(
    @PrimaryKey
    val name: String,
    val icon: String,       // Nome do icone Material
    val color: Long,        // Cor em ARGB
    val sortOrder: Int,
    val isActive: Boolean = true
)
