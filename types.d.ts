/**
 * Tapestry API Definitions
 * 
 * This file defines the global `tapestry` object available to TypeScript mods
 * running in the GraalVM runtime.
 */

/**
 * Flag indicating if the mod is running on the client side.
 * @category Environment
 */
export const isClient: boolean;

/**
 * Flag indicating if the mod is running on the server side (dedicated or integrated).
 * @category Environment
 */
export const isServer: boolean;

/**
 * Lifecycle and mod definition API.
 * @category Core
 */
export namespace mod {
    /**
     * Definition of a Tapestry mod.
     */
    interface ModDefinition {
        /**
         * The unique identifier for this mod. Must match [a-z][a-z0-9_]{0,63}.
         */
        id: string;

        /**
         * Hook called during the TS_READY phase to initialize the mod.
         * @param api The frozen host API surface.
         */
        onLoad(api: any): void;

        /**
         * Optional hook called when the mod is enabled.
         */
        onEnable?(): void;
    }

    /**
     * Defines and registers a new Tapestry mod.
     * Exactly one call per file is required.
     * 
     * @param definition The mod configuration and hooks.
     * @throws {IllegalStateException} if called multiple times in one file or outside TS_LOAD phase.
     */
    function define(definition: ModDefinition): void;

    /**
     * Registers a listener for a reactive event.
     * @param eventName The name of the event to listen for.
     * @param handler The function to call when the event is emitted.
     */
    function on(eventName: string, handler: (payload: any) => void): void;

    /**
     * Emits a reactive event.
     * @param eventName The name of the event to emit.
     * @param payload Optional data to pass to listeners.
     */
    function emit(eventName: string, payload?: any): void;

    /**
     * Unregisters a listener for a reactive event.
     * @param eventName The name of the event.
     * @param handler The handler function to remove.
     */
    function off(eventName: string, handler: (payload: any) => void): void;

    /**
     * State management API for creating reactive, transactional state.
     * @category State
     */
    namespace state {
        /**
         * Creates a new reactive state instance.
         * @param stateName Unique name for the state.
         * @param initialValue Initial value of the state.
         */
        function createState<T>(stateName: string, initialValue: T): State<T>;
    }

    /**
     * Capability-driven integration API.
     * @category Integration
     */
    namespace capability {
        /**
         * Retrieves a capability provided by another mod or the host.
         * @param name Qualified name of the capability.
         */
        function getCapability<T>(name: string): T;

        /**
         * Provides a capability implementation for other mods to use.
         * @param name Uniquely qualified name of the capability.
         * @param implementation The implementation object or function.
         */
        function provideCapability(name: string, implementation: any): void;

        /**
         * Declares a requirement for a capability.
         * @param name The capability name required.
         */
        function requireCapability(name: string): void;
    }
}

/**
 * A reactive state container.
 * @category State
 */
export interface State<T> {
    /** Gets the current value. */
    get(): T;
    /** Sets a new value and notifies subscribers. */
    set(value: T): void;
    /** Subscribes to changes in this state. */
    subscribe(handler: (value: T) => void): void;
}

/**
 * Environment and side-awareness API.
 * @category Environment
 */
export namespace env {
    /** Returns true if running on client. */
    function isClient(): boolean;
    /** Returns true if running on server. */
    function isServer(): boolean;
    /** The current side ("client" or "server"). */
    const side: "client" | "server";
}

/**
 * World generation and modification API.
 * @category Gameplay
 */
export namespace worldgen {
    /**
     * Registers a handler to resolve block states during world generation.
     * @param handler Function called to deterimine block placement.
     */
    function onResolveBlock(handler: (ctx: any) => void): void;
}

/**
 * Deterministic scheduling API.
 * @category Utilities
 */
export namespace scheduler {
    /** Schedules a callback after a delay. */
    function setTimeout(callback: () => void, delay: number): string;
    /** Schedules a callback to run repeatedly. */
    function setInterval(callback: () => void, interval: number): string;
    /** Cancels a scheduled timeout or interval. */
    function clearInterval(handle: string): void;
    /** Schedules a callback for the next tick. */
    function nextTick(callback: () => void): string;
}

/**
 * Configuration access API.
 * @category Core
 */
export namespace config {
    /** Gets configuration for a specific mod. */
    function get(modId: string): any;
    /** Gets configuration for the current mod. */
    function self(): any;
}

/**
 * Runtime and logging utilities.
 * @category Utilities
 */
export namespace runtime {
    /** Structured logging API. */
    namespace log {
        function info(message: string, context?: any): void;
        function warn(message: string, context?: any): void;
        function error(message: string, context?: any): void;
    }
}

/**
 * Secure JSON-RPC bridge for client-server communication.
 * @category Integration
 */
export namespace rpc {
    /** Defines a server-side API available for RPC calls. */
    function defineServerApi(apiDefinition: Record<string, Function>): void;
    /** Proxy for calling server-side methods. */
    const server: any;
    /** Emits an event to a specific player from the server. */
    function emitTo(player: any, eventName: string, payload: any): void;
    /** Registry for handling events sent from the server to the client. */
    namespace clientEvents {
        function on(eventName: string, handler: (payload: any) => void): void;
    }
    /** Watchable state system for efficient value synchronization. */
    namespace watch {
        function register(player: any, watchKey: string): void;
        function unregister(player: any, watchKey: string): void;
        function emit(watchKey: string, payload: any): void;
    }
}