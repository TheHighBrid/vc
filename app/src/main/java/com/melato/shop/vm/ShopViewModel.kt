package com.melato.shop.vm

import androidx.lifecycle.ViewModel
import com.melato.shop.data.ShopifyRepo
import com.melato.shop.data.model.CartItem
import com.melato.shop.data.model.Product
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class ShopViewModel : ViewModel() {

    private val _cart = MutableStateFlow<List<CartItem>>(emptyList())
    val cart: StateFlow<List<CartItem>> = _cart.asStateFlow()

    private val _selectedCategory = MutableStateFlow("all")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    val categories = ShopifyRepo.categories
    val allProducts = ShopifyRepo.products
    val featuredProducts = ShopifyRepo.getFeatured()
    val newProducts = ShopifyRepo.getNew()

    fun getProductsByCategory(categoryId: String) = ShopifyRepo.getByCategory(categoryId)

    fun selectCategory(id: String) { _selectedCategory.value = id }

    fun getProduct(id: String): Product? = ShopifyRepo.getProductById(id)

    fun addToCart(product: Product, size: String, color: String) {
        _cart.update { current ->
            val existing = current.find { it.product.id == product.id && it.size == size && it.color == color }
            if (existing != null) {
                current.map {
                    if (it.product.id == product.id && it.size == size && it.color == color)
                        it.copy(quantity = it.quantity + 1)
                    else it
                }
            } else {
                current + CartItem(product, size, color)
            }
        }
    }

    fun removeFromCart(item: CartItem) {
        _cart.update { it.filter { c -> c != item } }
    }

    fun updateQuantity(item: CartItem, delta: Int) {
        _cart.update { current ->
            current.mapNotNull {
                if (it == item) {
                    val newQty = it.quantity + delta
                    if (newQty <= 0) null else it.copy(quantity = newQty)
                } else it
            }
        }
    }

    fun cartCount() = _cart.value.sumOf { it.quantity }

    fun cartTotal() = _cart.value.sumOf { it.product.price * it.quantity }
}
