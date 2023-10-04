package net.dodian.uber.client

import com.jagex.runescape.Game
import com.jagex.runescape.Signlink
import net.dodian.library.extensions.argument
import java.math.BigInteger

fun main(args: Array<String>) {
    val game = Game()

    Game.RSA_MODULUS = BigInteger("114252807908471141280965118466752652725578840016194450252850862599551548121427163260119877710663309297681719645626014673790608915142202489925753755306548275500086990815927141549668285153330895244993443434433043980636252929259981445767355412435350808524087993282462913245189413704796672139307907828749291331833")
    Game.RSA_EXPONENT = BigInteger("65537")

    Game.server = args.argument("host") ?: "127.0.0.1"
    Game.gamePort = args.argument("port")?.toIntOrNull() ?: 43594
    Game.filePort = args.argument("gamePort")?.toIntOrNull() ?: 43595
    Game.httpPort = args.argument("httpPort")?.toIntOrNull() ?: 8080

    Game.nodeID = args.argument("node")?.toIntOrNull() ?: 10
    Game.portOffset = args.argument("portOffset")?.toIntOrNull() ?: 0

    Game.lowmem = args.argument("lowMemory")?.toBooleanStrictOrNull() ?: false
    Game.members = args.argument("members")?.toBooleanStrictOrNull() ?: true

    Signlink.storeid = args.argument("storeId")?.toIntOrNull() ?: -1
    Signlink.startpriv()

    game.init(765, 503)
}