package net.dodian.uber.client

import com.jagex.runescape.Game
import com.jagex.runescape.Signlink
import net.dodian.library.extensions.argument
import java.math.BigInteger

fun main(args: Array<String>) {
    val game = Game()

    Game.RSA_MODULUS = BigInteger("144536637563440057160808461977324382186710417775358184126989832762756698107899344110117559434824610238034809802491751871048292985661698319076446382120631557440502945377688897688601296108720422774470526242807665051371841237960974220302678715599412577220453538331305436281828154659005659234330053472006964242623")
    Game.RSA_EXPONENT = BigInteger("65537")

    Game.server = args.argument("host") ?: "127.0.0.1"
    Game.gamePort = args.argument("gamePort")?.toIntOrNull() ?: 43594
    Game.filePort = args.argument("filePort")?.toIntOrNull() ?: 43595
    Game.httpPort = args.argument("httpPort")?.toIntOrNull() ?: 8080

    Game.nodeID = args.argument("node")?.toIntOrNull() ?: 10
    Game.portOffset = args.argument("portOffset")?.toIntOrNull() ?: 0

    Game.lowmem = args.argument("lowMemory")?.toBooleanStrictOrNull() ?: false
    Game.members = args.argument("members")?.toBooleanStrictOrNull() ?: true

    Signlink.storeid = args.argument("storeId")?.toIntOrNull() ?: -1
    Signlink.startpriv()

    game.init(765, 503)
}