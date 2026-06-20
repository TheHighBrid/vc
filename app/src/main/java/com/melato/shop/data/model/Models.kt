package com.melato.shop.data.model

data class Product(
    val id: String,
    val title: String,
    val price: Double,
    val currency: String = "CAD",
    val description: String,
    val imageUrl: String,
    val category: String,
    val sizes: List<String> = listOf("XS", "S", "M", "L", "XL", "XXL"),
    val colors: List<String> = listOf("Black", "Grey", "Navy"),
    val isFeatured: Boolean = false,
    val isNew: Boolean = false,
    val tags: List<String> = emptyList()
)

data class CartItem(
    val product: Product,
    val size: String,
    val color: String,
    var quantity: Int = 1
)

data class Category(
    val id: String,
    val name: String,
    val emoji: String
)
