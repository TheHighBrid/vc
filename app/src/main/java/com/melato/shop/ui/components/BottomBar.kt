package com.melato.shop.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.melato.shop.ui.nav.Screen
import com.melato.shop.ui.theme.*

data class NavItem(
    val screen: Screen,
    val label: String,
    val icon: ImageVector,
    val selectedIcon: ImageVector
)

val navItems = listOf(
    NavItem(Screen.Home, "Home", Icons.Outlined.Home, Icons.Filled.Home),
    NavItem(Screen.Shop, "Shop", Icons.Outlined.Search, Icons.Filled.Search),
    NavItem(Screen.Cart, "Cart", Icons.Outlined.ShoppingBag, Icons.Filled.ShoppingBag),
    NavItem(Screen.Profile, "Profile", Icons.Outlined.Person, Icons.Filled.Person)
)

@Composable
fun MelatoBottomBar(
    currentRoute: String?,
    cartCount: Int,
    onNavigate: (Screen) -> Unit
) {
    Surface(
        color = Surface1,
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .height(64.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            navItems.forEach { item ->
                val selected = currentRoute == item.screen.route
                val tint by animateColorAsState(
                    targetValue = if (selected) Gold else TextMuted,
                    label = "nav_tint"
                )
                IconButton(
                    onClick = { onNavigate(item.screen) },
                    modifier = Modifier.size(56.dp)
                ) {
                    BadgedBox(
                        badge = {
                            if (item.screen == Screen.Cart && cartCount > 0) {
                                Badge(
                                    containerColor = Gold,
                                    contentColor = Black
                                ) {
                                    Text(
                                        text = cartCount.coerceAtMost(99).toString(),
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                }
                            }
                        }
                    ) {
                        Icon(
                            imageVector = if (selected) item.selectedIcon else item.icon,
                            contentDescription = item.label,
                            tint = tint,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }
            }
        }
    }
}
