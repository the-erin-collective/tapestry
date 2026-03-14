package com.tapestry.gameplay.traits;

import com.tapestry.gameplay.GameplayAPI;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TraitAPITest {
    private GameplayAPI api;

    @BeforeEach
    void setUp() {
        // phase controller must traverse the normal lifecycle so that
        // TS_REGISTER is reachable (cannot jump directly).
        com.tapestry.lifecycle.PhaseController.reset();
        com.tapestry.lifecycle.PhaseController pc = com.tapestry.lifecycle.PhaseController.getInstance();
        pc.advanceTo(com.tapestry.lifecycle.TapestryPhase.DISCOVERY);
        pc.advanceTo(com.tapestry.lifecycle.TapestryPhase.VALIDATION);
        pc.advanceTo(com.tapestry.lifecycle.TapestryPhase.REGISTRATION);
        pc.advanceTo(com.tapestry.lifecycle.TapestryPhase.FREEZE);
        pc.advanceTo(com.tapestry.lifecycle.TapestryPhase.TS_LOAD);
        pc.advanceTo(com.tapestry.lifecycle.TapestryPhase.TS_REGISTER);

        // ensure fresh API and phase state
        api = new GameplayAPI();
    }

    @Test
    void registerShouldDelegateToGameplayAPI() {
        // call static helper
        TraitAPI.register("foo_trait", new TraitConfig("tapestry:foo_items"));
        
        TraitDefinition def = api.getTraits().getTrait("foo_trait");
        assertNotNull(def, "TraitAPI.register should add trait to gameplay API");
        assertEquals("tapestry:foo_items", def.getTag());
    }

    @Test
    void consumeShouldDelegateToGameplayAPI() {
        // register trait first to avoid validation warnings
        api.getTraits().register("bar_trait", new TraitConfig());

        TraitAPI.consume("bar_trait", new ConsumptionConfig("minecraft:cow", "food"));
        assertFalse(api.getTraits().getConsumptions().isEmpty(), "Consumption should be recorded");
        Consumption c = api.getTraits().getConsumptions().get(0);
        assertEquals("bar_trait", c.getTraitName());
        assertEquals("minecraft:cow", c.getEntity());
        assertEquals("food", c.getBehavior());
    }
}
