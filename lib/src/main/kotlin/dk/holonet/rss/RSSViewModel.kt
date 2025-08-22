package dk.holonet.rss

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dk.holonet.rss.feed.Feed
import dk.holonet.rss.feed.FeedItem
import dk.holonet.rss.feed.FeedRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class RSSViewModel(
    private val feedRepository: FeedRepository
): ViewModel() {

    private val feeds = mutableListOf<Feed>()
    private val allItems = mutableListOf<FeedItem>()
    private val emittedItems = mutableSetOf<String>()
    private var isEmitting = false

    private val _currentItem = MutableStateFlow<FeedItem?>(null)
    val currentItem = _currentItem.asStateFlow()

    enum class SelectionStrategy {
        RANDOM,
        SEQUENTIAL
    }

    fun startEmittingFeeds(
        intervalMillis: Long = 5000L, // Default to 5 seconds
        strategy: SelectionStrategy = SelectionStrategy.RANDOM
    ) {
        if (isEmitting) return // Prevent multiple emissions

        viewModelScope.launch {
            isEmitting = true

            // Wait for feeds to be loaded
            while (feeds.isEmpty()) {
                delay(1000)
            }
            collectAllItems()

            while (isEmitting && allItems.isNotEmpty()) {
                val nextItem =selectNextItem(strategy)
                nextItem?.let {
                    _currentItem.emit(it)
                    delay(intervalMillis)
                }

                if (emittedItems.size >= allItems.size) {
                    resetEmittedItems()
                }
            }
        }
    }

    fun stopEmitting() {
        isEmitting = false
    }

    fun loadFeed(feedUrl: String) {
        viewModelScope.launch {
            val feed = feedRepository.getFeed(feedUrl)
            feeds.add(feed)
        }
    }

    private fun resetEmittedItems() {
        emittedItems.clear()
    }

    private fun selectNextItem(strategy: SelectionStrategy): FeedItem? {
        val availableItems = allItems.filter { item ->
            !emittedItems.contains(item.guid ?: item.title)
        }

        if (availableItems.isEmpty()) return null

        val item = when (strategy) {
            SelectionStrategy.RANDOM -> availableItems.randomOrNull()
            SelectionStrategy.SEQUENTIAL -> availableItems.firstOrNull()
        }

        item?.let {
            emittedItems.add(it.guid ?: it.title)
        }

        return item
    }

    private fun collectAllItems() {
        allItems.clear()
        feeds.forEach { feed ->
            allItems.addAll(feed.items)
        }
    }
}