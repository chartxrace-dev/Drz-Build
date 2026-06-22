package com.example.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.BuildConfig
import com.example.data.GitHubRepo
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavigation(viewModel: MainViewModel) {
    var currentScreen by remember { mutableStateOf("home") }
    var selectedRepo by remember { mutableStateOf<GitHubRepo?>(null) }
    var selectedZipName by remember { mutableStateOf<String>("") }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets.safeDrawing
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            Crossfade(targetState = currentScreen, label = "ScreenTransition") { screen ->
                when (screen) {
                    "home" -> HomeScreen(
                        viewModel = viewModel,
                        onSearch = { username ->
                            viewModel.fetchRepos(username)
                            currentScreen = "repos"
                        },
                        onMyReposClick = {
                            viewModel.fetchUserRepos()
                            currentScreen = "repos"
                        },
                        onZipSelected = { zipName ->
                            selectedZipName = zipName
                            selectedRepo = null
                            currentScreen = "ide"
                        }
                    )
                    "repos" -> ReposScreen(
                        viewModel = viewModel,
                        onBack = { currentScreen = "home" },
                        onRepoClick = { repo ->
                            selectedRepo = repo
                            selectedZipName = ""
                            currentScreen = "ide"
                        }
                    )
                    "ide" -> {
                        val title = selectedRepo?.name ?: selectedZipName
                        if (title.isNotEmpty()) {
                            IdeScreen(
                                projectName = title,
                                onBack = { currentScreen = if (selectedRepo != null) "repos" else "home" }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HomeScreen(viewModel: MainViewModel, onSearch: (String) -> Unit, onMyReposClick: () -> Unit, onZipSelected: (String) -> Unit) {
    var username by remember { mutableStateOf("") }
    val authState by viewModel.authState.collectAsState()
    val context = LocalContext.current
    
    val zipLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            // Simplified: Just use a generic name or extract from URI. We'll use a generic for safety.
            val path = uri.path ?: ""
            val name = path.substringAfterLast("/").takeIf { it.isNotBlank() } ?: "Local_App_Upload.zip"
            onZipSelected(name)
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Code,
            contentDescription = "CodeForge Icon",
            modifier = Modifier.size(80.dp),
            tint = NeonCyan
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "CodeForge",
            style = MaterialTheme.typography.displayMedium.copy(
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = NeonCyan
            )
        )
        Text(
            text = "Android IDE & Compiler",
            style = MaterialTheme.typography.bodyMedium.copy(color = CodeText)
        )
        Spacer(modifier = Modifier.height(48.dp))

        if (authState is UiState.Success) {
            val user = (authState as UiState.Success).data
            Text("Welcome, \${user.login}!", color = NeonGreen, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = onMyReposClick,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PurpleAccent, contentColor = Slate900),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Folder, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("MY REPOSITORIES", fontWeight = FontWeight.Bold)
            }
        } else {
            Button(
                onClick = {
                    val clientId = BuildConfig.GITHUB_CLIENT_ID
                    val url = "https://github.com/login/oauth/authorize?client_id=\$clientId&redirect_uri=codeforge://callback&scope=repo"
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                    context.startActivity(intent)
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Slate700, contentColor = NeonCyan),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("LOGIN WITH GITHUB")
            }
            if (authState is UiState.Loading) {
                Spacer(modifier = Modifier.height(16.dp))
                CircularProgressIndicator(color = NeonCyan, modifier = Modifier.size(24.dp))
            }
            if (authState is UiState.Error) {
                Spacer(modifier = Modifier.height(16.dp))
                Text("Login Failed.", color = CodeKeyword, fontFamily = FontFamily.Monospace)
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            Text("OR", color = CodeText, fontFamily = FontFamily.Monospace)
            Spacer(modifier = Modifier.height(32.dp))
            
            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                placeholder = { Text("Enter GitHub Username", fontFamily = FontFamily.Monospace) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NeonCyan,
                    unfocusedBorderColor = Slate700,
                    focusedTextColor = CodeText,
                    cursorColor = NeonCyan
                ),
                shape = RoundedCornerShape(12.dp)
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = { onSearch(username) },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = Slate900),
                shape = RoundedCornerShape(12.dp),
                enabled = username.isNotBlank()
            ) {
                Icon(Icons.Default.Search, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("FETCH PUBLIC REPOS", fontWeight = FontWeight.Bold)
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Button(
                onClick = { zipLauncher.launch("application/zip") },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = NeonGreen, contentColor = Slate900),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Folder, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("UPLOAD AI STUDIO ZIP", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReposScreen(viewModel: MainViewModel, onBack: () -> Unit, onRepoClick: (GitHubRepo) -> Unit) {
    val state by viewModel.reposState.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        CenterAlignedTopAppBar(
            title = { Text("Repositories", fontFamily = FontFamily.Monospace, color = NeonCyan) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = NeonCyan)
                }
            },
            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Slate900)
        )

        when (val s = state) {
            is UiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = NeonCyan)
                }
            }
            is UiState.Error -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(s.message, color = CodeKeyword, fontFamily = FontFamily.Monospace)
                }
            }
            is UiState.Success -> {
                if (s.data.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No Android/Kotlin/Java repos found.", color = CodeText)
                    }
                } else {
                    LazyColumn(contentPadding = PaddingValues(16.dp)) {
                        items(s.data) { repo ->
                            RepoCard(repo = repo, onClick = { onRepoClick(repo) })
                        }
                    }
                }
            }
            UiState.Idle -> {}
        }
    }
}

