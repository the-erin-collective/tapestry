package com.tapestry.extensions.types;

import org.graalvm.polyglot.io.FileSystem;
import org.graalvm.polyglot.io.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.util.*;
import java.util.stream.Stream;

/**
 * GraalVM integration for Phase 14 type resolution.
 * 
 * Provides a custom FileSystem that intercepts @tapestry/* module imports
 * and returns synthetic stubs via TapestryTypeResolver.
 */
public class GraalVMTypeIntegration {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(GraalVMTypeIntegration.class);
    
    private final TapestryTypeResolver typeResolver;
    private final TapestryFileSystem fileSystem;
    
    public GraalVMTypeIntegration(ExtensionTypeRegistry typeRegistry) {
        this.typeResolver = new TapestryTypeResolver(typeRegistry);
        this.fileSystem = new TapestryFileSystem(typeResolver);
    }
    
    /**
     * Gets the type resolver for use by other components.
     */
    public TapestryTypeResolver getTypeResolver() {
        return typeResolver;
    }
    
    /**
     * Gets the custom file system for GraalVM integration.
     */
    public FileSystem getFileSystem() {
        return fileSystem;
    }
    
    /**
     * Sets the current extension context for type resolution.
     * This should be called before executing extension code.
     */
    public void setCurrentExtension(String extensionId) {
        typeResolver.setCurrentExtension(extensionId);
    }
    
    /**
     * Clears the current extension context.
     */
    public void clearCurrentExtension() {
        typeResolver.clearCurrentExtension();
    }
    
    /**
     * Initializes the integration with GraalVM context.
     * This should be called during TS_REGISTER phase.
     */
    public void initialize() {
        LOGGER.info("Initializing GraalVM type integration for Phase 14");
        
        try {
            // The file system will be used when creating the GraalVM context
            LOGGER.info("Phase 14 GraalVM integration initialized successfully");
        } catch (Exception e) {
            LOGGER.error("Failed to initialize Phase 14 GraalVM integration", e);
            throw new RuntimeException("Phase 14 initialization failed", e);
        }
    }
    
    /**
     * Custom FileSystem implementation for @tapestry/* namespace.
     */
    private static class TapestryFileSystem implements FileSystem {
        
        private final TapestryTypeResolver typeResolver;
        
        public TapestryFileSystem(TapestryTypeResolver typeResolver) {
            this.typeResolver = typeResolver;
        }
        
        @Override
        public Path parsePath(URI uri) {
            String path = uri.getPath();
            if (path.startsWith("/@tapestry/")) {
                return new TapestryPath(path.substring(1)); // Remove leading /
            }
            return null; // Let default file system handle
        }
        
        @Override
        public Path parsePath(String path) {
            if (path.startsWith("@tapestry/")) {
                return new TapestryPath(path);
            }
            return null; // Let default file system handle
        }
        
        @Override
        public void close() throws IOException {
            // No resources to clean up
        }
        
        @Override
        public Path getWorkingDirectory() {
            return null;
        }
        
        @Override
        public String getSeparator() {
            return "/";
        }
        
        @Override
        public Iterable<Path> getRootDirectories() {
            return Collections.emptyList();
        }
        
        @Override
        public boolean isOpen() {
            return true;
        }
        
        @Override
        public boolean isReadOnly() {
            return true;
        }
        
        @Override
        public long getLastModifiedTime(Path path) throws IOException {
            if (path instanceof TapestryPath) {
                return 0; // Synthetic files have no modification time
            }
            throw new IOException("Path not found: " + path);
        }
        
        @Override
        public long getSize(Path path) throws IOException {
            if (path instanceof TapestryPath) {
                try {
                    String content = typeResolver.resolveModule(path.toString(), "type");
                    return content.getBytes().length;
                } catch (TapestryTypeResolver.TapestryTypeResolutionException e) {
                    throw new IOException("Failed to resolve module: " + path, e);
                }
            }
            throw new IOException("Path not found: " + path);
        }
        
        @Override
        public boolean exists(Path path) throws IOException {
            if (path instanceof TapestryPath) {
                return path.toString().startsWith("@tapestry/");
            }
            return false;
        }
        
        @Override
        public boolean isRegularFile(Path path) throws IOException {
            return path instanceof TapestryPath;
        }
        
        @Override
        public boolean isDirectory(Path path) throws IOException {
            return false; // No directories in @tapestry namespace
        }
        
        @Override
        public boolean isSymbolicLink(Path path) throws IOException {
            return false;
        }
        
        @Override
        public Stream<Path> newDirectoryStream(Path dir) throws IOException {
            throw new IOException("Directories not supported in @tapestry namespace");
        }
        
