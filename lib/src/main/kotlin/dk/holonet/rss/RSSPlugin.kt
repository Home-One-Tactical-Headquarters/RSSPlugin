package dk.holonet.rss

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.prof18.rssparser.RssParser
import dk.holonet.core.HoloNetModule
import dk.holonet.core.HoloNetPlugin
import dk.holonet.core.ModuleConfiguration
import dk.holonet.core.asList
import dk.holonet.rss.feed.FeedRepository
import dk.holonet.rss.util.AutoSizeText
import org.koin.core.component.inject
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module
import org.pf4j.Extension
import org.pf4j.PluginWrapper

class RSSPlugin(wrapper: PluginWrapper) : HoloNetPlugin(wrapper) {

    private val module = module {
        single { FeedRepository(RssParser()) }
        viewModel { RSSViewModel(get()) }
    }

    override fun start() {
        super.start()
        loadDependencies(module)
    }

    @Extension
    class RSSModule() : HoloNetModule() {

        private val viewModel: RSSViewModel by inject()
        private val animationSpeed = mutableIntStateOf(600)

        @Composable
        override fun render() {
            val state = viewModel.currentItem.collectAsState()

            Row(
                modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center
            ) {
                AnimatedContent(
                    targetState = state.value, transitionSpec = {
                        fadeIn(animationSpec = tween(animationSpeed.value)) + slideInHorizontally(
                            animationSpec = tween(animationSpeed.value), initialOffsetX = { it / 2 }) togetherWith fadeOut(
                            animationSpec = tween(animationSpeed.value)
                        ) + slideOutHorizontally(
                            animationSpec = tween(animationSpeed.value), targetOffsetX = { -it / 2 })
                    }) { currentItem ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(16.dp, 0.dp)
                    ) {
                        val title = currentItem?.title ?: return@AnimatedContent
                        val origin = currentItem.origin
                        val imageUrl = currentItem.imageUrl

                        imageUrl?.let {
                            AsyncImage(
                                model = imageUrl,
                                contentDescription = title
                            )

                            Spacer(Modifier.height(8.dp))
                        }

                        Text(
                            text = origin,
                            style = MaterialTheme.typography.overline,
                            textAlign = TextAlign.Center,
                            fontSize = 24.sp, color = Color.Gray
                        )

                        Spacer(Modifier.height(4.dp))

                        AutoSizeText(
                            text = title,
                            minTextSize = 16.sp,
                            maxTextSize = 32.sp,
                            style = MaterialTheme.typography.h4.copy(textAlign = TextAlign.Center),
                            alignment = Alignment.Center,
                            color = Color.White
                        )
                    }
                }
            }
        }

        override fun configure(configuration: ModuleConfiguration?) {
            super.configure(configuration)

            var feeds: List<String>? = null

            configuration?.config?.let { props ->
                props["feeds"]?.let {
                    feeds = it.asList<String>()
                }
            }

            feeds?.let {
                it.forEach { feed ->
                    viewModel.loadFeed(feed)
                }

                viewModel.startEmittingFeeds()
            } ?: println("No Feeds provided")
        }
    }
}