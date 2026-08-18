package com.zerowiki.source;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.file.Files;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LocalObjectStoreTest {
    @Test
    void writes_hashes_and_keeps_keys_inside_root() throws Exception {
        LocalObjectStore store = new LocalObjectStore(Files.createTempDirectory("zerowiki-store"));

        LocalObjectStore.StoredObject object = store.write("uploads/a", new ByteArrayInputStream("hello".getBytes()), 10);

        assertThat(object.byteSize()).isEqualTo(5);
        assertThat(object.sha256()).isEqualTo("2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824");
        assertThat(store.read("uploads/a")).containsExactly("hello".getBytes());
        assertThatThrownBy(() -> store.read("../outside")).isInstanceOf(IllegalArgumentException.class);
    }
}
