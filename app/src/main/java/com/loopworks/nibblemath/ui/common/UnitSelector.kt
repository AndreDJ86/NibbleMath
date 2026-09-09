package com.loopworks.nibblemath.ui.common

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
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
import com.loopworks.nibblemath.core.units.Unit as NibbleUnit

@Composable
fun UnitSelector(selected: NibbleUnit, onUnitChange: (NibbleUnit) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Row(verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(
            value = selected.symbol,
            onValueChange = {},
            readOnly = true,
            label = { Text("Unit") },
            modifier = Modifier
                .weight(1f)
                .clickable { expanded = true },
        )
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
