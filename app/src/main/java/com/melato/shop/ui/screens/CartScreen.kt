package com.melato.shop.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.melato.shop.data.model.CartItem
import com.melato.shop.ui.theme.*

@Composable
fun CartScreen(
    cartItems: List<CartItem>,
    total: Double,
    onRemove: (CartItem) -> Unit,
    onQuantityChange: (CartItem, Int) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NearBlack)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Text(
                "CART",
                style = MaterialTheme.typography.headlineLarge.copy(letterSpacing = 4.sp)
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "${cartItems.sumOf { it.quantity }} item${if (cartItems.sumOf { it.quantity } != 1) "s" else ""}",
                style = MaterialTheme.typography.bodyMedium,
                color = TextMuted
            )
        }

        if (cartItems.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("YOUR CART IS EMPTY", style = MaterialTheme.typography.labelLarge.copy(letterSpacing = 2.sp), color = TextMuted)
                    Spacer(Modifier.height(8.dp))
                    Text("Add some pieces to get started.", style = MaterialTheme.typography.bodyMedium, color = TextMuted)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(cartItems) { item ->
                    Surface(
                        color = Surface1,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(100.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AsyncImage(
                                model = item.product.imageUrl,
                                contentDescription = item.product.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .width(80.dp)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp))
                            )
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    item.product.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    maxLines = 1
                                )
                                Text(
                                    "${item.size}  ·  ${item.color}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextMuted
                                )
                                Row(
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        "$${String.format("%.0f", item.product.price * item.quantity)} CAD",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = Gold
                                    )
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(26.dp)
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(Surface3)
                                                .clickable { onQuantityChange(item, -1) },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(Icons.Filled.Remove, null, tint = TextSecondary, modifier = Modifier.size(14.dp))
                                        }
                                        Text(
                                            item.quantity.toString(),
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                        )
                                        Box(
                                            modifier = Modifier
                                                .size(26.dp)
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(Surface3)
                                                .clickable { onQuantityChange(item, 1) },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(Icons.Filled.Add, null, tint = TextSecondary, modifier = Modifier.size(14.dp))
                                        }
                                    }
                                }
                            }
                            IconButton(onClick = { onRemove(item) }) {
                                Icon(Icons.Outlined.Delete, contentDescription = "Remove", tint = TextMuted, modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                }
                item { Spacer(Modifier.height(8.dp)) }
            }

            // Summary + checkout
            Surface(
                color = Surface1,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .padding(20.dp)
                        .navigationBarsPadding()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("SUBTOTAL", style = MaterialTheme.typography.labelLarge.copy(letterSpacing = 1.sp), color = TextSecondary)
                        Text("$${String.format("%.2f", total)} CAD", style = MaterialTheme.typography.titleLarge, color = TextPrimary)
                    }
                    Spacer(Modifier.height(4.dp))
                    Text("Shipping calculated at checkout", style = MaterialTheme.typography.bodyMedium, color = TextMuted)
                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = {},
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = Black)
                    ) {
                        Text("CHECKOUT", style = MaterialTheme.typography.labelLarge.copy(letterSpacing = 2.sp))
                    }
                }
            }
        }
    }
}