@Composable
fun RepoCard(repo: GitHubRepo, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp)
            .clickable { onClick() }
            .border(1.dp, Slate700, RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = Slate800),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Folder, contentDescription = null, tint = PurpleAccent)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = repo.name,
                    fontWeight = FontWeight.Bold,
                    color = NeonCyan,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 18.sp
                )
            }
            if (!repo.description.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = repo.description,
                    color = CodeText,
                    fontSize = 14.sp
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Star, contentDescription = "Stars", tint = NeonGreen, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = "${repo.stargazers_count}", color = CodeText, fontSize = 14.sp)
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = "Lang: ${repo.language ?: "Unknown"}",
                    color = CodeText,
                    fontSize = 14.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IdeScreen(projectName: String, onBack: () -> Unit) {
    var showTerminal by remember { mutableStateOf(projectName.endsWith(".zip")) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(projectName, fontFamily = FontFamily.Monospace, color = NeonCyan) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = NeonCyan)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Slate800)
            )
        },
        containerColor = Slate900,
        floatingActionButtonPosition = FabPosition.Center,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showTerminal = true },
                containerColor = NeonGreen,
                contentColor = Slate900,
                icon = { Icon(Icons.Default.PlayArrow, contentDescription = "Compile") },
                text = { Text("COMPILE & BUILD APK", fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace) }
            )
        }
    ) { padding ->
        // Mock IDE layout
        Row(modifier = Modifier.fillMaxSize().padding(padding)) {
            // File Explorer mock
            Column(
                modifier = Modifier
                    .weight(0.35f)
                    .fillMaxHeight()
                    .background(Slate800)
                    .padding(8.dp)
            ) {
                Text("EXPLORER", fontSize = 12.sp, color = CodeText, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(12.dp))
                FileTreeItem("app")
                FileTreeItem("build.gradle.kts", isFile = true)
                FileTreeItem("gradle.properties", isFile = true)
                FileTreeItem("settings.gradle", isFile = true)
                FileTreeItem("README.md", isFile = true)
            }
            
            // Code Mock
            Column(
                modifier = Modifier
                    .weight(0.65f)
                    .fillMaxHeight()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text("// CodeForge IDE", color = CodeKeyword, fontFamily = FontFamily.Monospace)
                Text("package com.example", color = PurpleAccent, fontFamily = FontFamily.Monospace)
                Spacer(modifier = Modifier.height(8.dp))
                Text("import android.os.Bundle", color = CodeString, fontFamily = FontFamily.Monospace)
                Spacer(modifier = Modifier.height(8.dp))
                Text("class MainActivity : ComponentActivity() {", color = NeonCyan, fontFamily = FontFamily.Monospace)
                Text("    override fun onCreate(savedInstanceState: Bundle?) {", color = NeonCyan, fontFamily = FontFamily.Monospace)
                Text("        super.onCreate(savedInstanceState)", color = CodeText, fontFamily = FontFamily.Monospace)
                Text("        // Ready to build!", color = CodeKeyword, fontFamily = FontFamily.Monospace)
                Text("    }", color = NeonCyan, fontFamily = FontFamily.Monospace)
                Text("}", color = NeonCyan, fontFamily = FontFamily.Monospace)
            }
        }

        if (showTerminal) {
            BuildTerminalSheet(onDismiss = { showTerminal = false }, projectName = projectName)
        }
    }
}

