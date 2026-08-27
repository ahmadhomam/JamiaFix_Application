package com.jamiafix.app.ui.navigation

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Register : Screen("register")
    object StudentHome : Screen("student_home")
    object StaffHome : Screen("staff_home")
    object AdminDashboard : Screen("admin_dashboard")
    object ManageCampus : Screen("manage_campus")
    object CreateIssue : Screen("create_issue")

    object IssueDetail : Screen("issue_detail/{issueId}") {
        fun createRoute(issueId: Int): String = "issue_detail/$issueId"
    }
}
