package com.example.core.adb

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.io.File
import java.math.BigInteger
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.KeyFactory
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.MessageDigest
import java.security.SecureRandom
import java.security.Signature
import java.security.interfaces.RSAPublicKey
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Cryptographic provider for ADB RSA-2048 host keys.
 *
 * Security guarantees:
 * 1. Never stores raw RSA private keys on disk or logs secret bytes.
 * 2. Wraps the generated RSA-2048 private key using an AES-256-GCM master key held inside
 *    AndroidKeyStore (`AndroidKeyStore`), falling back to ephemeral in-memory keys ONLY inside
 *    unit/JVM tests where the hardware AndroidKeyStore provider is absent.
 * 3. Encodes the standard Android ADB RSAPublicKey binary struct (524 bytes + Base64 + comment)
 *    required by ADB `AUTH_RSAPUBLICKEY` and signs 20-byte SHA-1 tokens per `adbd` v1.0 protocol
 *    (PKCS#1 v1.5 DigestInfo prefix + 20-byte token signed with raw RSA private operation).
 */
interface AdbKeyManager {
    fun publicKeyFingerprintSha256(): String
    fun adbPublicKeyPayloadBytes(deviceComment: String = "tveoclean@android"): ByteArray
    fun signAdbToken(token: ByteArray): ByteArray
    fun rotateOrRevokeKeys()
}

class KeystoreAdbKeyManager(
    private val context: Context? = null
) : AdbKeyManager {

    companion object {
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val WRAP_KEY_ALIAS = "tveoclean_adb_master_aes_v1"
        private const val ENCRYPTED_KEY_FILE = "adb_rsa_wrapped_v1.bin"
        private const val RSA_BITS = 2048
        private const val ADB_PUBKEY_MODULUS_WORDS = 2048 / 32 // 64 uint32 words

        // ASN.1 DigestInfo header for SHA-1 (15 bytes) expected by Android's adbd RSA_verify
        private val SHA1_DIGEST_INFO_PREFIX = byteArrayOf(
            0x30, 0x21, 0x30, 0x09, 0x06, 0x05, 0x2B, 0x0E,
            0x03, 0x02, 0x1A, 0x05, 0x00, 0x04, 0x14
        )
    }

    @Volatile
    private var cachedKeyPair: KeyPair? = null

    private fun getOrCreateKeyPair(): KeyPair {
        cachedKeyPair?.let { return it }
        synchronized(this) {
            cachedKeyPair?.let { return it }
            val loaded = loadWrappedKeyPairFromDisk()
            if (loaded != null) {
                cachedKeyPair = loaded
                return loaded
            }
            val fresh = generateFreshRsaKeyPair()
            persistWrappedKeyPairToDisk(fresh)
            cachedKeyPair = fresh
            return fresh
        }
    }

    override fun publicKeyFingerprintSha256(): String {
        val pubEncoded = getOrCreateKeyPair().public.encoded
        val digest = MessageDigest.getInstance("SHA-256").digest(pubEncoded)
        return digest.joinToString(":") { "%02X".format(it) }
    }

    override fun signAdbToken(token: ByteArray): ByteArray {
        val keyPair = getOrCreateKeyPair()
        // Standard adbd sends a 20-byte random token in AUTH_TOKEN and verifies it as a SHA-1 DigestInfo
        return if (token.size == 20) {
            val digestInfo = ByteArray(SHA1_DIGEST_INFO_PREFIX.size + token.size)
            System.arraycopy(SHA1_DIGEST_INFO_PREFIX, 0, digestInfo, 0, SHA1_DIGEST_INFO_PREFIX.size)
            System.arraycopy(token, 0, digestInfo, SHA1_DIGEST_INFO_PREFIX.size, token.size)
            val cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding")
            cipher.init(Cipher.ENCRYPT_MODE, keyPair.private)
            cipher.doFinal(digestInfo)
        } else {
            val sig = Signature.getInstance("SHA1withRSA")
            sig.initSign(keyPair.private)
            sig.update(token)
            sig.sign()
        }
    }

    override fun adbPublicKeyPayloadBytes(deviceComment: String): ByteArray {
        val rsaPub = getOrCreateKeyPair().public as RSAPublicKey
        val structBytes = encodeAndroidRsaPublicKeyStruct(rsaPub)
        val b64 = encodeBase64NoWrap(structBytes)
        // Null-terminated ASCII string required by adbd AUTH_RSAPUBLICKEY
        return "$b64 $deviceComment\u0000".toByteArray(Charsets.UTF_8)
    }

    override fun rotateOrRevokeKeys() {
        synchronized(this) {
            cachedKeyPair = null
            context?.let { ctx ->
                val file = File(ctx.noBackupFilesDir, ENCRYPTED_KEY_FILE)
                if (file.exists()) {
                    file.delete()
                }
            }
            runCatching {
                val ks = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
                if (ks.containsAlias(WRAP_KEY_ALIAS)) {
                    ks.deleteEntry(WRAP_KEY_ALIAS)
                }
            }
            // Immediately provision a clean replacement keypair
            getOrCreateKeyPair()
        }
    }

    private fun generateFreshRsaKeyPair(): KeyPair {
        val generator = KeyPairGenerator.getInstance("RSA")
        generator.initialize(RSA_BITS, SecureRandom())
        return generator.generateKeyPair()
    }

    private fun getOrCreateKeystoreAesKey(): SecretKey? {
        if (context == null) return null
        return runCatching {
            val ks = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
            val existing = ks.getKey(WRAP_KEY_ALIAS, null) as? SecretKey
            if (existing != null) return existing

            val keyGen = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
            val spec = KeyGenParameterSpec.Builder(
                WRAP_KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()
            keyGen.init(spec)
            keyGen.generateKey()
        }.getOrNull()
    }

    private fun persistWrappedKeyPairToDisk(keyPair: KeyPair) {
        val ctx = context ?: return
        val aesKey = getOrCreateKeystoreAesKey() ?: return
        runCatching {
            val pubBytes = keyPair.public.encoded
            val privBytes = keyPair.private.encoded
            val plainBuffer = ByteBuffer.allocate(8 + pubBytes.size + privBytes.size)
                .putInt(pubBytes.size)
                .putInt(privBytes.size)
                .put(pubBytes)
                .put(privBytes)
                .array()

            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, aesKey)
            val iv = cipher.iv
            val cipherText = cipher.doFinal(plainBuffer)

            val outBuffer = ByteBuffer.allocate(4 + iv.size + cipherText.size)
                .putInt(iv.size)
                .put(iv)
                .put(cipherText)
                .array()

            val targetFile = File(ctx.noBackupFilesDir, ENCRYPTED_KEY_FILE)
            targetFile.writeBytes(outBuffer)
        }
    }

    private fun loadWrappedKeyPairFromDisk(): KeyPair? {
        val ctx = context ?: return null
        val targetFile = File(ctx.noBackupFilesDir, ENCRYPTED_KEY_FILE)
        if (!targetFile.exists()) return null
        val aesKey = getOrCreateKeystoreAesKey() ?: return null

        return runCatching {
            val raw = targetFile.readBytes()
            val buf = ByteBuffer.wrap(raw)
            val ivLen = buf.int
            if (ivLen !in 12..16 || raw.size <= 4 + ivLen) return null
            val iv = ByteArray(ivLen)
            buf.get(iv)
            val cipherText = ByteArray(buf.remaining())
            buf.get(cipherText)

            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, aesKey, GCMParameterSpec(128, iv))
            val plain = cipher.doFinal(cipherText)
            val plainBuf = ByteBuffer.wrap(plain)
            val pubLen = plainBuf.int
            val privLen = plainBuf.int
            val pubBytes = ByteArray(pubLen)
            val privBytes = ByteArray(privLen)
            plainBuf.get(pubBytes)
            plainBuf.get(privBytes)

            val kf = KeyFactory.getInstance("RSA")
            val pub = kf.generatePublic(X509EncodedKeySpec(pubBytes))
            val priv = kf.generatePrivate(PKCS8EncodedKeySpec(privBytes))
            KeyPair(pub, priv)
        }.getOrNull()
    }

    /**
     * Encodes the 524-byte little-endian RSAPublicKey struct expected by Android's `libcrypto_utils`:
     * uint32 modulus_size_words (64)
     * uint32 n0inv (-1 / n[0] mod 2^32)
     * uint8 modulus[256] (little-endian words)
     * uint8 rr[256] (R^2 mod N, little-endian words)
     * uint32 exponent (65537)
     */
    private fun encodeAndroidRsaPublicKeyStruct(pubKey: RSAPublicKey): ByteArray {
        val n = pubKey.modulus
        val r32 = BigInteger.ONE.shiftLeft(32)
        val r = BigInteger.ONE.shiftLeft(RSA_BITS)
        val rr = r.multiply(r).mod(n)

        val n0 = n.mod(r32)
        val n0Inv = n0.modInverse(r32).negate().mod(r32)

        val buffer = ByteBuffer.allocate(4 + 4 + 256 + 256 + 4).order(ByteOrder.LITTLE_ENDIAN)
        buffer.putInt(ADB_PUBKEY_MODULUS_WORDS)
        buffer.putInt(n0Inv.toInt())

        var tempN = n
        repeat(ADB_PUBKEY_MODULUS_WORDS) {
            val word = tempN.mod(r32).toInt()
            buffer.putInt(word)
            tempN = tempN.shiftRight(32)
        }

        var tempRr = rr
        repeat(ADB_PUBKEY_MODULUS_WORDS) {
            val word = tempRr.mod(r32).toInt()
            buffer.putInt(word)
            tempRr = tempRr.shiftRight(32)
        }

        buffer.putInt(pubKey.publicExponent.toInt())
        return buffer.array()
    }

    private fun encodeBase64NoWrap(data: ByteArray): String {
        val alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/"
        val out = StringBuilder(((data.size + 2) / 3) * 4)
        var i = 0
        while (i < data.size) {
            val b0 = data[i].toInt() and 0xFF
            val b1 = if (i + 1 < data.size) data[i + 1].toInt() and 0xFF else 0
            val b2 = if (i + 2 < data.size) data[i + 2].toInt() and 0xFF else 0
            val triple = (b0 shl 16) or (b1 shl 8) or b2
            out.append(alphabet[(triple ushr 18) and 0x3F])
            out.append(alphabet[(triple ushr 12) and 0x3F])
            out.append(if (i + 1 < data.size) alphabet[(triple ushr 6) and 0x3F] else '=')
            out.append(if (i + 2 < data.size) alphabet[triple and 0x3F] else '=')
            i += 3
        }
        return out.toString()
    }
}
