package net.dodian.cli

import net.dodian.services.impl.RsaService
import kotlin.io.path.Path

fun main(args: Array<String>) {
    RsaService().generateKeyPair(
        radix = if (args.isNotEmpty()) args[0].toInt() else 16,
        bitCount = if (args.size >= 2) args[1].toInt() else 2048,
        path = if (args.size >= 3) Path(args[2]) else Path("./data/rsa")
    )
}