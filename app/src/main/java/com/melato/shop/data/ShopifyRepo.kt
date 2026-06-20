package com.melato.shop.data

import com.melato.shop.data.model.Category
import com.melato.shop.data.model.Product

object ShopifyRepo {

    val categories = listOf(
        Category("all", "All", "✦"),
        Category("tracksuits", "Tracksuits", "🔥"),
        Category("tops", "Tops", "👕"),
        Category("bottoms", "Bottoms", "👖"),
        Category("accessories", "Accessories", "🧢")
    )

    val products = listOf(
        Product(
            id = "1",
            title = "MELATO Track Noir",
            price = 148.00,
            description = "Our signature all-black tracksuit. Crafted from premium French terry cotton with a subtle brushed interior. Relaxed silhouette, tapered leg, side-zip pockets. Made in Canada.",
            imageUrl = "https://images.unsplash.com/photo-1515886657613-9f3515b0c78f?w=600&q=80",
            category = "tracksuits",
            colors = listOf("Black"),
            isFeatured = true,
            isNew = true,
            tags = listOf("Bestseller")
        ),
        Product(
            id = "2",
            title = "MELATO Essential Grey",
            price = 138.00,
            description = "The essential grey set. Ultra-soft midweight fleece, contrast drawstring, embossed MELATO wordmark at chest. Versatile enough for the streets, comfortable enough for home.",
            imageUrl = "https://images.unsplash.com/photo-1552902865-b72c031ac5ea?w=600&q=80",
            category = "tracksuits",
            colors = listOf("Heather Grey", "Ash Grey"),
            isFeatured = true,
            tags = listOf("Core Collection")
        ),
        Product(
            id = "3",
            title = "MELATO Velour Classic",
            price = 168.00,
            description = "Luxe velour construction with a vintage-inspired fit. The Classic feels as premium as it looks — our most elevated tracksuit to date.",
            imageUrl = "https://images.unsplash.com/photo-1542291026-7eec264c27ff?w=600&q=80",
            category = "tracksuits",
            colors = listOf("Midnight Navy", "Deep Burgundy", "Forest Green"),
            isNew = true,
            tags = listOf("Premium")
        ),
        Product(
            id = "4",
            title = "MELATO Winter Fleece",
            price = 172.00,
            description = "Heavyweight sherpa-lined fleece built for Canadian winters. Extended hem, ribbed cuffs, kangaroo pocket. Built to last — season after season.",
            imageUrl = "https://images.unsplash.com/photo-1620012253295-c15cc3e65df4?w=600&q=80",
            category = "tracksuits",
            colors = listOf("Cream", "Oatmeal", "Charcoal"),
            isFeatured = true,
            tags = listOf("Winter Drop")
        ),
        Product(
            id = "5",
            title = "MELATO Sport Elite",
            price = 152.00,
            description = "Performance meets style. Moisture-wicking 4-way stretch fabric with contrast piping. Ideal for active days without sacrificing the aesthetic.",
            imageUrl = "https://images.unsplash.com/photo-1556306535-38febf6d5183?w=600&q=80",
            category = "tracksuits",
            colors = listOf("Black/Red", "Black/White", "Grey/Gold"),
            tags = listOf("Active")
        ),
        Product(
            id = "6",
            title = "MELATO Urban Edition",
            price = 144.00,
            description = "Earth-toned urban tracksuit with an oversized top and slim-tapered bottom. The Urban Edition is built for the city — wherever you take it.",
            imageUrl = "https://images.unsplash.com/photo-1529139574466-a303027bc851?w=600&q=80",
            category = "tracksuits",
            colors = listOf("Olive", "Stone", "Rust"),
            isNew = true,
            tags = listOf("New Arrival")
        ),
        Product(
            id = "7",
            title = "MELATO Signature Hoodie",
            price = 88.00,
            description = "The iconic MELATO hoodie. Heavyweight French terry, dropped shoulder, oversized fit with a clean minimal logo. A wardrobe essential.",
            imageUrl = "https://images.unsplash.com/photo-1564584217132-2271feaeb3c5?w=600&q=80",
            category = "tops",
            colors = listOf("Black", "White", "Grey", "Olive"),
            isFeatured = true,
            tags = listOf("Bestseller")
        ),
        Product(
            id = "8",
            title = "MELATO Track Pant",
            price = 72.00,
            description = "The standalone track pant. Elastic waist, tapered leg, two side pockets and one back pocket. Mix and match with any MELATO top.",
            imageUrl = "https://images.unsplash.com/photo-1594938298603-c8148c4b4abe?w=600&q=80",
            category = "bottoms",
            colors = listOf("Black", "Grey", "Navy", "Olive"),
            tags = listOf("Separates")
        )
    )

    fun getProductById(id: String) = products.find { it.id == id }

    fun getByCategory(categoryId: String) =
        if (categoryId == "all") products else products.filter { it.category == categoryId }

    fun getFeatured() = products.filter { it.isFeatured }

    fun getNew() = products.filter { it.isNew }
}
