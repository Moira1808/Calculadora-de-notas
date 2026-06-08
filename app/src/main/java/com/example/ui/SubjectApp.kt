@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.Evaluation
import com.example.data.Subject
import com.example.data.SubjectWithEvaluations
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import com.example.R
import com.example.ui.theme.MyApplicationTheme

// --- HELPER MATH EXTENSIONS ---
private fun Double.roundTo(decimals: Int): String {
    return String.format(java.util.Locale.US, "%.${decimals}f", this)
}

private fun Double.roundToDecimal(decimals: Int): Double {
    var multiplier = 1.0
    repeat(decimals) { multiplier *= 10.0 }
    return kotlin.math.round(this * multiplier) / multiplier
}

// --- GLASSMORPHISM COLOR UTILITY ---
@Composable
fun getGlassColors(isDark: Boolean): Pair<Color, BorderStroke> {
    val container = if (isDark) {
        Color(0x661E293B) // Frosted Dark Slate (translucent)
    } else {
        Color(0xCCFFFFFF) // Frosted White (more translucent & brilliant)
    }
    val border = if (isDark) {
        BorderStroke(
            width = 1.2.dp,
            brush = Brush.verticalGradient(
                listOf(
                    Color.White.copy(alpha = 0.28f),
                    Color.White.copy(alpha = 0.05f)
                )
            )
        )
    } else {
        BorderStroke(
            width = 1.2.dp,
            brush = Brush.verticalGradient(
                listOf(
                    Color.White.copy(alpha = 0.75f),
                    Color.White.copy(alpha = 0.20f)
                )
            )
        )
    }
    return container to border
}

