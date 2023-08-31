package net.dodian.services.impl

import com.github.michaelbull.logging.InlineLogger
import net.dodian.services.Service
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.util.io.pem.PemObject
import org.bouncycastle.util.io.pem.PemReader
import org.bouncycastle.util.io.pem.PemWriter
import java.io.PrintWriter
import java.math.BigInteger
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.security.KeyFactory
import java.security.KeyPairGenerator
import java.security.Security
import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey
import java.security.spec.PKCS8EncodedKeySpec
import java.text.SimpleDateFormat
import java.util.*
import kotlin.io.path.Path

private val logger = InlineLogger()
class RsaService : Service() {
    lateinit var exponent: BigInteger
    lateinit var modulus: BigInteger

    fun init() {
        val path = Paths.get("./data/rsa/key.pem")
        val radix = 16

        if (!Files.exists(path))
            generateKeyPair(path = path, radix = radix, bitCount = 1024)

        try {
            PemReader(Files.newBufferedReader(path)).use { reader ->
                val pem = reader.readPemObject()
                val keySpec = PKCS8EncodedKeySpec(pem.content)

                Security.addProvider(BouncyCastleProvider())
                val factory = KeyFactory.getInstance("RSA", "BC")

                val privateKey = factory.generatePrivate(keySpec) as RSAPrivateKey
                exponent = privateKey.privateExponent
                modulus = privateKey.modulus
            }
        } catch (exception: Exception) {
            logger.error(exception) {
                "Error parsing RSA key pair at: ${path.toAbsolutePath()}"
            }
        }
    }

    fun generateKeyPair(bitCount: Int = 2048, radix: Int? = 16, path: Path = Path("./data/rsa/")) {
        Security.addProvider(BouncyCastleProvider())

        val generator = KeyPairGenerator.getInstance("RSA", "BC")
        generator.initialize(bitCount)

        val keyPair = generator.generateKeyPair()

        val privateKey = keyPair.private as RSAPrivateKey
        val publicKey = keyPair.public as RSAPublicKey

        try {
            Files.createDirectories(path)

            PemWriter(Files.newBufferedWriter(path.resolve("key.pem"))).use { writer ->
                writer.writeObject(PemObject("RSA PRIVATE KEY", privateKey.encoded))
            }

            val pubWriter = PrintWriter(path.resolve("key.pub").toFile())

            val exponent: String
            val modulus: String

            if (radix != null) {
                exponent = publicKey.publicExponent.toString(radix)
                modulus = publicKey.modulus.toString(radix)
            } else {
                exponent = publicKey.publicExponent.toString()
                modulus = publicKey.modulus.toString()
            }

            pubWriter.println(
                "/* Auto-generated file using ${this::class.simpleName} ${
                    SimpleDateFormat("dd/M/yyyy hh:mm:ss").format(
                        Date()
                    )
                } */"
            )
            pubWriter.println("")
            pubWriter.println("Place these keys in the client:")
            pubWriter.println("--------------------")
            pubWriter.println("exponent: $exponent")
            pubWriter.println("modulus: $modulus")
            pubWriter.close()

            replaceClientRSA(exponent, modulus)
        } catch (exception: Exception) {
            logger.error(exception) {
                "Failed to write RSA keypair to: ${path.toAbsolutePath()}"
            }
        }
    }

    private fun replaceClientRSA(exponent: String, modulus: String) {
        var tries = 0
        var clientProjectPath = Path("data").toAbsolutePath()

        while (tries < 5 && clientProjectPath.parent != null && !clientProjectPath.endsWith("game-server")) {
            clientProjectPath = clientProjectPath.parent
            tries++
        }

        val file = clientProjectPath.parent.resolve(
            "game-client-new/src/main/java/net/dodian/client/ClientRSA.java"
        ).toFile()

        if (!file.exists())
            error("Nope, couldn't find client's file :(")

        val text = file.readText()
        val lines = file.readLines()

        val modLine = lines.single { it.contains("RSA_MODULUS") }
        val newModLine = modLine.replace("BigInteger\\(\"[0-9]+\"\\)".toRegex(), "BigInteger(\"$modulus\")")

        val expLine = lines.single { it.contains("RSA_EXPONENT") }
        val newExpLine = expLine.replace("BigInteger\\(\"[0-9]+\"\\)".toRegex(), "BigInteger(\"$exponent\")")

        file.writeText(
            text.replace(modLine, newModLine)
                .replace(expLine, newExpLine)
        )
    }

    override fun start() {}
}