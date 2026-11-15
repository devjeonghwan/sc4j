package org.fogofwarlabs.sc4j.beaconlink.channel;

import org.fogofwarlabs.sc4j.beaconlink.protocol.identity.IdentityKey;
import org.fogofwarlabs.sc4j.beaconlink.protocol.identity.IdentityKeyPair;
import org.fogofwarlabs.sc4j.beaconlink.protocol.message.SealedMessage;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;

/**
 * Tests the sealing of whispers: only the target's whisper key pair opens a whisper, for the sender and the target it was sealed for, a changed
 * whisper does not open, and an invalid whisper key seals nothing.
 */
final class WhisperSealUtilsTest {
    private static final byte @NotNull [] TEXT = "see you at nine".getBytes(StandardCharsets.UTF_8);
    private static final long CONNECTION = 0x0102030405060708L;

    @Test
    void open_should_give_the_text_to_the_target_only() {
        SecureRandom random = new SecureRandom();
        IdentityKey sender = IdentityKeyPair.generate(random).getPublicKey();
        IdentityKey target = IdentityKeyPair.generate(random).getPublicKey();
        IdentityKey stranger = IdentityKeyPair.generate(random).getPublicKey();
        EphemeralKeyPair whisperKeys = EphemeralKeyPair.generate(random);
        SealedMessage sealed = WhisperSealUtils.seal(random, sender, target, CONNECTION, whisperKeys.getPublicKey(), TEXT);

        Assertions.assertNotNull(sealed);
        Assertions.assertEquals(TEXT.length + WhisperSealUtils.TAG_LENGTH, sealed.getSealed().length);
        Assertions.assertArrayEquals(TEXT, WhisperSealUtils.open(whisperKeys, sender, target, sealed));
        Assertions.assertNull(WhisperSealUtils.open(EphemeralKeyPair.generate(random), sender, target, sealed), "another key pair");
        Assertions.assertNull(WhisperSealUtils.open(whisperKeys, stranger, target, sealed), "another sender");
        Assertions.assertNull(WhisperSealUtils.open(whisperKeys, sender, stranger, sealed), "another target");

        byte[] changed = sealed.getSealed();

        changed[0] ^= 1;

        Assertions.assertNull(WhisperSealUtils.open(whisperKeys, sender, target,
                                                    new SealedMessage(target, CONNECTION, sealed.getEphemeralKey(), changed)));
        Assertions.assertNull(WhisperSealUtils.seal(random, sender, target, CONNECTION, new byte[EphemeralKeyPair.KEY_LENGTH], TEXT), "the zero key");
    }
}
