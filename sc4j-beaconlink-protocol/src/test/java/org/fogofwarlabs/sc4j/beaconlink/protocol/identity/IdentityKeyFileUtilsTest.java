package org.fogofwarlabs.sc4j.beaconlink.protocol.identity;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;

/**
 * Tests the key file: it keeps the identity across reads, and broken or mismatched key files are refused.
 */
final class IdentityKeyFileUtilsTest {
    private static final byte @NotNull [] MESSAGE = "room".getBytes(StandardCharsets.US_ASCII);

    @Test
    void read_or_create_should_keep_the_identity_across_reads(@TempDir @NotNull Path directory) throws IOException {
        Path file = directory.resolve("beaconlink").resolve("identity.key");
        IdentityKeyPair created = IdentityKeyFileUtils.readOrCreate(file, new SecureRandom());
        IdentityKeyPair read = IdentityKeyFileUtils.readOrCreate(file, new SecureRandom());
        byte[] signature = read.sign(MESSAGE, 0, MESSAGE.length);

        Assertions.assertEquals(created.getPublicKey(), read.getPublicKey());
        Assertions.assertTrue(created.getPublicKey().verify(MESSAGE, 0, MESSAGE.length, signature, 0));
    }

    @Test
    void read_should_refuse_broken_and_mismatched_key_files(@TempDir @NotNull Path directory) throws IOException {
        Path broken = directory.resolve("broken.key");
        Path mismatched = directory.resolve("mismatched.key");
        SecureRandom random = new SecureRandom();

        Files.write(broken, new byte[]{'B', 'L', 'K', 'F'});
        IdentityKeyFileUtils.write(mismatched, IdentityKeyPair.generate(random));

        byte[] contents = Files.readAllBytes(mismatched);
        byte[] otherPublic = IdentityKeyPair.generate(random).getPublicKey().getBytes();

        System.arraycopy(otherPublic, 0, contents, contents.length - IdentityKey.LENGTH, IdentityKey.LENGTH);
        Files.write(mismatched, contents);

        Assertions.assertThrows(IOException.class, () -> IdentityKeyFileUtils.read(broken));
        Assertions.assertThrows(IOException.class, () -> IdentityKeyFileUtils.read(mismatched));
    }
}
