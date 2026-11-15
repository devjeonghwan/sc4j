package org.fogofwarlabs.sc4j.beaconlink.protocol.identity;

import org.fogofwarlabs.sc4j.base.exception.Exceptions;
import org.jetbrains.annotations.NotNull;

import java.security.*;
import java.security.interfaces.EdECPrivateKey;
import java.security.spec.EdECPrivateKeySpec;
import java.security.spec.NamedParameterSpec;
import java.util.Arrays;

/**
 * Holds the Ed25519 key pair of one install or one node: the private key that signs its messages and advertisements, and its public
 * {@link IdentityKey}. The private key never leaves the pair except into its key file ({@link IdentityKeyFileUtils}).
 */
public final class IdentityKeyPair {
    /**
     * The byte length of an Ed25519 private key.
     */
    public static final int PRIVATE_LENGTH = 32;

    private static final byte @NotNull [] CHECK_MESSAGE = {'c', 'h', 'e', 'c', 'k'};

    /**
     * Creates an {@link IdentityKeyPair} instance with a new random key pair.
     *
     * @param random the source of the private key
     * @return the created {@link IdentityKeyPair} instance
     */
    public static @NotNull IdentityKeyPair generate(@NotNull SecureRandom random) {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance(IdentityKey.ALGORITHM);

            generator.initialize(NamedParameterSpec.ED25519, random);

            KeyPair pair = generator.generateKeyPair();
            byte[] encoded = pair.getPublic().getEncoded();
            byte[] publicBytes = Arrays.copyOfRange(encoded, encoded.length - IdentityKey.LENGTH, encoded.length);
            byte[] privateBytes = ((EdECPrivateKey) pair.getPrivate()).getBytes().orElseThrow();

            return new IdentityKeyPair(pair.getPrivate(), privateBytes, IdentityKey.of(publicBytes));
        } catch (GeneralSecurityException exception) {
            throw Exceptions.newIllegalStateException(exception, "The JDK cannot create an {0} key pair.", IdentityKey.ALGORITHM);
        }
    }

    /**
     * Creates an {@link IdentityKeyPair} instance from stored key bytes and checks that the two keys belong together.
     *
     * @param privateBytes the {@value #PRIVATE_LENGTH} private key bytes
     * @param publicKey    the public key
     * @return the created {@link IdentityKeyPair} instance
     * @throws IllegalArgumentException if the private key has the wrong length or does not belong to the public key
     */
    public static @NotNull IdentityKeyPair of(byte @NotNull [] privateBytes, @NotNull IdentityKey publicKey) {
        if (privateBytes.length != PRIVATE_LENGTH) {
            throw Exceptions.newIllegalArgumentException("A private key has {0} bytes, not {1}.", PRIVATE_LENGTH, privateBytes.length);
        }

        PrivateKey privateKey;

        try {
            privateKey = KeyFactory.getInstance(IdentityKey.ALGORITHM)
                                   .generatePrivate(new EdECPrivateKeySpec(NamedParameterSpec.ED25519, privateBytes.clone()));
        } catch (GeneralSecurityException exception) {
            throw Exceptions.newIllegalArgumentException(exception, "The private key bytes are not an {0} key.", IdentityKey.ALGORITHM);
        }

        IdentityKeyPair pair = new IdentityKeyPair(privateKey, privateBytes.clone(), publicKey);
        byte[] signature = pair.sign(CHECK_MESSAGE, 0, CHECK_MESSAGE.length);

        if (!publicKey.verify(CHECK_MESSAGE, 0, CHECK_MESSAGE.length, signature, 0)) {
            throw Exceptions.newIllegalArgumentException("The private key does not belong to the public key {0}.", publicKey);
        }

        return pair;
    }


    private final @NotNull PrivateKey privateKey;
    private final byte @NotNull [] privateBytes;
    private final @NotNull IdentityKey publicKey;

    private IdentityKeyPair(@NotNull PrivateKey privateKey, byte @NotNull [] privateBytes, @NotNull IdentityKey publicKey) {
        this.privateKey   = privateKey;
        this.privateBytes = privateBytes;
        this.publicKey    = publicKey;
    }

    /**
     * @return the public key, the identity of the pair's owner
     */
    public @NotNull IdentityKey getPublicKey() {
        return this.publicKey;
    }

    /**
     * @return a copy of the private key bytes, for the key file only
     */
    byte @NotNull [] getPrivateBytes() {
        return this.privateBytes.clone();
    }


    /**
     * Signs a byte range.
     *
     * @param data   the bytes
     * @param offset the first byte to sign
     * @param length the number of bytes
     * @return the {@value IdentityKey#SIGNATURE_LENGTH} signature bytes
     */
    public byte @NotNull [] sign(byte @NotNull [] data, int offset, int length) {
        try {
            Signature signer = Signature.getInstance(IdentityKey.ALGORITHM);

            signer.initSign(this.privateKey);
            signer.update(data, offset, length);

            return signer.sign();
        } catch (GeneralSecurityException exception) {
            throw Exceptions.newIllegalStateException(exception, "The JDK cannot sign with an {0} key.", IdentityKey.ALGORITHM);
        }
    }

    /**
     * @return the public key, for logs; the private key is never shown
     */
    @Override
    public @NotNull String toString() {
        return this.publicKey.toString();
    }
}
