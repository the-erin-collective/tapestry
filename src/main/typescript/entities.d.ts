/**
 * Type definitions for entity behavior hooks in Tapestry.
 *
 * This module augments the global {@link tapestry} object with the
 * `entities` namespace used by Phase 6 behavior hook API.
 *
 * @module tapestry/entities
 */

declare global {
  interface TapestryObject {
    /**
     * Entity behavior hooks allowing mods to intervene in common
     * entity interactions such as feeding.
     */
    entities?: {
      /**
       * Registers a hook that is invoked whenever the specified entity
       * type is fed by a player.
       *
       * The handler receives a {@link FeedAttemptContext} which exposes
       * the item, the entity, and helper methods to accept or reject the
       * attempt.  If the handler does not explicitly accept or reject,
       * the default behavior (based on vanilla tags/traits) is used.
       *
       * @param entityType A resource identifier for the entity (e.g.
       *   "minecraft:cat").
       * @param handler Function called when the feed attempt occurs.
       */
      onFeedAttempt(entityType: string, handler: (ctx: FeedAttemptContext) => void): void;
    };
  }
}

/**
 * Context passed to entity feed attempt handlers.
 */
export interface FeedAttemptContext {
  /**
   * Information about the item being used to feed the entity.
   */
  readonly item: { id: string; count: number };

  /**
   * Information about the entity being fed.
   */
  readonly entity: { type: string; uuid: string };

  /**
   * Accept the feed attempt. This forces the attempt to succeed even if
   * the default rules would reject it.
   */
  accept(): void;

  /**
   * Reject the feed attempt. This forces the attempt to fail regardless
   * of the default rules.
   */
  reject(): void;
}

export { };