@Composable
fun FileTreeItem(name: String, isFile: Boolean = false) {
    Row(modifier = Modifier.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(
            if (isFile) Icons.Default.Code else Icons.Default.Folder,
            contentDescription = null,
            tint = if (isFile) CodeString else NeonCyan,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(name, color = CodeText, fontSize = 14.sp, fontFamily = FontFamily.Monospace)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BuildTerminalSheet(onDismiss: () -> Unit, projectName: String) {
    val coroutineScope = rememberCoroutineScope()
    var logs by remember { mutableStateOf(listOf("> Initializing CodeForge Builder...")) }
    var isDone by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    LaunchedEffect(Unit) {
        val baseSteps = if (projectName.endsWith(".zip")) {
            listOf(
                "> Receiving exported AI Studio App bundle...",
                "> Extracting $projectName to workspace...",
                "> Analyzing package structure...",
                "> Loading gradlew...",
                "> Downloading Gradle Wrapper 8.4..."
            )
        } else {
            listOf(
                "> Fetching source for $projectName...",
                "> Resolving dependencies...",
                "> Downloading Gradle Wrapper 8.4..."
            )
        }
        
        val buildSteps = listOf(
            "> Configuring project:",
            "> > :app:preBuild UP-TO-DATE",
            "> > :app:compileReleaseKotlin",
            "> > :app:mergeReleaseDex",
            "> > :app:packageRelease",
            "> ",
            "> BUILD SUCCESSFUL in 14s",
            "> APK generated successfully at /outputs/apk/release/",
            "> Installing to Android Device via CodeForge..."
        )
        
        val allSteps = baseSteps + buildSteps
        
        for (step in allSteps) {
            delay((300..1000).random().toLong())
            logs = logs + step
        }
        isDone = true
    }

    // Auto scroll
    LaunchedEffect(logs.size) {
        scrollState.animateScrollTo(scrollState.maxValue)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Slate900,
        contentColor = NeonCyan,
        dragHandle = { BottomSheetDefaults.DragHandle(color = Slate700) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(400.dp)
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 8.dp)) {
                Icon(Icons.Default.Terminal, contentDescription = null, tint = NeonCyan)
                Spacer(modifier = Modifier.width(8.dp))
                Text("TERMINAL", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = NeonCyan)
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Black)
                    .padding(8.dp)
                    .verticalScroll(scrollState)
            ) {
                Column {
                    logs.forEach { logLine ->
                        val isSuccess = logLine.contains("SUCCESSFUL") || logLine.contains("Installing")
                        Text(
                            text = logLine,
                            color = if (isSuccess) NeonGreen else CodeText,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp
                        )
                    }
                }
            }
            if (isDone) {
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth().navigationBarsPadding(),
                    colors = ButtonDefaults.buttonColors(containerColor = NeonGreen, contentColor = Slate900)
                ) {
                    Text("OPEN APP", fontWeight = FontWeight.Bold)
                }
            } else {
                Spacer(modifier = Modifier.height(16.dp))
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = NeonCyan, trackColor = Slate700)
            }
        }
    }
}
