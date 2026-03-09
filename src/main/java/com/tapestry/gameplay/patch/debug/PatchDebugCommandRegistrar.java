package com.tapestry.gameplay.patch.debug;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.tapestry.gameplay.patch.PatchPlan;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Registers the patch debug command with Minecraft's command system.
 * 
 * <p>This class integrates {@link PatchDebugCommand} with Fabric's command
 * registration system, making the {@code /tapestry patches <target_id>} command
 * available in both development and production environments.</p>
 * 
 * <p>The command allows developers and server administrators to inspect which
 * patches have been applied to specific gameplay targets, helping debug
 * modification conflicts and ordering issues.</p>
 * 
 * @see PatchDebugCommand
 * @see PatchPlan
 */
public class PatchDebugCommandRegistrar {
    private static final Logger LOGGER = LoggerFactory.getLogger(PatchDebugCommandRegistrar.class);
    
    /**
     * Registers the patch debug command with the command dispatcher.
     * 
     * <p>This method should be called during server initialization to register
     * the {@code /tapestry patches <target_id>} command. The command is available
     * to all players with operator permissions (level 2).</p>
     * 
     * <p>Command syntax:</p>
     * <pre>/tapestry patches &lt;target_id&gt;</pre>
     * 
     * <p>Example usage:</p>
     * <pre>/tapestry patches minecraft:villager/farmer</pre>
     * 
     * @param dispatcher The command dispatcher to register with
     * @param registryAccess The command registry access (unused but required by Fabric)
     * @param environment The command environment (unused but required by Fabric)
     * @param patchPlan The compiled patch plan to inspect
     * @throws NullPointerException if dispatcher or patchPlan is null
     */
    public static void register(
            CommandDispatcher<ServerCommandSource> dispatcher,
            CommandRegistryAccess registryAccess,
            CommandManager.RegistrationEnvironment environment,
            PatchPlan patchPlan) {
        
        if (dispatcher == null) {
            throw new NullPointerException("Command dispatcher cannot be null");
        }
        
        if (patchPlan == null) {
            throw new NullPointerException("PatchPlan cannot be null");
        }
        
        LOGGER.info("Registering /tapestry patches command");
        
        // Create the debug command instance
        PatchDebugCommand debugCommand = new PatchDebugCommand(patchPlan);
        
        // Build the command tree using Brigadier primitives directly to avoid
        // triggering Minecraft helper class initializers during testing.
        com.mojang.brigadier.builder.LiteralArgumentBuilder<ServerCommandSource> root =
            com.mojang.brigadier.builder.LiteralArgumentBuilder.<ServerCommandSource>literal("tapestry");
        com.mojang.brigadier.builder.LiteralArgumentBuilder<ServerCommandSource> patches =
            com.mojang.brigadier.builder.LiteralArgumentBuilder.<ServerCommandSource>literal("patches");
        com.mojang.brigadier.builder.RequiredArgumentBuilder<ServerCommandSource, String> arg =
            com.mojang.brigadier.builder.RequiredArgumentBuilder.<ServerCommandSource, String>argument("target_id", StringArgumentType.greedyString())
                .executes(context -> executeDebugCommand(context, debugCommand));
        
        // wire tree together
        patches.then(arg);
        root.then(patches);
        dispatcher.register(root);
        
        LOGGER.info("Successfully registered /tapestry patches command");
    }
    
    /**
     * Executes the debug command and sends the output to the command source.
     * 
     * <p>This method parses the target identifier from the command arguments,
     * executes the debug command, and sends the formatted output back to the
     * player or console that issued the command.</p>
     * 
     * @param context The command context containing arguments and source
     * @param debugCommand The debug command instance to execute
     * @return 1 if the command executed successfully, 0 otherwise
     */
    private static int executeDebugCommand(
            CommandContext<ServerCommandSource> context,
            PatchDebugCommand debugCommand) {
        
        try {
            // Get the target_id argument
            String targetIdString = StringArgumentType.getString(context, "target_id");
            
            // Parse the identifier using Identifier.of() for Minecraft 1.21+
            Identifier targetId = Identifier.of(targetIdString);
            
            // Execute the debug command
            String output = debugCommand.execute(targetId);
            
            // Send the output to the command source
            context.getSource().sendFeedback(
                () -> Text.literal(output),
                false // Don't broadcast to other operators
            );
            
            return 1; // Success
            
        } catch (Exception e) {
            LOGGER.error("Error executing /tapestry patches command", e);
            
            // Send error message to command source
            context.getSource().sendError(
                Text.literal("Error executing command: " + e.getMessage())
            );
            
            return 0; // Failure
        }
    }
}
