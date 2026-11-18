package com.loypos.app.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.AdminPanelSettings
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.MailOutline
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material.icons.outlined.PointOfSale
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.loypos.app.ui.AppWorkspace
import com.loypos.app.ui.LoyPosViewModel
import com.loypos.app.ui.theme.BackOfficeNavy
import com.loypos.app.ui.theme.BackOfficeSlate
import com.loypos.app.ui.theme.BackOfficeTeal
import com.loypos.app.ui.theme.LoyBackground
import com.loypos.app.ui.theme.LoyError
import com.loypos.app.ui.theme.LoyGreen
import com.loypos.app.ui.theme.LoyGreenAccent
import com.loypos.app.ui.theme.LoyTextSecondary

@Composable
fun SplashScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(LoyGreen),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = Color.White,
                modifier = Modifier.size(56.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Outlined.PointOfSale,
                        contentDescription = "LoyPOS",
                        tint = LoyGreen,
                        modifier = Modifier.size(30.dp),
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            Text(
                text = "LoyPOS",
                style = MaterialTheme.typography.titleLarge,
                color = Color.White,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(10.dp))
            CircularProgressIndicator(color = Color.White, strokeWidth = 2.5.dp, modifier = Modifier.size(22.dp))
        }
    }
}

@Composable
fun AuthGateScreen(
    viewModel: LoyPosViewModel,
    hasAnyUser: Boolean,
    hasAnyBackOfficeUser: Boolean,
    initialWorkspace: AppWorkspace = AppWorkspace.POS,
) {
    var workspace by remember(initialWorkspace) { mutableStateOf(initialWorkspace) }
    var showSignUpPos by remember(hasAnyUser) { mutableStateOf(!hasAnyUser) }
    var showSignUpBo by remember(hasAnyBackOfficeUser) { mutableStateOf(!hasAnyBackOfficeUser) }

    val isBackOffice = workspace == AppWorkspace.BACK_OFFICE
    val headerBrush = if (isBackOffice) {
        Brush.linearGradient(listOf(BackOfficeNavy, BackOfficeSlate))
    } else {
        Brush.linearGradient(listOf(LoyGreen, LoyGreenAccent))
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LoyBackground)
            .imePadding()
            .verticalScroll(rememberScrollState()),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(headerBrush)
                .statusBarsPadding()
                .padding(horizontal = 18.dp, vertical = 16.dp),
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 460.dp)
                    .align(Alignment.CenterStart),
            ) {
                Surface(
                    shape = RoundedCornerShape(11.dp),
                    color = Color.White.copy(alpha = 0.2f),
                    modifier = Modifier.size(40.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (isBackOffice) Icons.Outlined.AdminPanelSettings else Icons.Outlined.PointOfSale,
                            contentDescription = if (isBackOffice) "Back Office logo" else "LoyPOS logo",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    text = when {
                        isBackOffice && showSignUpBo -> "Set up Back Office"
                        isBackOffice -> "Back Office Portal"
                        showSignUpPos -> "Set up your rental business"
                        else -> "Welcome back"
                    },
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text = when {
                        isBackOffice && showSignUpBo -> "Create a dedicated Back Office admin login separate from the Rent Desk."
                        isBackOffice -> "Sign in with your Back Office credentials to manage analytics, equipment fleet, staff, and branches."
                        showSignUpPos -> "Create your owner account to manage event rentals offline-first on mobile and desktop."
                        else -> "Sign in as owner or dispatcher to open the Rent Desk."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.9f),
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(14.dp),
            contentAlignment = Alignment.TopCenter,
        ) {
            Column(
                modifier = Modifier.widthIn(max = 460.dp).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(9.dp),
            ) {
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = !isBackOffice,
                        onClick = { workspace = AppWorkspace.POS },
                        shape = SegmentedButtonDefaults.itemShape(0, 2),
                        icon = {
                            Icon(Icons.Outlined.PointOfSale, contentDescription = null, modifier = Modifier.size(16.dp))
                        },
                        modifier = Modifier.testTag("auth_mode_pos"),
                    ) {
                        Text("Rent Desk", style = MaterialTheme.typography.labelLarge)
                    }
                    SegmentedButton(
                        selected = isBackOffice,
                        onClick = { workspace = AppWorkspace.BACK_OFFICE },
                        shape = SegmentedButtonDefaults.itemShape(1, 2),
                        icon = {
                            Icon(Icons.Outlined.AdminPanelSettings, contentDescription = null, modifier = Modifier.size(16.dp))
                        },
                        modifier = Modifier.testTag("auth_mode_backoffice"),
                    ) {
                        Text("Back Office", style = MaterialTheme.typography.labelLarge)
                    }
                }

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(13.dp),
                    color = Color.White,
                    tonalElevation = 1.dp,
                ) {
                    if (isBackOffice) {
                        if (showSignUpBo) {
                            BackOfficeSignUpForm(
                                viewModel = viewModel,
                                hasExistingAccount = hasAnyBackOfficeUser,
                                onSwitchToSignIn = { showSignUpBo = false },
                            )
                        } else {
                            BackOfficeLoginForm(
                                viewModel = viewModel,
                                onSwitchToSignUp = { showSignUpBo = true },
                            )
                        }
                    } else {
                        if (showSignUpPos) {
                            SignUpForm(
                                viewModel = viewModel,
                                onSwitchToSignIn = { showSignUpPos = false },
                            )
                        } else {
                            LoginForm(
                                viewModel = viewModel,
                                onSwitchToSignUp = { showSignUpPos = true },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BackOfficeAuthScreen(
    viewModel: LoyPosViewModel,
    hasAnyBackOfficeUser: Boolean,
    canReturnToPos: Boolean,
    storeName: String,
) {
    if (canReturnToPos) {
        BackHandler { viewModel.switchToPos() }
    }

    var showSignUp by remember(hasAnyBackOfficeUser) { mutableStateOf(!hasAnyBackOfficeUser) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LoyBackground)
            .imePadding()
            .verticalScroll(rememberScrollState()),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.linearGradient(listOf(BackOfficeNavy, BackOfficeSlate)))
                .statusBarsPadding()
                .padding(horizontal = 18.dp, vertical = 16.dp),
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 460.dp)
                    .align(Alignment.CenterStart),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Surface(
                        shape = RoundedCornerShape(11.dp),
                        color = Color.White.copy(alpha = 0.16f),
                        modifier = Modifier.size(40.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Outlined.AdminPanelSettings,
                                contentDescription = "Back Office Auth",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = BackOfficeTeal,
                    ) {
                        Text(
                            text = "BACK OFFICE AUTH",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    text = if (showSignUp) "Set up Back Office Admin" else "$storeName · Back Office",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text = if (showSignUp) {
                        "Create a dedicated Back Office password separate from POS register accounts."
                    } else {
                        "Authenticate to access Back Office dashboards, profit margins, stock controls, and admin settings."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.88f),
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(14.dp),
            contentAlignment = Alignment.TopCenter,
        ) {
            Column(
                modifier = Modifier.widthIn(max = 460.dp).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(9.dp),
            ) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(13.dp),
                    color = Color.White,
                    tonalElevation = 1.dp,
                ) {
                    if (showSignUp) {
                        BackOfficeSignUpForm(
                            viewModel = viewModel,
                            hasExistingAccount = hasAnyBackOfficeUser,
                            initialShopName = storeName,
                            onSwitchToSignIn = { showSignUp = false },
                        )
                    } else {
                        BackOfficeLoginForm(
                            viewModel = viewModel,
                            onSwitchToSignUp = { showSignUp = true },
                        )
                    }
                }

                if (canReturnToPos) {
                    OutlinedButton(
                        onClick = { viewModel.switchToPos() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(42.dp)
                            .testTag("backoffice_auth_return_pos"),
                    ) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Return to Rent Desk")
                    }
                }
            }
        }
    }
}

@Composable
private fun BackOfficeLoginForm(
    viewModel: LoyPosViewModel,
    onSwitchToSignUp: () -> Unit,
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showPwd by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier.padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        OutlinedTextField(
            value = email,
            onValueChange = { email = it; error = null },
            label = { Text("Back Office Email") },
            leadingIcon = { Icon(Icons.Outlined.MailOutline, contentDescription = null) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("bo_login_email_input"),
        )
        OutlinedTextField(
            value = password,
            onValueChange = { password = it; error = null },
            label = { Text("Back Office Password") },
            leadingIcon = { Icon(Icons.Outlined.Lock, contentDescription = null) },
            trailingIcon = {
                IconButton(onClick = { showPwd = !showPwd }) {
                    Icon(
                        if (showPwd) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                        contentDescription = if (showPwd) "Hide password" else "Show password",
                    )
                }
            },
            visualTransformation = if (showPwd) VisualTransformation.None else PasswordVisualTransformation(),
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("bo_login_password_input"),
        )

        if (error != null) {
            Text(error!!, color = LoyError, style = MaterialTheme.typography.bodySmall)
        }

        Button(
            onClick = {
                busy = true
                error = null
                viewModel.signInBackOffice(email, password) { err ->
                    busy = false
                    error = err
                }
            },
            enabled = !busy,
            colors = ButtonDefaults.buttonColors(containerColor = BackOfficeNavy, contentColor = Color.White),
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .testTag("bo_login_button"),
        ) {
            if (busy) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
            } else {
                Text("Unlock Back Office")
            }
        }

        TextButton(
            onClick = onSwitchToSignUp,
            modifier = Modifier.align(Alignment.CenterHorizontally).testTag("bo_switch_to_signup"),
        ) {
            Text("Create a Back Office admin account", color = BackOfficeTeal)
        }
    }
}

@Composable
private fun BackOfficeSignUpForm(
    viewModel: LoyPosViewModel,
    hasExistingAccount: Boolean,
    initialShopName: String = "",
    onSwitchToSignIn: () -> Unit,
) {
    var fullName by remember { mutableStateOf("") }
    var shopName by remember { mutableStateOf(initialShopName) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var showPwd by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier.padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        OutlinedTextField(
            value = fullName,
            onValueChange = { fullName = it; error = null },
            label = { Text("Admin / Manager name") },
            leadingIcon = { Icon(Icons.Outlined.PersonOutline, contentDescription = null) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("bo_signup_name_input"),
        )
        if (initialShopName.isEmpty()) {
            OutlinedTextField(
                value = shopName,
                onValueChange = { shopName = it; error = null },
                label = { Text("Shop name (optional)") },
                leadingIcon = { Icon(Icons.Outlined.Storefront, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("bo_signup_shop_input"),
            )
        }
        OutlinedTextField(
            value = email,
            onValueChange = { email = it; error = null },
            label = { Text("Back Office Email") },
            leadingIcon = { Icon(Icons.Outlined.MailOutline, contentDescription = null) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("bo_signup_email_input"),
        )
        OutlinedTextField(
            value = password,
            onValueChange = { password = it; error = null },
            label = { Text("Password (min 8 characters)") },
            leadingIcon = { Icon(Icons.Outlined.Lock, contentDescription = null) },
            trailingIcon = {
                IconButton(onClick = { showPwd = !showPwd }) {
                    Icon(
                        if (showPwd) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                        contentDescription = if (showPwd) "Hide password" else "Show password",
                    )
                }
            },
            visualTransformation = if (showPwd) VisualTransformation.None else PasswordVisualTransformation(),
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("bo_signup_password_input"),
        )
        OutlinedTextField(
            value = confirm,
            onValueChange = { confirm = it; error = null },
            label = { Text("Confirm password") },
            leadingIcon = { Icon(Icons.Outlined.Lock, contentDescription = null) },
            visualTransformation = if (showPwd) VisualTransformation.None else PasswordVisualTransformation(),
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("bo_signup_confirm_input"),
        )

        if (error != null) {
            Text(error!!, color = LoyError, style = MaterialTheme.typography.bodySmall)
        }

        Button(
            onClick = {
                if (fullName.trim().isEmpty()) {
                    error = "Enter your admin name"
                    return@Button
                }
                if (password != confirm) {
                    error = "Passwords do not match"
                    return@Button
                }
                busy = true
                error = null
                viewModel.signUpBackOffice(email, password, fullName, shopName) { err ->
                    busy = false
                    error = err
                }
            },
            enabled = !busy,
            colors = ButtonDefaults.buttonColors(containerColor = BackOfficeNavy, contentColor = Color.White),
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .testTag("bo_signup_button"),
        ) {
            if (busy) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
            } else {
                Text("Create Back Office Account")
            }
        }

        if (hasExistingAccount) {
            TextButton(
                onClick = onSwitchToSignIn,
                modifier = Modifier.align(Alignment.CenterHorizontally).testTag("bo_switch_to_login"),
            ) {
                Text("Already have a Back Office account? Sign in", color = LoyTextSecondary)
            }
        }
    }
}

@Composable
private fun LoginForm(
    viewModel: LoyPosViewModel,
    onSwitchToSignUp: () -> Unit,
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showPwd by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier.padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        OutlinedTextField(
            value = email,
            onValueChange = { email = it; error = null },
            label = { Text("Email") },
            leadingIcon = { Icon(Icons.Outlined.MailOutline, contentDescription = null) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("login_email_input"),
        )
        OutlinedTextField(
            value = password,
            onValueChange = { password = it; error = null },
            label = { Text("Password / PIN") },
            leadingIcon = { Icon(Icons.Outlined.Lock, contentDescription = null) },
            trailingIcon = {
                IconButton(onClick = { showPwd = !showPwd }) {
                    Icon(
                        if (showPwd) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                        contentDescription = if (showPwd) "Hide password" else "Show password",
                    )
                }
            },
            visualTransformation = if (showPwd) VisualTransformation.None else PasswordVisualTransformation(),
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("login_password_input"),
        )

        if (error != null) {
            Text(error!!, color = LoyError, style = MaterialTheme.typography.bodySmall)
        }

        Button(
            onClick = {
                busy = true
                error = null
                viewModel.signIn(email, password) { err ->
                    busy = false
                    error = err
                }
            },
            enabled = !busy,
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .testTag("login_button"),
        ) {
            if (busy) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
            } else {
                Text("Sign in")
            }
        }

        TextButton(
            onClick = onSwitchToSignUp,
            modifier = Modifier.align(Alignment.CenterHorizontally).testTag("switch_to_signup_button"),
        ) {
            Text("Create a new shop account", color = LoyGreen)
        }
    }
}

@Composable
private fun SignUpForm(
    viewModel: LoyPosViewModel,
    onSwitchToSignIn: () -> Unit,
) {
    var shopName by remember { mutableStateOf("") }
    var branchName by remember { mutableStateOf("Main branch") }
    var ownerName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var showPwd by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier.padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        OutlinedTextField(
            value = shopName,
            onValueChange = { shopName = it; error = null },
            label = { Text("Rental company name") },
            leadingIcon = { Icon(Icons.Outlined.Storefront, contentDescription = null) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("signup_shop_input"),
        )
        OutlinedTextField(
            value = branchName,
            onValueChange = { branchName = it },
            label = { Text("First branch name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("signup_branch_input"),
        )
        OutlinedTextField(
            value = ownerName,
            onValueChange = { ownerName = it },
            label = { Text("Owner name (optional)") },
            leadingIcon = { Icon(Icons.Outlined.PersonOutline, contentDescription = null) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("signup_owner_input"),
        )
        OutlinedTextField(
            value = email,
            onValueChange = { email = it; error = null },
            label = { Text("Email") },
            leadingIcon = { Icon(Icons.Outlined.MailOutline, contentDescription = null) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("signup_email_input"),
        )
        OutlinedTextField(
            value = password,
            onValueChange = { password = it; error = null },
            label = { Text("Password (min 8 characters)") },
            leadingIcon = { Icon(Icons.Outlined.Lock, contentDescription = null) },
            trailingIcon = {
                IconButton(onClick = { showPwd = !showPwd }) {
                    Icon(
                        if (showPwd) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                        contentDescription = if (showPwd) "Hide password" else "Show password",
                    )
                }
            },
            visualTransformation = if (showPwd) VisualTransformation.None else PasswordVisualTransformation(),
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("signup_password_input"),
        )
        OutlinedTextField(
            value = confirm,
            onValueChange = { confirm = it; error = null },
            label = { Text("Confirm password") },
            leadingIcon = { Icon(Icons.Outlined.Lock, contentDescription = null) },
            visualTransformation = if (showPwd) VisualTransformation.None else PasswordVisualTransformation(),
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("signup_confirm_input"),
        )

        if (error != null) {
            Text(error!!, color = LoyError, style = MaterialTheme.typography.bodySmall)
        }

        Button(
            onClick = {
                if (shopName.trim().isEmpty()) {
                    error = "Enter your shop name"
                    return@Button
                }
                if (password != confirm) {
                    error = "Passwords do not match"
                    return@Button
                }
                busy = true
                error = null
                viewModel.signUp(email, password, shopName, ownerName, branchName) { err ->
                    busy = false
                    error = err
                }
            },
            enabled = !busy,
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .testTag("signup_button"),
        ) {
            if (busy) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
            } else {
                Text("Create account")
            }
        }

        TextButton(
            onClick = onSwitchToSignIn,
            modifier = Modifier.align(Alignment.CenterHorizontally).testTag("switch_to_login_button"),
        ) {
            Text("Already have an account? Sign in", color = LoyTextSecondary)
        }
    }
}
