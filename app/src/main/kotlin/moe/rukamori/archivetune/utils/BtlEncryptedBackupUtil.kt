/*
 * BTL Music (2026)
 * © ||BTL||™ (balajitechlabs)
 * GNU GPL-3.0 License
 */

package moe.rukamori.archivetune.utils

import java.io.InputStream
import java.io.OutputStream
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.CipherInputStream
import javax.crypto.CipherOutputStream
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

object BtlEncryptedBackupUtil {

    private const val ALGORITHM = "AES/GCM/NoPadding"
    private const val KEY_DERIVATION = "PBKDF2WithHmacSHA256"
    private const val ITERATIONS = 65536
    private const val KEY_LENGTH = 256
    private const val SALT_SIZE = 16
    private const val IV_SIZE = 12
    private const val TAG_LENGTH_BITS = 128

    fun encrypt(password: CharArray, plaintextJson: String, outputStream: OutputStream) {
        val random = SecureRandom()
        val salt = ByteArray(SALT_SIZE).also(random::nextBytes)
        val iv = ByteArray(IV_SIZE).also(random::nextBytes)

        val factory = SecretKeyFactory.getInstance(KEY_DERIVATION)
        val spec = PBEKeySpec(password, salt, ITERATIONS, KEY_LENGTH)
        val secretKey = SecretKeySpec(factory.generateSecret(spec).encoded, "AES")

        val cipher = Cipher.getInstance(ALGORITHM)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, GCMParameterSpec(TAG_LENGTH_BITS, iv))

        // Write header: Salt + IV
        outputStream.write(salt)
        outputStream.write(iv)

        CipherOutputStream(outputStream, cipher).use { cos ->
            cos.write(plaintextJson.toByteArray(Charsets.UTF_8))
            cos.flush()
        }
    }

    fun decrypt(password: CharArray, inputStream: InputStream): String {
        val salt = ByteArray(SALT_SIZE)
        val iv = ByteArray(IV_SIZE)

        if (inputStream.read(salt) != SALT_SIZE) error("Invalid backup file: salt missing")
        if (inputStream.read(iv) != IV_SIZE) error("Invalid backup file: iv missing")

        val factory = SecretKeyFactory.getInstance(KEY_DERIVATION)
        val spec = PBEKeySpec(password, salt, ITERATIONS, KEY_LENGTH)
        val secretKey = SecretKeySpec(factory.generateSecret(spec).encoded, "AES")

        val cipher = Cipher.getInstance(ALGORITHM)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(TAG_LENGTH_BITS, iv))

        return CipherInputStream(inputStream, cipher).use { cis ->
            cis.bufferedReader(Charsets.UTF_8).readText()
        }
    }

    fun encryptStream(password: CharArray, inputStream: InputStream, outputStream: OutputStream) {
        val random = SecureRandom()
        val salt = ByteArray(SALT_SIZE).also(random::nextBytes)
        val iv = ByteArray(IV_SIZE).also(random::nextBytes)

        val factory = SecretKeyFactory.getInstance(KEY_DERIVATION)
        val spec = PBEKeySpec(password, salt, ITERATIONS, KEY_LENGTH)
        val secretKey = SecretKeySpec(factory.generateSecret(spec).encoded, "AES")

        val cipher = Cipher.getInstance(ALGORITHM)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, GCMParameterSpec(TAG_LENGTH_BITS, iv))

        outputStream.write(MAGIC_HEADER)
        outputStream.write(salt)
        outputStream.write(iv)

        CipherOutputStream(outputStream, cipher).use { cos ->
            inputStream.copyTo(cos)
            cos.flush()
        }
    }

    fun decryptStream(password: CharArray, inputStream: InputStream, outputStream: OutputStream) {
        val magic = ByteArray(MAGIC_HEADER.size)
        if (inputStream.read(magic) != MAGIC_HEADER.size || !magic.contentEquals(MAGIC_HEADER)) {
            error("Invalid encrypted backup file: header mismatch")
        }
        val salt = ByteArray(SALT_SIZE)
        val iv = ByteArray(IV_SIZE)

        if (inputStream.read(salt) != SALT_SIZE) error("Invalid backup file: salt missing")
        if (inputStream.read(iv) != IV_SIZE) error("Invalid backup file: iv missing")

        val factory = SecretKeyFactory.getInstance(KEY_DERIVATION)
        val spec = PBEKeySpec(password, salt, ITERATIONS, KEY_LENGTH)
        val secretKey = SecretKeySpec(factory.generateSecret(spec).encoded, "AES")

        val cipher = Cipher.getInstance(ALGORITHM)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(TAG_LENGTH_BITS, iv))

        CipherInputStream(inputStream, cipher).use { cis ->
            cis.copyTo(outputStream)
            outputStream.flush()
        }
    }

    fun isEncryptedBackup(inputStream: InputStream): Boolean {
        if (!inputStream.markSupported()) return false
        inputStream.mark(MAGIC_HEADER.size)
        val magic = ByteArray(MAGIC_HEADER.size)
        val read = inputStream.read(magic)
        inputStream.reset()
        return read == MAGIC_HEADER.size && magic.contentEquals(MAGIC_HEADER)
    }

    private val MAGIC_HEADER = "BTLBAK01".toByteArray(Charsets.UTF_8)
}
