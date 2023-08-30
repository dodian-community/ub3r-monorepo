package net.dodian.cli

import net.dodian.services.impl.RsaService
import kotlin.io.path.Path

fun main(args: Array<String>) {
    RsaService().generateKeyPair(
        radix = args.argument("radix")?.toIntOrNull(),
        bitCount = args.argument("bitCount")?.toIntOrNull() ?: 1024,
        path = Path(args.argument("path") ?: "./data/rsa/")
    )
}

fun Array<String>.argument(key: String) = this.firstOrNull {
    it.lowercase().startsWith("--${key.lowercase()}=")
}?.split("=")?.get(1)