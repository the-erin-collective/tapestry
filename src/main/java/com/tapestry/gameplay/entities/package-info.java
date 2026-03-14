/**
 * Entity behavior modification APIs.
 * 
 * This package provides TypeScript-accessible APIs for entity interaction hooks
 * and behavior modification through the behavior hook system.
 * 
 * Key Classes:
 * - EntitiesApi: Main TypeScript API exposing entity behavior hooks
 * 
 * API Usage:
 * 
 * Registering feed attempt hooks:
 * ```ts
 * tapestry.entities.onFeedAttempt("minecraft:cat", (ctx) => {
 *   if (ctx.item.id === "mymod:special_fish") {
 *     ctx.accept();
 *   }
 * });
 * ```
 * 
 * The handler receives a FeedAttemptContext with:
 * - item: The item being used to feed the entity
 * - entity: The entity being fed
 * - result: The feed result (success/failure/reject)
 * 
 * This allows mods to extend entity feeding behavior beyond what traits provide,
 * enabling custom logic for specific item-entity combinations.
 */
package com.tapestry.gameplay.entities;
