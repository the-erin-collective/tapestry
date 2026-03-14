package com.tapestry.gameplay.entities;

import org.graalvm.polyglot.proxy.ProxyObject;
import org.graalvm.polyglot.proxy.ProxyExecutable;

import java.util.HashMap;
import java.util.Map;

/**
 * Context object passed to feed attempt hooks.
 * 
 * Contains information about the feed attempt including the item,
 * entity, and result, allowing mods to make decisions about whether
 * to accept or reject the feed.
 */
public class FeedAttemptContext {

    private final ItemInfo item;
    private final EntityInfo entity;
    private FeedResult result;

    public FeedAttemptContext(ItemInfo item, EntityInfo entity) {
        this.item = item;
        this.entity = entity;
        this.result = FeedResult.UNKNOWN;
    }

    /**
     * Accept the feed attempt.
     */
    public void accept() {
        this.result = FeedResult.ACCEPTED;
    }

    /**
     * Reject the feed attempt.
     */
    public void reject() {
        this.result = FeedResult.REJECTED;
    }

    public ItemInfo getItem() {
        return item;
    }

    public EntityInfo getEntity() {
        return entity;
    }

    public FeedResult getResult() {
        return result;
    }

    /**
     * Convert to ProxyObject for TypeScript access.
     */
    public ProxyObject toProxy() {
        Map<String, Object> context = new HashMap<>();
        
        context.put("item", item.toProxy());
        context.put("entity", entity.toProxy());
        context.put("accept", (ProxyExecutable) args -> { accept(); return null; });
        context.put("reject", (ProxyExecutable) args -> { reject(); return null; });

        return ProxyObject.fromMap(context);
    }

    /**
     * Result of the feed attempt.
     */
    public enum FeedResult {
        UNKNOWN,
        ACCEPTED,
        REJECTED
    }

    /**
     * Information about an item being used in a feed attempt.
     */
    public static class ItemInfo {
        private final String id;
        private final int count;

        public ItemInfo(String id, int count) {
            this.id = id;
            this.count = count;
        }

        public String getId() {
            return id;
        }

        public int getCount() {
            return count;
        }

        public ProxyObject toProxy() {
            Map<String, Object> item = new HashMap<>();
            item.put("id", id);
            item.put("count", count);
            return ProxyObject.fromMap(item);
        }
    }

    /**
     * Information about an entity being fed.
     */
    public static class EntityInfo {
        private final String type;
        private final String uuid;

        public EntityInfo(String type, String uuid) {
            this.type = type;
            this.uuid = uuid;
        }

        public String getType() {
            return type;
        }

        public String getUuid() {
            return uuid;
        }

        public ProxyObject toProxy() {
            Map<String, Object> entity = new HashMap<>();
            entity.put("type", type);
            entity.put("uuid", uuid);
            return ProxyObject.fromMap(entity);
        }
    }
}
