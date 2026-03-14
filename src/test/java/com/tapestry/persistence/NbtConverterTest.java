package com.tapestry.persistence;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class NbtConverterTest {

    @Test
    void primitiveTypesRoundTrip() {
        assertEquals("foo", NbtConverter.fromNbt(NbtConverter.toNbt("foo")));
        assertEquals(123, NbtConverter.fromNbt(NbtConverter.toNbt(123)));
        assertEquals(42L, NbtConverter.fromNbt(NbtConverter.toNbt(42L)));
        assertEquals(3.14, NbtConverter.fromNbt(NbtConverter.toNbt(3.14)));
        assertEquals(2.7f, NbtConverter.fromNbt(NbtConverter.toNbt(2.7f)));
        assertEquals(true, NbtConverter.fromNbt(NbtConverter.toNbt(true)));
        assertNull(NbtConverter.fromNbt(NbtConverter.toNbt(null)));
    }

    @Test
    void listConversion() {
        List<Object> original = Arrays.asList("a", 1, false);
        Object converted = NbtConverter.fromNbt(NbtConverter.toNbt(original));
        assertTrue(converted instanceof List);
        assertEquals(original, converted);
    }

    @Test
    void mapConversion() {
        Map<String, Object> original = new HashMap<>();
        original.put("x", "test");
        original.put("n", 5);
        original.put("nested", Map.of("foo", 1));

        NbtCompound compound = NbtConverter.toNbtCompound(original);
        Map<String, Object> recovered = NbtConverter.toJavaMap(compound);
        assertEquals(original, recovered);
    }

    @Test
    void unsupportedTypeThrows() {
        assertThrows(IllegalArgumentException.class, () -> NbtConverter.toNbt(UUID.randomUUID()));
    }
}
