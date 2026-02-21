package com.theveloper.pixelplay.presentation.telegram.auth

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.FocusInteraction
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material.icons.rounded.Sms
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.media3.common.util.UnstableApi
import com.theveloper.pixelplay.R
import com.theveloper.pixelplay.presentation.telegram.dashboard.TelegramDashboardScreen
import com.theveloper.pixelplay.ui.theme.GoogleSansRounded
import com.theveloper.pixelplay.ui.theme.PixelPlayTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import org.drinkless.tdlib.TdApi
import racra.compose.smooth_corner_rect_library.AbsoluteSmoothCornerShape
import java.util.Locale

@AndroidEntryPoint
@OptIn(UnstableApi::class)
class TelegramLoginActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PixelPlayTheme {
                TelegramLoginScreen(onFinish = { finish() })
            }
        }
    }
}

@kotlin.OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
@OptIn(UnstableApi::class)
@Composable
fun TelegramLoginScreen(
    viewModel: TelegramLoginViewModel = hiltViewModel(),
    onFinish: () -> Unit
) {
    val authState by viewModel.authorizationState.collectAsState(initial = null)
    val isLoading by viewModel.isLoading.collectAsState()
    val phoneNumber by viewModel.phoneNumber.collectAsState()
    val code by viewModel.code.collectAsState()
    val password by viewModel.password.collectAsState()
    var showSearchSheet by remember { mutableStateOf(false) }

    if (showSearchSheet) {
        com.theveloper.pixelplay.presentation.telegram.channel.TelegramChannelSearchSheet(
            onDismissRequest = { showSearchSheet = false },
            onSongSelected = { song ->
                viewModel.downloadAndPlay(song)
            }
        )
    }

    val context = androidx.compose.ui.platform.LocalContext.current
    LaunchedEffect(Unit) {
        viewModel.playbackRequest.collect { song: com.theveloper.pixelplay.data.model.Song ->
            val intent = android.content.Intent(context, com.theveloper.pixelplay.MainActivity::class.java).apply {
                action = "com.theveloper.pixelplay.ACTION_PLAY_SONG"
                putExtra("song", song as android.os.Parcelable)
                addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
            context.startActivity(intent)
            onFinish()
        }
    }

    if (authState is TdApi.AuthorizationStateReady && !isLoading) {
        // Show the new Dashboard
        TelegramDashboardScreen(
            onAddChannel = { showSearchSheet = true },
            onBack = onFinish
        )
    } else {
        // Expressive Login UI
        val gradientColors = listOf(
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
            MaterialTheme.colorScheme.surface
        )

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { },
                    navigationIcon = {
                        FilledIconButton(
                            onClick = onFinish,
                            modifier = Modifier.padding(start = 8.dp),
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                                contentColor = MaterialTheme.colorScheme.onSurface
                            )
                        ) {
                            Icon(
                                Icons.AutoMirrored.Rounded.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent
                    )
                )
            },
            containerColor = Color.Transparent
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(brush = Brush.verticalGradient(gradientColors))
                    .padding(paddingValues)
            ) {
                if (isLoading) {
                    // Expressive Loading State
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        LoadingIndicator(
                            modifier = Modifier.size(64.dp),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            text = "Connecting to Telegram...",
                            style = MaterialTheme.typography.titleMedium,
                            fontFamily = GoogleSansRounded,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                } else {
                    // Determine current step for indicator
                    val currentStep = when (authState) {
                        is TdApi.AuthorizationStateWaitPhoneNumber -> 0
                        is TdApi.AuthorizationStateWaitCode -> 1
                        is TdApi.AuthorizationStateWaitPassword -> 2
                        else -> -1
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Spacer(modifier = Modifier.height(32.dp))

                        // Telegram Branding Header
                        TelegramBrandingHeader()

                        Spacer(modifier = Modifier.height(32.dp))

                        // Step Indicator
                        if (currentStep >= 0) {
                            StepIndicator(
                                currentStep = currentStep,
                                totalSteps = 3
                            )
                            Spacer(modifier = Modifier.height(32.dp))
                        }

                        // Animated Content for Auth States
                        AnimatedContent(
                            targetState = authState,
                            transitionSpec = {
                                (slideInHorizontally { width -> width } + fadeIn())
                                    .togetherWith(slideOutHorizontally { width -> -width } + fadeOut())
                            },
                            label = "AuthStateTransition"
                        ) { state ->
                            when (state) {
                                is TdApi.AuthorizationStateWaitPhoneNumber -> {
                                    ExpressivePhoneNumberInput(
                                        phoneNumber = phoneNumber,
                                        onPhoneNumberChanged = viewModel::onPhoneNumberChanged,
                                        onSend = viewModel::sendPhoneNumber
                                    )
                                }
                                is TdApi.AuthorizationStateWaitCode -> {
                                    ExpressiveCodeInput(
                                        code = code,
                                        onCodeChanged = viewModel::onCodeChanged,
                                        onCheck = viewModel::checkCode
                                    )
                                }
                                is TdApi.AuthorizationStateWaitPassword -> {
                                    ExpressivePasswordInput(
                                        password = password,
                                        onPasswordChanged = viewModel::onPasswordChanged,
                                        onCheck = viewModel::checkPassword
                                    )
                                }
                                is TdApi.AuthorizationStateLoggingOut -> StatusMessage("Logging out...")
                                is TdApi.AuthorizationStateClosing -> StatusMessage("Closing...")
                                is TdApi.AuthorizationStateClosed -> StatusMessage("Session Closed")
                                null -> StatusMessage("Initializing Telegram...")
                                else -> StatusMessage("State: ${state?.javaClass?.simpleName}")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TelegramBrandingHeader() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Telegram-style icon
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(R.drawable.telegram),
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = MaterialTheme.colorScheme.onPrimary
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Connect Telegram",
            style = MaterialTheme.typography.headlineMedium,
            fontFamily = GoogleSansRounded,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Stream music from your Telegram channels",
            style = MaterialTheme.typography.bodyLarge,
            fontFamily = GoogleSansRounded,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun StepIndicator(
    currentStep: Int,
    totalSteps: Int
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(totalSteps) { step ->
            val isActive = step <= currentStep
            val isCurrent = step == currentStep

            val scale by animateFloatAsState(
                targetValue = if (isCurrent) 1.2f else 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                ),
                label = "stepScale"
            )

            Box(
                modifier = Modifier
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                    }
                    .size(if (isCurrent) 12.dp else 10.dp)
                    .clip(CircleShape)
                    .background(
                        if (isActive) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outlineVariant
                    )
            )
        }
    }
}

@Composable
private fun StatusMessage(message: String) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            fontFamily = GoogleSansRounded,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@kotlin.OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ExpressivePhoneNumberInput(
    phoneNumber:          String,
    onPhoneNumberChanged: (String) -> Unit,
    onSend:               () -> Unit
) {
    var countryCode      by remember { mutableStateOf("") }
    var localNumber      by remember { mutableStateOf("") }
    var codeFieldFocused by remember { mutableStateOf(false) }
    var numFieldFocused  by remember { mutableStateOf(false) }
    var isExpanded       by remember { mutableStateOf(false) }

    val numFocusRequester  = remember { FocusRequester() }
    val codeFocusRequester = remember { FocusRequester() }
    val focusManager       = LocalFocusManager.current
    val context            = LocalContext.current
    val isActive           = codeFieldFocused || numFieldFocused

    LaunchedEffect(Unit) {
        val tm = context.getSystemService(Context.TELEPHONY_SERVICE)
                as? android.telephony.TelephonyManager
        val iso = tm?.networkCountryIso?.uppercase()?.takeIf { it.isNotEmpty() }
            ?: tm?.simCountryIso?.uppercase()?.takeIf { it.isNotEmpty() }
            ?: Locale.getDefault().country
        getDialCodeForCountry(iso).takeIf { it.isNotEmpty() }?.let { countryCode = it }
    }

    LaunchedEffect(countryCode, localNumber) {
        onPhoneNumberChanged(
            if (countryCode.isNotEmpty()) "+$countryCode$localNumber" else localNumber
        )
    }

    LaunchedEffect(isExpanded) {
        if (isExpanded) {
            if (countryCode.isEmpty()) codeFocusRequester.requestFocus()
            else numFocusRequester.requestFocus()
        }
    }

    val inputShape = AbsoluteSmoothCornerShape(
        cornerRadiusTR = 16.dp, cornerRadiusTL = 16.dp,
        cornerRadiusBR = 16.dp, cornerRadiusBL = 16.dp,
        smoothnessAsPercentTR = 60, smoothnessAsPercentTL = 60,
        smoothnessAsPercentBR = 60, smoothnessAsPercentBL = 60
    )

    val borderColor by animateColorAsState(
        targetValue = if (isActive) MaterialTheme.colorScheme.primary
        else          MaterialTheme.colorScheme.outline,
        label = "borderColor"
    )
    val borderWidth    = if (isActive) 2.dp else 1.dp
    val containerColor = if (isActive) MaterialTheme.colorScheme.surfaceContainerHighest
    else          MaterialTheme.colorScheme.surfaceContainer
    val labelColor     = if (isActive) MaterialTheme.colorScheme.primary
    else          MaterialTheme.colorScheme.onSurfaceVariant

    Column(
        modifier            = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AuthStepHeader(
            icon     = Icons.Rounded.Phone,
            title    = "Phone Number",
            subtitle = "Enter your Telegram phone number with country code"
        )

        Spacer(Modifier.height(24.dp))

        Box(modifier = Modifier.fillMaxWidth()) {

            // Field surface
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(inputShape)
                    .background(containerColor)
                    .border(BorderStroke(borderWidth, borderColor), inputShape)
                    .clickable(enabled = !isExpanded) { isExpanded = true }
            ) {
                AnimatedContent(
                    targetState   = isExpanded,
                    transitionSpec = {
                        (fadeIn(tween(200)) + slideInHorizontally { it / 4 })
                            .togetherWith(fadeOut(tween(150)))
                    },
                    label = "phoneExpand"
                ) { expanded ->
                    if (!expanded) {
                        // Collapsed: phone icon + "Phone number" hint
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier          = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp)
                        ) {
                            Icon(
                                imageVector        = Icons.Rounded.Phone,
                                contentDescription = null,
                                tint               = MaterialTheme.colorScheme.primary,
                                modifier           = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(12.dp))
                            Text(
                                text  = "Phone number",
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontFamily = GoogleSansRounded,
                                    color      = MaterialTheme.colorScheme.onSurfaceVariant
                                        .copy(alpha = 0.5f)
                                )
                            )
                        }
                    } else {
                        //  Expanded: +code | local number
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier          = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp)
                        ) {
                            Text(
                                text       = "+",
                                style      = MaterialTheme.typography.bodyLarge,
                                fontFamily = GoogleSansRounded,
                                fontWeight = FontWeight.SemiBold,
                                color      = MaterialTheme.colorScheme.onSurface
                            )

                            BasicTextField(
                                value         = countryCode,
                                onValueChange = { raw ->
                                    val digits = raw.filter { it.isDigit() }.take(4)
                                    countryCode = digits
                                    val shouldJump = raw.endsWith(" ") ||
                                            digits.length >= 3 ||
                                            (digits.isNotEmpty() && isoForDialCode(digits).isNotEmpty())
                                    if (shouldJump) {
                                        countryCode = digits.trimEnd()
                                        numFocusRequester.requestFocus()
                                    }
                                },
                                modifier        = Modifier
                                    .width(40.dp)
                                    .focusRequester(codeFocusRequester)
                                    .onFocusChanged { codeFieldFocused = it.isFocused },
                                textStyle       = MaterialTheme.typography.bodyLarge.copy(
                                    fontFamily = GoogleSansRounded,
                                    fontWeight = FontWeight.SemiBold,
                                    color      = MaterialTheme.colorScheme.onSurface
                                ),
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Number,
                                    imeAction    = ImeAction.Next
                                ),
                                keyboardActions = KeyboardActions(
                                    onNext = { numFocusRequester.requestFocus() }
                                ),
                                cursorBrush   = SolidColor(MaterialTheme.colorScheme.primary),
                                decorationBox = { inner ->
                                    Box(contentAlignment = Alignment.CenterStart) {
                                        if (countryCode.isEmpty()) {
                                            Text(
                                                text  = "91",
                                                style = MaterialTheme.typography.bodyLarge.copy(
                                                    fontFamily = GoogleSansRounded,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color      = MaterialTheme.colorScheme
                                                        .onSurfaceVariant
                                                        .copy(alpha = 0.4f)
                                                )
                                            )
                                        }
                                        inner()
                                    }
                                }
                            )

                            Spacer(Modifier.width(8.dp))

                            Box(
                                modifier = Modifier
                                    .width(1.dp)
                                    .height(24.dp)
                                    .background(
                                        if (isActive)
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                                        else
                                            MaterialTheme.colorScheme.outlineVariant
                                    )
                            )

                            Spacer(Modifier.width(12.dp))

                            BasicTextField(
                                value         = localNumber,
                                onValueChange = { raw ->
                                    localNumber = raw.filter { it.isDigit() }.take(15)
                                },
                                modifier      = Modifier
                                    .weight(1f)
                                    .focusRequester(numFocusRequester)
                                    .onFocusChanged { numFieldFocused = it.isFocused },
                                textStyle     = MaterialTheme.typography.bodyLarge.copy(
                                    fontFamily    = GoogleSansRounded,
                                    color         = MaterialTheme.colorScheme.onSurface,
                                    letterSpacing = 1.sp
                                ),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number,imeAction=ImeAction.Done),
                                cursorBrush   = SolidColor(MaterialTheme.colorScheme.primary),
                                decorationBox = { inner ->
                                    Box(contentAlignment = Alignment.CenterStart) {
                                        if (localNumber.isEmpty()) {
                                            Text(
                                                text  = "00000 00000",
                                                style = MaterialTheme.typography.bodyLarge.copy(
                                                    fontFamily    = GoogleSansRounded,
                                                    color         = MaterialTheme.colorScheme
                                                        .onSurfaceVariant
                                                        .copy(alpha = 0.4f),
                                                    letterSpacing = 1.sp
                                                )
                                            )
                                        }
                                        inner()
                                    }
                                }
                            )
                        }
                    }
                }
            }

            // Floating label — animates in when expanded
            // Replace the entire AnimatedVisibility block with this:
            if (isExpanded) {
                Text(
                    text     = "Phone number",
                    style    = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = GoogleSansRounded,
                        color      = labelColor
                    ),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(start = 12.dp)
                        .offset(y = (-8).dp)
                        .background(containerColor, RoundedCornerShape(4.dp))
                        .padding(horizontal = 4.dp)
                )
            }
        }

        Spacer(Modifier.height(32.dp))

        ExpressiveButton(
            text    = "Send Code",
            onClick = onSend,
            enabled = countryCode.isNotEmpty() && localNumber.isNotBlank()
        )
    }
}

