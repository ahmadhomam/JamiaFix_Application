package com.jamiafix.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.jamiafix.app.JamiaFixApp
import com.jamiafix.app.data.model.UserRole
import com.jamiafix.app.ui.screens.admin.AdminDashboardScreen
import com.jamiafix.app.ui.screens.admin.ManageCampusScreen
import com.jamiafix.app.ui.screens.auth.AuthViewModel
import com.jamiafix.app.ui.screens.auth.LoginScreen
import com.jamiafix.app.ui.screens.auth.RegisterScreen
import com.jamiafix.app.ui.screens.issue.CreateIssueScreen
import com.jamiafix.app.ui.screens.issue.IssueDetailScreen
import com.jamiafix.app.ui.screens.issue.IssueDetailViewModel
import com.jamiafix.app.ui.screens.issue.ReportIssueViewModel
import com.jamiafix.app.ui.screens.staff.StaffHomeScreen
import com.jamiafix.app.ui.screens.student.StudentHomeScreen

@Composable
fun AppNavigation(app: JamiaFixApp) {
    val navController = rememberNavController()
    val authViewModel: AuthViewModel = viewModel(
        factory = AuthViewModel.Factory(app.authRepository)
    )

    val isLoggedIn by app.authRepository.isLoggedIn.collectAsState(initial = false)
    val userRole by app.authRepository.userRole.collectAsState(initial = UserRole.STUDENT)

    val startDestination = if (isLoggedIn) {
        when (userRole) {
            UserRole.STUDENT -> Screen.StudentHome.route
            UserRole.STAFF -> Screen.StaffHome.route
            UserRole.ADMIN -> Screen.AdminDashboard.route
        }
    } else {
        Screen.Login.route
    }

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        // --- Auth Routes ---
        composable(Screen.Login.route) {
            LoginScreen(
                authViewModel = authViewModel,
                onLoginSuccess = { role ->
                    val destination = when (role) {
                        UserRole.STUDENT -> Screen.StudentHome.route
                        UserRole.STAFF -> Screen.StaffHome.route
                        UserRole.ADMIN -> Screen.AdminDashboard.route
                    }
                    navController.navigate(destination) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                onNavigateToRegister = {
                    navController.navigate(Screen.Register.route)
                }
            )
        }

        composable(Screen.Register.route) {
            RegisterScreen(
                authViewModel = authViewModel,
                onRegisterSuccess = { role ->
                    val destination = when (role) {
                        UserRole.STUDENT -> Screen.StudentHome.route
                        UserRole.STAFF -> Screen.StaffHome.route
                        UserRole.ADMIN -> Screen.AdminDashboard.route
                    }
                    navController.navigate(destination) {
                        popUpTo(Screen.Register.route) { inclusive = true }
                    }
                },
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        // --- Role Home Routes ---
        composable(Screen.StudentHome.route) {
            StudentHomeScreen(
                authRepository = app.authRepository,
                issueRepository = app.issueRepository,
                onNavigateToCreateIssue = {
                    navController.navigate(Screen.CreateIssue.route)
                },
                onNavigateToDetail = { issueId ->
                    navController.navigate(Screen.IssueDetail.createRoute(issueId))
                },
                onLogout = {
                    authViewModel.logout()
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.StaffHome.route) {
            StaffHomeScreen(
                authRepository = app.authRepository,
                issueRepository = app.issueRepository,
                onNavigateToDetail = { issueId ->
                    navController.navigate(Screen.IssueDetail.createRoute(issueId))
                },
                onLogout = {
                    authViewModel.logout()
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.AdminDashboard.route) {
            AdminDashboardScreen(
                authRepository = app.authRepository,
                issueRepository = app.issueRepository,
                onNavigateToManageCampus = {
                    navController.navigate(Screen.ManageCampus.route)
                },
                onNavigateToDetail = { issueId ->
                    navController.navigate(Screen.IssueDetail.createRoute(issueId))
                },
                onLogout = {
                    authViewModel.logout()
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        // --- Meta & Issue Flow Routes ---
        composable(Screen.ManageCampus.route) {
            ManageCampusScreen(
                metadataRepository = app.metadataRepository,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.CreateIssue.route) {
            val reportViewModel: ReportIssueViewModel = viewModel(
                factory = ReportIssueViewModel.Factory(
                    app.issueRepository,
                    app.metadataRepository,
                    app.authRepository
                )
            )
            CreateIssueScreen(
                viewModel = reportViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.IssueDetail.route,
            arguments = listOf(navArgument("issueId") { type = NavType.IntType })
        ) { backStackEntry ->
            val issueId = backStackEntry.arguments?.getInt("issueId") ?: 0
            val detailViewModel: IssueDetailViewModel = viewModel(
                factory = IssueDetailViewModel.Factory(
                    issueId = issueId,
                    issueRepository = app.issueRepository,
                    authRepository = app.authRepository
                )
            )
            IssueDetailScreen(
                viewModel = detailViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
