package com.modmoney.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.modmoney.ui.garage.CarEditorScreen
import com.modmoney.ui.garage.GarageScreen
import com.modmoney.ui.mods.ModEditorScreen
import com.modmoney.ui.mods.CarHomeScreen

object Routes {
    const val GARAGE = "garage"
    const val CAR_EDITOR = "carEditor"          // ?carId={id}  (0 = new)
    const val CAR_HOME = "carHome"              // /{carId}
    const val MOD_EDITOR = "modEditor"          // /{carId}?modId={id}

    fun carEditor(carId: Long = 0L) = "$CAR_EDITOR?carId=$carId"
    fun carHome(carId: Long) = "$CAR_HOME/$carId"
    fun modEditor(carId: Long, modId: Long = 0L) = "$MOD_EDITOR/$carId?modId=$modId"
}

@Composable
fun ModMoneyNavHost(
    vm: GarageViewModel,
    darkTheme: Boolean,
    onToggleTheme: () -> Unit,
    navController: NavHostController = rememberNavController()
) {
    NavHost(navController = navController, startDestination = Routes.GARAGE) {

        composable(Routes.GARAGE) {
            // Clear per-car selection when returning to the garage list.
            LaunchedEffect(Unit) { vm.selectCar(null) }
            GarageScreen(
                vm = vm,
                darkTheme = darkTheme,
                onToggleTheme = onToggleTheme,
                onAddCar = { navController.navigate(Routes.carEditor()) },
                onOpenCar = { navController.navigate(Routes.carHome(it)) },
                onEditCar = { navController.navigate(Routes.carEditor(it)) }
            )
        }

        composable(
            route = "${Routes.CAR_EDITOR}?carId={carId}",
            arguments = listOf(navArgument("carId") {
                type = NavType.LongType; defaultValue = 0L
            })
        ) { entry ->
            val carId = entry.arguments?.getLong("carId") ?: 0L
            CarEditorScreen(
                vm = vm,
                carId = carId,
                onDone = { navController.popBackStack() }
            )
        }

        composable(
            route = "${Routes.CAR_HOME}/{carId}",
            arguments = listOf(navArgument("carId") { type = NavType.LongType })
        ) { entry ->
            val carId = entry.arguments?.getLong("carId") ?: 0L
            LaunchedEffect(carId) { vm.selectCar(carId) }
            CarHomeScreen(
                vm = vm,
                carId = carId,
                onBack = { navController.popBackStack() },
                onAddMod = { navController.navigate(Routes.modEditor(carId)) },
                onEditMod = { modId -> navController.navigate(Routes.modEditor(carId, modId)) },
                onEditCar = { navController.navigate(Routes.carEditor(carId)) }
            )
        }

        composable(
            route = "${Routes.MOD_EDITOR}/{carId}?modId={modId}",
            arguments = listOf(
                navArgument("carId") { type = NavType.LongType },
                navArgument("modId") { type = NavType.LongType; defaultValue = 0L }
            )
        ) { entry ->
            val carId = entry.arguments?.getLong("carId") ?: 0L
            val modId = entry.arguments?.getLong("modId") ?: 0L
            ModEditorScreen(
                vm = vm,
                carId = carId,
                modId = modId,
                onDone = { navController.popBackStack() }
            )
        }
    }
}
