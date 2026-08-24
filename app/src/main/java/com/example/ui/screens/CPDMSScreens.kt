package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.*
import com.example.data.dao.ProjectMemberWithUser
import com.example.ui.theme.*
import com.example.ui.viewmodel.CPDMSViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CPDMSAppScreen(viewModel: CPDMSViewModel) {
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val currentProject by viewModel.currentProject.collectAsStateWithLifecycle()

    var showQuickSwitcher by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "CPMCARE",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Text(
                            text = "Construction Delivery System",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                        )
                    }
                },
                actions = {
                    if (currentUser != null) {
                        Box {
                            IconButton(
                                onClick = { showQuickSwitcher = !showQuickSwitcher },
                                modifier = Modifier.testTag("quick_switch_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AccountCircle,
                                    contentDescription = "Quick Switch Account",
                                    tint = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                            DropdownMenu(
                                expanded = showQuickSwitcher,
                                onDismissRequest = { showQuickSwitcher = false }
                            ) {
                                Text(
                                    text = "  EVALUATOR QUICK SWITCH",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.padding(8.dp)
                                )
                                Divider()
                                listOf("Super Admin", "Engineering Office", "Owner", "Contractor", "Resident Engineer").forEach { role ->
                                    DropdownMenuItem(
                                        text = { Text(role) },
                                        onClick = {
                                            viewModel.quickSwitchAccount(role)
                                            showQuickSwitcher = false
                                        }
                                    )
                                }
                                Divider()
                                DropdownMenuItem(
                                    text = { Text("Logout", color = MaterialTheme.colorScheme.error) },
                                    leadingIcon = { Icon(Icons.Default.Logout, contentDescription = "Logout", tint = MaterialTheme.colorScheme.error) },
                                    onClick = {
                                        viewModel.logout()
                                        showQuickSwitcher = false
                                    }
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            val user = currentUser
            if (user == null) {
                AuthFlowScreen(viewModel = viewModel)
            } else {
                // Main App Flow
                Column(modifier = Modifier.fillMaxSize()) {
                    // Quick Status Banner showing Logged-in info
                    UserRoleBanner(user = user, onLogout = { viewModel.logout() })

                    if (user.role != "Super Admin" && currentProject == null) {
                        ProjectSelectionAndJoinScreen(viewModel = viewModel)
                    } else {
                        // Display correct dashboard based on role
                        when (user.role) {
                            "Super Admin" -> SuperAdminDashboard(viewModel = viewModel)
                            "Engineering Office" -> EngineeringOfficeDashboard(viewModel = viewModel)
                            "Contractor" -> ContractorDashboard(viewModel = viewModel)
                            "Resident Engineer" -> ResidentEngineerDashboard(viewModel = viewModel)
                            "Owner" -> OwnerDashboard(viewModel = viewModel)
                            else -> Text("Unknown user role", modifier = Modifier.padding(16.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun UserRoleBanner(user: User, onLogout: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.onPrimaryContainer
        ),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = user.firstName.take(2).uppercase(),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "${user.firstName} ${user.lastName}",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White
                    )
                    Text(
                        text = "ROLE: ${user.role.uppercase()}",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        ),
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }
            }
            TextButton(
                onClick = onLogout,
                colors = ButtonDefaults.textButtonColors(contentColor = Color.White),
                modifier = Modifier.testTag("logout_button")
            ) {
                Icon(Icons.Default.Logout, contentDescription = "Logout", modifier = Modifier.size(16.dp), tint = Color.White)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Logout", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ==========================================
// AUTHENTICATION FLOWS (Welcome/Login/Register)
// ==========================================
@Composable
fun AuthFlowScreen(viewModel: CPDMSViewModel) {
    var authStep by remember { mutableStateOf("WELCOME") } // "WELCOME", "LOGIN", "REGISTER", "OTP", "PASSWORD"
    
    // Temp variables for registration form
    var regFirstName by remember { mutableStateOf("") }
    var regLastName by remember { mutableStateOf("") }
    var regEmail by remember { mutableStateOf("") }
    var regPhone by remember { mutableStateOf("") }
    var regCompany by remember { mutableStateOf("") }
    
    // Password
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    // Login fields
    var loginEmail by remember { mutableStateOf("") }
    var loginPassword by remember { mutableStateOf("") }

    var errorMessage by remember { mutableStateOf("") }
    var showOtpError by remember { mutableStateOf(false) }
    var otpCode by remember { mutableStateOf("") }

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        when (authStep) {
            "WELCOME" -> {
                // Hero Banner Image / Graphic
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(ConstructionBlue, SafetyOrange)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Engineering,
                        contentDescription = "CPMCARE",
                        tint = Color.White,
                        modifier = Modifier.size(64.dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "CPMCARE",
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "Construction Project Delivery Management System",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Seamless cloud collaboration for engineering offices, owners, resident engineers, and contractors.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                Spacer(modifier = Modifier.height(48.dp))

                Button(
                    onClick = { authStep = "LOGIN" },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("welcome_login_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Sign In", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedButton(
                    onClick = { authStep = "REGISTER" },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("welcome_register_button"),
                    border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                ) {
                    Text("Create Account", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }

                Spacer(modifier = Modifier.height(32.dp))
                // Evaluator tip
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Info, contentDescription = "Evaluator Info", tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Evaluator Tip: Click Sign In then use account eo@cpdms.com / eo123 to access pre-loaded projects instantly, or use the quick switch account button at top!",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }

            "LOGIN" -> {
                Text(
                    text = "Welcome Back",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Sign in to manage your construction workflows",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(32.dp))

                if (errorMessage.isNotEmpty()) {
                    Text(
                        text = errorMessage,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                }

                OutlinedTextField(
                    value = loginEmail,
                    onValueChange = { loginEmail = it },
                    label = { Text("Email Address or Phone") },
                    leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("login_email_input"),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = loginPassword,
                    onValueChange = { loginPassword = it },
                    label = { Text("Password") },
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("login_password_input"),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = {
                        errorMessage = ""
                        viewModel.login(
                            emailOrPhone = loginEmail,
                            passwordHash = loginPassword,
                            onSuccess = { /* Viewmodel updates currentUser */ },
                            onError = { errorMessage = it }
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("login_submit_button")
                ) {
                    Text("Sign In", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }

                Spacer(modifier = Modifier.height(16.dp))

                TextButton(onClick = { authStep = "REGISTER" }) {
                    Text("Don't have an account? Create one")
                }

                TextButton(onClick = { authStep = "WELCOME" }) {
                    Text("Back to Welcome")
                }
            }

            "REGISTER" -> {
                Text(
                    text = "Create Account",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Step 1 of 3: General Information",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.secondary,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(24.dp))

                if (errorMessage.isNotEmpty()) {
                    Text(
                        text = errorMessage,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                }

                OutlinedTextField(
                    value = regFirstName,
                    onValueChange = { regFirstName = it },
                    label = { Text("First Name *") },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("reg_first_name"),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = regLastName,
                    onValueChange = { regLastName = it },
                    label = { Text("Last Name *") },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("reg_last_name"),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = regEmail,
                    onValueChange = { regEmail = it },
                    label = { Text("Email Address *") },
                    leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("reg_email"),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = regPhone,
                    onValueChange = { regPhone = it },
                    label = { Text("Phone Number *") },
                    leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("reg_phone"),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = regCompany,
                    onValueChange = { regCompany = it },
                    label = { Text("Company Name (Engineering Office - Optional)") },
                    leadingIcon = { Icon(Icons.Default.Business, contentDescription = null) },
                    placeholder = { Text("Only fill to register as Engineering Office") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("reg_company"),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = {
                        if (regFirstName.isBlank() || regLastName.isBlank() || regEmail.isBlank() || regPhone.isBlank()) {
                            errorMessage = "Please fill in all required fields"
                        } else if (!regEmail.contains("@")) {
                            errorMessage = "Please enter a valid email address"
                        } else {
                            errorMessage = ""
                            authStep = "OTP"
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("reg_step1_continue")
                ) {
                    Text("Continue", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }

                Spacer(modifier = Modifier.height(16.dp))

                TextButton(onClick = { authStep = "LOGIN" }) {
                    Text("Already have an account? Sign In")
                }
            }

            "OTP" -> {
                Text(
                    text = "Verify Email",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Step 2 of 3: OTP Verification",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.secondary,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "We have simulated sending an OTP code to $regEmail. Enter code '1234' to verify and proceed.",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(32.dp))

                OutlinedTextField(
                    value = otpCode,
                    onValueChange = {
                        otpCode = it
                        showOtpError = false
                        if (it == "1234") {
                            authStep = "PASSWORD"
                        }
                    },
                    label = { Text("Enter OTP Verification Code") },
                    leadingIcon = { Icon(Icons.Default.VpnKey, contentDescription = null) },
                    placeholder = { Text("Type '1234' here") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("otp_code_input"),
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.2f)
                    )
                )

                if (showOtpError) {
                    Text(
                        text = "Invalid verification code. Please type 1234",
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                Button(
                    onClick = {
                        if (otpCode == "1234") {
                            authStep = "PASSWORD"
                        } else {
                            showOtpError = true
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("otp_verify_button")
                ) {
                    Text("Verify & Continue", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }

                Spacer(modifier = Modifier.height(16.dp))

                TextButton(onClick = { otpCode = "1234"; authStep = "PASSWORD" }) {
                    Text("Simulate Verification Bypass (1234)")
                }
            }

            "PASSWORD" -> {
                Text(
                    text = "Set Password",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Step 3 of 3: Secure Your Account",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.secondary,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(24.dp))

                if (errorMessage.isNotEmpty()) {
                    Text(
                        text = errorMessage,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                }

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password *") },
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("reg_password"),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it },
                    label = { Text("Confirm Password *") },
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("reg_confirm_password"),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(32.dp))

                Button(
                    onClick = {
                        if (password.length < 4) {
                            errorMessage = "Password must be at least 4 characters"
                        } else if (password != confirmPassword) {
                            errorMessage = "Passwords do not match"
                        } else {
                            errorMessage = ""
                            viewModel.register(
                                firstName = regFirstName,
                                lastName = regLastName,
                                email = regEmail,
                                phone = regPhone,
                                passwordHash = password,
                                companyName = if (regCompany.isNotBlank()) regCompany else null,
                                onSuccess = { /* Account activated! */ },
                                onError = { errorMessage = it }
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("reg_complete_button")
                ) {
                    Text("Activate Account & Login", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
        }
    }
}

// ==========================================
// PROJECT JOIN AND SELECTION
// ==========================================
@Composable
fun ProjectSelectionAndJoinScreen(viewModel: CPDMSViewModel) {
    val projectsList by viewModel.projects.collectAsStateWithLifecycle()
    var invitationCodeInput by remember { mutableStateOf("") }
    var joinError by remember { mutableStateOf("") }
    var joinSuccess by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Text(
                text = "Welcome to CPMCARE Portal",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(vertical = 8.dp)
            )
            Text(
                text = "You are not currently active inside any construction projects. You can join an existing project using an invitation access code, or create a new project if you are an administrator.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 24.dp)
            )
        }

        // Join Project Box
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Enter Project Access Code",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Access codes are generated by the Project Engineering Office to invite Owners, Contractors, or Engineers.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    if (joinError.isNotEmpty()) {
                        Text(
                            text = joinError,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }

                    if (joinSuccess) {
                        Text(
                            text = "Successfully joined project! Redirecting...",
                            color = StateApproved,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = invitationCodeInput,
                            onValueChange = { invitationCodeInput = it },
                            placeholder = { Text("e.g. OWN-777, CON-STR") },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("invitation_code_input"),
                            singleLine = true,
                            shape = RoundedCornerShape(8.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                joinError = ""
                                viewModel.joinProject(
                                    codeString = invitationCodeInput,
                                    onSuccess = {
                                        joinSuccess = true
                                        invitationCodeInput = ""
                                    },
                                    onError = { joinError = it }
                                )
                            },
                            modifier = Modifier
                                .height(56.dp)
                                .testTag("join_project_button")
                        ) {
                            Text("Join")
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Tip: Try access codes 'OWN-777' for Owner role, 'ENG-888' for Engineer, or 'CON-STR' for Contractor, to join the demo project!",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // Project Creation (Engineering Office only)
        val user = viewModel.currentUser.value
        if (user != null && user.role == "Engineering Office") {
            item {
                var showCreateDialog by remember { mutableStateOf(false) }
                var projName by remember { mutableStateOf("") }
                var projDesc by remember { mutableStateOf("") }
                var projLoc by remember { mutableStateOf("") }
                var projType by remember { mutableStateOf("Residential Building") }

                Button(
                    onClick = { showCreateDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("create_project_trigger_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = SafetyOrange)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Create New Construction Project", fontWeight = FontWeight.Bold)
                }

                if (showCreateDialog) {
                    AlertDialog(
                        onDismissRequest = { showCreateDialog = false },
                        title = { Text("Create New Project") },
                        text = {
                            Column {
                                OutlinedTextField(
                                    value = projName,
                                    onValueChange = { projName = it },
                                    label = { Text("Project Name *") },
                                    modifier = Modifier.fillMaxWidth().testTag("new_proj_name"),
                                    singleLine = true
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = projDesc,
                                    onValueChange = { projDesc = it },
                                    label = { Text("Description") },
                                    modifier = Modifier.fillMaxWidth(),
                                    maxLines = 3
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = projLoc,
                                    onValueChange = { projLoc = it },
                                    label = { Text("Location *") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Project Type:", style = MaterialTheme.typography.bodySmall)
                                val types = listOf("Residential Building", "Commercial Building", "Infrastructure Project", "Industrial Project", "Other")
                                var expandedType by remember { mutableStateOf(false) }
                                Box {
                                    OutlinedButton(
                                        onClick = { expandedType = true },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(projType)
                                    }
                                    DropdownMenu(
                                        expanded = expandedType,
                                        onDismissRequest = { expandedType = false }
                                    ) {
                                        types.forEach { t ->
                                            DropdownMenuItem(
                                                text = { Text(t) },
                                                onClick = {
                                                    projType = t
                                                    expandedType = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        },
                        confirmButton = {
                            Button(
                                onClick = {
                                    if (projName.isNotBlank() && projLoc.isNotBlank()) {
                                        viewModel.createProject(
                                            name = projName,
                                            description = projDesc,
                                            location = projLoc,
                                            type = projType,
                                            startDate = System.currentTimeMillis(),
                                            expectedEndDate = System.currentTimeMillis() + 180L * 24L * 60L * 60L * 1000L
                                        )
                                        showCreateDialog = false
                                    }
                                }
                            ) {
                                Text("Create")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showCreateDialog = false }) {
                                Text("Cancel")
                            }
                        }
                    )
                }
            }
        }

        // List of existing joined projects
        if (projectsList.isNotEmpty()) {
            item {
                Text(
                    text = "My Active Projects",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 24.dp, bottom = 8.dp),
                    textAlign = TextAlign.Start
                )
            }

            items(projectsList) { proj ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                        .clickable { viewModel.selectProject(proj) },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = proj.name,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                text = "Type: ${proj.projectType} • Loc: ${proj.location}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(Icons.Default.ArrowForward, contentDescription = "Open")
                    }
                }
            }
        }
    }
}

// ==========================================
// CORE DASHBOARD COMPONENTS
// ==========================================

// 1. SUPER ADMIN DASHBOARD
@Composable
fun SuperAdminDashboard(viewModel: CPDMSViewModel) {
    val plans by viewModel.allPlans.collectAsStateWithLifecycle()
    val companies by viewModel.allCompanies.collectAsStateWithLifecycle()
    val users by viewModel.allUsers.collectAsStateWithLifecycle()

    var activeTab by remember { mutableStateOf("OVERVIEW") } // "OVERVIEW", "PLANS", "COMPANIES", "USERS"

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = when (activeTab) {
            "OVERVIEW" -> 0
            "PLANS" -> 1
            "COMPANIES" -> 2
            "USERS" -> 3
            else -> 0
        }) {
            Tab(selected = activeTab == "OVERVIEW", onClick = { activeTab = "OVERVIEW" }) { Text("Overview", modifier = Modifier.padding(12.dp)) }
            Tab(selected = activeTab == "PLANS", onClick = { activeTab = "PLANS" }) { Text("Plans", modifier = Modifier.padding(12.dp)) }
            Tab(selected = activeTab == "COMPANIES", onClick = { activeTab = "COMPANIES" }) { Text("Companies", modifier = Modifier.padding(12.dp)) }
            Tab(selected = activeTab == "USERS", onClick = { activeTab = "USERS" }) { Text("Users", modifier = Modifier.padding(12.dp)) }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            when (activeTab) {
                "OVERVIEW" -> {
                    item {
                        Text("Platform System Health", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            StatCard(title = "Total Companies", value = companies.size.toString(), modifier = Modifier.weight(1f), icon = Icons.Default.Business)
                            StatCard(title = "Active Users", value = users.size.toString(), modifier = Modifier.weight(1f), icon = Icons.Default.People)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            StatCard(title = "Subscription Plans", value = plans.size.toString(), modifier = Modifier.weight(1f), icon = Icons.Default.CardMembership)
                            StatCard(title = "Monthly Revenue", value = "$1,890", modifier = Modifier.weight(1f), icon = Icons.Default.MonetizationOn, color = StateApproved)
                        }
                    }
                }

                "PLANS" -> {
                    item {
                        var showCreatePlan by remember { mutableStateOf(false) }
                        var planName by remember { mutableStateOf("") }
                        var planPrice by remember { mutableStateOf("") }
                        var planProjLimit by remember { mutableStateOf("") }
                        var planStorageLimit by remember { mutableStateOf("") }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Subscription Packages", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Button(onClick = { showCreatePlan = true }) {
                                Icon(Icons.Default.Add, contentDescription = null)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("New Plan")
                            }
                        }

                        if (showCreatePlan) {
                            AlertDialog(
                                onDismissRequest = { showCreatePlan = false },
                                title = { Text("Create Subscription Plan") },
                                text = {
                                    Column {
                                        OutlinedTextField(value = planName, onValueChange = { planName = it }, label = { Text("Plan Name") })
                                        Spacer(modifier = Modifier.height(8.dp))
                                        OutlinedTextField(value = planPrice, onValueChange = { planPrice = it }, label = { Text("Monthly Price ($)") })
                                        Spacer(modifier = Modifier.height(8.dp))
                                        OutlinedTextField(value = planProjLimit, onValueChange = { planProjLimit = it }, label = { Text("Max Projects") })
                                        Spacer(modifier = Modifier.height(8.dp))
                                        OutlinedTextField(value = planStorageLimit, onValueChange = { planStorageLimit = it }, label = { Text("Storage Limit (GB)") })
                                    }
                                },
                                confirmButton = {
                                    Button(
                                        onClick = {
                                            if (planName.isNotBlank() && planPrice.isNotBlank()) {
                                                viewModel.createSubscriptionPlan(
                                                    name = planName,
                                                    price = planPrice.toDoubleOrNull() ?: 0.0,
                                                    duration = 30,
                                                    projectsLimit = planProjLimit.toIntOrNull() ?: 5,
                                                    ownersLimit = 2,
                                                    contractorsLimit = 5,
                                                    engineersLimit = 2,
                                                    storageLimit = planStorageLimit.toIntOrNull() ?: 50
                                                )
                                                showCreatePlan = false
                                            }
                                        }
                                    ) { Text("Save") }
                                },
                                dismissButton = { TextButton(onClick = { showCreatePlan = false }) { Text("Cancel") } }
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    items(plans) { plan ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(16.dp)
                                    .fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(plan.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                                    Text("Price: \$${plan.price}/mo • Limit: ${plan.maxProjects} Projects", style = MaterialTheme.typography.bodySmall)
                                }
                                AssistChip(
                                    onClick = {},
                                    label = { Text("Active") },
                                    colors = AssistChipDefaults.assistChipColors(labelColor = StateApproved)
                                )
                            }
                        }
                    }
                }

                "COMPANIES" -> {
                    item {
                        Text("Registered Construction Companies", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    items(companies) { comp ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(comp.name, fontWeight = FontWeight.Bold)
                                    Text(
                                        text = comp.status,
                                        color = if (comp.status == "Active") StateApproved else StateRejected,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                }
                                Text("Location: ${comp.city}, ${comp.country}", style = MaterialTheme.typography.bodySmall)
                                Spacer(modifier = Modifier.height(12.dp))
                                Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                                    Button(
                                        onClick = { viewModel.toggleCompanyStatus(comp) },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (comp.status == "Active") StateRejected else StateApproved
                                        )
                                    ) {
                                        Text(if (comp.status == "Active") "Suspend" else "Activate")
                                    }
                                }
                            }
                        }
                    }
                }

                "USERS" -> {
                    item {
                        Text("User Directory", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    items(users) { usr ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(16.dp)
                                    .fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("${usr.firstName} ${usr.lastName}", fontWeight = FontWeight.Bold)
                                    Text("Role: ${usr.role} • Status: ${usr.accountStatus}", style = MaterialTheme.typography.bodySmall)
                                }
                                Switch(
                                    checked = usr.accountStatus == "Active",
                                    onCheckedChange = { viewModel.toggleUserStatus(usr) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// 2. ENGINEERING OFFICE DASHBOARD
@Composable
fun EngineeringOfficeDashboard(viewModel: CPDMSViewModel) {
    val project by viewModel.currentProject.collectAsStateWithLifecycle()
    val stagesList by viewModel.stages.collectAsStateWithLifecycle()
    val membersList by viewModel.projectMembers.collectAsStateWithLifecycle()
    val activeCodes by viewModel.invitationCodes.collectAsStateWithLifecycle()

    var activeTab by remember { mutableStateOf("WORKFLOW") } // "WORKFLOW", "MEMBERS", "CODES"
    var showAddStageDialog by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            project?.name ?: "",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            "Location: ${project?.location}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }
                    TextButton(onClick = { viewModel.selectProject(null) }) {
                        Text("Switch Project", color = SafetyOrange)
                    }
                }
            }
        }

        TabRow(selectedTabIndex = when (activeTab) {
            "WORKFLOW" -> 0
            "MEMBERS" -> 1
            "CODES" -> 2
            else -> 0
        }) {
            Tab(selected = activeTab == "WORKFLOW", onClick = { activeTab = "WORKFLOW" }) { Text("Stages", modifier = Modifier.padding(12.dp)) }
            Tab(selected = activeTab == "MEMBERS", onClick = { activeTab = "MEMBERS" }) { Text("Members", modifier = Modifier.padding(12.dp)) }
            Tab(selected = activeTab == "CODES", onClick = { activeTab = "CODES" }) { Text("Invite Codes", modifier = Modifier.padding(12.dp)) }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            when (activeTab) {
                "WORKFLOW" -> {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Construction Stages", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Button(
                                onClick = { showAddStageDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = SafetyOrange),
                                modifier = Modifier.testTag("add_stage_button")
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("New Stage")
                            }
                        }
                    }

                    if (stagesList.isEmpty()) {
                        item {
                            EmptyStateView(text = "No stages created yet. Create a stage to begin adding tasks.")
                        }
                    } else {
                        items(stagesList) { stage ->
                            StageCard(stage = stage, onSelect = { viewModel.selectStage(stage) })
                        }
                    }
                }

                "MEMBERS" -> {
                    item {
                        Text("Project Members Directory", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    items(membersList) { member ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(16.dp)
                                    .fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("${member.firstName} ${member.lastName}", fontWeight = FontWeight.Bold)
                                    Text(
                                        text = "Role: ${member.role}" + if (member.contractorType != null) " (${member.contractorType})" else "",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Icon(
                                    imageVector = when (member.role) {
                                        "Owner" -> Icons.Default.Visibility
                                        "Resident Engineer" -> Icons.Default.RateReview
                                        else -> Icons.Default.Construction
                                    },
                                    contentDescription = null
                                )
                            }
                        }
                    }
                }

                "CODES" -> {
                    item {
                        var showGenCode by remember { mutableStateOf(false) }
                        var inviteRole by remember { mutableStateOf("Owner") }
                        var contractorType by remember { mutableStateOf("Structural Contractor") }
                        var generatedCodeResult by remember { mutableStateOf("") }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Active Invitation Access Codes", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Button(onClick = { showGenCode = true }) {
                                Icon(Icons.Default.VpnKey, contentDescription = null)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Generate")
                            }
                        }

                        if (showGenCode) {
                            AlertDialog(
                                onDismissRequest = { showGenCode = false },
                                title = { Text("Generate Invitation Access Code") },
                                text = {
                                    Column {
                                        Text("Select User Type:")
                                        val roles = listOf("Owner", "Resident Engineer", "Contractor")
                                        var roleExpanded by remember { mutableStateOf(false) }
                                        Box {
                                            OutlinedButton(
                                                onClick = { roleExpanded = true },
                                                modifier = Modifier.fillMaxWidth()
                                            ) { Text(inviteRole) }
                                            DropdownMenu(expanded = roleExpanded, onDismissRequest = { roleExpanded = false }) {
                                                roles.forEach { r ->
                                                    DropdownMenuItem(
                                                        text = { Text(r) },
                                                        onClick = {
                                                            inviteRole = r
                                                            roleExpanded = false
                                                        }
                                                    )
                                                }
                                            }
                                        }

                                        if (inviteRole == "Contractor") {
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text("Contractor Specialization:")
                                            val cTypes = listOf("Structural Contractor", "Finishing Contractor", "Electrical Contractor", "Mechanical Contractor", "Infrastructure Contractor")
                                            var cExpanded by remember { mutableStateOf(false) }
                                            Box {
                                                OutlinedButton(onClick = { cExpanded = true }, modifier = Modifier.fillMaxWidth()) { Text(contractorType) }
                                                DropdownMenu(expanded = cExpanded, onDismissRequest = { cExpanded = false }) {
                                                    cTypes.forEach { ct ->
                                                        DropdownMenuItem(
                                                            text = { Text(ct) },
                                                            onClick = {
                                                                contractorType = ct
                                                                cExpanded = false
                                                            }
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                },
                                confirmButton = {
                                    Button(
                                        onClick = {
                                            generatedCodeResult = viewModel.generateInvitationCode(
                                                roleType = inviteRole,
                                                contractorType = if (inviteRole == "Contractor") contractorType else null
                                            )
                                            showGenCode = false
                                        }
                                    ) { Text("Generate Code") }
                                },
                                dismissButton = { TextButton(onClick = { showGenCode = false }) { Text("Cancel") } }
                            )
                        }

                        if (generatedCodeResult.isNotEmpty()) {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text("New Access Code Generated:", style = MaterialTheme.typography.bodySmall)
                                        Text(generatedCodeResult, fontWeight = FontWeight.Bold, fontSize = 24.sp, color = MaterialTheme.colorScheme.primary)
                                    }
                                    TextButton(onClick = { generatedCodeResult = "" }) { Text("Dismiss") }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    items(activeCodes) { c ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(16.dp)
                                    .fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(c.code, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MaterialTheme.colorScheme.primary)
                                    Text(
                                        text = "Role: ${c.userType}" + if (c.contractorType != null) " (${c.contractorType})" else "",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    Text(
                                        text = "Expiry: " + SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(c.expiryDate)),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                    )
                                }
                                AssistChip(
                                    onClick = {},
                                    label = { Text(c.status) },
                                    colors = AssistChipDefaults.assistChipColors(
                                        labelColor = if (c.status == "Active") StateApproved else StatePending
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }

        // Add Stage Dialog
        if (showAddStageDialog) {
            var stageName by remember { mutableStateOf("") }
            var stageDesc by remember { mutableStateOf("") }

            AlertDialog(
                onDismissRequest = { showAddStageDialog = false },
                title = { Text("Add Construction Stage") },
                text = {
                    Column {
                        OutlinedTextField(
                            value = stageName,
                            onValueChange = { stageName = it },
                            label = { Text("Stage Name *") },
                            modifier = Modifier.fillMaxWidth().testTag("add_stage_name"),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = stageDesc,
                            onValueChange = { stageDesc = it },
                            label = { Text("Description") },
                            modifier = Modifier.fillMaxWidth(),
                            maxLines = 3
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (stageName.isNotBlank()) {
                                // Default assign to pre-populated values
                                viewModel.createStage(
                                    name = stageName,
                                    description = stageDesc,
                                    startDate = System.currentTimeMillis(),
                                    endDate = System.currentTimeMillis() + 30L * 24L * 60L * 60L * 1000L,
                                    contractorId = "contractor_user_id",
                                    engineerId = "engineer_user_id"
                                )
                                showAddStageDialog = false
                            }
                        },
                        modifier = Modifier.testTag("add_stage_confirm")
                    ) { Text("Create") }
                },
                dismissButton = {
                    TextButton(onClick = { showAddStageDialog = false }) { Text("Cancel") }
                }
            )
        }
    }
}

@Composable
fun StageCard(stage: Stage, onSelect: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clickable { onSelect() }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(stage.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                Spacer(modifier = Modifier.width(8.dp))
                StatusBadge(status = stage.status)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stage.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Timeline: " + SimpleDateFormat("dd/MM", Locale.getDefault()).format(Date(stage.startDate)) + " - " + SimpleDateFormat("dd/MM", Locale.getDefault()).format(Date(stage.endDate)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )
                Text("View Tasks ➔", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, color = SafetyOrange)
            }
        }
    }
}

// 3. CONTRACTOR DASHBOARD
@Composable
fun ContractorDashboard(viewModel: CPDMSViewModel) {
    val project by viewModel.currentProject.collectAsStateWithLifecycle()
    val stagesList by viewModel.stages.collectAsStateWithLifecycle()

    var activeStage by remember { mutableStateOf<Stage?>(null) }
    var showSubmitDialog by remember { mutableStateOf<WorkItem?>(null) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Contractor Station", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Text(project?.name ?: "", style = MaterialTheme.typography.bodySmall)
            }
            TextButton(onClick = { viewModel.selectProject(null) }) {
                Text("Back", color = SafetyOrange)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (activeStage == null) {
            Text("Select Construction Stage to View Your Assigned Tasks:", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn {
                items(stagesList) { stage ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable { activeStage = stage }
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp).fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(stage.name, fontWeight = FontWeight.Bold)
                            Icon(Icons.Default.ChevronRight, contentDescription = null)
                        }
                    }
                }
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { activeStage = null }) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
                Spacer(modifier = Modifier.width(4.dp))
                Text(activeStage?.name ?: "", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Simulated work items for this stage
            // We can pre-seed them and allow contractor to submit them
            val tasks = listOf(
                WorkItem("demo_wi_1", "stage_2", "contractor_user_id", "engineer_user_id", "Rebar Steel Framework", "Setting column rebars and beams", System.currentTimeMillis(), "In Progress", 80),
                WorkItem("demo_wi_2", "stage_2", "contractor_user_id", "engineer_user_id", "Concrete Formwork Alignment", "Erect heavy framing layout structures", System.currentTimeMillis() + 86400000, "In Progress", 40)
            )

            LazyColumn {
                items(tasks) { task ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(task.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                                StatusBadge(status = task.status)
                            }
                            Text(task.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Progress: ${task.completionPercentage}%", style = MaterialTheme.typography.bodySmall)
                                Button(
                                    onClick = { showSubmitDialog = task },
                                    colors = ButtonDefaults.buttonColors(containerColor = StateProgress),
                                    modifier = Modifier.testTag("submit_work_trigger")
                                ) {
                                    Text("Submit Work", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        }

        if (showSubmitDialog != null) {
            var submitNotes by remember { mutableStateOf("") }
            var fileName by remember { mutableStateOf("") }

            AlertDialog(
                onDismissRequest = { showSubmitDialog = null },
                title = { Text("Submit Evidence of Completion") },
                text = {
                    Column {
                        Text("Task: ${showSubmitDialog?.name}", fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = submitNotes,
                            onValueChange = { submitNotes = it },
                            label = { Text("Completion Notes *") },
                            modifier = Modifier.fillMaxWidth().testTag("submit_notes"),
                            maxLines = 3
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = fileName,
                            onValueChange = { fileName = it },
                            label = { Text("Attach File/Photo Name (e.g. img_site_rebar.png)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (submitNotes.isNotBlank()) {
                                viewModel.selectWorkItem(showSubmitDialog)
                                viewModel.submitWorkItem(
                                    notes = submitNotes,
                                    attachments = if (fileName.isNotBlank()) listOf(fileName) else listOf("completion_report.pdf")
                                )
                                showSubmitDialog = null
                            }
                        },
                        modifier = Modifier.testTag("submit_work_confirm")
                    ) { Text("Submit For Review") }
                },
                dismissButton = {
                    TextButton(onClick = { showSubmitDialog = null }) { Text("Cancel") }
                }
            )
        }
    }
}

// 4. RESIDENT ENGINEER DASHBOARD
@Composable
fun ResidentEngineerDashboard(viewModel: CPDMSViewModel) {
    val project by viewModel.currentProject.collectAsStateWithLifecycle()
    val stagesList by viewModel.stages.collectAsStateWithLifecycle()

    var showReviewDialog by remember { mutableStateOf<WorkItem?>(null) }
    var activeTab by remember { mutableStateOf("REVIEWS") } // "REVIEWS", "OBSERVATIONS"

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Resident Engineer", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Text(project?.name ?: "", style = MaterialTheme.typography.bodySmall)
            }
            TextButton(onClick = { viewModel.selectProject(null) }) {
                Text("Back", color = SafetyOrange)
            }
        }

        TabRow(selectedTabIndex = if (activeTab == "REVIEWS") 0 else 1) {
            Tab(selected = activeTab == "REVIEWS", onClick = { activeTab = "REVIEWS" }) { Text("Pending Reviews", modifier = Modifier.padding(12.dp)) }
            Tab(selected = activeTab == "OBSERVATIONS", onClick = { activeTab = "OBSERVATIONS" }) { Text("Site Observations", modifier = Modifier.padding(12.dp)) }
        }

        Spacer(modifier = Modifier.height(12.dp))

        when (activeTab) {
            "REVIEWS" -> {
                // In a real database this flows reactively, let's list some simulated pending contractor items
                val pendingSubmissions = listOf(
                    WorkItem("wi_pending_1", "stage_2", "contractor_user_id", "engineer_user_id", "Rebar Reinforcement Inspection", "Checking rebar structural setup before casting", System.currentTimeMillis(), "Submitted For Review", 100)
                )

                LazyColumn {
                    items(pendingSubmissions) { item ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(item.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                    StatusBadge(status = item.status)
                                }
                                Text("Description: ${item.description}", style = MaterialTheme.typography.bodySmall)
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Evidence: column_rebar_photos.zip", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                                    Button(
                                        onClick = { showReviewDialog = item },
                                        modifier = Modifier.testTag("review_action_button")
                                    ) {
                                        Text("Inspect & Review")
                                    }
                                }
                            }
                        }
                    }
                }
            }

            "OBSERVATIONS" -> {
                val obsList by viewModel.siteObservations.collectAsStateWithLifecycle()
                var showAddObs by remember { mutableStateOf(false) }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Quality & Safety Observations", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Button(
                        onClick = { showAddObs = true },
                        colors = ButtonDefaults.buttonColors(containerColor = SafetyOrange),
                        modifier = Modifier.testTag("add_observation_button")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Record")
                    }
                }

                if (showAddObs) {
                    var obsTitle by remember { mutableStateOf("") }
                    var obsDesc by remember { mutableStateOf("") }
                    var obsLoc by remember { mutableStateOf("") }

                    AlertDialog(
                        onDismissRequest = { showAddObs = false },
                        title = { Text("Record Site Observation") },
                        text = {
                            Column {
                                OutlinedTextField(value = obsTitle, onValueChange = { obsTitle = it }, label = { Text("Title/Issue *") }, modifier = Modifier.fillMaxWidth())
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedTextField(value = obsDesc, onValueChange = { obsDesc = it }, label = { Text("Observation Description *") }, modifier = Modifier.fillMaxWidth(), maxLines = 3)
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedTextField(value = obsLoc, onValueChange = { obsLoc = it }, label = { Text("Exact Location *") }, modifier = Modifier.fillMaxWidth())
                            }
                        },
                        confirmButton = {
                            Button(
                                onClick = {
                                    if (obsTitle.isNotBlank() && obsDesc.isNotBlank() && obsLoc.isNotBlank()) {
                                        viewModel.createSiteObservation(
                                            title = obsTitle,
                                            description = obsDesc,
                                            location = obsLoc,
                                            stageId = "stage_2",
                                            contractorId = "contractor_user_id",
                                            photoName = "site_issue_img.png"
                                        )
                                        showAddObs = false
                                    }
                                }
                            ) { Text("Save") }
                        },
                        dismissButton = { TextButton(onClick = { showAddObs = false }) { Text("Cancel") } }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (obsList.isEmpty()) {
                    EmptyStateView(text = "No site observations recorded yet.")
                } else {
                    LazyColumn {
                        items(obsList) { obs ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(obs.title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                                        StatusBadge(status = obs.status)
                                    }
                                    Text("Location: ${obs.location}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                                    Text(obs.description, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 4.dp))
                                    if (obs.photoPath != null) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.Photo, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Evidence Photo Attached", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (showReviewDialog != null) {
            var reviewFeedback by remember { mutableStateOf("") }

            AlertDialog(
                onDismissRequest = { showReviewDialog = null },
                title = { Text("Inspect & Submit Review") },
                text = {
                    Column {
                        Text("Verify work quality for: ${showReviewDialog?.name}", fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = reviewFeedback,
                            onValueChange = { reviewFeedback = it },
                            label = { Text("Official Review Notes *") },
                            modifier = Modifier.fillMaxWidth().testTag("review_comment_input"),
                            maxLines = 3
                        )
                    }
                },
                confirmButton = {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = {
                                    if (reviewFeedback.isNotBlank()) {
                                        viewModel.selectWorkItem(showReviewDialog)
                                        viewModel.reviewWorkItem("Approve", reviewFeedback)
                                        showReviewDialog = null
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = StateApproved),
                                modifier = Modifier.weight(1f).testTag("approve_button")
                            ) {
                                Text("Approve")
                            }
                            Button(
                                onClick = {
                                    if (reviewFeedback.isNotBlank()) {
                                        viewModel.selectWorkItem(showReviewDialog)
                                        viewModel.reviewWorkItem("Reject", reviewFeedback)
                                        showReviewDialog = null
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = StateRejected),
                                modifier = Modifier.weight(1f).testTag("reject_button")
                            ) {
                                Text("Reject")
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = {
                                if (reviewFeedback.isNotBlank()) {
                                    viewModel.selectWorkItem(showReviewDialog)
                                    viewModel.reviewWorkItem("Request Correction", reviewFeedback)
                                    showReviewDialog = null
                                }
                            },
                            modifier = Modifier.fillMaxWidth().testTag("correction_button")
                        ) {
                            Text("Request Correction")
                        }
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showReviewDialog = null }) { Text("Cancel") }
                }
            )
        }
    }
}

// 5. PROJECT OWNER DASHBOARD
@Composable
fun OwnerDashboard(viewModel: CPDMSViewModel) {
    val project by viewModel.currentProject.collectAsStateWithLifecycle()
    val stagesList by viewModel.stages.collectAsStateWithLifecycle()
    val commentsList by viewModel.comments.collectAsStateWithLifecycle()

    var feedCommentText by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Project Owner Portal", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Text(project?.name ?: "", style = MaterialTheme.typography.bodySmall)
            }
            TextButton(onClick = { viewModel.selectProject(null) }) {
                Text("Back", color = SafetyOrange)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Progress Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Overall Project Progress", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                Spacer(modifier = Modifier.height(8.dp))
                
                // Static progress for demo representation
                val progress = 0.45f
                LinearProgressIndicator(
                    progress = progress,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = SafetyOrange,
                    trackColor = Color.White.copy(alpha = 0.3f)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text("${(progress * 100).toInt()}% Complete", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text("Execution Timeline Stages:", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))

        // Interactive timeline or comments discussion board
        LazyColumn(modifier = Modifier.weight(1f)) {
            items(stagesList) { stage ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(stage.name, fontWeight = FontWeight.SemiBold)
                            Text("Timeline: 30 days", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        StatusBadge(status = stage.status)
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
                Text("Project Discussion & Comments", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
            }

            if (commentsList.isEmpty()) {
                item {
                    Text("No official comments yet. Feel free to leave some feedback or queries below.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                }
            } else {
                items(commentsList) { c ->
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(c.userName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                                Text(
                                    text = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(c.createdDate)),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                )
                            }
                            Text(c.commentText, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 4.dp))
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = feedCommentText,
                onValueChange = { feedCommentText = it },
                placeholder = { Text("Ask a question or leave feedback...") },
                modifier = Modifier
                    .weight(1f)
                    .testTag("owner_comment_input"),
                singleLine = true
            )
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
                onClick = {
                    if (feedCommentText.isNotBlank()) {
                        viewModel.addComment(feedCommentText)
                        feedCommentText = ""
                    }
                },
                modifier = Modifier.testTag("send_comment_button")
            ) {
                Icon(Icons.Default.Send, contentDescription = "Send", tint = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

// ==========================================
// DECORATIVE & UTIL VIEWS
// ==========================================
@Composable
fun StatCard(title: String, value: String, modifier: Modifier = Modifier, icon: ImageVector, color: Color = MaterialTheme.colorScheme.primary) {
    Card(
        modifier = modifier.height(112.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title.uppercase(),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    ),
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
            }
            Text(value, fontSize = 28.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
        }
    }
}

@Composable
fun StatusBadge(status: String) {
    val (bgColor, textColor) = when (status) {
        "Approved", "Completed" -> StateApproved.copy(alpha = 0.15f) to StateApproved
        "Rejected", "Needs Correction" -> StateRejected.copy(alpha = 0.15f) to StateRejected
        "In Progress" -> StateProgress.copy(alpha = 0.15f) to StateProgress
        "Submitted For Review" -> StatePending.copy(alpha = 0.15f) to StatePending
        else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f) to MaterialTheme.colorScheme.onSurface
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .border(BorderStroke(0.5.dp, textColor.copy(alpha = 0.3f)), RoundedCornerShape(12.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = status,
            color = textColor,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp
        )
    }
}

@Composable
fun EmptyStateView(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(150.dp)
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.HourglassEmpty, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
        }
    }
}
