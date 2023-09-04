package net.dodian.uber.game.dispatcher.io

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope

class IOCoroutineScope(
    override val coroutineContext: CoroutineDispatcher
) : CoroutineScope by CoroutineScope(coroutineContext)
