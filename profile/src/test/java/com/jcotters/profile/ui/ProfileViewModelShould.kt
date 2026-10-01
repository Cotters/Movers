package com.jcotters.profile.ui

import com.jcotters.contract.detail.domain.models.Movie
import com.jcotters.profile.domain.FetchUserProfileUseCase
import com.jcotters.profile.domain.LogoutUseCase
import com.jcotters.profile.domain.ObserveUserBookmarksUseCase
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileViewModelShould {
    @Before
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `remove a movie from a retained profile when its bookmark changes`() = runTest {
        val movie = Movie(7, "Saved movie", "Overview", "2026")
        val savedMovies = MutableStateFlow(listOf(movie))
        val fetchProfile = mockk<FetchUserProfileUseCase>()
        coEvery { fetchProfile.invoke() } returns Result.success(mockk())
        val observeBookmarks = mockk<ObserveUserBookmarksUseCase>()
        every { observeBookmarks() } returns savedMovies
        val model = ProfileViewModel(fetchProfile, observeBookmarks, mockk<LogoutUseCase>())
        runCurrent()
        assertEquals(listOf(movie), model.uiState.value.bookmarkedMovies)
        savedMovies.value = emptyList()
        runCurrent()
        assertEquals(emptyList<Movie>(), model.uiState.value.bookmarkedMovies)
    }
}
