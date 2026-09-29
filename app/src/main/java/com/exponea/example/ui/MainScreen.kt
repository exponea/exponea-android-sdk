package com.exponea.example.ui

import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.exponea.example.R
import com.exponea.example.automation.AutomationIds
import com.exponea.example.automation.automationId
import com.exponea.example.ui.anonymize.AnonymizeScreen
import com.exponea.example.ui.components.BrBottomNavItem
import com.exponea.example.ui.components.BrBottomNavigation
import com.exponea.example.ui.components.SdkConfigDrawerContent
import com.exponea.example.ui.components.SdkStatusIndicator
import com.exponea.example.ui.contentblocks.ContentBlocksScreen
import com.exponea.example.ui.contentblocks.ContentBlocksXmlActivity
import com.exponea.example.ui.fetching.FetchingScreen
import com.exponea.example.ui.flushing.FlushingScreen
import com.exponea.example.ui.navigation.Routes
import com.exponea.example.ui.theme.BrBlack
import com.exponea.example.ui.theme.BrWhite
import com.exponea.example.ui.tracking.TrackingScreen
import kotlinx.coroutines.launch

private val bottomNavItems = listOf(
    BrBottomNavItem(Routes.FETCHING, "Fetching", R.drawable.ic_cloud_download_black_24dp, AutomationIds.NAV_FETCHING),
    BrBottomNavItem(Routes.TRACKING, "Tracking", R.drawable.ic_track_black, AutomationIds.NAV_TRACKING),
    BrBottomNavItem(Routes.FLUSHING, "Flushing", R.drawable.ic_cloud_upload_black_24dp, AutomationIds.NAV_FLUSHING),
    BrBottomNavItem(Routes.ANONYMIZE, "Anonymize", R.drawable.ic_anonymous, AutomationIds.NAV_ANONYMIZE),
    BrBottomNavItem(Routes.CONTENT_BLOCKS, "In-app CB", R.drawable.ic_content_blocks, AutomationIds.NAV_CONTENT_BLOCKS)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    onShowLogs: () -> Unit,
    deeplinkRoute: String? = null,
    onDeeplinkConsumed: () -> Unit = {}
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val context = LocalContext.current
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    LaunchedEffect(deeplinkRoute) {
        deeplinkRoute?.let { route ->
            navController.navigate(route) {
                popUpTo(navController.graph.findStartDestination().id) {
                    saveState = true
                }
                launchSingleTop = true
                restoreState = true
            }
            onDeeplinkConsumed()
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = false,
        drawerContent = {
            SdkConfigDrawerContent(
                onClose = { scope.launch { drawerState.close() } }
            )
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    navigationIcon = {
                        IconButton(
                            onClick = { scope.launch { drawerState.open() } },
                            modifier = Modifier.automationId(AutomationIds.OPEN_CONFIG_DRAWER)
                        ) {
                            Image(
                                painter = painterResource(R.drawable.ic_br_mini),
                                contentDescription = "Bloomreach configuration",
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    },
                title = {},
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BrBlack,
                    titleContentColor = BrWhite
                ),
                actions = {
                    SdkStatusIndicator()
                    IconButton(
                        onClick = {
                            context.startActivity(Intent(context, ContentBlocksXmlActivity::class.java))
                        }
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_content_blocks_xml),
                            contentDescription = "Open XML content blocks",
                            modifier = Modifier.size(24.dp),
                            tint = BrWhite
                        )
                    }
                    IconButton(
                        onClick = onShowLogs,
                        modifier = Modifier.automationId(AutomationIds.OPEN_LOGS)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_logs),
                            contentDescription = AutomationIds.OPEN_LOGS,
                            modifier = Modifier.size(24.dp),
                            tint = BrWhite
                        )
                    }
                }
            )
        },
        bottomBar = {
            val selectedRoute = bottomNavItems
                .firstOrNull { item ->
                    currentDestination?.hierarchy?.any { it.route == item.route } == true
                }
                ?.route
            BrBottomNavigation(
                items = bottomNavItems,
                selectedRoute = selectedRoute,
                onItemClick = { route ->
                    navController.navigate(route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = Routes.FETCHING,
                modifier = Modifier.padding(innerPadding)
            ) {
                composable(Routes.FETCHING) { FetchingScreen() }
                composable(Routes.TRACKING) { TrackingScreen() }
                composable(Routes.FLUSHING) { FlushingScreen() }
                composable(Routes.ANONYMIZE) { AnonymizeScreen() }
                composable(Routes.CONTENT_BLOCKS) { ContentBlocksScreen() }
            }
        }
    }
}
