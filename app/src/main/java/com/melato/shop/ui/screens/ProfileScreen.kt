package com.melato.shop.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.melato.shop.ui.theme.*

@Composable
fun ProfileScreen() {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(NearBlack),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.height(24.dp))
                Box(
                    modifier = Modifier
                        .size(88.dp)
                        .clip(CircleShape)
                        .background(Surface2)
                        .border(2.dp, Gold, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Outlined.Person,
                        contentDescription = null,
                        tint = Gold,
                        modifier = Modifier.size(40.dp)
                    )
                }
                Spacer(Modifier.height(16.dp))
                Text(
                    "MELATO MEMBER",
                    style = MaterialTheme.typography.labelLarge.copy(letterSpacing = 3.sp),
                    color = Gold
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Sign in to track orders and more",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextMuted
                )
                Spacer(Modifier.height(24.dp))

                Button(
                    onClick = {},
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = Black)
                ) {
                    Text("SIGN IN", style = MaterialTheme.typography.labelLarge.copy(letterSpacing = 2.sp))
                }
                Spacer(Modifier.height(10.dp))
                OutlinedButton(
                    onClick = {},
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary)
                ) {
                    Text("CREATE ACCOUNT", style = MaterialTheme.typography.labelLarge.copy(letterSpacing = 2.sp))
                }
            }
        }

        item {
            Divider(color = Divider, modifier = Modifier.padding(vertical = 8.dp))
            ProfileSection("ACCOUNT") {
                ProfileRow(Icons.Outlined.ShoppingBag, "My Orders")
                ProfileRow(Icons.Outlined.FavoriteBorder, "Wishlist")
                ProfileRow(Icons.Outlined.LocationOn, "Addresses")
            }
        }

        item {
            Divider(color = Divider, modifier = Modifier.padding(vertical = 8.dp))
            ProfileSection("SUPPORT") {
                ProfileRow(Icons.Outlined.Info, "Size Guide")
                ProfileRow(Icons.Outlined.LocalShipping, "Shipping & Returns")
                ProfileRow(Icons.Outlined.Email, "Contact Us")
            }
        }

        item {
            Divider(color = Divider, modifier = Modifier.padding(vertical = 8.dp))
            ProfileSection("ABOUT") {
                ProfileRow(Icons.Outlined.Language, "melato.ca")
                ProfileRow(Icons.Outlined.Shield, "Privacy Policy")
                ProfileRow(Icons.Outlined.Article, "Terms of Service")
            }
            Spacer(Modifier.height(24.dp))
            Text(
                "MELATO  ·  CANADA  ·  EST. 2024",
                style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 2.sp),
                color = TextMuted,
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentWidth(Alignment.CenterHorizontally)
            )
        }
    }
}

@Composable
private fun ProfileSection(title: String, content: @Composable () -> Unit) {
    Column(modifier = Modifier.padding(horizontal = 20.dp)) {
        Spacer(Modifier.height(16.dp))
        Text(
            title,
            style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 2.sp),
            color = TextMuted
        )
        Spacer(Modifier.height(12.dp))
        Surface(
            color = Surface1,
            shape = RoundedCornerShape(12.dp)
        ) {
            Column { content() }
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun ProfileRow(icon: ImageVector, label: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(icon, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(20.dp))
            Text(label, style = MaterialTheme.typography.bodyLarge, color = TextPrimary)
        }
        Icon(Icons.Outlined.ChevronRight, contentDescription = null, tint = TextMuted, modifier = Modifier.size(18.dp))
    }
}
