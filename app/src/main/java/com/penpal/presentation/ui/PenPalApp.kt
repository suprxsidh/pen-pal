package com.penpal.presentation.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.penpal.presentation.ui.screens.HomeScreen

sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object Recording : Screen("recording/{storyId}") {
        fun createRoute(storyId: String) = "recording/$storyId"
    }
    data object Graph : Screen("graph/{storyId}") {
        fun createRoute(storyId: String) = "graph/$storyId"
    }
    data object SceneDetail : Screen("scene/{sceneId}") {
        fun createRoute(sceneId: String) = "scene/$sceneId"
    }
    data object CharacterDetail : Screen("character/{characterId}") {
        fun createRoute(characterId: String) = "character/$characterId"
    }
}

@Composable
fun PenPalApp() {
    val navController = rememberNavController()
    val context = LocalContext.current

    NavHost(
        navController = navController,
        startDestination = Screen.Home.route
    ) {
        composable(Screen.Home.route) {
            HomeScreen(
                onNavigateToRecording = { storyId ->
                    navController.navigate(Screen.Recording.createRoute(storyId))
                },
                onNavigateToGraph = { storyId ->
                    navController.navigate(Screen.Graph.createRoute(storyId))
                }
            )
        }
    }
}