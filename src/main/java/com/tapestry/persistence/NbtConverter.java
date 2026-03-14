package com.tapestry.persistence;

import net.minecraft.nbt.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Utility for converting between vanilla NBT objects and plain Java types.
 *
 * Only JSON-compatible values are supported: String, Number, Boolean,
 * List, and Map.  Any attempt to convert an unsupported type throws an
 * {@link IllegalArgumentException}.
 */
public class NbtConverter {

    private NbtConverter() { }

    /**
     * Convert a Java object into an NBT element.
     */
    public static NbtElement toNbt(Object value) {
        if (value == null) {
            return NbtEnd.INSTANCE;
        }
        if (value instanceof String s) {
            return NbtString.of(s);
        }
        if (value instanceof Integer i) {
            return NbtInt.of(i);
        }
        if (value instanceof Long l) {
            return NbtLong.of(l);
        }
        if (value instanceof Double d) {
            return NbtDouble.of(d);
        }
        if (value instanceof Float f) {
            return NbtFloat.of(f);
        }
        if (value instanceof Boolean b) {
            return NbtByte.of((byte) (b ? 1 : 0));
        }
        if (value instanceof Map<?, ?> m) {
            return toNbtCompound((Map<String, Object>) m);
        }
        if (value instanceof List<?> list) {
            NbtList nbtList = new NbtList();
            for (Object item : list) {
                nbtList.add(toNbt(item));
            }
            return nbtList;
        }
        throw new IllegalArgumentException("Unsupported player data type: " +
                (value == null ? "null" : value.getClass()));
    }

    /**
     * Helper used by {@link #toNbt} when converting a map.
     */
    @SuppressWarnings("unchecked")
    public static NbtCompound toNbtCompound(Map<String, Object> map) {
        NbtCompound compound = new NbtCompound();
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            compound.put(entry.getKey(), toNbt(entry.getValue()));
        }
        return compound;
    }

    /**
     * Convert an NBT element back into a Java object.
     */
    public static Object fromNbt(NbtElement element) {
        if (element instanceof NbtEnd) {
            return null;
        }
        if (element instanceof NbtString s) {
            return s.asString();
        }
        if (element instanceof NbtInt i) {
            return i.intValue();
        }
        if (element instanceof NbtLong l) {
            return l.longValue();
        }
        if (element instanceof NbtDouble d) {
            return d.doubleValue();
        }
        if (element instanceof NbtFloat f) {
            return f.floatValue();
        }
        if (element instanceof NbtByte b) {
            // treat byte 1 as true, otherwise false
            return b.byteValue() != 0;
        }
        if (element instanceof NbtList list) {
            List<Object> out = new ArrayList<>();
            for (NbtElement sub : list) {
                out.add(fromNbt(sub));
            }
            return out;
        }
        if (element instanceof NbtCompound comp) {
            return toJavaMap(comp);
        }
        // fall back to raw element
        return element;
    }

    /**
     * Convert an NbtCompound to a Java map.
     */
    public static Map<String, Object> toJavaMap(NbtCompound compound) {
        Map<String, Object> map = new HashMap<>();
        for (String key : compound.getKeys()) {
            map.put(key, fromNbt(compound.get(key)));
        }
        return map;
    }
}