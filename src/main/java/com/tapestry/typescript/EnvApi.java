package com.tapestry.typescript;

import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;
import org.graalvm.polyglot.proxy.ProxyExecutable;
import org.graalvm.polyglot.proxy.ProxyObject;
import org.graalvm.polyglot.Value;

/**
 * Phase 16 side awareness API for TypeScript mods.
 * Provides env.isClient(), env.isServer(), and env.side.
 */
public class EnvApi {
    
    /**
     * Creates the env namespace object for TypeScript.
     */
    public static ProxyObject createNamespace() {
        return new ProxyObject() {
            @Override
            public String[] getMemberKeys() {
                return new String[]{"isClient", "isServer", "side"};
            }
            
            @Override
            public Object getMember(String key) {
                switch (key) {
                    case "isClient":
                        return (ProxyExecutable) arguments -> isClient();
                    case "isServer":
                        return (ProxyExecutable) arguments -> isServer();
                    case "side":
                        return getSide();
                    default:
                        return null;
                }
            }
            
            @Override
            public boolean hasMember(String key) {
                return "isClient".equals(key) || "isServer".equals(key) || "side".equals(key);
            }
            
            @Override
            public void putMember(String key, Value value) {
                // Read-only namespace
                throw new RuntimeException("env namespace is read-only");
            }
        };
    }
    
    /**
     * Checks if current runtime is client-side.
     */
    private static boolean isClient() {
        return FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT;
    }
    
    /**
     * Checks if current runtime is server-side.
     */
    private static boolean isServer() {
        return FabricLoader.getInstance().getEnvironmentType() == EnvType.SERVER;
    }
    
    /**
     * Gets the current side as a string.
     */
    private static String getSide() {
        return isClient() ? "client" : "server";
    }
}
