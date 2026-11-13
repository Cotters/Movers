package com.jcotters.profile.navigation

import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jcotters.navigation.EntryProviderInstaller
import com.jcotters.navigation.Navigator
import com.jcotters.profile.domain.Notifications
import com.jcotters.profile.ui.NotificationsRow
import com.jcotters.profile.ui.ProfileScreen
import com.jcotters.profile.ui.ProfileViewModel
import org.koin.compose.getKoin
import org.koin.compose.viewmodel.koinViewModel

object ProfileNavigationModule {
    fun provideEntries(onOpenStyleGuide: ((Navigator) -> Unit)? = null): EntryProviderInstaller = { navigator, _ ->
        entry<ProfileKey> {
            val viewModel: ProfileViewModel = koinViewModel()
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            val notifications = getKoin().getOrNull<Notifications>()
            ProfileScreen(
                viewState = state,
                onOpenStyleGuide = onOpenStyleGuide?.let { open -> { open(navigator) } },
                onRetry = viewModel::retry,
                onSaveName = viewModel::saveName,
                onSaveColor = viewModel::saveColor,
                savingColor = viewModel.savingColor.collectAsStateWithLifecycle().value,
                colorError = viewModel.colorError.collectAsStateWithLifecycle().value,
                savingName = viewModel.savingName.collectAsStateWithLifecycle().value,
                nameError = viewModel.nameError.collectAsStateWithLifecycle().value,
                notifications = {
                    if (notifications != null) NotificationsRow(
                        settings = notifications.settings.collectAsStateWithLifecycle().value,
                        onEnable = notifications::requestPermission,
                        onSettings = notifications::openSettings,
                        onRetry = notifications::refresh,
                    )
                },
            )
        }
    }
}