// Reverse lookup: given a dial code string, return the ISO that maps to it
// Used to auto-detect when user has typed a complete known code (e.g. "1", "44", "91")
fun isoForDialCode(dialCode: String): String {
    val allIsos = listOf(
        "AF","AL","DZ","AD","AO","AR","AM","AU","AT","AZ","BS","BH","BD","BY","BE",
        "BZ","BJ","BT","BO","BA","BW","BR","BN","BG","BF","BI","KH","CM","CA","CV",
        "CF","TD","CL","CN","CO","KM","CG","CR","HR","CU","CY","CZ","DK","DJ","DO",
        "EC","EG","SV","GQ","ER","EE","ET","FJ","FI","FR","GA","GM","GE","DE","GH",
        "GR","GT","GN","GW","GY","HT","HN","HK","HU","IS","IN","ID","IR","IQ","IE",
        "IL","IT","JM","JP","JO","KZ","KE","KP","KR","KW","KG","LA","LV","LB","LS",
        "LR","LY","LI","LT","LU","MO","MK","MG","MW","MY","MV","ML","MT","MR","MU",
        "MX","MD","MC","MN","ME","MA","MZ","MM","NA","NP","NL","NZ","NI","NE","NG",
        "NO","OM","PK","PW","PA","PG","PY","PE","PH","PL","PT","QA","RO","RU","RW",
        "SA","SN","RS","SL","SG","SK","SI","SO","ZA","SS","ES","LK","SD","SR","SZ",
        "SE","CH","SY","TW","TJ","TZ","TH","TL","TG","TO","TT","TN","TR","TM","UG",
        "UA","AE","GB","US","UY","UZ","VU","VE","VN","YE","ZM","ZW"
    )
    return allIsos.firstOrNull { getDialCodeForCountry(it) == dialCode } ?: ""
}

