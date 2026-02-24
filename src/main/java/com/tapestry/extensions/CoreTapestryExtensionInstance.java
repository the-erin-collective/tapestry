package com.tapestry.extensions;

import com.tapestry.typescript.PlayersApi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Core Tapestry extension instance.
 * Implements the core extension with the actual capability registration.
 */
public final class CoreTapestryExtensionInstance implements TapestryExtension {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(CoreTapestryExtensionInstance.class);

    @Override
    public TapestryExtensionDescriptor describe() {
        return CoreTapestryExtension.DESCRIPTOR;
    }

    @Override
    public void register(TapestryExtensionContext ctx) throws ExtensionRegistrationException {
        LOGGER.info("Registering core Tapestry extension capabilities...");
        
        try {
            // Create the PlayersApi instance (will be updated with PlayerService later)
            var playersApi = new PlayersApi(null);
            var playersNs = playersApi.createNamespace();
            
            // Register player identity & discovery APIs
            ctx.api().addFunction("tapestry", "players.list", (org.graalvm.polyglot.proxy.ProxyExecutable) playersNs.getMember("list"));
            ctx.api().addFunction("tapestry", "players.get", (org.graalvm.polyglot.proxy.ProxyExecutable) playersNs.getMember("get"));
            ctx.api().addFunction("tapestry", "players.findByName", (org.graalvm.polyglot.proxy.ProxyExecutable) playersNs.getMember("findByName"));
            
            // Register player messaging APIs  
            ctx.api().addFunction("tapestry", "players.sendChat", (org.graalvm.polyglot.proxy.ProxyExecutable) playersNs.getMember("sendChat"));
            ctx.api().addFunction("tapestry", "players.sendActionBar", (org.graalvm.polyglot.proxy.ProxyExecutable) playersNs.getMember("sendActionBar"));
            ctx.api().addFunction("tapestry", "players.sendTitle", (org.graalvm.polyglot.proxy.ProxyExecutable) playersNs.getMember("sendTitle"));
            
            // Register player query APIs
            ctx.api().addFunction("tapestry", "players.getPosition", (org.graalvm.polyglot.proxy.ProxyExecutable) playersNs.getMember("getPosition"));
            ctx.api().addFunction("tapestry", "players.getLook", (org.graalvm.polyglot.proxy.ProxyExecutable) playersNs.getMember("getLook"));
            ctx.api().addFunction("tapestry", "players.getGameMode", (org.graalvm.polyglot.proxy.ProxyExecutable) playersNs.getMember("getGameMode"));
            
            // Register raycasting API
            ctx.api().addFunction("tapestry", "players.raycastBlock", (org.graalvm.polyglot.proxy.ProxyExecutable) playersNs.getMember("raycastBlock"));
            
            LOGGER.info("Core Tapestry extension capabilities registered successfully");
            
        } catch (Exception e) {
            throw new ExtensionRegistrationException("Failed to register core capabilities", e);
        }
    }
}
