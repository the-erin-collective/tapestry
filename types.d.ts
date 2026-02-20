// TypeScript type definitions for Tapestry Platform APIs
// These definitions cover the APIs used by TWILA and other TypeScript mods

// ============================================================================
// RAYCASTING API
// ============================================================================

export interface BlockPos {
  x: number;
  y: number;
  z: number;
}

export interface RaycastOptions {
  maxDistance: number;
  includeFluids?: boolean;
}

export interface BlockHitResult {
  hit: true;
  blockPos: BlockPos;
  blockId: string;     // e.g., "minecraft:stone"
  blockName: string;   // Localized display name
  side: string;        // e.g., "up", "north", "south", "east", "west", "down"
}

export interface BlockMissResult {
  hit: false;
}

export type RaycastResult = BlockHitResult | BlockMissResult;

// ============================================================================
// OVERLAY SYSTEM API
// ============================================================================

export type OverlayAnchor = 
  | "TOP_LEFT"
  | "TOP_CENTER"
  | "TOP_RIGHT"
  | "CENTER"
  | "BOTTOM_LEFT"
  | "BOTTOM_CENTER"
  | "BOTTOM_RIGHT";

export interface UINode {
  type: string;
  props?: Record<string, unknown>;
  children?: (UINode | string)[];
}

export interface OverlayDefinition {
  id: string;
  anchor: OverlayAnchor;
  zIndex?: number;
  visible?: boolean;
  render: () => UINode | UINode[];
}

export interface OverlayAPI {
  register(overlay: OverlayDefinition): void;
  setVisible(modId: string, overlayId: string, visible: boolean): void;
  getCount(): number;
  template(template: string, data?: Record<string, unknown>): UINode | UINode[];
  add(fragment: UINode | UINode[]): void;
}

// ============================================================================
// SCHEDULER API
// ============================================================================

export interface SchedulerContext {
  modId: string;
  tick: number;
  handle: string;
}

export type SchedulerCallback = (context: SchedulerContext) => void;

export interface SchedulerAPI {
  setTimeout(callback: SchedulerCallback, delay: number): string;
  setInterval(callback: SchedulerCallback, interval: number): string;
  clearInterval(handle: string): void;
  nextTick(callback: SchedulerCallback): string;
}

// ============================================================================
// PLAYER API
// ============================================================================

export interface PlayerSnapshot {
  uuid: string;
  name: string;
}

export interface Position {
  x: number;
  y: number;
  z: number;
}

export interface LookDirection {
  x: number;
  y: number;
  z: number;
}

export interface LookSnapshot {
  yaw: number;
  pitch: number;
  dir: LookDirection;
}

export interface PlayersAPI {
  list(): PlayerSnapshot[];
  get(uuid: string): PlayerSnapshot | null;
  findByName(name: string): PlayerSnapshot | null;
  sendChat(uuid: string, message: string): void;
  sendActionBar(uuid: string, message: string): void;
  sendTitle(uuid: string, options: {
    title: string;
    subtitle?: string;
    fadeIn?: number;
    stay?: number;
    fadeOut?: number;
  }): void;
  getPosition(uuid: string): Position | null;
  getLook(uuid: string): LookSnapshot | null;
  getGameMode(uuid: string): string;
  raycastBlock(uuid: string, options: RaycastOptions): RaycastResult;
}

// ============================================================================
// MOD DEFINITION API
// ============================================================================

export interface TapestryObject {
  mod: {
    define(definition: ModDefinition): void;
    on?: (event: string, callback: (context: unknown) => void) => void;
    emit?: (event: string, data: unknown) => void;
    off?: (event: string, callback?: (context: unknown) => void) => void;
    state?: {
      get<T>(key: string): T | null;
      set<T>(key: string, value: T): void;
      delete(key: string): void;
    };
  };
  client?: {
    overlay: OverlayAPI;
    mod?: {
      on?: (event: string, callback: (context: unknown) => void) => void;
      emit?: (event: string, data: unknown) => void;
      off?: (event: string, callback?: (context: unknown) => void) => void;
    };
  };
  scheduler: SchedulerAPI;
  players: PlayersAPI;
}

export interface ModDefinition {
  id: string;
  onLoad?: (tapestry: TapestryObject) => void;
  onEnable?: (tapestry: TapestryObject) => void;
}

// ============================================================================
// CONSOLE API (Global)
// ============================================================================

declare global {
  const console: {
    log: (...args: unknown[]) => void;
    warn: (...args: unknown[]) => void;
    error: (...args: unknown[]) => void;
  };

  const tapestry: TapestryObject;
}

export {};