fun getDialCodeForCountry(isoCode: String): String = when (isoCode.uppercase()) {
    "AF" -> "93";  "AL" -> "355"; "DZ" -> "213"; "AD" -> "376"; "AO" -> "244"
    "AR" -> "54";  "AM" -> "374"; "AU" -> "61";  "AT" -> "43";  "AZ" -> "994"
    "BS" -> "1";   "BH" -> "973"; "BD" -> "880"; "BY" -> "375"; "BE" -> "32"
    "BZ" -> "501"; "BJ" -> "229"; "BT" -> "975"; "BO" -> "591"; "BA" -> "387"
    "BW" -> "267"; "BR" -> "55";  "BN" -> "673"; "BG" -> "359"; "BF" -> "226"
    "BI" -> "257"; "KH" -> "855"; "CM" -> "237"; "CA" -> "1";   "CV" -> "238"
    "CF" -> "236"; "TD" -> "235"; "CL" -> "56";  "CN" -> "86";  "CO" -> "57"
    "KM" -> "269"; "CG" -> "242"; "CR" -> "506"; "HR" -> "385"; "CU" -> "53"
    "CY" -> "357"; "CZ" -> "420"; "DK" -> "45";  "DJ" -> "253"; "DO" -> "1"
    "EC" -> "593"; "EG" -> "20";  "SV" -> "503"; "GQ" -> "240"; "ER" -> "291"
    "EE" -> "372"; "ET" -> "251"; "FJ" -> "679"; "FI" -> "358"; "FR" -> "33"
    "GA" -> "241"; "GM" -> "220"; "GE" -> "995"; "DE" -> "49";  "GH" -> "233"
    "GR" -> "30";  "GT" -> "502"; "GN" -> "224"; "GW" -> "245"; "GY" -> "592"
    "HT" -> "509"; "HN" -> "504"; "HK" -> "852"; "HU" -> "36";  "IS" -> "354"
    "IN" -> "91";  "ID" -> "62";  "IR" -> "98";  "IQ" -> "964"; "IE" -> "353"
    "IL" -> "972"; "IT" -> "39";  "JM" -> "1";   "JP" -> "81";  "JO" -> "962"
    "KZ" -> "7";   "KE" -> "254"; "KP" -> "850"; "KR" -> "82";  "KW" -> "965"
    "KG" -> "996"; "LA" -> "856"; "LV" -> "371"; "LB" -> "961"; "LS" -> "266"
    "LR" -> "231"; "LY" -> "218"; "LI" -> "423"; "LT" -> "370"; "LU" -> "352"
    "MO" -> "853"; "MK" -> "389"; "MG" -> "261"; "MW" -> "265"; "MY" -> "60"
    "MV" -> "960"; "ML" -> "223"; "MT" -> "356"; "MR" -> "222"; "MU" -> "230"
    "MX" -> "52";  "MD" -> "373"; "MC" -> "377"; "MN" -> "976"; "ME" -> "382"
    "MA" -> "212"; "MZ" -> "258"; "MM" -> "95";  "NA" -> "264"; "NP" -> "977"
    "NL" -> "31";  "NZ" -> "64";  "NI" -> "505"; "NE" -> "227"; "NG" -> "234"
    "NO" -> "47";  "OM" -> "968"; "PK" -> "92";  "PW" -> "680"; "PA" -> "507"
    "PG" -> "675"; "PY" -> "595"; "PE" -> "51";  "PH" -> "63";  "PL" -> "48"
    "PT" -> "351"; "QA" -> "974"; "RO" -> "40";  "RU" -> "7";   "RW" -> "250"
    "SA" -> "966"; "SN" -> "221"; "RS" -> "381"; "SL" -> "232"; "SG" -> "65"
    "SK" -> "421"; "SI" -> "386"; "SO" -> "252"; "ZA" -> "27";  "SS" -> "211"
    "ES" -> "34";  "LK" -> "94";  "SD" -> "249"; "SR" -> "597"; "SZ" -> "268"
    "SE" -> "46";  "CH" -> "41";  "SY" -> "963"; "TW" -> "886"; "TJ" -> "992"
    "TZ" -> "255"; "TH" -> "66";  "TL" -> "670"; "TG" -> "228"; "TO" -> "676"
    "TT" -> "1";   "TN" -> "216"; "TR" -> "90";  "TM" -> "993"; "UG" -> "256"
    "UA" -> "380"; "AE" -> "971"; "GB" -> "44";  "US" -> "1";   "UY" -> "598"
    "UZ" -> "998"; "VU" -> "678"; "VE" -> "58";  "VN" -> "84";  "YE" -> "967"
    "ZM" -> "260"; "ZW" -> "263"
    else -> ""
}

