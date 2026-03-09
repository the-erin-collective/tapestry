package net.minecraft.loot;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Test helper that exposes a public no-arg constructor for LootTable by
 * subclassing inside the same package (to access the package-private
 * constructor).
 */
public class DummyLootTable extends LootTable {
    public DummyLootTable() {
        super(null, Optional.empty(), Collections.emptyList(), Collections.emptyList());
    }
}
