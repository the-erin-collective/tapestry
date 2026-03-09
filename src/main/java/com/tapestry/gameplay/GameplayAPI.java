package com.tapestry.gameplay;

import com.mojang.brigadier.CommandDispatcher;
import com.tapestry.gameplay.brewing.BrewingRecipe;
import com.tapestry.gameplay.composition.CompositionOrchestrator;
import com.tapestry.gameplay.items.ItemRegistration;
import com.tapestry.gameplay.loot.LootModifier;
import com.tapestry.gameplay.patch.PatchPlan;
import com.tapestry.gameplay.patch.debug.PatchDebugCommandRegistrar;
import com.tapestry.gameplay.traits.TraitSystem;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Main facade for Tapestry Gameplay API.
 * 
 * Provides access to trait system, item registration, brewing recipes, and loot modification.
 * This is the primary entry point for gameplay API functionality.
 */
public class GameplayAPI {
    private static final Logger LOGGER = LoggerFactory.getLogger(GameplayAPI.class);
    
    private final TraitSystem traits;
    private final ItemRegistration items;
    private final BrewingRecipe brewing;
    private final LootModifier loot;
    private final CompositionOrchestrator compositionOrchestrator;
    
    /**
     * Creates a new gameplay API instance.
     */
    public GameplayAPI() {
        this.traits = new TraitSystem();
        this.items = new ItemRegistration();
        this.items.setTraitSystem(this.traits); // Wire trait system for validation
        this.brewing = new BrewingRecipe();
        this.loot = new LootModifier();
        this.compositionOrchestrator = new CompositionOrchestrator(traits, items);
        
        LOGGER.info("GameplayAPI initialized");
    }
    
    /**
     * Registers built-in traits provided by Tapestry framework.
     * This should be called during TS_REGISTER phase.
     */
    public void registerBuiltInTraits() {
        LOGGER.info("Registering built-in traits");
        
        // Register fish_food trait
        traits.register("fish_food", new com.tapestry.gameplay.traits.TraitConfig("tapestry:fish_items"));
        
        // Register milk_like trait
        traits.register("milk_like", new com.tapestry.gameplay.traits.TraitConfig("tapestry:milk_items"));
        
        // Register egg_like trait
        traits.register("egg_like", new com.tapestry.gameplay.traits.TraitConfig("tapestry:egg_items"));
        
        // Register honey_like trait
        traits.register("honey_like", new com.tapestry.gameplay.traits.TraitConfig("tapestry:honey_items"));
        
        // Register plant_fiber trait
        traits.register("plant_fiber", new com.tapestry.gameplay.traits.TraitConfig("tapestry:plant_fibers"));
        
        LOGGER.info("Built-in traits registered successfully");
    }
    
    /**
     * Gets the trait system.
     * 
     * @return the trait system
     */
    public TraitSystem getTraits() {
        return traits;
    }
    
    /**
     * Gets the item registration API.
     * 
     * @return the item registration
     */
    public ItemRegistration getItems() {
        return items;
    }
    
    /**
     * Gets the brewing recipe API.
     * 
     * @return the brewing recipe API
     */
    public BrewingRecipe getBrewing() {
        return brewing;
    }
    
    /**
     * Gets the loot modifier API.
     * 
     * @return the loot modifier
     */
    public LootModifier getLoot() {
        return loot;
    }
    
    /**
     * Executes the COMPOSITION phase.
     * 
     * This method should be called after all traits and items are registered
     * during the TS_REGISTER phase. It performs:
     * 1. Trait-to-item mapping resolution
     * 2. Behavior tag generation
     * 3. Registry freezing
     * 
     * @throws IOException if tag generation fails
     * @throws IllegalStateException if trait resolution fails
     */
    public void executeComposition() throws IOException {
        compositionOrchestrator.executeComposition();
    }
    
    /**
     * Gets the composition orchestrator.
     * 
     * @return the composition orchestrator
     */
    public CompositionOrchestrator getCompositionOrchestrator() {
        return compositionOrchestrator;
    }
    
    /**
     * Executes the INITIALIZATION phase.
     * 
     * This method should be called after the COMPOSITION phase completes.
     * It performs:
     * 1. Brewing recipe registration with Fabric
     * 2. Other initialization tasks
     * 
     * @throws IllegalArgumentException if Fabric registration fails
     */
    public void executeInitialization() {
        LOGGER.info("Executing INITIALIZATION phase");
        
        // Register brewing recipes with Fabric
        com.tapestry.gameplay.brewing.FabricBrewingRegistry.registerRecipes(
            brewing.getAllRecipes()
        );
        
        LOGGER.info("INITIALIZATION phase completed successfully");
    }
    
    /**
     * Registers debug commands with the command dispatcher.
     * 
     * <p>This method should be called during server initialization to register
     * the {@code /tapestry patches <target_id>} command. The command is available
     * in both development and production environments.</p>
     * 
     * @param dispatcher The command dispatcher to register with
     * @param registryAccess The command registry access
     * @param environment The command environment
     * @param patchPlan The compiled patch plan to inspect
     * @throws NullPointerException if any parameter is null
     */
    public void registerCommands(
            CommandDispatcher<ServerCommandSource> dispatcher,
            CommandRegistryAccess registryAccess,
            CommandManager.RegistrationEnvironment environment,
            PatchPlan patchPlan) {
        
        LOGGER.info("Registering gameplay debug commands");
        
        // Register the patch debug command
        PatchDebugCommandRegistrar.register(dispatcher, registryAccess, environment, patchPlan);
        
        LOGGER.info("Gameplay debug commands registered successfully");
    }
}
