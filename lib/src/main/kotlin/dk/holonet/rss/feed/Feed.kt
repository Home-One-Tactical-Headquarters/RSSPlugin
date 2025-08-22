package dk.holonet.rss.feed

data class Feed(
    val title: String,
    val items: List<FeedItem>
)

data class FeedItem(
    val title: String,
    val subtitle: String?,
    val origin: String,
    val content: String?,
    val imageUrl: String?,
    val dateString: String,
    val guid: String?,
)