@kotlin.OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ExpressiveCodeInput(
    code: String,
    onCodeChanged: (String) -> Unit,
    onCheck: () -> Unit
) {
    val inputShape = AbsoluteSmoothCornerShape(
        cornerRadiusTR = 16.dp, cornerRadiusTL = 16.dp,
        cornerRadiusBR = 16.dp, cornerRadiusBL = 16.dp,
        smoothnessAsPercentTR = 60, smoothnessAsPercentTL = 60,
        smoothnessAsPercentBR = 60, smoothnessAsPercentBL = 60
    )

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AuthStepHeader(
            icon = Icons.Rounded.Sms,
            title = "Verification Code",
            subtitle = "Enter the code sent to your Telegram app"
        )

        Spacer(Modifier.height(24.dp))

        OutlinedTextField(
            value = code,
            onValueChange = onCodeChanged,
            label = { Text("Code", fontFamily = GoogleSansRounded) },
            placeholder = { Text("12345") },
            leadingIcon = {
                Icon(
                    Icons.Rounded.Sms,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = inputShape,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = Color.Transparent
            )
        )

        Spacer(Modifier.height(32.dp))

        ExpressiveButton(
            text = "Verify",
            onClick = onCheck,
            enabled = code.isNotBlank()
        )
    }
}

