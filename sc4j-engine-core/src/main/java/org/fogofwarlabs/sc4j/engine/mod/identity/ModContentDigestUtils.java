package org.fogofwarlabs.sc4j.engine.mod.identity;

import org.fogofwarlabs.sc4j.base.exception.Exceptions;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Computes the content digest of a mod jar: SHA-256 over every file entry in entry-name order, each as its UTF-8 name, a
 * zero byte, its size as eight big-endian bytes and its contents. Directory entries, timestamps, compression and the
 * order of entries in the jar do not change the digest; any change of a name or of a file's bytes does.
 */
public final class ModContentDigestUtils {
    /**
     * The digest algorithm.
     */
    public static final @NotNull String DIGEST_ALGORITHM = "SHA-256";

    /**
     * Computes the content digest of a jar.
     *
     * @param jar the mod jar
     * @return the SHA-256 digest
     * @throws IOException if the jar cannot be read as a zip file
     */
    public static byte @NotNull [] compute(@NotNull Path jar) throws IOException {
        MessageDigest digest = createDigest();
        ByteBuffer size = ByteBuffer.allocate(Long.BYTES);

        try (ZipFile zip = new ZipFile(jar.toFile())) {
            List<ZipEntry> entries = new ArrayList<>();
            Enumeration<? extends ZipEntry> enumeration = zip.entries();

            while (enumeration.hasMoreElements()) {
                ZipEntry entry = enumeration.nextElement();

                if (!entry.isDirectory()) {
                    entries.add(entry);
                }
            }

            entries.sort(Comparator.comparing(ZipEntry::getName));

            for (ZipEntry entry : entries) {
                byte[] contents;

                try (InputStream input = zip.getInputStream(entry)) {
                    contents = input.readAllBytes();
                }

                digest.update(entry.getName().getBytes(StandardCharsets.UTF_8));
                digest.update((byte) 0);
                digest.update(size.clear().putLong(contents.length).array());
                digest.update(contents);
            }
        }

        return digest.digest();
    }

    /**
     * Creates a SHA-256 digest.
     *
     * @return the created {@link MessageDigest} instance
     * @throws AssertionError if the SHA-256 algorithm is unavailable
     */
    static @NotNull MessageDigest createDigest() {
        try {
            return MessageDigest.getInstance(DIGEST_ALGORITHM);
        } catch (NoSuchAlgorithmException exception) {
            throw Exceptions.newAssertionError(exception, "Required SHA-256 algorithm is unavailable");
        }
    }


    private ModContentDigestUtils() {
    }
}
