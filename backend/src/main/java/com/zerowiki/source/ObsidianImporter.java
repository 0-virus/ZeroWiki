package com.zerowiki.source;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

final class ObsidianImporter {
    static final int MAX_ENTRIES = 1000;
    static final long MAX_UNCOMPRESSED_BYTES = 1_073_741_824L;
    static final int MAX_DOCUMENT_BYTES = 200 * 1024 * 1024;

    private ObsidianImporter() {
    }

    static List<Entry> markdownEntries(InputStream input) {
        List<Entry> result = new ArrayList<>();
        long totalBytes = 0;
        int entries = 0;
        try (ZipInputStream zip = new ZipInputStream(input)) {
            ZipEntry entry;
            byte[] buffer = new byte[8192];
            while ((entry = zip.getNextEntry()) != null) {
                if (++entries > MAX_ENTRIES) {
                    throw new IllegalArgumentException("ZIP contains too many entries");
                }
                String name = safeName(entry.getName());
                boolean markdown = !entry.isDirectory()
                        && (name.toLowerCase(java.util.Locale.ROOT).endsWith(".md")
                        || name.toLowerCase(java.util.Locale.ROOT).endsWith(".markdown"));
                ByteArrayOutputStream document = markdown ? new ByteArrayOutputStream() : null;
                long entryBytes = 0;
                int read;
                while ((read = zip.read(buffer)) >= 0) {
                    entryBytes += read;
                    totalBytes += read;
                    if (entryBytes > MAX_DOCUMENT_BYTES || totalBytes > MAX_UNCOMPRESSED_BYTES) {
                        throw new IllegalArgumentException("ZIP decompressed size exceeds limit");
                    }
                    if (document != null) {
                        document.write(buffer, 0, read);
                    }
                }
                if (document != null) {
                    result.add(new Entry(name, document.toByteArray()));
                }
                zip.closeEntry();
            }
            return List.copyOf(result);
        } catch (IOException exception) {
            throw new IllegalArgumentException("invalid Obsidian ZIP", exception);
        }
    }

    private static String safeName(String raw) {
        if (raw == null || raw.isBlank() || raw.indexOf('\0') >= 0) {
            throw new IllegalArgumentException("ZIP entry path is invalid");
        }
        String name = raw.replace('\\', '/');
        if (name.startsWith("/") || name.matches("^[A-Za-z]:.*")) {
            throw new IllegalArgumentException("ZIP path traversal detected");
        }
        for (String part : name.split("/", -1)) {
            if (part.equals("..")) {
                throw new IllegalArgumentException("ZIP path traversal detected");
            }
        }
        return name;
    }

    record Entry(String name, byte[] content) {
    }
}