@kotlin.OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ExpressivePasswordInput(
    password: String,
    onPasswordChanged: (String) -> Unit,
    onCheck: () -> Unit
) {
    val inputShape = AbsoluteSmoothCornerShape(
        cornerRadiusTR = 16.dp, cornerRadiusTL = 16.dp,
        cornerRadiusBR = 16.dp, cornerRadiusBL = 16.dp,
        smoothnessAsPercentTR = 60, smoothnessAsPercentTL = 60,
        smoothnessAsPercentBR = 60, smoothnessAsPercentBL = 60
    )

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AuthStepHeader(
            icon = Icons.Rounded.Lock,
            title = "Two-Factor Password",
            subtitle = "Enter your 2FA password to continue"
        )

        Spacer(Modifier.height(24.dp))

        OutlinedTextField(
            value = password,
            onValueChange = onPasswordChanged,
            label = { Text("Password", fontFamily = GoogleSansRounded) },
            leadingIcon = {
                Icon(
                    Icons.Rounded.Lock,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = inputShape,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = Color.Transparent
            )
        )

        Spacer(Modifier.height(32.dp))

        ExpressiveButton(
            text = "Verify Password",
            onClick = onCheck,
            enabled = password.isNotBlank()
        )
    }
}

@Composable
private fun AuthStepHeader(
    icon: ImageVector,
    title: String,
    subtitle: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(0.dp)
    ){
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.secondaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(28.dp),
                tint = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontFamily = GoogleSansRounded,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                fontFamily = GoogleSansRounded,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Start
            )
        }
    }
}

@kotlin.OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ExpressiveButton(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean = true
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "buttonScale"
    )

    MediumExtendedFloatingActionButton(
        text = {
            Text(
                text = text,
                fontFamily = GoogleSansRounded,
                fontWeight = FontWeight.SemiBold
            )
        },
        icon = { Icon(Icons.Rounded.Check, contentDescription = null) },
        onClick = onClick,
        expanded = true,
        shape = CircleShape,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
        containerColor = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHighest,
        contentColor = if (enabled) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
        interactionSource = interactionSource
    )
}