        @Override
        public SeekableByteChannel newByteChannel(Path path, Set<? extends OpenOption> options) throws IOException {
            if (path instanceof TapestryPath) {
                try {
                    String content = typeResolver.resolveModule(path.toString(), "type");
                    ByteBuffer buffer = ByteBuffer.wrap(content.getBytes());
                    return new SeekableByteChannel() {
                        private int position = 0;
                        
                        @Override
                        public boolean isOpen() {
                            return true;
                        }
                        
                        @Override
                        public void close() throws IOException {}
                        
                        @Override
                        public long position() throws IOException {
                            return position;
                        }
                        
                        @Override
                        public SeekableByteChannel position(long newPosition) throws IOException {
                            position = (int) newPosition;
                            return this;
                        }
                        
                        @Override
                        public long size() throws IOException {
                            return buffer.limit();
                        }
                        
                        @Override
                        public int read(ByteBuffer dst) throws IOException {
                            if (position >= buffer.limit()) {
                                return -1;
                            }
                            buffer.position(position);
                            int read = Math.min(dst.remaining(), buffer.remaining());
                            for (int i = 0; i < read; i++) {
                                dst.put(buffer.get());
                            }
                            position += read;
                            return read;
                        }
                        
                        @Override
                        public SeekableByteChannel truncate(long size) throws IOException {
                            throw new UnsupportedOperationException("Read-only channel");
                        }
                        
                        @Override
                        public int write(ByteBuffer src) throws IOException {
                            throw new UnsupportedOperationException("Read-only channel");
                        }
                    };
                } catch (TapestryTypeResolver.TapestryTypeResolutionException e) {
                    throw new IOException("Failed to resolve module: " + path, e);
                }
            }
            throw new IOException("Path not found: " + path);
        }
        
        @Override
        public void createDirectory(Path dir, Set<? extends OpenOption> options) throws IOException {
            throw new IOException("Directories not supported in @tapestry namespace");
        }
        
        @Override
        public void delete(Path path) throws IOException {
            throw new IOException("Read-only file system");
        }
        
        @Override
        public void copy(Path source, Path target, Set<CopyOption> options) throws IOException {
            throw new IOException("Read-only file system");
        }
        
        @Override
        public void move(Path source, Path target, Set<CopyOption> options) throws IOException {
            throw new IOException("Read-only file system");
        }
        
        @Override
        public Map<String, Object> readAttributes(Path path, String attributes) throws IOException {
            if (path instanceof TapestryPath) {
                Map<String, Object> attrs = new HashMap<>();
                attrs.put("isRegularFile", true);
                attrs.put("isDirectory", false);
                attrs.put("size", getSize(path));
                attrs.put("lastModifiedTime", getLastModifiedTime(path));
                return attrs;
            }
            throw new IOException("Path not found: " + path);
        }
        
        @Override
        public void setAttribute(Path path, String attribute, Object value) throws IOException {
            throw new IOException("Read-only file system");
        }
    }
    
    /**
     * Custom Path implementation for @tapestry/* modules.
     */
    private static class TapestryPath implements Path {
        
        private final String modulePath;
        
        public TapestryPath(String modulePath) {
            this.modulePath = modulePath;
        }
        
        @Override
        public FileSystem getFileSystem() {
            return null; // Not used in this context
        }
        
        @Override
        public boolean isAbsolute() {
            return true;
        }
        
        @Override
        public Path getRoot() {
            return this;
        }
        
        @Override
        public Path getFileName() {
            return this;
        }
        
        @Override
        public Path getParent() {
            return null;
        }
        
        @Override
        public int getNameCount() {
            return 1;
        }
        
        @Override
        public Path getName(int index) {
            if (index == 0) {
                return this;
            }
            throw new IllegalArgumentException("Invalid index: " + index);
        }
        
        @Override
        public Path subpath(int beginIndex, int endIndex) {
            if (beginIndex == 0 && endIndex == 1) {
                return this;
            }
            throw new IllegalArgumentException("Invalid subpath range");
        }
        
        @Override
        public boolean startsWith(Path other) {
            return toString().equals(other.toString());
        }
        
        @Override
        public boolean endsWith(Path other) {
            return toString().equals(other.toString());
        }
        
        @Override
        public Path normalize() {
            return this;
        }
        
        @Override
        public Path resolve(Path other) {
            return this;
        }
        
        @Override
        public Path resolveSibling(Path other) {
            return this;
        }
        
        @Override
        public Path relativize(Path other) {
            throw new UnsupportedOperationException("relativize not supported for TapestryPath");
        }
        
        @Override
        public URI toUri() {
            return URI.create("tapestry://" + modulePath);
        }
        
        @Override
        public Path toAbsolutePath() {
            return this;
        }
        
        @Override
        public Path toRealPath(LinkOption... options) throws IOException {
            return this;
        }
        
        @Override
        public WatchKey register(WatchService watcher, WatchEvent.Kind<?>... events) throws IOException {
            throw new UnsupportedOperationException("register not supported for TapestryPath");
        }
        
        @Override
        public int compareTo(Path other) {
            return toString().compareTo(other.toString());
        }
        
        @Override
        public String toString() {
            return modulePath;
        }
    }
}
