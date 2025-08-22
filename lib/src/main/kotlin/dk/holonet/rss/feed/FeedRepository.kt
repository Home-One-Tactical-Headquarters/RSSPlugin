package dk.holonet.rss.feed

import com.prof18.rssparser.RssParser

class FeedRepository(
    private val parser: RssParser
) {
    suspend fun getFeed(url: String): Feed {
        val channel = parser.getRssChannel(url)
        
        return Feed(
            title = channel.title ?: "",
            items = channel.items.mapNotNull {
                val title = it.title
                val subtitle = it.description
                val pubDate = it.pubDate

                if (title == null || pubDate == null) {
                    return@mapNotNull null
                }

                FeedItem(
                    title = title,
                    subtitle = subtitle,
                    origin = channel.title ?: "",
                    content = it.content,
                    imageUrl = it.image,
                    dateString = pubDate,
                    guid = it.guid
                )
            }
        )
    }
}