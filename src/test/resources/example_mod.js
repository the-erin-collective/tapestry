// Example TypeScript mod for Phase 2 testing
// This demonstrates the tapestry.mod.define() API and hook registration

tapestry.mod.define({
  id: "example_mod",

  onLoad(api) {
    console.log("Example mod loading - API available:", api);
    
    // Register a worldgen hook during TS_READY phase
    api.worldgen.onResolveBlock((ctx, vanillaBlock) => {
      console.log("Hook called for block:", vanillaBlock, "from mod:", ctx.modId);
      
      if (vanillaBlock === "minecraft:stone") {
        return "minecraft:diamond_block";
      }
      
      return null; // No override for other blocks
    });
  }
});
