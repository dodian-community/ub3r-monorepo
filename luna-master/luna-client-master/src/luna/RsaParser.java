package luna;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Scanner;

/**
 * @author lare96 <http://github.com/lare96>
 */
public final class RsaParser {

    private static BigInteger modulus;
    private static BigInteger exponent;
    private static final Path RSA = Paths.get("./rsa/rsapub.toml");

    public static void parse() throws IOException {
        try (Scanner reader = new Scanner(RSA)) {
            reader.nextLine();
            modulus = parseValue(reader.nextLine());
            exponent = parseValue(reader.nextLine());
        }
    }

    private static BigInteger parseValue(String token) {
        int start = token.indexOf('"') + 1;
        int finish = token.lastIndexOf('"');
        String value = token.substring(start, finish);
        return new BigInteger(value);
    }

    public static BigInteger getModulus() {
        if (modulus == null) {
            throw new NullPointerException("RSA modulus value was not parsed.");
        }
        return modulus;
    }

    public static BigInteger getExponent() {
        if (exponent == null) {
            throw new NullPointerException("RSA exponent value was not parsed.");
        }
        return exponent;
    }
}