package com.zerowiki.source;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Temporary object-store adapter for the M2 slice.
 *
 * // ASSUMPTION(UD-13): production S3/R2 credentials and signed URL provider are not selected yet.
 * // The key stays internal; replace this component with the selected object-store adapter before production.
 */
@Component
public class LocalObjectStore {
    private final Path root;

    @Autowired
    public LocalObjectStore(@Value("${zerowiki.storage.local-root:build/object-store}") String root) {
        this(Path.of(root));
    }

    LocalObjectStore(Path root) {
        this.root = root.toAbsolutePath().normalize();
    }

    public StoredObject write(String key, InputStream input, long maxBytes) {
        Path target = resolve(key);
        Path temporary = target.resolveSibling(target.getFileName() + ".uploading");
        try {
            Files.createDirectories(target.getParent());
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            long size = 0;
            byte[] buffer = new byte[8192];
            try (OutputStream output = Files.newOutputStream(temporary)) {
                int read;
                while ((read = input.read(buffer)) >= 0) {
                    size += read;
                    if (size > maxBytes) {
                        throw new IllegalArgumentException("upload exceeds configured size limit");
                    }
                    digest.update(buffer, 0, read);
                    output.write(buffer, 0, read);
                }
            }
            Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            return new StoredObject(size, hex(digest.digest()));
        } catch (IOException exception) {
            deleteQuietly(temporary);
            throw new IllegalStateException("object storage write failed", exception);
        } catch (NoSuchAlgorithmException exception) {
            deleteQuietly(temporary);
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        } catch (RuntimeException exception) {
            deleteQuietly(temporary);
            throw exception;
        }
    }

    public byte[] read(String key) {
        try {
            return Files.readAllBytes(resolve(key));
        } catch (IOException exception) {
            throw new IllegalStateException("object storage read failed", exception);
        }
    }

    public InputStream open(String key) {
        try {
            return Files.newInputStream(resolve(key));
        } catch (IOException exception) {
            throw new IllegalStateException("object storage read failed", exception);
        }
    }

    public boolean exists(String key) {
        return Files.isRegularFile(resolve(key));
    }

    public void delete(String key) {
        try {
            Files.deleteIfExists(resolve(key));
        } catch (IOException exception) {
            throw new IllegalStateException("object storage delete failed", exception);
        }
    }

    private Path resolve(String key) {
        if (key == null || key.isBlank() || key.indexOf('\0') >= 0) {
            throw new IllegalArgumentException("invalid storage key");
        }
        Path resolved = root.resolve(key).normalize();
        if (!resolved.startsWith(root)) {
            throw new IllegalArgumentException("invalid storage key");
        }
        return resolved;
    }

    private void deleteQuietly(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException ignored) {
            // The original failure is more useful to the caller.
        }
    }

    private String hex(byte[] bytes) {
        StringBuilder result = new StringBuilder(bytes.length * 2);
        for (byte value : bytes) {
            result.append(Character.forDigit((value >>> 4) & 0xf, 16));
            result.append(Character.forDigit(value & 0xf, 16));
        }
        return result.toString();
    }

    public record StoredObject(long byteSize, String sha256) {
    }
}
