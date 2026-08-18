package com.zerowiki.source;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ObsidianImporterTest {
    @Test
    void extracts_markdown_and_ignores_non_markdown_entries() throws Exception {
        byte[] zip = zip(List.of(new Entry("notes/first.md", "# first"),
                new Entry("image.png", "binary")));

        List<ObsidianImporter.Entry> entries = ObsidianImporter.markdownEntries(new ByteArrayInputStream(zip));

        assertThat(entries).extracting(ObsidianImporter.Entry::name).containsExactly("notes/first.md");
        assertThat(new String(entries.get(0).content())).isEqualTo("# first");
    }

    @Test
    void rejects_zip_path_traversal() throws Exception {
        byte[] zip = zip(List.of(new Entry("../escape.md", "bad")));

        assertThatThrownBy(() -> ObsidianImporter.markdownEntries(new ByteArrayInputStream(zip)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("traversal");
    }

    @Test
    void detects_secrets_before_source_persistence() {
        assertThat(SecretDetector.containsSecret("api_key=0123456789abcdef".getBytes())).isTrue();
        assertThat(SecretDetector.containsSecret("ordinary markdown".getBytes())).isFalse();
    }

    private byte[] zip(List<Entry> entries) throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(output)) {
            for (Entry item : entries) {
                zip.putNextEntry(new ZipEntry(item.name()));
                zip.write(item.content().getBytes());
                zip.closeEntry();
            }
        }
        return output.toByteArray();
    }

    private record Entry(String name, String content) {
    }
}