// --- MAIN ENTRANCE APP WITH LOCAL STORES & THEMES ---
@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun SubjectApp(viewModel: SubjectViewModel) {
    val context = LocalContext.current
    val sharedPref = remember { context.getSharedPreferences("salvacion_prefs", android.content.Context.MODE_PRIVATE) }

    // Persistent State configurations
    var isDarkThemeState by remember {
        mutableStateOf(sharedPref.getBoolean("is_dark_theme", true))
    }
    var userNameState by remember {
        mutableStateOf(sharedPref.getString("user_name", "") ?: "")
    }

    // Core streams
    val subjects by viewModel.allSubjectsWithEvaluations.collectAsStateWithLifecycle()
    val archivedSubjects by viewModel.archivedSubjectsWithEvaluations.collectAsStateWithLifecycle()
    val selectedId by viewModel.selectedSubjectId.collectAsStateWithLifecycle()
    val selectedSubject by viewModel.selectedSubjectWithEvaluations.collectAsStateWithLifecycle()
    val isSimulatorMode by viewModel.isSimulatorMode.collectAsStateWithLifecycle()

    // Dialog state variables
    var showAddSubjectDialog by remember { mutableStateOf(false) }
    var showPanicDialog by remember { mutableStateOf(false) }
    var showConfigDialog by remember { mutableStateOf(false) }
    var showArchiveConfirmDialog by remember { mutableStateOf(false) }
    var showArchivedHistoryScreen by remember { mutableStateOf(false) }

    // Onboarding UI: First run name requested
    if (userNameState.isBlank()) {
        MyApplicationTheme(darkTheme = isDarkThemeState) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = if (isDarkThemeState) Color(0xFF0F172A) else Color(0xFFEEF2F6)
            ) {
                // Glass background effects
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .drawBehind {
                            val centerColor = if (isDarkThemeState) Color(0x33818CF8) else Color(0x44D8B4FE)
                            drawCircle(
                                brush = Brush.radialGradient(
                                    colors = listOf(centerColor, Color.Transparent),
                                    center = Offset(size.width * 0.5f, size.height * 0.3f),
                                    radius = size.minDimension * 0.75f
                                ),
                                radius = size.minDimension * 0.75f,
                                center = Offset(size.width * 0.5f, size.height * 0.3f)
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    var nameInput by remember { mutableStateOf("") }
                    val (glassBg, glassBorder) = getGlassColors(isDarkThemeState)

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = glassBg),
                        border = glassBorder
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(28.dp)
                                .fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.ic_app_logo_glass),
                                contentDescription = "Logo de la aplicación",
                                modifier = Modifier
                                    .size(100.dp)
                                    .clip(RoundedCornerShape(20.dp)),
                                contentScale = ContentScale.Fit
                            )
                            Text(
                                text = "Planificador Académico",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = "Te ayudamos a optimizar tus metas académicas con precisión en tiempo real. Para comenzar, comparte tu nombre con nosotros:",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )

                            OutlinedTextField(
                                value = nameInput,
                                onValueChange = { nameInput = it },
                                label = { Text("¿Cómo te llamas?") },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                                )
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            // Interactive premium glass card button
                            Card(
                                onClick = {
                                    if (nameInput.isNotBlank()) {
                                        val finalName = nameInput.trim()
                                        sharedPref.edit().putString("user_name", finalName).apply()
                                        userNameState = finalName
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isDarkThemeState) Color(0xFF4F46E5) else Color(0xFF6366F1),
                                    contentColor = Color.White
                                ),
                                enabled = nameInput.isNotBlank()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = Color.White)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Comenzar mi Semestre",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        return
    }

    // MAIN VIEW IN CONTAINER (Theme state is propagated dynamically inside)
    MyApplicationTheme(darkTheme = isDarkThemeState) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = if (isDarkThemeState) Color(0xFF0F172A) else Color(0xFFEEF2F6)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .drawBehind {
                        // Drawing Glowing radial background orbs
                        val topLeftColor = if (isDarkThemeState) Color(0x36818CF8) else Color(0x44D8B4FE)
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(topLeftColor, Color.Transparent),
                                center = Offset(-size.width * 0.1f, -size.height * 0.1f),
                                radius = size.minDimension * 0.85f
                            ),
                            radius = size.minDimension * 0.85f,
                            center = Offset(-size.width * 0.1f, -size.height * 0.1f)
                        )

                        val bottomRightColor = if (isDarkThemeState) Color(0x2EF472B6) else Color(0x3E93C5FD)
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(bottomRightColor, Color.Transparent),
                                center = Offset(size.width * 1.1f, size.height * 0.9f),
                                radius = size.minDimension * 0.9f
                            ),
                            radius = size.minDimension * 0.9f,
                            center = Offset(size.width * 1.1f, size.height * 0.9f)
                        )
                    }
            ) {
                BoxWithConstraints {
                    val isTablet = maxWidth >= 600.dp

                    // Active flow (Archive feature completely removed)
                    if (true) {
                        // Active flow
                        if (isTablet) {
                            Row(modifier = Modifier.fillMaxSize()) {
                                Box(modifier = Modifier.width(370.dp)) {
                                    DashboardScreen(
                                        name = userNameState,
                                        isDark = isDarkThemeState,
                                        subjects = subjects,
                                        selectedId = selectedId,
                                        onSelectSubject = { viewModel.selectSubject(it) },
                                        onAddSubjectClick = { showAddSubjectDialog = true },
                                        onConfigClick = { showConfigDialog = true }
                                    )
                                }
                                VerticalDivider(
                                    thickness = 1.dp,
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                )
                                Box(modifier = Modifier.weight(1f)) {
                                    if (selectedSubject != null) {
                                        DetailScreen(
                                            isDark = isDarkThemeState,
                                            subjectWithEvaluations = selectedSubject!!,
                                            isSimulatorMode = isSimulatorMode,
                                            onSimulatorModeToggle = { viewModel.toggleSimulatorMode(it) },
                                            onBackClick = { viewModel.selectSubject(null) },
                                            onAddEvaluation = { name, weight, grade, category ->
                                                viewModel.addEvaluation(
                                                    selectedSubject!!.subject.id,
                                                    name,
                                                    grade,
                                                    weight,
                                                    category
                                                )
                                            },
                                            onUpdateEvaluation = { viewModel.updateEvaluation(it) },
                                            onDeleteEvaluation = { viewModel.deleteEvaluation(it) },
                                            onDeleteSubject = { viewModel.deleteSubject(selectedSubject!!.subject) },
                                            isTablet = true
                                        )
                                    } else {
                                        DetailPlaceholder(isDark = isDarkThemeState)
                                    }
                                }
                            }
                        } else {
                            if (selectedSubject != null) {
                                BackHandler { viewModel.selectSubject(null) }
                                DetailScreen(
                                    isDark = isDarkThemeState,
                                    subjectWithEvaluations = selectedSubject!!,
                                    isSimulatorMode = isSimulatorMode,
                                    onSimulatorModeToggle = { viewModel.toggleSimulatorMode(it) },
                                    onBackClick = { viewModel.selectSubject(null) },
                                    onAddEvaluation = { name, weight, grade, category ->
                                        viewModel.addEvaluation(
                                            selectedSubject!!.subject.id,
                                            name,
                                            grade,
                                            weight,
                                            category
                                        )
                                    },
                                    onUpdateEvaluation = { viewModel.updateEvaluation(it) },
                                    onDeleteEvaluation = { viewModel.deleteEvaluation(it) },
                                    onDeleteSubject = { viewModel.deleteSubject(selectedSubject!!.subject) },
                                    isTablet = false
                                )
                            } else {
                                DashboardScreen(
                                    name = userNameState,
                                    isDark = isDarkThemeState,
                                    subjects = subjects,
                                    selectedId = null,
                                    onSelectSubject = { viewModel.selectSubject(it) },
                                    onAddSubjectClick = { showAddSubjectDialog = true },
                                    onConfigClick = { showConfigDialog = true }
                                )
                            }
                        }
                    }
                }
            }

            // --- DEU CARD/PANEL DIALOGS ---
            if (showAddSubjectDialog) {
                AddSubjectDialog(
                    isDark = isDarkThemeState,
                    onDismiss = { showAddSubjectDialog = false },
                    onConfirm = { name, target ->
                        viewModel.addSubject(name, target)
                        showAddSubjectDialog = false
                    }
                )
            }

            if (showConfigDialog) {
                ConfigDialog(
                    isDark = isDarkThemeState,
                    onThemeToggle = {
                        isDarkThemeState = !isDarkThemeState
                        sharedPref.edit().putBoolean("is_dark_theme", isDarkThemeState).apply()
                    },
                    onDismiss = { showConfigDialog = false },
                    onResetClick = {
                        showConfigDialog = false
                        showPanicDialog = true
                    }
                )
            }

            if (showPanicDialog) {
                val (glassBg, glassBorder) = getGlassColors(isDarkThemeState)
                AlertDialog(
                    onDismissRequest = { showPanicDialog = false },
                    properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
                    content = {
                        Card(
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            colors = CardDefaults.cardColors(containerColor = glassBg),
                            border = glassBorder
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(28.dp)
                                    )
                                    Text(
                                        text = "Alerta Académica",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.titleLarge,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }

                                Text(
                                    text = "¿Estás totalmente segura de reiniciar el trimestre? Esta acción es irreversible, borrará todas tus materias, evaluaciones e historial guardado de este teléfono móvil de manera permanente.",
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Card(
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)
                                        ),
                                        onClick = { showPanicDialog = false }
                                    ) {
                                        Box(
                                            modifier = Modifier.padding(12.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                "Cancelar",
                                                fontWeight = FontWeight.Medium,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }

                                    Card(
                                        modifier = Modifier.weight(1.3f),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.error
                                        ),
                                        onClick = {
                                            viewModel.resetTrimester()
                                            showPanicDialog = false
                                        }
                                    ) {
                                        Box(
                                            modifier = Modifier.padding(12.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                "Confirmar Borrado",
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                )
            }
        }
    }
}

// --- GIGANTE ENCABEZADO Y PANTALLA PRINCIPAL ---
@Composable
fun DashboardScreen(
    name: String,
    isDark: Boolean,
    subjects: List<SubjectWithEvaluations>,
    selectedId: Int?,
    onSelectSubject: (Int) -> Unit,
    onAddSubjectClick: () -> Unit,
    onConfigClick: () -> Unit
) {
    val (headerBg, headerBorder) = getGlassColors(isDark)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        // GIGANTE ENCABEZADO DE UN TERCIO SUPERIOR (STYLE GLASSMORPHISM)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            shape = RoundedCornerShape(26.dp),
            colors = CardDefaults.cardColors(containerColor = headerBg),
            border = headerBorder
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(22.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    // Title Greetings with Logo
                    Row(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_app_logo_glass),
                            contentDescription = "Logo",
                            modifier = Modifier
                                .size(56.dp)
                                .clip(RoundedCornerShape(12.dp)),
                            contentScale = ContentScale.Fit
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "¡Hola, $name! 👋",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(bottom = 2.dp)
                            )
                            Text(
                                text = "Estudiante de la Universidad Arturo Michelena",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                            Text(
                                text = "Modo Salvación Activo • Sistema de Calificación Base 20.0",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                    }

                    // Settings trigger
                    Card(
                        onClick = onConfigClick,
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                    ) {
                        Box(
                            modifier = Modifier.padding(8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Configuración",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }

        // SCROLLABLE LIST OF SUBJECTS
        if (subjects.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Vacío",
                        tint = MaterialTheme.colorScheme.secondary.copy(alpha = 0.4f),
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "¡Tu trimestre está libre!",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Añade tu primera materia para comenzar con la medición en tiempo real y el de salvación de promedio.",
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                    Spacer(modifier = Modifier.height(18.dp))

                    // Glass card behave as button
                    val (gBg, gBorder) = getGlassColors(isDark)
                    Card(
                        onClick = onAddSubjectClick,
                        modifier = Modifier
                            .fillMaxWidth(0.8f),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = gBg),
                        border = gBorder
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Crear Primera Materia",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(subjects, key = { it.subject.id }) { item ->
                    SubjectDashboardCard(
                        item = item,
                        isSelected = item.subject.id == selectedId,
                        onClick = { onSelectSubject(item.subject.id) }
                    )
                }

                // Interactive glass card button to Add Subject at the end of the lists
                item {
                    val (gBg, gBorder) = getGlassColors(isDark)
                    Card(
                        onClick = onAddSubjectClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = gBg),
                        border = gBorder
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Añadir Nueva Materia",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}

// --- ACTIVE SUBJECTS CARD VIEW ---
@Composable
fun SubjectDashboardCard(
    item: SubjectWithEvaluations,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val accumulated = item.accumulatedPoints
    val target = item.subject.targetGrade
    val isPassed = accumulated >= target

    val statusColor = when {
        isPassed -> Color(0xFF10B981) // Emerald Green
        item.requiredGradeForPass != null && item.requiredGradeForPass!! <= 20.0 -> Color(0xFFF59E0B) // Amber
        else -> Color(0xFFEF4444) // Crimson Red
    }

    val borderStroke = if (isSelected) {
        BorderStroke(2.5.dp, statusColor)
    } else {
        BorderStroke(1.2.dp, statusColor.copy(alpha = 0.5f))
    }

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                statusColor.copy(alpha = 0.08f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        border = borderStroke
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = item.subject.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                if (isPassed) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFE8F5E9))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "SALVADO",
                            color = Color(0xFF2E7D32),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "${item.evaluatedWeight.roundTo(0)}% Evaluado",
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Score status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = "Llevas: ${accumulated.roundTo(2)} / 20.0 pts",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isPassed) Color(0xFF2E7D32) else MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = "Meta: ${target.roundTo(1)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Progress bar and passing indicator
            SubjectProgressIndicator(
                accumulated = accumulated,
                target = target
            )
        }
    }
}

// --- DETAILED SCREEN VIEW ---
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DetailScreen(
    isDark: Boolean,
    subjectWithEvaluations: SubjectWithEvaluations,
    isSimulatorMode: Boolean,
    onSimulatorModeToggle: (Boolean) -> Unit,
    onBackClick: () -> Unit,
    onAddEvaluation: (String, Double, Double?, String) -> Unit,
    onUpdateEvaluation: (Evaluation) -> Unit,
    onDeleteEvaluation: (Evaluation) -> Unit,
    onDeleteSubject: () -> Unit,
    isTablet: Boolean
) {
    var showAddEvalSheet by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    val (glassBg, glassBorder) = getGlassColors(isDark)

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        if (!isTablet) {
                            IconButton(onClick = onBackClick) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Volver",
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                        Text(
                            text = subjectWithEvaluations.subject.name,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Row {
                        IconButton(onClick = { showDeleteConfirmDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Eliminar Materia",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            // Section 1: The Magic Salvación Alerter Dashboard
            item {
                SalvationAlertBoard(item = subjectWithEvaluations)
            }

            // Section 1.5: Simulator Mode Switch Guard Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSimulatorMode) {
                            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.25f)
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f)
                        }
                    ),
                    border = BorderStroke(
                        1.dp,
                        if (isSimulatorMode) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outlineVariant
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .padding(12.dp)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isSimulatorMode) "✨ Modo Simulador: ACTIVO" else "🔬 Activar Modo Simulador",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (isSimulatorMode) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Proyecta notas y añade exámenes hipotéticos sin alterar tus valores reales.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = isSimulatorMode,
                            onCheckedChange = { onSimulatorModeToggle(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.secondary,
                                checkedTrackColor = MaterialTheme.colorScheme.secondaryContainer
                            )
                        )
                    }
                }
            }

            // Realtime Sum weight verification warning alert
            if (subjectWithEvaluations.totalWeight > 100.0) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f)
                        ),
                        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.error)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "⚠️ ¡Advertencia! Los porcentajes de evaluación superan el 100% (Total: ${subjectWithEvaluations.totalWeight.roundTo(1)}%). Revisa los pesos asignados para que tu calificación se calcule de forma precisa.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Section 2: Summary Row
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                "Puntos Acumulados",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.secondary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "${subjectWithEvaluations.accumulatedPoints.roundTo(2)} / 20.0",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                    }

                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                "Peso Evaluado",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.secondary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            val isOverLimit = subjectWithEvaluations.totalWeight > 100.0
                            Text(
                                text = "${subjectWithEvaluations.totalWeight.roundTo(1)}% / 100%",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = if (isOverLimit) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            // Section 3: Evaluations List Label
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Desglose de Evaluaciones",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Text(
                        text = "${subjectWithEvaluations.evaluations.size} registradas",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }

            // Section 4: Evaluations Cards Lists
            if (subjectWithEvaluations.evaluations.isEmpty()) {
                item {
                    // Glass Card EMPTY STATE behaves as addition launcher (Req 4)
                    Card(
                        onClick = { showAddEvalSheet = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        colors = CardDefaults.cardColors(containerColor = glassBg),
                        shape = RoundedCornerShape(18.dp),
                        border = glassBorder
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "✨",
                                fontSize = 32.sp
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                "No hay evaluaciones añadidas",
                                fontWeight = FontWeight.ExtraBold,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "Esta tarjeta de vidrio es interactiva. Haz clic sobre ella para añadir tu primer examen, exposición o proyecto al plan de evaluación.",
                                style = MaterialTheme.typography.bodySmall,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.82f),
                                modifier = Modifier.padding(horizontal = 12.dp)
                            )
                        }
                    }
                }
            } else {
                items(subjectWithEvaluations.evaluations, key = { it.id }) { evaluation ->
                    EvaluationListItem(
                        evaluation = evaluation,
                        onUpdate = onUpdateEvaluation,
                        onDelete = { onDeleteEvaluation(evaluation) },
                        isSimulatorMode = isSimulatorMode
                    )
                }

                // Section 4.5: Category Stats
                val evaluatedEvals = subjectWithEvaluations.evaluations.filter { it.grade != null }
                if (evaluatedEvals.isNotEmpty()) {
                    val categoryStats = evaluatedEvals
                        .groupBy { it.category }
                        .map { (cat, evals) ->
                            val avg = evals.mapNotNull { it.grade }.average()
                            cat to avg
                        }
                        .filter { it.second.isFinite() }

                    if (categoryStats.isNotEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(8.dp))
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
                                ),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Text(
                                        text = "Calificaciones Promedio por Categoría",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Calificaciones simples calculadas en base a tus notas ingresadas.",
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))

                                    categoryStats.forEach { (cat, avgGrade) ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 4.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "Calificación en $cat${if (cat.endsWith("l") || cat.endsWith("z") || cat.endsWith("n")) "" else "es"}:",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Text(
                                                text = "${avgGrade.roundTo(2)} / 20.0 pts",
                                                fontWeight = FontWeight.Bold,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = if (avgGrade >= 9.50) Color(0xFF2E7D32) else Color(0xFFD32F2F)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Add Evaluation trigger interactive glass card (No flat buttons)
                item {
                    val (addBg, addBorder) = getGlassColors(isDark)
                    Card(
                        onClick = { showAddEvalSheet = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = addBg),
                        border = addBorder
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Insertar Nueva Evaluación",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }

    // --- SHEET POPUPS OR DIALOG FOR ADDING EVALUATION ---
    if (showAddEvalSheet) {
        val (addGlassBg, addGlassBorder) = getGlassColors(isDark)
        AddEvaluationDialog(
            isDark = isDark,
            onDismiss = { showAddEvalSheet = false },
            onConfirm = { name, weight, grade, category ->
                onAddEvaluation(name, weight, grade, category)
                showAddEvalSheet = false
            }
        )
    }

    if (showDeleteConfirmDialog) {
        val (delGlass, delBorder) = getGlassColors(isDark)
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
            content = {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    colors = CardDefaults.cardColors(containerColor = delGlass),
                    border = delBorder
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(28.dp)
                            )
                            Text(
                                "Eliminar Materia",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.error
                            )
                        }

                        Text(
                            text = "¿Estás totalmente segura de que deseas borrar '${subjectWithEvaluations.subject.name}' de tu plan de estudios? Esto eliminará todos los registros de calificaciones de forma definitiva.",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Card(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)
                                ),
                                onClick = { showDeleteConfirmDialog = false }
                            ) {
                                Box(
                                    modifier = Modifier.padding(12.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        "Cancelar",
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }

                            Card(
                                modifier = Modifier.weight(1.2f),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.error
                                ),
                                onClick = {
                                    onDeleteSubject()
                                    showDeleteConfirmDialog = false
                                }
                            ) {
                                Box(
                                    modifier = Modifier.padding(12.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        "Eliminar",
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                }
            }
        )
    }
}

// --- EVALUATION ITEM LIST COMPONENT ---
@Composable
fun EvaluationListItem(
    evaluation: Evaluation,
    onUpdate: (Evaluation) -> Unit,
    onDelete: () -> Unit,
    isSimulatorMode: Boolean
) {
    val context = LocalContext.current
    var isLocked by remember(evaluation.id) { mutableStateOf(evaluation.grade != null) }
    val effectiveLocked = if (isSimulatorMode) false else isLocked
    var showUnlockWarningDialog by remember { mutableStateOf(false) }

    val isPending = evaluation.grade == null
    val isZero = evaluation.grade == 0.0

    val animatedBorderColor = when {
        !effectiveLocked && !isPending -> MaterialTheme.colorScheme.primary
        isZero -> Color(0xFFEF5350)
        isPending -> MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
        else -> MaterialTheme.colorScheme.outline
    }

    val cardBorder = BorderStroke(
        width = if (!effectiveLocked && !isPending) 2.dp else 1.dp,
        color = animatedBorderColor
    )

    if (showUnlockWarningDialog) {
        val isDark = isSystemInDarkTheme()
        val (glassBg, glassBorder) = getGlassColors(isDark)
        AlertDialog(
            onDismissRequest = { showUnlockWarningDialog = false },
            properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
            content = {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    colors = CardDefaults.cardColors(containerColor = glassBg),
                    border = glassBorder
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                "Desbloquear Nota Real 🔓",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Text(
                            text = "Vas a desbloquear una nota de evaluación real. Desbloquearla te permitirá modificarla. ¿Deseas activar el modo de edición?",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Card(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)
                                ),
                                onClick = { showUnlockWarningDialog = false }
                            ) {
                                Box(
                                    modifier = Modifier.padding(12.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        "Cancelar",
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }

                            Card(
                                modifier = Modifier.weight(1.2f),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.primary
                                ),
                                onClick = {
                                    isLocked = false
                                    showUnlockWarningDialog = false
                                    android.widget.Toast.makeText(
                                        context,
                                        "Modo Edición Activado para ${evaluation.name} 🔓",
                                        android.widget.Toast.LENGTH_SHORT
                                    ).show()
                                }
                            ) {
                                Box(
                                    modifier = Modifier.padding(12.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        "Desbloquear",
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                }
            }
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isPending) {
                MaterialTheme.colorScheme.surface.copy(alpha = 0.4f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        border = cardBorder
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = evaluation.name,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "Categoría: ${evaluation.category}",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Peso evaluativo: ${evaluation.weight}% • Calificación obtenida: ${
                            if (isPending) "Pendiente" else "${evaluation.grade} pts"
                        }",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (evaluation.grade != null && !isSimulatorMode) {
                        IconButton(
                            onClick = {
                                if (isLocked) {
                                    showUnlockWarningDialog = true
                                } else {
                                    isLocked = true
                                    android.widget.Toast.makeText(
                                        context,
                                        "Calificación Bloqueada 🔒",
                                        android.widget.Toast.LENGTH_SHORT
                                    ).show()
                                }
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Text(
                                text = if (isLocked) "🔒" else "🔓",
                                fontSize = 18.sp
                            )
                        }
                    }

                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Eliminar evaluación",
                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Checkbox and evaluation grade toggle
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable {
                    if (!effectiveLocked) {
                        val newGrade = if (isPending) 10.0 else null
                        onUpdate(evaluation.copy(grade = newGrade))
                        val msg = if (isPending) "Evaluación calificada" else "Evaluación como pendiente"
                        android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_SHORT).show()
                    }
                }
            ) {
                Checkbox(
                    checked = !isPending,
                    onCheckedChange = { checked ->
                        if (!effectiveLocked) {
                            val newGrade = if (checked) 10.0 else null
                            onUpdate(evaluation.copy(grade = newGrade))
                            val msg = if (checked) "Evaluación calificada" else "Evaluación como pendiente"
                            android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.size(24.dp),
                    enabled = !effectiveLocked || isPending
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Estado: " + if (isPending) "Pendiente por calificar 📝" else "Evaluado con éxito! ✅",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = if (isPending) MaterialTheme.colorScheme.secondary else Color(0xFF2E7D32)
                )
            }

            // Grade input options
            if (!isPending) {
                Spacer(modifier = Modifier.height(8.dp))

                if (!effectiveLocked) {
                    var gradeTempText by remember(evaluation.grade) {
                        mutableStateOf(evaluation.grade?.toString() ?: "")
                    }
                    var hasErrorWithGradeInput by remember { mutableStateOf(false) }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = gradeTempText,
                            onValueChange = {
                                gradeTempText = it
                                val parsed = it.toDoubleOrNull()
                                hasErrorWithGradeInput = parsed == null || parsed < 0.0 || parsed > 20.0
                            },
                            label = { Text("Escribir Nota (0.00 - 20.00)") },
                            isError = hasErrorWithGradeInput,
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline
                            )
                        )

                        Card(
                            onClick = {
                                val parsedValue = gradeTempText.toDoubleOrNull()
                                if (parsedValue != null && parsedValue in 0.0..20.0) {
                                    val roundedValue = parsedValue.roundToDecimal(2)
                                    onUpdate(evaluation.copy(grade = roundedValue))
                                    if (!isSimulatorMode) {
                                        isLocked = true
                                    }
                                    android.widget.Toast.makeText(
                                        context,
                                        "Nota guardada con éxito",
                                        android.widget.Toast.LENGTH_SHORT
                                    ).show()
                                } else {
                                    android.widget.Toast.makeText(
                                        context,
                                        "La nota tiene que estar entre 0.00 y 20.00 ❌",
                                        android.widget.Toast.LENGTH_SHORT
                                    ).show()
                                }
                            },
                            modifier = Modifier.height(56.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = Color.White
                            ),
                            enabled = !hasErrorWithGradeInput && gradeTempText.isNotBlank()
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .padding(horizontal = 16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("Guardar", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                } else {
                    var sliderValue by remember(evaluation.grade) {
                        mutableFloatStateOf(evaluation.grade?.toFloat() ?: 10f)
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "0.0",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.width(20.dp)
                        )

                        Slider(
                            value = sliderValue,
                            onValueChange = {
                                if (!effectiveLocked) {
                                    sliderValue = it
                                }
                            },
                            onValueChangeFinished = {
                                if (!effectiveLocked) {
                                    val finalValue = sliderValue.toDouble().roundToDecimal(1)
                                    onUpdate(evaluation.copy(grade = finalValue))
                                    val msg = "Nota guardada: $finalValue / 20.0 pts"
                                    android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_SHORT).show()
                                    if (!isSimulatorMode) {
                                        isLocked = true
                                    }
                                }
                            },
                            valueRange = 0f..20f,
                            enabled = !effectiveLocked,
                            modifier = Modifier
                                .weight(1f)
                                .height(18.dp),
                            colors = SliderDefaults.colors(
                                thumbColor = if (effectiveLocked) MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f) else MaterialTheme.colorScheme.primary,
                                activeTrackColor = if (effectiveLocked) MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f) else MaterialTheme.colorScheme.primary
                            )
                        )

                        Text(
                            text = "20.0",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier
                                .width(25.dp)
                                .padding(start = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

// --- CLAY/GLASS ALERTS BOARD OR CALCULATION WIDGET ---
@Composable
fun SalvationAlertBoard(item: SubjectWithEvaluations) {
    val acc = item.accumulatedPoints
    val target = item.subject.targetGrade
    val remW = item.remainingWeight
    val req = item.requiredGradeForPass
    val isDark = isSystemInDarkTheme()

    if (acc >= target) {
        val successBg = if (isDark) Color(0x2210B981) else Color(0x3310B981)
        val successBorder = if (isDark) Color(0x4410B981) else Color(0x8010B981)
        val successText = if (isDark) Color(0xFF34D399) else Color(0xFF047857)

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = successBg, contentColor = successText),
            border = BorderStroke(1.5.dp, successBorder)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    modifier = Modifier.size(36.dp),
                    tint = successText
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "¡CONSEGUIDO! A SALVO 🎉",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 14.sp,
                        color = successText
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Llevas acumulado ${acc.roundTo(2)} puntos. La meta era ${target.roundTo(1)}. ¡Aprobaste perfectamente!",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = successText.copy(alpha = 0.9f)
                    )
                }
            }
        }
    } else {
        if (remW <= 0.1) {
            val errorBg = if (isDark) Color(0x22EF4444) else Color(0x33EF4444)
            val errorBorder = if (isDark) Color(0x44EF4444) else Color(0x80EF4444)
            val errorText = if (isDark) Color(0xFFF87171) else Color(0xFFB91C1C)

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = errorBg, contentColor = errorText),
                border = BorderStroke(1.5.dp, errorBorder)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        modifier = Modifier.size(36.dp),
                        tint = errorText
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "NO SE ALCANZÓ LA META",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = errorText
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Has alcanzado un total de ${acc.roundTo(2)} de 20.0 y no quedan evaluaciones pendientes.",
                            style = MaterialTheme.typography.bodySmall,
                            color = errorText.copy(alpha = 0.9f)
                        )
                    }
                }
            }
        } else {
            if (req != null) {
                if (req <= 20.0) {
                    val motivation = when {
                        req <= 10.0 -> "¡Súper accesible! Prácticamente ya estás del otro lado. 🙌"
                        req <= 14.0 -> "¡Muy lograble! Con un repaso moderado pasas sin estrés. 📚"
                        req <= 17.5 -> "¡A estudiar! Necesitas ponerle bastante empeño para asegurar. 💪"
                        else -> "🔥 ¡Esforzándote al máximo! Necesitas exámenes casi perfectos."
                    }

                    val gradientBrush = Brush.horizontalGradient(
                        colors = listOf(
                            Color(0xFF4F46E5),
                            Color(0xFFEC4899)
                        )
                    )

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.Transparent, contentColor = Color.White),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.45f))
                    ) {
                        Box(
                            modifier = Modifier
                                .background(gradientBrush)
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color.White.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("🤔", fontSize = 20.sp)
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "¿CÓMO ME SALVO? 🤔",
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 11.sp,
                                        color = Color.White.copy(alpha = 0.9f),
                                        letterSpacing = 1.sp
                                    )
                                    Spacer(modifier = Modifier.height(3.dp))
                                    Text(
                                        text = "Necesitas obtener exactamente ${req.roundTo(2)} de 20.0 en el ${remW.roundTo(1)}% restante para aprobar con 9.50 de calificación.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(3.dp))
                                    Text(
                                        text = motivation,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.White.copy(alpha = 0.8f)
                                    )
                                }
                            }
                        }
                    }
                } else {
                    val warningBg = if (isDark) Color(0x22F59E0B) else Color(0x33F59E0B)
                    val warningBorder = if (isDark) Color(0x44F59E0B) else Color(0x80F59E0B)
                    val warningText = if (isDark) Color(0xFFFBBF24) else Color(0xFFB45309)

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = warningBg, contentColor = warningText),
                        border = BorderStroke(1.5.dp, warningBorder)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                modifier = Modifier.size(36.dp),
                                tint = warningText
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "RANGO DE PELIGRO EXTREMO",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 13.sp,
                                    color = warningText
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Matemáticamente necesitarías obtener ${req.roundTo(2)} de 20.0 en el ${remW.roundTo(1)}% restante para salvarte con 9.50 de calificación. ¡Habla con el profesor por algún proyecto extra!",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = warningText.copy(alpha = 0.9f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// --- DEDICATED HISTORY OF RETIRED TRIMESTERS SCREEN (Req 3) ---
@Composable
fun ArchivedHistoryView(
    archivedSubjects: List<SubjectWithEvaluations>,
    isDark: Boolean,
    onCloseClick: () -> Unit
) {
    val (glassBg, glassBorder) = getGlassColors(isDark)

    var expandedSubjectId by remember { mutableStateOf<Int?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(16.dp)
    ) {
        // Dynamic Headers
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Interactive back card
            Card(
                onClick = onCloseClick,
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
            ) {
                Box(
                    modifier = Modifier.padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Cerrar Historial",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Column {
                Text(
                    text = "Trimestres Archivados 📁",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Logros académicos y calificaciones preservadas",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (archivedSubjects.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .padding(16.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = glassBg),
                    border = glassBorder
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("📂", fontSize = 42.sp)
                        Text(
                            "Historial Vacío",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            "Aún no has archivado ningún trimestre académico. Cuando completes un ciclo, puedes archivar el trimestre actual desde el menú de Ajustes ⚙️.",
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(archivedSubjects, key = { "archived_" + it.subject.id }) { item ->
                    val avg = item.currentAverageGrade
                    val isPassed = avg >= 9.5
                    val isExpanded = expandedSubjectId == item.subject.id

                    Card(
                        onClick = {
                            expandedSubjectId = if (isExpanded) null else item.subject.id
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = glassBg),
                        border = glassBorder
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.subject.name,
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Calificaciones: ${item.evaluations.size} registradas",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "${avg.roundTo(2)} / 20.0",
                                        fontWeight = FontWeight.Black,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = if (isPassed) Color(0xFF2E7D32) else Color(0xFFD32F2F)
                                    )
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(
                                                if (isPassed) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
                                            )
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = if (isPassed) "APROBADO" else "REPROBADO",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 8.sp,
                                            color = if (isPassed) Color(0xFF2E7D32) else Color(0xFFC62828)
                                        )
                                    }
                                }
                            }

                            if (isExpanded) {
                                Spacer(modifier = Modifier.height(12.dp))
                                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                                Spacer(modifier = Modifier.height(8.dp))

                                Text(
                                    text = "Desglose de calificaciones archivadas:",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.secondary
                                )

                                Spacer(modifier = Modifier.height(6.dp))

                                if (item.evaluations.isEmpty()) {
                                    Text(
                                        "No se registraron evaluaciones individuales en esta materia.",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                } else {
                                    item.evaluations.forEach { eval ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 3.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "${eval.name} (${eval.weight}%):",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Text(
                                                text = if (eval.grade != null) "${eval.grade} de 20.0" else "Pendiente",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Large glass interactive card button to return
        Card(
            onClick = onCloseClick,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Box(
                modifier = Modifier.padding(14.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Volver al Trimestre Activo ⬅️",
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

// --- ACTIVE SUBJECT PROGRESS BAR ---
@Composable
fun SubjectProgressIndicator(
    accumulated: Double,
    target: Double
) {
    val progressPercent = (accumulated / 20.0).coerceIn(0.0, 1.0).toFloat()
    val targetPercent = (target / 20.0).coerceIn(0.0, 1.0).toFloat()

    val barColor = if (accumulated >= target) {
        Color(0xFF2E7D32)
    } else {
        MaterialTheme.colorScheme.primary
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(16.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        )

        Box(
            modifier = Modifier
                .fillMaxWidth(progressPercent)
                .fillMaxHeight()
                .clip(RoundedCornerShape(8.dp))
                .background(barColor)
        )

        Canvas(modifier = Modifier.fillMaxSize()) {
            val drawWidth = size.width
            val xPos = drawWidth * targetPercent

            drawLine(
                color = Color.Red.copy(alpha = 0.8f),
                start = Offset(x = xPos, y = 0f),
                end = Offset(x = xPos, y = size.height),
                strokeWidth = 2.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(5f, 5f), 0f)
            )
        }
    }
}

// --- SELECTION PLACEHOLDER FOR TABLETS ---
@Composable
fun DetailPlaceholder(isDark: Boolean) {
    val (gBg, gBorder) = getGlassColors(isDark)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(0.85f),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = gBg),
            border = gBorder
        ) {
            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("📊", fontSize = 48.sp)
                Text(
                    text = "Selección de Plan Académico",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "Selecciona una materia del plan de estudio o crea una nueva para ver el desglose, proyectar simulaciones y optimizar tu meta de salvación en tiempo real.",
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// --- ADD NEW SUBJECT GLASS DIALOG ---
@Composable
fun AddSubjectDialog(
    isDark: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (String, Double) -> Unit
) {
    val (glassBg, glassBorder) = getGlassColors(isDark)
    var name by remember { mutableStateOf("") }
    var targetInput by remember { mutableStateOf("9.5") }
    var hasErrorWithTarget by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = glassBg),
            border = glassBorder
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Nueva Materia 📘",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nombre de la Materia") },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                    )
                )

                OutlinedTextField(
                    value = targetInput,
                    onValueChange = {
                        targetInput = it
                        val parsed = it.toDoubleOrNull()
                        hasErrorWithTarget = parsed == null || parsed < 0.0 || parsed > 20.0
                    },
                    label = { Text("Nota de Aprobación Objetivo (0.00 - 20.00)") },
                    singleLine = true,
                    isError = hasErrorWithTarget,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                    )
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f)
                        ),
                        onClick = onDismiss
                    ) {
                        Box(
                            modifier = Modifier.padding(12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Cancelar", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Medium)
                        }
                    }

                    Card(
                        modifier = Modifier.weight(1.3f),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = Color.White
                        ),
                        enabled = name.isNotBlank() && !hasErrorWithTarget && targetInput.isNotBlank(),
                        onClick = {
                            val parsedTarget = targetInput.toDoubleOrNull() ?: 9.5
                            onConfirm(name.trim(), parsedTarget)
                        }
                    ) {
                        Box(
                            modifier = Modifier.padding(12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Crear Materia", fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }
        }
    }
}

// --- ADD NEW EVALUATION GLASS DIALOG (Req 4) ---
@Composable
fun AddEvaluationDialog(
    isDark: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (String, Double, Double?, String) -> Unit
) {
    val (glassBg, glassBorder) = getGlassColors(isDark)

    var name by remember { mutableStateOf("") }
    var weightInput by remember { mutableStateOf("25") }
    var gradeInput by remember { mutableStateOf("") }
    var isPending by remember { mutableStateOf(true) }

    val categoriesList = listOf("Parcial", "Quiz", "Exposición", "Laboratorio", "Taller", "Proyecto", "Otro")
    var selectedCategory by remember { mutableStateOf("Parcial") }

    var errorMessage by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = glassBg),
            border = glassBorder
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Añadir Evaluación ➕",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nombre de la Evaluación (Ej: Primer Parcial)") },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                    )
                )

                OutlinedTextField(
                    value = weightInput,
                    onValueChange = { weightInput = it },
                    label = { Text("Peso / Porcentaje de la Nota (0 - 100%)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                    )
                )

                // Category selector
                Text(
                    "Tipo de Actividad académica:",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val context = LocalContext.current
                    Box(modifier = Modifier.fillMaxWidth()) {
                        var expanded by remember { mutableStateOf(false) }
                        Card(
                            onClick = { expanded = !expanded },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
                            ),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(selectedCategory, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                            }
                        }

                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false },
                            modifier = Modifier.fillMaxWidth(0.8f)
                        ) {
                            categoriesList.forEach { category ->
                                DropdownMenuItem(
                                    text = { Text(category) },
                                    onClick = {
                                        selectedCategory = category
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Is complete toggle
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { isPending = !isPending }
                ) {
                    Checkbox(
                        checked = !isPending,
                        onCheckedChange = { isPending = !it }
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Ya tengo calificada esta evaluación",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                if (!isPending) {
                    OutlinedTextField(
                        value = gradeInput,
                        onValueChange = { gradeInput = it },
                        label = { Text("Nota Acumulada de Evaluación (0.00 - 20.00)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                        )
                    )
                }

                if (errorMessage.isNotBlank()) {
                    Text(
                        text = errorMessage,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f)
                        ),
                        onClick = onDismiss
                    ) {
                        Box(
                            modifier = Modifier.padding(12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Cancelar", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Medium)
                        }
                    }

                    Card(
                        modifier = Modifier.weight(1.3f),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = Color.White
                        ),
                        enabled = name.isNotBlank() && weightInput.isNotBlank() && (isPending || gradeInput.isNotBlank()),
                        onClick = {
                            val finalW = weightInput.toDoubleOrNull()
                            val finalG = if (isPending) null else gradeInput.toDoubleOrNull()

                            if (finalW == null || finalW <= 0.0 || finalW > 100.0) {
                                errorMessage = "Ingresa un peso (%) mayor a 0 y menor a 100%."
                                return@Card
                            }

                            if (!isPending && (finalG == null || finalG < 0.0 || finalG > 20.0)) {
                                errorMessage = "Ingresa una nota válida entre 0.00 y 20.00."
                                return@Card
                            }

                            errorMessage = ""
                            onConfirm(name.trim(), finalW, finalG, selectedCategory)
                        }
                    ) {
                        Box(
                            modifier = Modifier.padding(12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Guardar", fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }
        }
    }
}

// --- CONFIGURATION / PROTECTED SETTINGS GLASS POPUP (Req 4) ---
@Composable
fun ConfigDialog(
    isDark: Boolean,
    onThemeToggle: () -> Unit,
    onDismiss: () -> Unit,
    onResetClick: () -> Unit
) {
    val (glassBg, glassBorder) = getGlassColors(isDark)

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = glassBg),
            border = glassBorder
        ) {
            Column(
                modifier = Modifier
                    .padding(22.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Ajustes del Trimestre ⚙️",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = "Configura el aspecto visual o haz reset del almacenamiento de datos.",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Toggle Theme Mode Card in settings (Moved here per user request)
                Card(
                    onClick = onThemeToggle,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (isDark) "☀️" else "🌙",
                                fontSize = 18.sp
                            )
                        }
                        Column {
                            Text(
                                text = if (isDark) "Cambiar a Modo Claro" else "Cambiar a Modo Oscuro",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Alternar aspecto visual de la aplicación",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Protected reset zone (Req 4)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.03f)
                    ),
                    border = BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.15f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Zona de Seguridad de Datos",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }

                        // Danger reset button behave as card
                        Card(
                            onClick = onResetClick,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.12f)
                            ),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.3f))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "Reiniciar y Limpiar Todo 🚨",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Card(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier.padding(12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Cerrar Panel", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
        }
    }
}
