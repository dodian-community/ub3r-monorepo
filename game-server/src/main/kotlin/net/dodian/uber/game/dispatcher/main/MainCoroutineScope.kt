package net.dodian.uber.game.dispatcher.main

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope

class MainCoroutineScope(
    override val coroutineContext: CoroutineDispatcher
) : CoroutineScope by CoroutineScope(coroutineContext)
