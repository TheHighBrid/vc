package com.melato.shop.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.melato.shop.data.ShopifyRepo
import com.melato.shop.ui.components.ProductCard
import com.melato.shop.ui.nav.Screen
import com.melato.shop.ui.theme.*

@Composable
fun HomeScreen(
    cartCount: Int,
    onProductClick: (String) -> Unit,
    onCartClick: () -> Unit,
    onShopClick: () -> Unit
) {
    val featured = ShopifyRepo.getFeatured()
    val newArrivals = ShopifyRepo.getNew()
    val hero = featured.firstOrNull()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(NearBlack),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item {
            // Top bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "MELATO",
                    style = MaterialTheme.typography.headlineLarge.copy(
                        letterSpacing = 5.sp,
                        fontWeight = FontWeight.Black
                    )
                )
                BadgedBox(
                    badge = {
                        if (cartCount > 0) {
                            Badge(containerColor = Gold, contentColor = Black) {
                                Text(cartCount.toString(), style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                ) {
                    IconButton(onClick = onCartClick) {
                        Icon(
                            Icons.Outlined.ShoppingBag,
                            contentDescription = "Cart",
                            tint = TextPrimary,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }
            }
        }

        // Hero banner
        if (hero != null) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(480.dp)
                        .padding(horizontal = 20.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .clickable { onProductClick(hero.id) }
                ) {
                    AsyncImage(
                        model = hero.imageUrl,
                        contentDescription = hero.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        androidx.compose.ui.graphics.Color.Transparent,
                                        NearBlack.copy(alpha = 0.85f)
                                    ),
                                    startY = 200f
                                )
                            )
                    )
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(24.dp)
                    ) {
                        if (hero.isNew) {
                            Surface(
                                color = Gold,
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    "NEW DROP",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Black,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                            Spacer(Modifier.height(10.dp))
                        }
                        Text(
                            text = hero.title,
                            style = MaterialTheme.typography.displaySmall,
                            color = White
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "$${String.format("%.0f", hero.price)} CAD",
                            style = MaterialTheme.typography.titleLarge,
                            color = Gold
                        )
                        Spacer(Modifier.height(16.dp))
                        Button(
                            onClick = { onProductClick(hero.id) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = White,
                                contentColor = Black
                            ),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 28.dp, vertical = 12.dp)
                        ) {
                            Text(
                                "SHOP NOW",
                                style = MaterialTheme.typography.labelLarge.copy(letterSpacing = 1.5.sp)
                            )
                        }
                    }
                }
                Spacer(Modifier.height(32.dp))
            }
        }

        // New Arrivals
        if (newArrivals.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("NEW ARRIVALS", style = MaterialTheme.typography.labelLarge.copy(letterSpacing = 2.sp))
                    Text(
                        "See all",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Gold,
                        modifier = Modifier.clickable(onClick = onShopClick)
                    )
                }
                Spacer(Modifier.height(14.dp))
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(newArrivals) { product ->
                        ProductCard(
                            product = product,
                            onClick = { onProductClick(product.id) },
                            modifier = Modifier.width(200.dp),
                            imageHeight = 260.dp
                        )
                    }
                }
                Spacer(Modifier.height(32.dp))
            }
        }

        // Categories row
        item {
            Text(
                "COLLECTIONS",
                style = MaterialTheme.typography.labelLarge.copy(letterSpacing = 2.sp),
                modifier = Modifier.padding(horizontal = 20.dp)
            )
            Spacer(Modifier.height(14.dp))
            LazyRow(
                contentPadding = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(ShopifyRepo.categories.drop(1)) { cat ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .border(1.dp, Divider, RoundedCornerShape(10.dp))
                            .clickable(onClick = onShopClick)
                            .padding(horizontal = 18.dp, vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(cat.emoji, fontSize = 16.sp)
                            Text(
                                cat.name.uppercase(),
                                style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 1.sp),
                                color = TextSecondary
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(32.dp))
        }

        // Featured section title
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("FEATURED", style = MaterialTheme.typography.labelLarge.copy(letterSpacing = 2.sp))
                Text(
                    "View all",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Gold,
                    modifier = Modifier.clickable(onClick = onShopClick)
                )
            }
            Spacer(Modifier.height(14.dp))
        }

        // Featured grid (2 columns)
        val featuredChunked = featured.drop(1).chunked(2)
        items(featuredChunked) { row ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                row.forEach { product ->
                    ProductCard(
                        product = product,
                        onClick = { onProductClick(product.id) },
                        modifier = Modifier.weight(1f),
                        imageHeight = 220.dp
                    )
                }
                if (row.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
            Spacer(Modifier.height(12.dp))
        }

        item { Spacer(Modifier.height(16.dp)) }
    }
}
