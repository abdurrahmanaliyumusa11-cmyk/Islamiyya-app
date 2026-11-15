package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Class
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Assessment
import androidx.compose.material.icons.outlined.Class
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.Screen
import com.example.ui.SchoolViewModel
import com.example.ui.components.AdminLoginDialog
import com.example.ui.screens.AddEditStudentScreen
import com.example.ui.screens.AttendanceDashboardScreen
import com.example.ui.screens.ClassAttendanceScreen
import com.example.ui.screens.ClassManagementScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.FeeManagementScreen
import com.example.ui.screens.ReportsScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SmsNotificationScreen
import com.example.ui.screens.StudentProfileScreen
import com.example.ui.screens.StudentsListScreen
import com.example.ui.screens.TeachersListScreen
import com.example.ui.theme.IslamicGoldAccent
import com.example.ui.theme.IslamicGreenContainer
import com.example.ui.theme.IslamicGreenDark
import com.example.ui.theme.IslamicGreenPrimary
import com.example.ui.theme.MyApplicationTheme

data class NavTab(
    val screen: Screen,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: SchoolViewModel = viewModel()
            val themeMode by viewModel.themeMode.collectAsState()
            val isDark = when (themeMode) {
                com.example.util.ThemeMode.SYSTEM -> androidx.compose.foundation.isSystemInDarkTheme()
                com.example.util.ThemeMode.LIGHT -> false
                com.example.util.ThemeMode.DARK -> true
            }

            MyApplicationTheme(darkTheme = isDark) {
                MainAppScreen(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun MainAppScreen(viewModel: SchoolViewModel = viewModel()) {
    val currentScreen by viewModel.currentScreen.collectAsState()
    val selectedStudent by viewModel.selectedStudent.collectAsState()
    val studentToEdit by viewModel.studentToEdit.collectAsState()
    val selectedClassForAttendance by viewModel.selectedClassForAttendance.collectAsState()
    val isAuthenticated by viewModel.isAuthenticated.collectAsState()
    val currentRole by viewModel.currentRole.collectAsState()
    val allTeachers by viewModel.allTeachers.collectAsState()
    val snackbarMsg by viewModel.snackbarMessage.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    var showLoginDialog by remember { mutableStateOf(false) }

    LaunchedEffect(snackbarMsg) {
        snackbarMsg?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearSnackbar()
        }
    }

    // Handle system back navigation
    BackHandler(enabled = currentScreen != Screen.DASHBOARD) {
        viewModel.navigateBack()
    }

    val navTabs = listOf(
        NavTab(Screen.DASHBOARD, "Home", Icons.Filled.Dashboard, Icons.Outlined.Dashboard),
        NavTab(Screen.STUDENTS, "Students", Icons.Filled.People, Icons.Outlined.People),
        NavTab(Screen.ADD_STUDENT, "Register", Icons.Filled.PersonAdd, Icons.Outlined.PersonAdd),
        NavTab(Screen.CLASSES, "Classes", Icons.Filled.Class, Icons.Outlined.Class),
        NavTab(Screen.REPORTS, "Reports", Icons.Filled.Assessment, Icons.Outlined.Assessment),
        NavTab(Screen.SETTINGS, "Settings", Icons.Filled.Settings, Icons.Outlined.Settings)
    )

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            // Show bottom navigation bar on root screens
            if (currentScreen != Screen.STUDENT_PROFILE &&
                currentScreen != Screen.ADD_STUDENT &&
                currentScreen != Screen.CLASS_ATTENDANCE &&
                currentScreen != Screen.ATTENDANCE_DASHBOARD &&
                currentScreen != Screen.SMS_NOTIFICATIONS &&
                currentScreen != Screen.FEE_MANAGEMENT
            ) {
                NavigationBar(
                    windowInsets = WindowInsets.navigationBars,
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 6.dp,
                    modifier = Modifier.testTag("main_navigation_bar")
                ) {
                    navTabs.forEach { tab ->
                        val isSelected = currentScreen == tab.screen
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = {
                                if (tab.screen == Screen.ADD_STUDENT) {
                                    if (isAuthenticated && currentRole.canAddOrEditStudents) {
                                        viewModel.startAddNewStudent()
                                    } else if (!isAuthenticated) {
                                        showLoginDialog = true
                                    } else {
                                        viewModel.showAccessDeniedMessage("Admin")
                                    }
                                } else {
                                    viewModel.navigateTo(tab.screen)
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
                                    contentDescription = tab.label,
                                    modifier = Modifier.size(22.dp)
                                )
                            },
                            label = {
                                Text(
                                    text = tab.label,
                                    fontSize = 10.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = IslamicGreenDark,
                                selectedTextColor = IslamicGreenPrimary,
                                indicatorColor = IslamicGreenContainer,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            modifier = Modifier.testTag("nav_tab_${tab.screen.name.lowercase()}")
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 700.dp) // Responsive for mobile, foldables, tablets and desktop
            ) {
                AnimatedContent(
                    targetState = currentScreen,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "ScreenTransition"
                ) { targetScreen ->
                    when (targetScreen) {
                        Screen.DASHBOARD -> DashboardScreen(
                            viewModel = viewModel,
                            onRequireLogin = { showLoginDialog = true }
                        )

                        Screen.STUDENTS -> StudentsListScreen(
                            viewModel = viewModel,
                            onRequireLogin = { showLoginDialog = true }
                        )

                        Screen.STUDENT_PROFILE -> {
                            selectedStudent?.let { student ->
                                StudentProfileScreen(
                                    student = student,
                                    viewModel = viewModel,
                                    onBack = { viewModel.navigateBack() },
                                    onRequireLogin = { showLoginDialog = true }
                                )
                            } ?: DashboardScreen(
                                viewModel = viewModel,
                                onRequireLogin = { showLoginDialog = true }
                            )
                        }

                        Screen.ADD_STUDENT -> AddEditStudentScreen(
                            viewModel = viewModel,
                            studentToEdit = studentToEdit,
                            onBack = { viewModel.navigateBack() }
                        )

                        Screen.CLASSES -> ClassManagementScreen(
                            viewModel = viewModel,
                            onRequireLogin = { showLoginDialog = true }
                        )

                        Screen.CLASS_ATTENDANCE -> {
                            selectedClassForAttendance?.let { schoolClass ->
                                ClassAttendanceScreen(
                                    schoolClass = schoolClass,
                                    viewModel = viewModel,
                                    onBack = { viewModel.navigateBack() }
                                )
                            } ?: ClassManagementScreen(
                                viewModel = viewModel,
                                onRequireLogin = { showLoginDialog = true }
                            )
                        }

                        Screen.ATTENDANCE_DASHBOARD -> AttendanceDashboardScreen(
                            viewModel = viewModel,
                            onBack = { viewModel.navigateBack() }
                        )

                        Screen.TEACHERS -> TeachersListScreen(
                            viewModel = viewModel,
                            onBack = { viewModel.navigateBack() },
                            onRequireLogin = { showLoginDialog = true }
                        )

                        Screen.SMS_NOTIFICATIONS -> SmsNotificationScreen(
                            viewModel = viewModel,
                            onBack = { viewModel.navigateBack() },
                            onRequireLogin = { showLoginDialog = true }
                        )

                        Screen.FEE_MANAGEMENT -> FeeManagementScreen(
                            viewModel = viewModel,
                            onBack = { viewModel.navigateBack() },
                            onRequireLogin = { showLoginDialog = true }
                        )

                        Screen.REPORTS -> ReportsScreen(
                            viewModel = viewModel,
                            onRequireLogin = { showLoginDialog = true }
                        )

                        Screen.SETTINGS -> SettingsScreen(
                            viewModel = viewModel,
                            onRequireLogin = { showLoginDialog = true }
                        )
                    }
                }
            }
        }
    }

    if (showLoginDialog) {
        AdminLoginDialog(
            teachers = allTeachers,
            onDismiss = { showLoginDialog = false },
            onLogin = { pin, role, teacher ->
                val ok = viewModel.loginWithRole(pin, role, teacher)
                if (ok) {
                    showLoginDialog = false
                }
                ok
            }
        )
    }
}
