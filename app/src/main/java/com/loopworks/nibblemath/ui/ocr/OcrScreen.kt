package com.loopworks.nibblemath.ui.ocr

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.loopworks.nibblemath.R
import com.loopworks.nibblemath.core.units.Unit as NibbleUnit
import com.loopworks.nibblemath.data.di.AppContainer
import com.loopworks.nibblemath.data.model.Recipe
import com.loopworks.nibblemath.ui.recipe.IngredientDraft
import com.loopworks.nibblemath.ui.recipe.IngredientPickerDialog
import com.loopworks.nibblemath.ui.recipe.IngredientRow
import com.loopworks.nibblemath.ui.recipe.RecipeDraft
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OcrScreen(
    container: AppContainer,
    bookId: Long,
    onBack: () -> Unit,
    onSaved: (Long) -> Unit,
    initialDraft: RecipeDraft? = null,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val ocrEngine = remember(context) { OcrEngine(context) }
    val defaultName = stringResource(R.string.new_recipe)

    var imageFile by remember { mutableStateOf<File?>(null) }
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    var ocrText by remember { mutableStateOf<String?>(null) }
    var draft by remember { mutableStateOf(initialDraft) }
    var ocrLoading by remember { mutableStateOf(false) }
    var ocrError by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    var permissionDenied by remember { mutableStateOf(false) }
    var copyFailed by remember { mutableStateOf(false) }
    var pickerKey by remember { mutableStateOf<Long?>(null) }
    var nextKey by remember { mutableStateOf(-1L) }

    val takePhotoLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicturePreview(),
    ) { captured ->
        if (captured != null) {
            val file = saveBitmap(captured, context)
            if (file != null) {
                imageFile?.delete()
                imageFile = file
                copyFailed = false
            } else {
                copyFailed = true
            }
        }
    }
    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            val file = copyUri(uri, context)
            if (file != null) {
                imageFile?.delete()
                imageFile = file
                copyFailed = false
            } else {
                copyFailed = true
            }
        }
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            permissionDenied = false
            takePhotoLauncher.launch(null)
        } else {
            permissionDenied = true
        }
    }

    fun startCamera() {
        permissionDenied = false
        if (context.checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            takePhotoLauncher.launch(null)
        } else {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    fun discard() {
        imageFile?.delete()
        imageFile = null
        bitmap = null
        ocrText = null
        draft = null
        ocrLoading = false
        ocrError = false
        copyFailed = false
    }

    fun updateDraft(transform: (RecipeDraft) -> RecipeDraft) {
        draft = draft?.let(transform)
    }

    fun addIngredient() {
        val key = nextKey
        nextKey = key - 1
        updateDraft { it.copy(ingredients = it.ingredients + IngredientDraft(key, null, "", "1", NibbleUnit.G)) }
        pickerKey = key
    }

    fun removeIngredient(key: Long) {
        updateDraft { it.copy(ingredients = it.ingredients.filter { line -> line.key != key }) }
    }

    fun updateIngredient(key: Long, transform: (IngredientDraft) -> IngredientDraft) {
        updateDraft {
            it.copy(ingredients = it.ingredients.map { line -> if (line.key == key) transform(line) else line })
        }
    }

    fun save() {
        val current = draft ?: return
        if (!current.canSave || saving) return
        scope.launch {
            saving = true
            val name = current.name.trim()
            val yieldAmount = current.yieldAmount ?: 1.0
            val yieldItem = current.yieldItem.trim()
            val recipeId = withContext(Dispatchers.IO) {
                container.recipeRepository.create(bookId, name, yieldAmount, yieldItem)
            }
            val base = Recipe(
                id = recipeId,
                bookId = bookId,
                name = name,
                yieldAmount = yieldAmount,
                yieldItem = yieldItem,
                notes = "",
                sortOrder = 0,
                steps = emptyList(),
                ingredients = emptyList(),
            )
            withContext(Dispatchers.IO) { container.recipeRepository.update(current.toRecipe(base)) }
            val sourceFile = imageFile
            withContext(Dispatchers.IO) { sourceFile?.delete() }
            saving = false
            onSaved(recipeId)
        }
    }

    LaunchedEffect(imageFile) {
        val file = imageFile
        if (file == null) {
            bitmap = null
            ocrText = null
            if (initialDraft == null) {
                draft = null
            }
            ocrLoading = false
            ocrError = false
            return@LaunchedEffect
        }
        ocrLoading = true
        ocrError = false
        ocrText = null
        draft = null
        bitmap = withContext(Dispatchers.IO) { BitmapFactory.decodeFile(file.absolutePath) }
        try {
            val text = ocrEngine.recognize(file)
            ocrText = text
            draft = withContext(Dispatchers.IO) {
                OcrTextParser.parse(text).toDraft(container.catalogRepository, defaultName)
            }
        } catch (e: Exception) {
            ocrError = true
        }
        ocrLoading = false
    }

    val currentFile = imageFile
    val currentBitmap = bitmap
    val currentDraft = draft

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.ocr_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back),
                        )
                    }
                },
                actions = {
                    TextButton(onClick = { save() }, enabled = currentDraft?.canSave == true && !saving) {
                        Text(stringResource(R.string.action_save))
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (currentFile == null) {
                if (initialDraft == null) {
                    Text(
                        text = stringResource(R.string.ocr_no_image),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = ::startCamera,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(stringResource(R.string.ocr_take_photo))
                    }
                    OutlinedButton(
                        onClick = { galleryLauncher.launch(arrayOf("image/*")) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(stringResource(R.string.ocr_choose_gallery))
                    }
                }
            } else {
                if (currentBitmap != null) {
                    Image(
                        bitmap = currentBitmap.asImageBitmap(),
                        contentDescription = stringResource(R.string.ocr_image_preview),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(280.dp),
                        contentScale = ContentScale.Fit,
                    )
                } else {
                    Text(
                        text = stringResource(R.string.ocr_image_unavailable),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                Text(
                    text = currentFile.name,
                    style = MaterialTheme.typography.bodyMedium,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = ::startCamera) {
                        Text(stringResource(R.string.ocr_take_another))
                    }
                    TextButton(onClick = { discard() }) {
                        Text(stringResource(R.string.ocr_remove))
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))

                if (ocrLoading) {
                    Text(
                        text = stringResource(R.string.ocr_extracting),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                } else if (ocrError) {
                    Text(
                        text = stringResource(R.string.ocr_failed),
                        color = MaterialTheme.colorScheme.error,
                    )
                } else {
                    val recognizedText = ocrText
                    if (!recognizedText.isNullOrBlank()) {
                        Text(
                            text = stringResource(R.string.ocr_raw_text),
                            style = MaterialTheme.typography.labelLarge,
                        )
                        Text(
                            text = recognizedText,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
            val current = currentDraft
            if (current != null) {
                Text(
                    text = stringResource(R.string.ocr_draft_title),
                    style = MaterialTheme.typography.titleMedium,
                )
                OutlinedTextField(
                    value = current.name,
                    onValueChange = { value -> updateDraft { it.copy(name = value) } },
                    label = { Text(stringResource(R.string.recipe_name_label)) },
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = current.yieldText,
                        onValueChange = { value -> updateDraft { it.copy(yieldText = value) } },
                        label = { Text(stringResource(R.string.recipe_yield_label)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = current.yieldItem,
                        onValueChange = { value -> updateDraft { it.copy(yieldItem = value) } },
                        label = { Text(stringResource(R.string.recipe_yield_item_label)) },
                        modifier = Modifier.weight(1f),
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = stringResource(R.string.recipe_ingredients_title),
                    style = MaterialTheme.typography.titleMedium,
                )
                if (current.ingredients.isEmpty()) {
                    Text(
                        text = stringResource(R.string.ocr_no_ingredients),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                current.ingredients.forEach { line ->
                    IngredientRow(
                        line = line,
                        onNameClick = { pickerKey = line.key },
                        onAmountChange = { value -> updateIngredient(line.key) { it.copy(amountText = value) } },
                        onUnitChange = { unit -> updateIngredient(line.key) { it.copy(unit = unit) } },
                        onRemove = { removeIngredient(line.key) },
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
                OutlinedButton(onClick = { addIngredient() }) {
                    Icon(Icons.Filled.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.recipe_add_ingredient))
                }
                TextButton(onClick = { discard() }) {
                    Text(stringResource(R.string.ocr_discard))
                }
            }
            if (permissionDenied) {
                Text(
                    text = stringResource(R.string.ocr_permission_needed),
                    color = MaterialTheme.colorScheme.error,
                )
            }
            if (copyFailed) {
                Text(
                    text = stringResource(R.string.ocr_copy_failed),
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }

    val activePickerKey = pickerKey
    if (activePickerKey != null) {
        IngredientPickerDialog(
            container = container,
            onDismiss = { pickerKey = null },
            onSelect = { ingredientId, name, unit, product ->
                val key = pickerKey
                if (key != null) {
                    updateIngredient(key) {
                        it.copy(
                            ingredientId = ingredientId,
                            ingredientName = name,
                            unit = unit,
                            productId = product?.id,
                            productName = product?.name,
                            productPackSize = product?.packSize,
                            productPrice = product?.price,
                        )
                    }
                    pickerKey = null
                }
            },
        )
    }
}

private fun saveBitmap(bitmap: Bitmap, context: Context): File? {
    val dir = File(context.filesDir, "ocr").apply { mkdirs() }
    val file = File(dir, "ocr-${System.currentTimeMillis()}.jpg")
    return try {
        FileOutputStream(file).use { output ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 95, output)
        }
        file
    } catch (e: Exception) {
        file.delete()
        null
    }
}

private fun copyUri(uri: Uri, context: Context): File? {
    val dir = File(context.filesDir, "ocr").apply { mkdirs() }
    val file = File(dir, "gallery-${System.currentTimeMillis()}.jpg")
    return try {
        val input = context.contentResolver.openInputStream(uri) ?: return null
        input.use { source ->
            FileOutputStream(file).use { target ->
                source.copyTo(target)
            }
        }
        file
    } catch (e: Exception) {
        file.delete()
        null
    }
}
