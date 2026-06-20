package com.melato.shop.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.melato.shop.data.model.Product
import com.melato.shop.ui.theme.*

@Composable
fun ProductDetailScreen(
    product: Product,
    onBack: () -> Unit,
    onAddToCart: (String, String) -> Unit
) {
    var selectedSize by remember { mutableStateOf(product.sizes.getOrElse(2) { product.sizes.first() }) }
    var selectedColor by remember { mutableStateOf(product.colors.first()) }
    var addedToCart by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(NearBlack)
    ) {
        item {
            // Hero image
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(500.dp)
            ) {
                AsyncImage(
                    model = product.imageUrl,
                    contentDescription = product.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    NearBlack.copy(alpha = 0.4f),
                                    androidx.compose.ui.graphics.Color.Transparent,
                                    NearBlack.copy(alpha = 0.2f)
                                )
                            )
                        )
                )
                // Back button
                Box(
                    modifier = Modifier
                        .statusBarsPadding()
                        .padding(16.dp)
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Surface1.copy(alpha = 0.85f))
                        .clickable(onClick = onBack)
                        .align(Alignment.TopStart),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = TextPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                // Wishlist
                Box(
                    modifier = Modifier
                        .statusBarsPadding()
                        .padding(16.dp)
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Surface1.copy(alpha = 0.85f))
                        .align(Alignment.TopEnd),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Outlined.FavoriteBorder,
                        contentDescription = "Wishlist",
                        tint = TextPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                if (product.isNew) {
                    Surface(
                        modifier = Modifier
                            .padding(16.dp)
                            .align(Alignment.BottomStart),
                        color = Gold,
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            "NEW DROP",
                            style = MaterialTheme.typography.labelMedium,
                            color = Black,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }
            }
        }

        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 20.dp)
            ) {
                // Title + price
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = product.title,
                            style = MaterialTheme.typography.headlineLarge
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = product.category.replaceFirstChar { it.uppercase() },
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextMuted
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "$${String.format("%.0f", product.price)}",
                            style = MaterialTheme.typography.headlineMedium,
                            color = Gold
                        )
                        Text(
                            text = "CAD",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextMuted
                        )
                    }
                }

                Spacer(Modifier.height(24.dp))
                Divider(color = Divider)
                Spacer(Modifier.height(24.dp))

                // Color picker
                Text(
                    "COLOUR",
                    style = MaterialTheme.typography.labelLarge.copy(letterSpacing = 2.sp),
                    color = TextSecondary
                )
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    product.colors.forEach { color ->
                        val isSelected = color == selectedColor
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) Surface3 else Surface2)
                                .border(
                                    width = if (isSelected) 1.5.dp else 1.dp,
                                    color = if (isSelected) Gold else Divider,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clickable { selectedColor = color }
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = color,
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (isSelected) TextPrimary else TextSecondary
                            )
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))

                // Size picker
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "SIZE",
                        style = MaterialTheme.typography.labelLarge.copy(letterSpacing = 2.sp),
                        color = TextSecondary
                    )
                    Text(
                        "Size Guide",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Gold
                    )
                }
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    product.sizes.forEach { size ->
                        val isSelected = size == selectedSize
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) Gold else Surface2)
                                .border(
                                    width = if (isSelected) 0.dp else 1.dp,
                                    color = Divider,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clickable { selectedSize = size },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = size,
                                style = MaterialTheme.typography.labelLarge,
                                color = if (isSelected) Black else TextSecondary
                            )
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))
                Divider(color = Divider)
                Spacer(Modifier.height(20.dp))

                // Description
                Text(
                    "DESCRIPTION",
                    style = MaterialTheme.typography.labelLarge.copy(letterSpacing = 2.sp),
                    color = TextSecondary
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    text = product.description,
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextSecondary
                )

                Spacer(Modifier.height(32.dp))

                // Add to cart
                Button(
                    onClick = {
                        onAddToCart(selectedSize, selectedColor)
                        addedToCart = true
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (addedToCart) Surface3 else Gold,
                        contentColor = if (addedToCart) TextPrimary else Black
                    )
                ) {
                    Text(
                        text = if (addedToCart) "✓  ADDED TO CART" else "ADD TO CART",
                        style = MaterialTheme.typography.labelLarge.copy(letterSpacing = 2.sp)
                    )
                }

                Spacer(Modifier.height(12.dp))

                OutlinedButton(
                    onClick = {},
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = ButtonDefaults.outlinedButtonBorder.copy(width = 1.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary)
                ) {
                    Text(
                        "BUY NOW",
                        style = MaterialTheme.typography.labelLarge.copy(letterSpacing = 2.sp)
                    )
                }

                Spacer(Modifier.height(40.dp))
            }
        }
    }
}
