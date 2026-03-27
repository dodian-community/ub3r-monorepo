package net.dodian.uber.game.model.player.casino

import net.dodian.uber.game.systems.ui.dialogue.DialogueService
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.netty.listener.out.RemoveInterfaces
import net.dodian.uber.game.netty.listener.out.SendMessage
import net.dodian.uber.game.netty.listener.out.SendString
import net.dodian.uber.game.persistence.audit.AsyncSqlService
import net.dodian.uber.game.persistence.db.DbTables
import net.dodian.utilities.Misc
import net.dodian.utilities.Utils
import net.dodian.uber.game.persistence.db.dbConnection
import net.dodian.uber.game.config.gameWorldId

class SlotMachine {
    @JvmField var slotsGames: Int = -1
    @Volatile @JvmField var peteBalance: Int = 0
    @Volatile @JvmField var slotsJackpot: Int = 240000

    @Volatile @JvmField var CoinsBillion_Win: Int = 0
    @Volatile @JvmField var CoinsBillion_Lose: Int = 0
    @Volatile @JvmField var Coins_Win: Int = 0
    @Volatile @JvmField var Coins_Lose: Int = 0

    private val symbols = ArrayList<Symbol>()
    private val counterLock = Any()

    init {
        symbols.add(Symbol(1, "0", intArrayOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16)))
        symbols.add(Symbol(2, "1", intArrayOf(17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32, 33)))
        symbols.add(Symbol(3, "2", intArrayOf(34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46, 47, 48, 49, 50)))
        symbols.add(Symbol(4, "3", intArrayOf(51, 52, 53, 54, 55, 56, 57, 58, 59, 60, 61, 62, 63, 64, 65, 66, 67)))
        symbols.add(Symbol(5, "4", intArrayOf(68, 69, 70, 71, 72, 73, 74, 75, 76, 77, 78, 79, 80, 81, 82, 83, 84)))
        symbols.add(Symbol(6, "5", intArrayOf(85, 86, 87, 88, 89, 90, 91, 92, 93, 94, 95, 96, 97, 98, 99, 100, 101)))
        symbols.add(Symbol(7, "X", intArrayOf(102, 103, 104, 105, 106, 107, 108, 109, 110, 111, 112, 113, 114, 115, 116, 117, 118, 119, 120, 121, 122, 123, 124, 125)))
        symbols.add(Symbol(8, "7", intArrayOf(126, 127)))
    }

    fun spin(): Spin {
        val spinSymbols = Array(3) { translate(Misc.random(127)) ?: symbols[0] }
        return Spin(spinSymbols)
    }

    private fun translate(stop: Int): Symbol? {
        for (symbol in symbols) {
            if (symbol.check(stop)) {
                return symbol
            }
        }
        return null
    }

    fun rollDice(client: Client, amount: Int) {
        if (amount > 50_000_000 || amount < 100_000) {
            DialogueService.setDialogueSent(client, true)
            client.convoId = -1
            client.send(RemoveInterfaces())
            client.clearWalkableInterface()
            if (amount > 50_000_000) {
                client.send(SendMessage("You can't bet more than 50M"))
            }
            if (amount < 100_000) {
                client.send(SendMessage("Party pete do not accept anything less then 100k as a gamble!"))
            }
            return
        }
        if (client.playerHasItem(995, 2_000_000_000)) {
            DialogueService.setDialogueSent(client, true)
            client.convoId = -1
            client.send(RemoveInterfaces())
            client.send(SendMessage("You can't bet with more then 2b cash in your inventory!"))
            return
        }
        if (!client.playerHasItem(995, amount)) {
            DialogueService.setDialogueSent(client, true)
            client.convoId = -1
            client.send(RemoveInterfaces())
            client.send(SendMessage("You do not have enough cash!"))
            return
        }

        client.deleteItem(995, amount)
        client.send(SendString("", 7815))
        client.send(SendString("", 8399))
        client.send(SendString("Royal Dice", 7815))
        client.send(SendString("Bet: $amount", 8399))

        val first = ((Math.random() * 999_999_999).toInt() % 6) + 1
        val second = ((Math.random() * 999_999_999).toInt() % 6) + 1
        val total = first + second

        client.send(SendString(first.toString(), 8424))
        client.send(SendString(second.toString(), 8425))

        if (total > 7 && total < 12) {
            client.send(SendString("Roll: $total.  You win!", 8426))
            client.addItem(995, amount * 2)
            if (client.playerGroup != 6 || client.playerRights < 2) {
                trackDice(1, amount)
            }
        } else if (total == 12) {
            client.send(SendString("Roll: $total.  You win a Jackpot!", 8426))
            client.addItem(995, amount + amount + amount / 2)
            if (amount + amount / 2 > 50_000_000) {
                Client.publicyell(client.playerName + " has won " + Utils.format(amount + amount / 2) + " gp jackpot at the Dice!")
            }
            if (client.playerGroup != 6 || client.playerRights < 2) {
                trackDice(1, amount + amount / 2)
            }
        } else {
            client.send(SendString("Roll: $total.  You lose", 8426))
            if (client.playerGroup != 6 || client.playerRights < 2) {
                trackDice(2, amount)
            }
        }

        client.checkItemUpdate()
        client.setWalkableInterface(6675)
    }

    fun loadGamble() {
        if (gameWorldId == 5) {
            return
        }
        AsyncSqlService.execute(
            "slot-machine-load-gamble",
            Runnable {
                try {
                    dbConnection.use { conn ->
                        conn.createStatement().use { stmt ->
                            stmt.executeQuery("SELECT * FROM ${DbTables.GAME_PETE_CO}").use { results ->
                                var winBillions = 0
                                var winCoins = 0
                                var loseBillions = 0
                                var loseCoins = 0
                                while (results.next()) {
                                    when (results.getInt("Tracker_ID")) {
                                        1 -> {
                                            winBillions = results.getInt("CoinsBillion")
                                            winCoins = results.getInt("Coins")
                                        }

                                        2 -> {
                                            loseBillions = results.getInt("CoinsBillion")
                                            loseCoins = results.getInt("Coins")
                                        }
                                    }
                                }
                                synchronized(counterLock) {
                                    CoinsBillion_Win = winBillions
                                    Coins_Win = winCoins
                                    CoinsBillion_Lose = loseBillions
                                    Coins_Lose = loseCoins
                                }
                            }
                        }
                    }
                } catch (exception: Exception) {
                    exception.printStackTrace()
                }
            },
        )
    }

    fun trackDice(id: Int, amount: Int) {
        if (gameWorldId == 5) {
            return
        }
        val query: String
        synchronized(counterLock) {
            if (id == 1) {
                Coins_Win += amount
                if (Coins_Win > 1_000_000_000) {
                    Coins_Win -= 1_000_000_000
                    CoinsBillion_Win += 1
                }
                query = "INSERT ${DbTables.GAME_PETE_CO} SET CoinsBillion = $CoinsBillion_Win, Coins = $Coins_Win where Tracker_ID=1"
            } else {
                Coins_Lose += amount
                if (Coins_Lose > 1_000_000_000) {
                    Coins_Lose -= 1_000_000_000
                    CoinsBillion_Lose += 1
                }
                query = "INSERT ${DbTables.GAME_PETE_CO} SET CoinsBillion = $CoinsBillion_Lose, Coins = $Coins_Lose where Tracker_ID=2"
            }
        }

        AsyncSqlService.execute(
            "slot-machine-track-dice",
            Runnable {
                try {
                    dbConnection.use { conn ->
                        conn.createStatement().use { stmt ->
                            stmt.executeUpdate(query)
                        }
                    }
                } catch (exception: Exception) {
                    exception.printStackTrace()
                }
            },
        )
    }

    fun playSlots(client: Client, times: Int) {
        if (!client.playerHasItem(995, 3000)) {
            client.send(RemoveInterfaces())
            client.send(SendMessage("You don't have enough gold to play!"))
            return
        }

        slotsGames = times
        if (slotsGames > 0) {
            slotsGames--
            client.send(SendString("PETE'S SLOTS CO - JACKPOT: $slotsGames plays remaining", 13896))
        } else {
            client.send(SendString("PETE'S SLOTS CO - JACKPOT: Manual play", 13896))
        }

        client.deleteItem(995, 3000)
        if (peteBalance + 3000 < Int.MAX_VALUE) {
            peteBalance += (3000 * 0.15).toInt()
        }
        if (slotsJackpot + 3000 < Int.MAX_VALUE) {
            slotsJackpot += (3000 * 0.50).toInt()
        }

        val spin = spin()
        val winnings = spin.getWinnings()
        client.send(SendString("", 13884))
        client.send(SendString(spin.getSymbols()[0].output(), 13885))
        client.send(SendString(spin.getSymbols()[1].output(), 13886))
        client.send(SendString(spin.getSymbols()[2].output(), 13887))

        if (winnings in 1..239_999) {
            client.addItem(995, winnings)
            client.send(SendMessage("You have won " + Utils.format(winnings) + " gp!"))
        } else if (winnings >= 240_000) {
            client.send(SendMessage("You have won the jackpot!  There is a 15% tax on winnings."))
            val amount = (winnings * 0.85).toInt()
            client.addItem(995, amount)
            client.send(SendMessage("You receive " + Utils.format(amount) + " gp"))
            client.send(SendString("Jackpot!", 18812))
            slotsJackpot = (slotsJackpot / 0.85).toInt()
            Client.publicyell(client.playerName + " has won the " + Utils.format(winnings) + " gp jackpot at the slots!")
        }
    }
}
