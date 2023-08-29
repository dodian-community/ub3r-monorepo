package net.dodian.uber.service

import com.github.michaelbull.logging.InlineLogger
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

class RsaService {
    lateinit var exponent: BigInteger
    lateinit var modulus: BigInteger

    fun init() {
        val path = Paths.get("./data/rsa/key.pem")
        val radix = 16

        if (!Files.exists(path))
            generateKeyPair(path = path, radix = radix)

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

    fun generateKeyPair(bitCount: Int = 2048, radix: Int = 16, path: Path = Path("./data/rsa/")) {
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
            pubWriter.println(
                "/* Auto-generated file using ${this::class.java} ${
                    SimpleDateFormat("dd/M/yyyy hh:mm:ss").format(
                        Date()
                    )
                } */"
            )
            pubWriter.println("")
            pubWriter.println("Place these keys in the client (find BigInteger(\"10001\" in client code):")
            pubWriter.println("--------------------")
            pubWriter.println("public key: " + publicKey.publicExponent.toString(radix))
            pubWriter.println("modulus: " + publicKey.modulus.toString(radix))
            pubWriter.close()
        } catch (exception: Exception) {
            logger.error(exception) {
                "Failed to write RSA keypair to: ${path.toAbsolutePath()}"
            }
        }
    }
}

fun main(args: Array<String>) {
    RsaService().generateKeyPair(
        radix = if (args.isNotEmpty()) args[0].toInt() else 16,
        bitCount = if (args.size >= 2) args[1].toInt() else 2048,
        path = if (args.size >= 3) Path(args[2]) else Path("./data/rsa")
    )
}