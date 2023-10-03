package net.dodian.uber.utils

import com.github.michaelbull.logging.InlineLogger
import net.dodian.extensions.toPath
import net.dodian.library.extensions.argument
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.util.io.pem.PemObject
import org.bouncycastle.util.io.pem.PemReader
import org.bouncycastle.util.io.pem.PemWriter
import java.io.PrintWriter
import java.math.BigInteger
import java.nio.file.Files
import java.nio.file.Path
import java.security.KeyFactory
import java.security.KeyPairGenerator
import java.security.Security
import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey
import java.security.spec.PKCS8EncodedKeySpec

private val logger = InlineLogger()

fun main(args: Array<String>) {
    RsaManager(
        radix = args.argument("radix")?.toIntOrNull(),
        bitCount = args.argument("bitCount")?.toIntOrNull() ?: 1024,
        path = (args.argument("path") ?: "./data/rsa").toPath()
    ).generateKeyPair(true)
}

data class RsaKeyPair(
    val modulus: BigInteger,
    val exponent: BigInteger
)

class RsaManager(
    val path: Path = "./data/rsa".toPath(),
    val bitCount: Int = 1024,
    val radix: Int? = null
) {

    fun getPair(): RsaKeyPair {
        if (!Files.exists(path.resolve("key.pem")))
            generateKeyPair()

        try {
            PemReader(Files.newBufferedReader(path.resolve("key.pem"))).use { reader ->
                val pem = reader.readPemObject()
                val keySpec = PKCS8EncodedKeySpec(pem.content)

                Security.addProvider(BouncyCastleProvider())
                val factory = KeyFactory.getInstance("RSA", "BC")

                val private = factory.generatePrivate(keySpec) as RSAPrivateKey

                return RsaKeyPair(
                    exponent = private.privateExponent,
                    modulus = private.modulus
                )
            }
        } catch (exception: Exception) {
            logger.error(exception) {
                "Error parsing RSA key pair at: ${path.toAbsolutePath()}"
            }
        }

        error("Failed to load RSA keypair...")
    }

    fun generateKeyPair(overwrite: Boolean = false) {
        if (Files.exists(path.resolve("key.pem")) && !overwrite)
            return

        Security.addProvider(BouncyCastleProvider())

        val generator = KeyPairGenerator.getInstance("RSA", "BC")
        generator.initialize(bitCount)

        val pair = generator.generateKeyPair()

        val private = pair.private as RSAPrivateKey
        val public = pair.public as RSAPublicKey

        try {
            Files.createDirectories(path)

            PemWriter(Files.newBufferedWriter(path.resolve("key.pem"))).use { writer ->
                writer.writeObject(PemObject("RSA PRIVATE KEY", private.encoded))
            }

            val exponent: String
            val modulus: String

            if (radix != null) {
                exponent = public.publicExponent.toString(radix)
                modulus = public.modulus.toString(radix)
            } else {
                exponent = public.publicExponent.toString()
                modulus = public.modulus.toString()
            }

            PrintWriter(path.resolve("key.pub").toFile()).use { writer ->
                writer.println("Place these keys in the client:")
                writer.println("--------------------")
                writer.println("exponent: $exponent")
                writer.println("modulus: $modulus")
            }
        } catch (exception: Exception) {
            logger.error(exception) {
                "Failed to write RSA keypair to: ${path.toAbsolutePath()}"
            }
        }
    }
}