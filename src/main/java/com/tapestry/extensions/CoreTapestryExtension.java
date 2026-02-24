package com.tapestry.extensions;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Core Tapestry extension provider.
 * This is the built-in extension that owns core capabilities.
 * It must always be present and never fails silently.
 */
public final class CoreTapestryExtension implements TapestryExtensionProvider {

    static final TapestryExtensionDescriptor DESCRIPTOR =
        new TapestryExtensionDescriptor(
            "tapestry",                    // id
            "Tapestry Core",               // displayName
            "1.0.0",                       // version
            "1.0.0",                       // minTapestry
            List.of(                       // capabilities
                new CapabilityDecl("players.list", CapabilityType.API, false, Map.of(), "tapestry.players.list"),
                new CapabilityDecl("players.get", CapabilityType.API, false, Map.of(), "tapestry.players.get"),
                new CapabilityDecl("players.findByName", CapabilityType.API, false, Map.of(), "tapestry.players.findByName"),
                new CapabilityDecl("players.sendChat", CapabilityType.API, false, Map.of(), "tapestry.players.sendChat"),
                new CapabilityDecl("players.sendActionBar", CapabilityType.API, false, Map.of(), "tapestry.players.sendActionBar"),
                new CapabilityDecl("players.sendTitle", CapabilityType.API, false, Map.of(), "tapestry.players.sendTitle"),
                new CapabilityDecl("players.getPosition", CapabilityType.API, false, Map.of(), "tapestry.players.getPosition"),
                new CapabilityDecl("players.getLook", CapabilityType.API, false, Map.of(), "tapestry.players.getLook"),
                new CapabilityDecl("players.getGameMode", CapabilityType.API, false, Map.of(), "tapestry.players.getGameMode"),
                new CapabilityDecl("players.raycastBlock", CapabilityType.API, false, Map.of(), "tapestry.players.raycastBlock")
            ),
            List.of(),                     // requires
            List.of(),                     // requiresCapabilities
            Optional.empty(),              // typeExportEntry
            List.of()                      // typeImports
        );

    @Override
    public TapestryExtensionDescriptor describe() {
        return DESCRIPTOR;
    }

    @Override
    public TapestryExtension create() {
        return new CoreTapestryExtensionInstance();
    }
}
