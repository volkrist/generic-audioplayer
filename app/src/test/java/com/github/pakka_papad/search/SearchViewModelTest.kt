package com.github.pakka_papad.search

import com.github.pakka_papad.MainDispatcherRule
import com.github.pakka_papad.data.music.Song
import com.github.pakka_papad.data.search.SearchRepository
import com.github.pakka_papad.data.services.PlayerService
import com.github.pakka_papad.util.MessageStore
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals

class SearchViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val messageStore = mockk<MessageStore>(relaxed = true)
    private val playerService = mockk<PlayerService>()
    private val searchRepository = mockk<SearchRepository>()

    private val query = "Good"
    private lateinit var songs: List<Song>
    private lateinit var viewModel: SearchViewModel

    @Before
    fun setup() {
        songs = buildList {
            repeat(5) {
                val mockSong = mockk<Song>()
                every { mockSong.title } returns "Good song $it"
                add(mockSong)
            }
        }
        coEvery { searchRepository.search(query) } returns SearchResult(songs = songs)
        coEvery { playerService.startServiceIfNotRunning(any(), any()) } answers {
            // PlayerService drives queue internally
        }
        viewModel = SearchViewModel(
            messageStore = messageStore,
            playerService = playerService,
            searchRepository = searchRepository,
            crashReporter = mockk(relaxed = true),
        )
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun TestScope.startCollection() {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.searchResult.collect()
        }
    }

    @Test
    fun `verify updateQuery debounced search`() = runTest {
        startCollection()
        viewModel.updateQuery(query)
        advanceTimeBy(350)
        assertEquals(query, viewModel.query.value)
        assertEquals(songs, viewModel.searchResult.value.songs)
        coVerify(exactly = 1) { searchRepository.search(query) }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `verify setQueue`() = runTest {
        startCollection()
        viewModel.updateQuery(query)
        advanceTimeBy(350)
        advanceUntilIdle()

        viewModel.setQueue(songs, 0)

        coVerify(exactly = 1) {
            playerService.startServiceIfNotRunning(songs, 0)
        }
    }
}
