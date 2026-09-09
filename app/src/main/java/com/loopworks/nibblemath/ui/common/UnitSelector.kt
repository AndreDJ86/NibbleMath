package com.loopworks.nibblemath.ui.common

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.loopworks.nibblemath.core.units.Unit as NibbleUnit

@Composable
fun UnitSelector(
    selected: NibbleUnit,
    onUnitChange: (NibbleUnit) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    Row(modifier = modifier.height(64.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.weight(1f)) {
            OutlinedTextField(
                value = selected.symbol,
                onValueChange = {},
                readOnly = true,
                label = { Text("Unit") },
                modifier = Modifier.fillMaxWidth(),
            )
            Box(modifier = Modifier.fillMaxSize().clickable { expanded = true })
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            NibbleUnit.entries.forEach { unit ->
                DropdownMenuItem(
                    text = { Text(unit.symbol) },
                    onClick = {
                        onUnitChange(unit)
                        expanded = false
                    },
                )
            }
        }
    }
}
