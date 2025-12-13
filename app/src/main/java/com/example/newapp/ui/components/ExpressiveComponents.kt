package com.example.newapp.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.newapp.ui.theme.ExpressiveMotion

@Composable
fun ExpressiveButtonGroup(
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit
) {
    // A simple implementation of a segmented-style button group
    // In a real implementation, we would manage shapes of children dynamically.
    // Here we assume the user puts buttons inside.
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp) // Expressive encourages distinct items capable of merging
    ) {
        content()
    }
}

@Composable
fun SplitButton(
    text: String,
    onClick: () -> Unit,
    onDropdownClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.height(IntrinsicSize.Min) // Match heights
    ) {
        // Main Action
        Button(
            onClick = onClick,
            shape = RoundedCornerShape(topStart = 24.dp, bottomStart = 24.dp, topEnd = 4.dp, bottomEnd = 4.dp),
            modifier = Modifier.weight(1f).fillMaxHeight()
        ) {
            Text(text)
        }
        
        Spacer(modifier = Modifier.width(2.dp))
        
        // Dropdown Action
        FilledTonalButton(
            onClick = onDropdownClick,
            shape = RoundedCornerShape(topStart = 4.dp, bottomStart = 4.dp, topEnd = 24.dp, bottomEnd = 24.dp),
            contentPadding = PaddingValues(0.dp),
            modifier = Modifier.width(48.dp).fillMaxHeight()
        ) {
            Icon(Icons.Default.ArrowDropDown, contentDescription = "More")
        }
    }
}

@Composable
fun ExpressiveFabMenu(
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val rotation by animateFloatAsState(
        targetValue = if (expanded) 45f else 0f,
        animationSpec = ExpressiveMotion.BouncySpring,
        label = "fab_rotation"
    )

    Column(
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = modifier
    ) {
        // Menu Items
        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn() + expandVertically() + scaleIn(animationSpec = ExpressiveMotion.BouncySpring),
            exit = fadeOut() + shrinkVertically() + scaleOut()
        ) {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                SmallFloatingActionButton(
                    onClick = { /* Do something */ },
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit")
                }
                SmallFloatingActionButton(
                    onClick = { /* Do something */ },
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer
                ) {
                    Icon(Icons.Default.Favorite, contentDescription = "Favorite")
                }
            }
        }

        // Main FAB
        LargeFloatingActionButton(
            onClick = { expanded = !expanded },
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            shape = if (expanded) RoundedCornerShape(16.dp) else RoundedCornerShape(24.dp) // Shape morph
        ) {
            Icon(
                Icons.Default.Add,
                contentDescription = "Menu",
                modifier = Modifier.rotate(rotation)
            )
        }
    }
}
