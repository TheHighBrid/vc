package com.melato.shop.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.melato.shop.data.ShopifyRepo
import com.melato.shop.ui.components.ProductCard
import com.melato.shop.ui.theme.*

@Composable
fun ShopScreen(
    onProductClick: (String) -> Unit
) {
    var selectedCategory by remember { mutableStateOf("all") }
    val products = remember(selectedCategory) { ShopifyRepo.getByCategory(selectedCategory) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(NearBlack),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                Text(
                    "SHOP",
                    style = MaterialTheme.typography.headlineLarge.copy(letterSpacing = 4.sp)
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "${products.size} pieces",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextMuted
                )
            }
        }

        item {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(ShopifyRepo.categories) { cat ->
                    val selected = cat.id == selectedCategory
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (selected) Gold else Surface2)
                            .border(
                                width = 1.dp,
                                color = if (selected) Gold else Divider,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable { selectedCategory = cat.id }
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (cat.id == "all") "ALL" else cat.name.uppercase(),
                            style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 1.sp),
                            color = if (selected) Black else TextSecondary
                        )
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
        }

        val chunked = products.chunked(2)
        items(chunked) { row ->
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
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}
