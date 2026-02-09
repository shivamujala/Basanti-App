@file:OptIn(ExperimentalMaterial3Api::class, UnstableApi::class, ExperimentalSharedTransitionApi::class)

package com.basanti.app

import android.app.Activity
import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import android.graphics.Canvas
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.Keep
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.core.content.FileProvider
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.remoteConfigSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.CancellationException

@Keep
data class Video(
    val id: String = "",
    val userID: String = "",
    val requestID: String = "",
    val title: String = "",
    val videoUrl: String = "",
    val thumbnailUrl: String = ""
)

@Keep
data class MovieRequest(
    val id: String = "",
    val userID: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val phoneNumber: String = "",
    val email: String = "",
    val movieTitle: String = "",
    val tmdbID: Int = 0,
    val mediaType: String = "",
    val tmdbUrl: String = "",
    val posterUrl: String = "",
    val screenshotUrl: String = "",
    val message: String = "",
    val timestamp: com.google.firebase.Timestamp? = null,
    val status: String = "Pending"
)

// Theme Colors
private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFD0BCFF),
    secondary = Color(0xFFCCC2DC),
    tertiary = Color(0xFFEFB8C8)
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF6650a4),
    secondary = Color(0xFF625b71),
    tertiary = Color(0xFF7D5260)
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val sharedPref = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        
        // Initialize Cloudinary
        try {
            val config = mapOf(
                "cloud_name" to "dbyehzzvi"
            )
            MediaManager.init(this, config)
        } catch (e: Exception) {
            // Log.e("CLOUDINARY", "Init failed: ${e.message}")
        }

        setContent {
            var isDarkMode by rememberSaveable { 
                mutableStateOf(sharedPref.getBoolean("is_dark_mode", false)) 
            }
            
            MaterialTheme(
                colorScheme = if (isDarkMode) DarkColorScheme else LightColorScheme
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    DashboardScreen(
                        isDarkMode = isDarkMode,
                        onThemeChange = { dark -> 
                            isDarkMode = dark
                            sharedPref.edit().putBoolean("is_dark_mode", dark).apply()
                        },
                        onLogout = {
                            FirebaseAuth.getInstance().signOut()
                            startActivity(Intent(this, LoginActivity::class.java))
                            finish()
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun DashboardScreen(
    isDarkMode: Boolean,
    onThemeChange: (Boolean) -> Unit,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) }
    var selectedVideo by remember { mutableStateOf<Video?>(null) }
    var showExitDialog by remember { mutableStateOf(false) }
    var showForceUpdateDialog by remember { mutableStateOf(false) }
    var updateUrl by remember { mutableStateOf("") }
    var isDownloading by remember { mutableStateOf(false) }

    val remoteConfig = remember { FirebaseRemoteConfig.getInstance() }
    
    fun performUpdateCheck(manual: Boolean = false) {
        remoteConfig.fetchAndActivate().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val latestVersion = remoteConfig.getLong("latest_version")
                val url = remoteConfig.getString("update_url")
                if (url.isNotEmpty()) updateUrl = url
                
                if (latestVersion > BuildConfig.VERSION_CODE) {
                    showForceUpdateDialog = true
                } else if (manual) {
                    Toast.makeText(context, "App is up to date", Toast.LENGTH_SHORT).show()
                }
            } else if (manual) {
                Toast.makeText(context, "Update check failed", Toast.LENGTH_SHORT).show()
            }
        }
    }

    LaunchedEffect(Unit) {
        val configSettings = remoteConfigSettings {
            minimumFetchIntervalInSeconds = 3600
        }
        remoteConfig.setConfigSettingsAsync(configSettings)
        remoteConfig.setDefaultsAsync(mapOf("latest_version" to 1L, "update_url" to ""))
        performUpdateCheck(manual = false)
    }

    if (showForceUpdateDialog) {
        AlertDialog(
            onDismissRequest = { },
            title = { Text("Update Available", fontWeight = FontWeight.Bold) },
            text = { 
                Column {
                    Text("A new version of Basanti is available. Download now to continue.")
                    if (isDownloading) {
                        Spacer(modifier = Modifier.height(16.dp))
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        Text("Downloading...", fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (!isDownloading && updateUrl.isNotEmpty()) {
                            isDownloading = true
                            downloadAndInstallApk(context, updateUrl)
                        } else if (updateUrl.isEmpty()) {
                            Toast.makeText(context, "Update URL not found", Toast.LENGTH_SHORT).show()
                        }
                    },
                    enabled = !isDownloading
                ) {
                    Text(if (isDownloading) "Downloading..." else "Update Now")
                }
            }
        )
    }

    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            title = { Text("Exit App") },
            text = { Text("Do you want to exit the app?") },
            confirmButton = {
                TextButton(onClick = { (context as? Activity)?.finish() }) {
                    Text("Exit")
                }
            },
            dismissButton = {
                TextButton(onClick = { showExitDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    SharedTransitionLayout {
        AnimatedContent(
            targetState = selectedVideo,
            label = "video_transition",
            transitionSpec = {
                fadeIn(tween(500)) togetherWith fadeOut(tween(500))
            }
        ) { targetVideo ->
            if (targetVideo != null) {
                AdvancedExoPlayer(
                    video = targetVideo,
                    sharedTransitionScope = this@SharedTransitionLayout,
                    animatedVisibilityScope = this@AnimatedContent
                ) {
                    selectedVideo = null
                }
            } else {
                BackHandler {
                    if (selectedTab != 0) {
                        selectedTab = 0
                    } else {
                        showExitDialog = true
                    }
                }

                Scaffold(
                    bottomBar = {
                        NavigationBar {
                            NavigationBarItem(
                                selected = selectedTab == 0,
                                onClick = { selectedTab = 0 },
                                icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                                label = { Text("Home") }
                            )
                            NavigationBarItem(
                                selected = selectedTab == 1,
                                onClick = { selectedTab = 1 },
                                icon = { Icon(Icons.Default.AddBox, contentDescription = "Request") },
                                label = { Text("Request") }
                            )
                            NavigationBarItem(
                                selected = selectedTab == 2,
                                onClick = { selectedTab = 2 },
                                icon = { Icon(Icons.Default.Settings, contentDescription = "Setting") },
                                label = { Text("Setting") }
                            )
                        }
                    }
                ) { innerPadding ->
                    Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                        when (selectedTab) {
                            0 -> HomeScreen(
                                onVideoClick = { selectedVideo = it },
                                sharedTransitionScope = this@SharedTransitionLayout,
                                animatedVisibilityScope = this@AnimatedContent
                            )
                            1 -> MovieRequestScreen()
                            2 -> SettingTab(
                                isDarkMode = isDarkMode,
                                onThemeChange = onThemeChange,
                                onCheckUpdates = { performUpdateCheck(manual = true) },
                                onLogout = onLogout
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MovieRequestScreen() {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val db = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()
    var movieTitle by remember { mutableStateOf("") }
    var requestMsg by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }
    
    // User Profile Info
    var userFirstName by remember { mutableStateOf("") }
    var userLastName by remember { mutableStateOf("") }
    var userPhone by remember { mutableStateOf("") }
    var userEmail by remember { mutableStateOf("") }

    val currentUid = auth.currentUser?.uid ?: ""
    val previousRequests = remember { mutableStateListOf<MovieRequest>() }
    var showLimitDialog by remember { mutableStateOf(false) }

    // Use Snapshot Listener for Real-time updates
    DisposableEffect(currentUid) {
        if (currentUid.isNotEmpty()) {
            val query = db.collection("movie_requests")
                .whereEqualTo("userID", currentUid)
            
            val listenerRegistration = query.addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("FIRESTORE_ERROR", "Listen failed: ${error.message}", error)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val list = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(MovieRequest::class.java)?.copy(id = doc.id)
                    }.sortedByDescending { it.timestamp }
                    
                    previousRequests.clear()
                    previousRequests.addAll(list)
                }
            }
            onDispose { listenerRegistration.remove() }
        } else {
            onDispose { }
        }
    }

    LaunchedEffect(currentUid) {
        if (currentUid.isNotEmpty()) {
            db.collection("users").document(currentUid).get().addOnSuccessListener { doc ->
                if (doc.exists()) {
                    userFirstName = doc.getString("firstName") ?: ""
                    userLastName = doc.getString("lastName") ?: ""
                    userPhone = doc.getString("phoneNumber") ?: ""
                    userEmail = doc.getString("email") ?: ""
                }
            }
        }
    }

    // TMDB States
    var searchResults by remember { mutableStateOf<List<TmdbMovie>>(emptyList()) }
    var showDropdown by remember { mutableStateOf(false) }
    var selectedTmdbMovie by remember { mutableStateOf<TmdbMovie?>(null) }
    var isSearchingTmdb by remember { mutableStateOf(false) }
    
    // Custom Browser/Screenshot States
    var showNotFoundDialog by remember { mutableStateOf(false) }
    var showBrowser by remember { mutableStateOf(false) }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var webViewInstance by remember { mutableStateOf<WebView?>(null) }
    var showFullScreenPreview by remember { mutableStateOf(false) }
    
    // Use your actual TMDB API Key here
    val TMDB_API_KEY = "ca1f03affd324c47af93e549386271cb"
    
    val tmdbApi = remember {
        Retrofit.Builder()
            .baseUrl("https://api.themoviedb.org/3/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(TmdbApi::class.java)
    }

    LaunchedEffect(movieTitle) {
        if (movieTitle.trim().isNotEmpty() && selectedTmdbMovie == null) {
            isSearchingTmdb = true
            delay(600) // Debounce
            try {
                val response = tmdbApi.searchMulti(TMDB_API_KEY, movieTitle.trim())
                searchResults = response.results
                    .filter { it.media_type == "movie" || it.media_type == "tv" }
                    .sortedByDescending { it.popularity ?: 0.0 }
                showDropdown = searchResults.isNotEmpty()
                
                if (searchResults.isEmpty() && movieTitle.trim().length > 2) {
                    showNotFoundDialog = true
                }
            } catch (e: Exception) {
                if (e !is CancellationException) {
                    Log.e("TMDB_ERROR", "Search failed: ${e.message}")
                }
            } finally {
                isSearchingTmdb = false
            }
        } else {
            showDropdown = false
            searchResults = emptyList()
            isSearchingTmdb = false
        }
    }

    if (showLimitDialog) {
        AlertDialog(
            onDismissRequest = { showLimitDialog = false },
            title = { Text("Request Limit Reached", fontWeight = FontWeight.Bold) },
            text = { Text("Please wait, as there are already three movie requests in the queue. You can submit more once your current requests are processed.") },
            confirmButton = {
                TextButton(onClick = { showLimitDialog = false }) {
                    Text("OK")
                }
            }
        )
    }

    if (showFullScreenPreview && selectedImageUri != null) {
        Dialog(
            onDismissRequest = { showFullScreenPreview = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
                AsyncImage(
                    model = selectedImageUri,
                    contentDescription = "Full Preview",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
                IconButton(
                    onClick = { showFullScreenPreview = false },
                    modifier = Modifier.align(Alignment.TopEnd).padding(16.dp).background(Color.Black.copy(alpha = 0.5f), CircleShape)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                }
            }
        }
    }

    if (showBrowser) {
        ModalBottomSheet(
            onDismissRequest = { showBrowser = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Google Search", fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 8.dp))
                        Button(onClick = { 
                            webViewInstance?.let { wv ->
                                try {
                                    val bitmap = Bitmap.createBitmap(wv.width, wv.height, Bitmap.Config.ARGB_8888)
                                    val canvas = Canvas(bitmap)
                                    wv.draw(canvas)
                                    
                                    val file = File(context.cacheDir, "capture_${System.currentTimeMillis()}.png")
                                    FileOutputStream(file).use { out ->
                                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                                    }
                                    selectedImageUri = Uri.fromFile(file)
                                    showBrowser = false
                                    Toast.makeText(context, "Screenshot Captured!", Toast.LENGTH_SHORT).show()
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Failed to capture screenshot", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }) {
                            Icon(Icons.Default.Screenshot, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Take Screenshot")
                        }
                    }
                    
                    AndroidView(
                        factory = { ctx ->
                            WebView(ctx).apply {
                                webViewClient = WebViewClient()
                                settings.javaScriptEnabled = true
                                webViewInstance = this
                                try {
                                    val query = URLEncoder.encode(movieTitle, "UTF-8")
                                    loadUrl("https://www.google.com/search?q=$query")
                                } catch (e: Exception) {
                                    loadUrl("https://www.google.com")
                                }
                            }
                        },
                        modifier = Modifier.fillMaxSize().weight(1f)
                    )
                    
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                        elevation = CardDefaults.cardElevation(4.dp),
                        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(16.dp).fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                "Find movie and click 'Take Screenshot' at the top.",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }

    if (showNotFoundDialog) {
        AlertDialog(
            onDismissRequest = { showNotFoundDialog = false },
            title = { Text("Movie Not Found", fontWeight = FontWeight.Bold) },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "Please search for the movie on Google and upload a screenshot to help us find it.",
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Button(
                        onClick = { 
                            showNotFoundDialog = false 
                            showBrowser = true 
                        },
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                    ) {
                        Icon(Icons.Default.Search, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Search on Google")
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showNotFoundDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()).imePadding()) {
        Text("Request a Movie", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text("Tell us which movie you want to see next!", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Box(modifier = Modifier.fillMaxWidth()) {
            Column {
                OutlinedTextField(
                    value = movieTitle,
                    onValueChange = { 
                        movieTitle = it
                        if (it.isNotEmpty()) isSearchingTmdb = true
                        if (selectedTmdbMovie != null) selectedTmdbMovie = null 
                    },
                    label = { Text("Movie Title") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    trailingIcon = {
                        if (isSearchingTmdb) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        } else if (movieTitle.isNotEmpty()) {
                            IconButton(onClick = { movieTitle = ""; showDropdown = false; selectedTmdbMovie = null; selectedImageUri = null }) {
                                Icon(Icons.Default.Close, contentDescription = null)
                            }
                        }
                    }
                )
                if (isSearchingTmdb) {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(2.dp)
                            .padding(horizontal = 2.dp),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            if (showDropdown) {
                Popup(
                    alignment = Alignment.TopStart,
                    offset = androidx.compose.ui.unit.IntOffset(0, 160),
                    onDismissRequest = { showDropdown = false },
                    properties = PopupProperties(focusable = false)
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth(0.9f)
                            .heightIn(max = 350.dp),
                        elevation = CardDefaults.cardElevation(8.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        LazyColumn {
                            items(searchResults) { movie ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            movieTitle = movie.displayTitle
                                            selectedTmdbMovie = movie
                                            showDropdown = false
                                            focusManager.clearFocus()
                                        }
                                        .padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    AsyncImage(
                                        model = movie.fullPosterUrl,
                                        contentDescription = null,
                                        modifier = Modifier.size(45.dp, 65.dp).clip(RoundedCornerShape(4.dp)),
                                        contentScale = ContentScale.Crop
                                    )
                                    Column(modifier = Modifier.padding(start = 12.dp)) {
                                        Text(movie.displayTitle, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text("${movie.media_type.replaceFirstChar { it.uppercase() }} • ${movie.displayDate}", fontSize = 12.sp, color = Color.Gray)
                                    }
                                }
                                HorizontalDivider(thickness = 0.5.dp, color = Color.LightGray.copy(alpha = 0.5f))
                            }
                        }
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))

        AnimatedVisibility(
            visible = selectedTmdbMovie != null,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            selectedTmdbMovie?.let { movie ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                ) {
                    Row(modifier = Modifier.padding(12.dp)) {
                        AsyncImage(
                            model = movie.fullPosterUrl,
                            contentDescription = null,
                            modifier = Modifier.size(90.dp, 135.dp).clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop
                        )
                        Column(modifier = Modifier.padding(start = 16.dp)) {
                            Text(movie.displayTitle, fontWeight = FontWeight.Bold, fontSize = 17.sp, color = MaterialTheme.colorScheme.primary)
                            Text(movie.displayDate, fontSize = 13.sp, color = Color.Gray)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                movie.overview ?: "No description available.",
                                fontSize = 13.sp,
                                lineHeight = 18.sp
                            )
                        }
                    }
                }
            }
        }

        OutlinedTextField(
            value = requestMsg,
            onValueChange = { requestMsg = it },
            label = { Text("Message (Optional)") },
            modifier = Modifier.fillMaxWidth().height(120.dp),
            maxLines = 5
        )
        
        Spacer(modifier = Modifier.height(16.dp))

        if (selectedImageUri != null) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("Screenshot Attached (Click to Preview)", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(modifier = Modifier.fillMaxWidth()) {
                        AsyncImage(
                            model = selectedImageUri,
                            contentDescription = "Attached Screenshot",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { showFullScreenPreview = true },
                            contentScale = ContentScale.Crop
                        )
                        IconButton(
                            onClick = { selectedImageUri = null },
                            modifier = Modifier.align(Alignment.TopEnd).background(Color.Black.copy(alpha = 0.5f), CircleShape).size(32.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        if (isSubmitting) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                CircularProgressIndicator()
                Text("Submitting Request...", modifier = Modifier.padding(top = 8.dp), fontSize = 12.sp)
            }
        } else {
            Button(
                onClick = {
                    if (movieTitle.isBlank()) {
                        Toast.makeText(context, "Please enter movie title", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    val pendingCount = previousRequests.count { it.status == "Pending" }
                    if (pendingCount >= 3) {
                        showLimitDialog = true
                        return@Button
                    }

                    isSubmitting = true
                    
                    fun saveToFirestore(finalScreenshotUrl: String = "") {
                        val tmdbID = selectedTmdbMovie?.id ?: 0
                        val mediaType = selectedTmdbMovie?.media_type ?: "movie"
                        val tmdbUrl = if (tmdbID != 0) {
                            if (mediaType == "tv") "https://www.themoviedb.org/tv/$tmdbID"
                            else "https://www.themoviedb.org/movie/$tmdbID"
                        } else ""

                        val requestData = mapOf(
                            "userID" to (auth.currentUser?.uid ?: ""),
                            "firstName" to userFirstName,
                            "lastName" to userLastName,
                            "phoneNumber" to userPhone,
                            "email" to userEmail,
                            "movieTitle" to movieTitle,
                            "tmdbID" to tmdbID,
                            "mediaType" to mediaType,
                            "tmdbUrl" to tmdbUrl,
                            "posterUrl" to (selectedTmdbMovie?.fullPosterUrl ?: ""),
                            "screenshotUrl" to finalScreenshotUrl,
                            "message" to requestMsg,
                            "timestamp" to com.google.firebase.Timestamp.now(),
                            "status" to "Pending"
                        )
                        db.collection("movie_requests").add(requestData)
                            .addOnSuccessListener { docRef ->
                                Toast.makeText(context, "Request submitted successfully!", Toast.LENGTH_LONG).show()
                                
                                // Safe Coroutine launch for background process
                                CoroutineScope(Dispatchers.IO).launch {
                                    val vidkingUrl = "https://vidking.net/embed/$mediaType/$tmdbID"
                                    try {
                                        val url = URL(vidkingUrl)
                                        val connection = url.openConnection() as HttpURLConnection
                                        connection.requestMethod = "HEAD"
                                        connection.connectTimeout = 5000
                                        connection.readTimeout = 5000
                                        val responseCode = connection.responseCode
                                        
                                        if (responseCode == HttpURLConnection.HTTP_OK) {
                                            delay(60000) // User requested 1 minute delay
                                            
                                            withContext(Dispatchers.Main) {
                                                db.collection("movie_requests").document(docRef.id)
                                                    .update("status", "Fulfilled")
                                                
                                                val videoData = mapOf(
                                                    "userID" to (auth.currentUser?.uid ?: ""),
                                                    "requestID" to docRef.id,
                                                    "title" to movieTitle,
                                                    "videoUrl" to vidkingUrl,
                                                    "thumbnailUrl" to (selectedTmdbMovie?.fullBackdropUrl ?: selectedTmdbMovie?.fullPosterUrl ?: "")
                                                )
                                                db.collection("videos").add(videoData)
                                            }
                                        }
                                    } catch (e: Exception) {
                                        Log.e("AUTO_FULFILL", "Check failed: ${e.message}")
                                    }
                                }

                                isSubmitting = false
                                movieTitle = ""
                                requestMsg = ""
                                selectedTmdbMovie = null
                                selectedImageUri = null
                            }
                            .addOnFailureListener {
                                isSubmitting = false
                                Toast.makeText(context, "Failed to submit request", Toast.LENGTH_SHORT).show()
                            }
                    }

                    if (selectedImageUri != null) {
                        MediaManager.get().upload(selectedImageUri)
                            .unsigned("dxtv8m1l")
                            .callback(object : UploadCallback {
                                override fun onStart(requestId: String) {}
                                override fun onProgress(requestId: String, bytes: Long, totalBytes: Long) {}
                                override fun onSuccess(requestId: String, resultData: Map<*, *>) {
                                    val imageUrl = resultData["secure_url"] as String
                                    saveToFirestore(imageUrl)
                                }
                                override fun onError(requestId: String, error: ErrorInfo) {
                                    isSubmitting = false
                                    Toast.makeText(context, "Screenshot upload failed", Toast.LENGTH_SHORT).show()
                                }
                                override fun onReschedule(requestId: String, error: ErrorInfo) {}
                            }).dispatch()
                    } else {
                        saveToFirestore("")
                    }
                },
                modifier = Modifier.fillMaxWidth().pointerInput(Unit) {
                    detectTapGestures(
                        onLongPress = {
                            val sharedPref = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
                            sharedPref.edit().clear().apply()
                            Toast.makeText(context, "Preferences Cleared", Toast.LENGTH_SHORT).show()
                        }
                    )
                },
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(if (selectedImageUri != null) "Submit Request with Screenshot" else "Submit Request", fontWeight = FontWeight.Bold)
            }
        }

        if (previousRequests.isNotEmpty()) {
            Spacer(modifier = Modifier.height(40.dp))
            Text("Previous Requests", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))
            
            previousRequests.forEach { req ->
                key(req.id) {
                    var showDeleteConfirm by remember { mutableStateOf(false) }
                    
                    if (showDeleteConfirm) {
                        AlertDialog(
                            onDismissRequest = { showDeleteConfirm = false },
                            title = { Text("Delete Request") },
                            text = { Text("Are you sure you want to delete this request?") },
                            confirmButton = {
                                TextButton(onClick = {
                                    val requestId = req.id
                                    db.collection("movie_requests").document(requestId).delete()
                                        .addOnSuccessListener {
                                            // Delete associated videos
                                            db.collection("videos")
                                                .whereEqualTo("requestID", requestId)
                                                .get()
                                                .addOnSuccessListener { snapshot ->
                                                    for (document in snapshot.documents) {
                                                        document.reference.delete()
                                                    }
                                                }
                                            
                                            Toast.makeText(context, "Request deleted", Toast.LENGTH_SHORT).show()
                                            previousRequests.remove(req)
                                        }
                                        .addOnFailureListener {
                                            Toast.makeText(context, "Delete failed", Toast.LENGTH_SHORT).show()
                                        }
                                    showDeleteConfirm = false
                                }) {
                                    Text("Delete", color = MaterialTheme.colorScheme.error)
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { showDeleteConfirm = false }) {
                                    Text("Cancel")
                                }
                            }
                        )
                    }

                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                    ) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(50.dp, 75.dp)) {
                                val imageModel = if (req.posterUrl.isNotEmpty()) req.posterUrl else if (req.screenshotUrl.isNotEmpty()) req.screenshotUrl else null
                                if (imageModel != null) {
                                    AsyncImage(
                                        model = imageModel,
                                        contentDescription = null,
                                        modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(4.dp)),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Box(modifier = Modifier.fillMaxSize().background(Color.Gray.copy(alpha = 0.2f), RoundedCornerShape(4.dp)), contentAlignment = Alignment.Center) {
                                        Icon(Icons.Default.Movie, contentDescription = null, tint = Color.Gray)
                                    }
                                }
                            }
                            
                            Column(modifier = Modifier.padding(start = 16.dp).weight(1f)) {
                                Text(req.movieTitle, fontWeight = FontWeight.Bold, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                val dateStr = req.timestamp?.toDate()?.let { 
                                    SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(it)
                                } ?: ""
                                Text(dateStr, fontSize = 12.sp, color = Color.Gray)
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Surface(
                                    color = when(req.status) {
                                        "Pending" -> Color(0xFFFF9800).copy(alpha = 0.1f)
                                        "Fulfilled", "Success" -> Color(0xFF4CAF50).copy(alpha = 0.1f)
                                        "Rejected" -> Color(0xFFF44336).copy(alpha = 0.1f)
                                        else -> Color.Gray.copy(alpha = 0.1f)
                                    },
                                    shape = RoundedCornerShape(16.dp)
                                ) {
                                    Text(
                                        text = req.status,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = when(req.status) {
                                            "Pending" -> Color(0xFFFF9800)
                                            "Fulfilled", "Success" -> Color(0xFF4CAF50)
                                            "Rejected" -> Color(0xFFF44336)
                                            else -> Color.Gray
                                        }
                                    )
                                }
                                IconButton(onClick = { showDeleteConfirm = true }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete Request", tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f), modifier = Modifier.size(20.dp))
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

fun downloadAndInstallApk(context: Context, url: String) {
    val destination = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS).toString() + "/Basanti_Update.apk"
    val file = File(destination)
    if (file.exists()) file.delete()

    val request = DownloadManager.Request(Uri.parse(url))
        .setTitle("Basanti Update")
        .setDescription("Downloading new version...")
        .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
        .setDestinationUri(Uri.fromFile(file))

    val manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
    val downloadId = manager.enqueue(request)

    val onComplete = object : BroadcastReceiver() {
        override fun onReceive(ctxt: Context, intent: Intent) {
            val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
            if (downloadId == id) {
                installApk(context, file)
                context.unregisterReceiver(this)
            }
        }
    }
    context.registerReceiver(onComplete, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE), Context.RECEIVER_EXPORTED)
}

fun installApk(context: Context, file: File) {
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val install = Intent(Intent.ACTION_VIEW)
    install.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    install.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    install.setDataAndType(uri, "application/vnd.android.package-archive")
    context.startActivity(install)
}

@Composable
fun HomeScreen(
    onVideoClick: (Video) -> Unit,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope
) {
    val db = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()
    val currentUserId = auth.currentUser?.uid ?: ""
    val videos = remember { mutableStateListOf<Video>() }
    var isLoading by remember { mutableStateOf(true) }
    var isRefreshing by remember { mutableStateOf(false) }

    DisposableEffect(currentUserId) {
        if (currentUserId.isNotEmpty()) {
            val query = db.collection("videos")
                .whereEqualTo("userID", currentUserId)
            
            val listenerRegistration = query.addSnapshotListener { snapshot, error ->
                if (error != null) {
                    isLoading = false
                    isRefreshing = false
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val list = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(Video::class.java)?.copy(id = doc.id)
                    }
                    videos.clear()
                    videos.addAll(list)
                }
                isLoading = false
                isRefreshing = false
            }
            onDispose { listenerRegistration.remove() }
        } else {
            onDispose { }
        }
    }

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = {
            isRefreshing = true
            // Snapshot listener handles the refresh, we just toggle isRefreshing
            // But if we want to force refresh, we could potentially do something here
            // However, listeners are real-time, so it's usually not needed.
            // Just a small delay to simulate refresh if needed:
            CoroutineScope(Dispatchers.Main).launch {
                delay(1000)
                isRefreshing = false
            }
        },
        modifier = Modifier.fillMaxSize()
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Text("Your Library", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))

            if (isLoading && !isRefreshing) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (videos.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("No videos assigned to you.")
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("UID: $currentUserId", fontSize = 10.sp, color = Color.Gray)
                    }
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    items(videos) { video ->
                        VideoThumbnailItem(
                            video = video,
                            onClick = { onVideoClick(video) },
                            sharedTransitionScope = sharedTransitionScope,
                            animatedVisibilityScope = animatedVisibilityScope
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun VideoThumbnailItem(
    video: Video,
    onClick: () -> Unit,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope
) {
    with(sharedTransitionScope) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClick() }
                .sharedElement(
                    rememberSharedContentState(key = "video_${video.id}"),
                    animatedVisibilityScope = animatedVisibilityScope
                ),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
        ) {
            Column {
                Box(modifier = Modifier.fillMaxWidth().height(200.dp)) {
                    AsyncImage(
                        model = video.thumbnailUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    Box(
                        modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color.White.copy(alpha = 0.8f),
                            modifier = Modifier.size(56.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Play",
                                modifier = Modifier.padding(12.dp).size(32.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
                Text(
                    text = video.title,
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@Composable
fun AdvancedExoPlayer(
    video: Video,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val isVidking = video.videoUrl.contains("vidking.net")

    if (isVidking) {
        VidkingPlayer(
            video = video,
            sharedTransitionScope = sharedTransitionScope,
            animatedVisibilityScope = animatedVisibilityScope,
            onBack = onBack
        )
    } else {
        val lifecycleOwner = LocalLifecycleOwner.current
        var isBuffering by remember { mutableStateOf(true) }
        var isPlaying by remember { mutableStateOf(true) }
        var showControls by remember { mutableStateOf(true) }
        var resizeMode by remember { mutableIntStateOf(AspectRatioFrameLayout.RESIZE_MODE_FIT) }
        var playbackProgress by remember { mutableFloatStateOf(0f) }
        var currentTime by remember { mutableLongStateOf(0L) }
        var totalDuration by remember { mutableLongStateOf(0L) }
        var isLandscape by remember { mutableStateOf(false) }

        val exoPlayer = remember {
            ExoPlayer.Builder(context).build().apply {
                val mediaItem = MediaItem.fromUri(video.videoUrl)
                setMediaItem(mediaItem)
                prepare()
                playWhenReady = true
                addListener(object : Player.Listener {
                    override fun onPlaybackStateChanged(state: Int) {
                        isBuffering = state == Player.STATE_BUFFERING
                        if (state == Player.STATE_READY) {
                            totalDuration = duration
                        }
                    }
                    override fun onIsPlayingChanged(playing: Boolean) {
                        isPlaying = playing
                    }
                })
            }
        }

        fun toggleSystemUI(landscape: Boolean) {
            val window = (context as? Activity)?.window ?: return
            val controller = WindowInsetsControllerCompat(window, window.decorView)
            if (landscape) {
                controller.hide(WindowInsetsCompat.Type.systemBars())
                controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            } else {
                controller.show(WindowInsetsCompat.Type.systemBars())
            }
        }

        LaunchedEffect(isLandscape) {
            toggleSystemUI(isLandscape)
        }

        DisposableEffect(lifecycleOwner) {
            val observer = LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_PAUSE) {
                    exoPlayer.pause()
                }
            }
            lifecycleOwner.lifecycle.addObserver(observer)
            onDispose {
                lifecycleOwner.lifecycle.removeObserver(observer)
            }
        }

        LaunchedEffect(isPlaying) {
            while (true) {
                if (isPlaying) {
                    currentTime = exoPlayer.currentPosition
                    playbackProgress = if (totalDuration > 0) {
                        currentTime.toFloat() / totalDuration
                    } else 0f
                }
                delay(500)
            }
        }

        BackHandler {
            if (isLandscape) {
                isLandscape = false
                (context as? Activity)?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            } else {
                onBack()
            }
        }

        LaunchedEffect(showControls) {
            if (showControls) {
                delay(5000)
                showControls = false
            }
        }

        DisposableEffect(Unit) {
            onDispose {
                exoPlayer.release()
                toggleSystemUI(false)
                (context as? Activity)?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            }
        }

        fun formatTime(ms: Long): String {
            val totalSeconds = ms / 1000
            val minutes = totalSeconds / 60
            val seconds = totalSeconds % 60
            return String.format(Locale.getDefault(), "%d:%02d", minutes, seconds)
        }

        with(sharedTransitionScope) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
                    .sharedElement(
                        rememberSharedContentState(key = "video_${video.id}"),
                        animatedVisibilityScope = animatedVisibilityScope
                    )
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = { showControls = !showControls },
                            onDoubleTap = { offset ->
                                val width = size.width
                                if (offset.x < width / 2) {
                                    exoPlayer.seekBack()
                                } else {
                                    exoPlayer.seekForward()
                                }
                            }
                        )
                    }
            ) {
                AndroidView(
                    factory = {
                        PlayerView(it).apply {
                            player = exoPlayer
                            useController = false
                            // Using TextureView can fix surface issues on some devices (MTK chips)
                            // where SurfaceView causes a black screen or "Null anb" errors.
                            @Suppress("DEPRECATION")
                            setShutterBackgroundColor(android.graphics.Color.TRANSPARENT)
                            this.resizeMode = resizeMode
                            layoutParams = FrameLayout.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                        }
                    },
                    update = { 
                        it.resizeMode = resizeMode
                    },
                    modifier = Modifier.fillMaxSize().align(Alignment.Center)
                )

                if (isBuffering) {
                    CircularProgressIndicator(
                        color = Color.Red,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                AnimatedVisibility(
                    visible = showControls,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.TopCenter)
                                .background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.7f), Color.Transparent)))
                                .padding(top = if(isLandscape) 16.dp else 48.dp, start = 16.dp, end = 16.dp, bottom = 16.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = {
                                    if (isLandscape) {
                                        isLandscape = false
                                        (context as? Activity)?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                                    } else {
                                        onBack()
                                    }
                                }) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                                }
                                Text(
                                    video.title,
                                    color = Color.White,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    modifier = Modifier.padding(start = 8.dp).weight(1f)
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.align(Alignment.Center),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(24.dp)
                        ) {
                            IconButton(onClick = { exoPlayer.seekBack() }, modifier = Modifier.size(48.dp)) {
                                Icon(Icons.Default.Replay10, contentDescription = null, tint = Color.White, modifier = Modifier.fillMaxSize())
                            }
                            
                            Surface(
                                shape = CircleShape,
                                color = Color.White.copy(alpha = 0.2f),
                                modifier = Modifier.size(84.dp).clickable { if (isPlaying) exoPlayer.pause() else exoPlayer.play() }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(48.dp)
                                    )
                                }
                            }

                            IconButton(onClick = { exoPlayer.seekForward() }, modifier = Modifier.size(48.dp)) {
                                Icon(Icons.Default.Forward10, contentDescription = null, tint = Color.White, modifier = Modifier.fillMaxSize())
                            }
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.BottomCenter)
                                .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f))))
                                .padding(bottom = if(isLandscape) 16.dp else 32.dp, start = 16.dp, end = 16.dp)
                        ) {
                            Column {
                                Slider(
                                    value = playbackProgress,
                                    onValueChange = {
                                        playbackProgress = it
                                        exoPlayer.seekTo((it * totalDuration).toLong())
                                    },
                                    modifier = Modifier.fillMaxWidth().height(24.dp),
                                    colors = SliderDefaults.colors(thumbColor = Color.Red, activeTrackColor = Color.Red)
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("${formatTime(currentTime)} / ${formatTime(totalDuration)}", color = Color.White, fontSize = 12.sp)
                                    Spacer(modifier = Modifier.weight(1f))
                                    IconButton(onClick = {
                                        isLandscape = !isLandscape
                                        (context as? Activity)?.requestedOrientation = if (isLandscape) ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE else ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                                    }) {
                                        Icon(if (isLandscape) Icons.Default.FullscreenExit else Icons.Default.ScreenRotation, contentDescription = null, tint = Color.White)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun VidkingPlayer(
    video: Video,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var isLandscape by remember { mutableStateOf(false) }

    fun toggleSystemUI(landscape: Boolean) {
        val window = (context as? Activity)?.window ?: return
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        if (landscape) {
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        } else {
            controller.show(WindowInsetsCompat.Type.systemBars())
        }
    }

    LaunchedEffect(isLandscape) {
        toggleSystemUI(isLandscape)
    }

    BackHandler {
        if (isLandscape) {
            isLandscape = false
            (context as? Activity)?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        } else {
            onBack()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            toggleSystemUI(false)
            (context as? Activity)?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
    }

    with(sharedTransitionScope) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .sharedElement(
                    rememberSharedContentState(key = "video_${video.id}"),
                    animatedVisibilityScope = animatedVisibilityScope
                )
        ) {
            AndroidView(
                factory = { ctx ->
                    WebView(ctx).apply {
                        settings.apply {
                            javaScriptEnabled = true
                            domStorageEnabled = true
                            mediaPlaybackRequiresUserGesture = false
                            cacheMode = WebSettings.LOAD_DEFAULT
                            userAgentString = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
                        }
                        
                        webViewClient = object : WebViewClient() {
                            override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                                return if (url?.contains("vidking.net") == true) false else true
                            }
                        }
                        webChromeClient = WebChromeClient()
                        loadUrl(video.videoUrl)
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopStart)
                    .background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.6f), Color.Transparent)))
                    .padding(top = if(isLandscape) 16.dp else 48.dp, start = 16.dp, end = 16.dp, bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = {
                    if (isLandscape) {
                        isLandscape = false
                        (context as? Activity)?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                    } else {
                        onBack()
                    }
                }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                Text(
                    video.title,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 8.dp).weight(1f)
                )
                IconButton(onClick = {
                    isLandscape = !isLandscape
                    (context as? Activity)?.requestedOrientation = if (isLandscape) ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE else ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                }) {
                    Icon(Icons.Default.ScreenRotation, contentDescription = "Rotate", tint = Color.White)
                }
            }
        }
    }
}

@Composable
fun SettingTab(
    isDarkMode: Boolean,
    onThemeChange: (Boolean) -> Unit,
    onCheckUpdates: () -> Unit,
    onLogout: () -> Unit
) {
    var showSettingInfo by remember { mutableStateOf(false) }

    if (showSettingInfo) {
        SettingInfoScreen(onBack = { showSettingInfo = false }, onLogout = onLogout)
    } else {
        SettingList(
            isDarkMode = isDarkMode,
            onThemeChange = { onThemeChange(it) },
            onCheckUpdates = { onCheckUpdates() }, 
            onSettingInfoClick = { showSettingInfo = true }, 
            onLogout = onLogout
        )
    }
}

@Composable
fun SettingList(
    isDarkMode: Boolean,
    onThemeChange: (Boolean) -> Unit,
    onCheckUpdates: () -> Unit,
    onSettingInfoClick: () -> Unit, 
    onLogout: () -> Unit
) {
    var showLogoutDialog by remember { mutableStateOf(false) }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Logout") },
            text = { Text("Are you sure you want to logout?") },
            confirmButton = {
                TextButton(onClick = {
                    showLogoutDialog = false
                    onLogout()
                }) {
                    Text("Logout", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Text("Settings", modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        
        ListItem(
            headlineContent = { Text("Profile info") },
            leadingContent = { Icon(Icons.Default.Person, contentDescription = null) },
            trailingContent = { Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null) },
            modifier = Modifier.clickable { onSettingInfoClick() }
        )
        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
        
        ListItem(
            headlineContent = { Text("Check for updates") },
            leadingContent = { Icon(Icons.Default.SystemUpdate, contentDescription = null) },
            modifier = Modifier.clickable { onCheckUpdates() }
        )
        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
        
        ListItem(
            headlineContent = { Text("Theme") },
            supportingContent = { Text(if (isDarkMode) "Dark" else "Light") },
            leadingContent = { Icon(if (isDarkMode) Icons.Default.DarkMode else Icons.Default.LightMode, contentDescription = null) },
            trailingContent = {
                Switch(
                    checked = isDarkMode,
                    onCheckedChange = { onThemeChange(it) }
                )
            }
        )
        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
        
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                Button(
                    onClick = { showLogoutDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Logout")
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Made by Shivam Ujala",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.Gray
                )
            }
        }
    }
}

@Composable
fun SettingInfoScreen(onBack: () -> Unit, onLogout: () -> Unit) {
    val context = LocalContext.current
    val db = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()
    val uid = auth.currentUser?.uid ?: ""
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }
    var isUpdating by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Logout") },
            text = { Text("Are you sure you want to logout?") },
            confirmButton = {
                TextButton(onClick = {
                    showLogoutDialog = false
                    onLogout()
                }) {
                    Text("Logout", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    BackHandler { onBack() }

    LaunchedEffect(Unit) {
        if (uid.isNotEmpty()) {
            db.collection("users").document(uid).get().addOnSuccessListener { document ->
                if (document.exists()) {
                    firstName = document.getString("firstName") ?: ""
                    lastName = document.getString("lastName") ?: ""
                    phoneNumber = document.getString("phoneNumber") ?: ""
                    email = document.getString("email") ?: ""
                }
                isLoading = false
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { onBack() }) { 
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Text("Profile Info", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.weight(1f))
            IconButton(onClick = { showLogoutDialog = true }) {
                Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Logout", tint = MaterialTheme.colorScheme.error)
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
        if (isLoading) {
            CircularProgressIndicator()
        } else {
            OutlinedTextField(value = firstName, onValueChange = { firstName = it }, label = { Text("First Name") }, modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(value = lastName, onValueChange = { lastName = it }, label = { Text("Last Name") }, modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(value = phoneNumber, onValueChange = {}, label = { Text("Phone Number") }, modifier = Modifier.fillMaxWidth(), enabled = false)
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(value = email, onValueChange = {}, label = { Text("Email Address") }, modifier = Modifier.fillMaxWidth(), enabled = false)
            Spacer(modifier = Modifier.height(32.dp))
            if (isUpdating) {
                CircularProgressIndicator()
            } else {
                Button(onClick = {
                    isUpdating = true
                    db.collection("users").document(uid).set(
                        mapOf("firstName" to firstName, "lastName" to lastName),
                        com.google.firebase.firestore.SetOptions.merge()
                    ).addOnSuccessListener { 
                        isUpdating = false
                        Toast.makeText(context, "Updated", Toast.LENGTH_SHORT).show() 
                    }.addOnFailureListener {
                        isUpdating = false
                        Toast.makeText(context, "Update failed", Toast.LENGTH_SHORT).show()
                    }
                }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                    Text("Update", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
