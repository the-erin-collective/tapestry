#!/usr/bin/env node

/**
 * Link Validation Script for Tapestry Documentation
 * 
 * Validates all links in markdown files:
 * - Internal links (relative paths within docs)
 * - External links (HTTP/HTTPS URLs)
 * - Asset references (images, diagrams)
 * 
 * Requirements: 7.1, 7.2, 7.3
 */

import { readdir, readFile, stat } from 'fs/promises';
import { join, dirname, resolve, relative } from 'path';
import { fileURLToPath } from 'url';

const __filename = fileURLToPath(import.meta.url);
const __dirname = dirname(__filename);
const DOCS_ROOT = resolve(__dirname, '../docs');

// Configuration
const CONFIG = {
  externalTimeout: 10000, // 10 seconds timeout for external links
  maxRetries: 2,          // Retry failed external links twice
  retryDelay: 1000,       // 1 second delay between retries
  userAgent: 'Tapestry-Link-Validator/1.0'
};

// Link patterns
const LINK_PATTERNS = {
  // Markdown links: [text](url)
  markdown: /\[([^\]]+)\]\(([^)]+)\)/g,
  // Reference-style links: [text][ref]
  reference: /\[([^\]]+)\]\[([^\]]+)\]/g,
  // Reference definitions: [ref]: url
  refDefinition: /^\[([^\]]+)\]:\s*(.+)$/gm,
  // Image links: ![alt](url)
  image: /!\[([^\]]*)\]\(([^)]+)\)/g
};

class LinkValidator {
  constructor() {
    this.results = {
      total: 0,
      internal: 0,
      external: 0,
      assets: 0,
      broken: [],
      warnings: []
    };
    this.checkedExternalLinks = new Map(); // Cache external link checks
  }

  /**
   * Extract all links from markdown content
   */
  extractLinks(content, filePath) {
    const links = [];
    const references = new Map();

    // Extract reference definitions first
    let match;
    while ((match = LINK_PATTERNS.refDefinition.exec(content)) !== null) {
      references.set(match[1].toLowerCase(), match[2].trim());
    }

    // Extract markdown links [text](url)
    LINK_PATTERNS.markdown.lastIndex = 0;
    while ((match = LINK_PATTERNS.markdown.exec(content)) !== null) {
      links.push({
        text: match[1],
        url: match[2].trim(),
        type: 'markdown',
        line: this.getLineNumber(content, match.index),
        filePath
      });
    }

    // Extract image links ![alt](url)
    LINK_PATTERNS.image.lastIndex = 0;
    while ((match = LINK_PATTERNS.image.exec(content)) !== null) {
      links.push({
        text: match[1] || 'image',
        url: match[2].trim(),
        type: 'image',
        line: this.getLineNumber(content, match.index),
        filePath
      });
    }

    // Extract reference-style links [text][ref]
    LINK_PATTERNS.reference.lastIndex = 0;
    while ((match = LINK_PATTERNS.reference.exec(content)) !== null) {
      const refKey = match[2].toLowerCase();
      if (references.has(refKey)) {
        links.push({
          text: match[1],
          url: references.get(refKey),
          type: 'reference',
          line: this.getLineNumber(content, match.index),
          filePath
        });
      }
    }

    return links;
  }

  /**
   * Get line number for a character index in content
   */
  getLineNumber(content, index) {
    return content.substring(0, index).split('\n').length;
  }

  /**
   * Classify link type
   */
  classifyLink(url) {
    // Remove anchor and query params for classification
    const cleanUrl = url.split('#')[0].split('?')[0];
    
    if (cleanUrl.startsWith('http://') || cleanUrl.startsWith('https://')) {
      return 'external';
    }
    
    if (cleanUrl.startsWith('/')) {
      // Absolute path within docs (e.g., /guide/getting-started)
      return 'internal-absolute';
    }
    
    if (cleanUrl.match(/\.(png|jpg|jpeg|gif|svg|webp|ico)$/i)) {
      return 'asset';
    }
    
    if (cleanUrl.endsWith('.md') || cleanUrl.includes('/') || cleanUrl === '') {
      return 'internal-relative';
    }
    
    // Could be anchor-only link
    if (url.startsWith('#')) {
      return 'anchor';
    }
    
    return 'unknown';
  }

  /**
   * Validate internal link
   */
  async validateInternalLink(link) {
    const { url, filePath } = link;
    const [path, anchor] = url.split('#');
    
    if (!path) {
      // Anchor-only link - would need content parsing to validate
      return { valid: true, type: 'anchor' };
    }

    try {
      let targetPath;
      
      if (path.startsWith('/')) {
        // Absolute path from docs root
        targetPath = join(DOCS_ROOT, path);
      } else {
        // Relative path from current file
        const currentDir = dirname(filePath);
        targetPath = resolve(currentDir, path);
      }

      // Try multiple variations to handle VitePress clean URLs
      const pathsToTry = [targetPath];
      
      // If path doesn't end with .md, try adding it
      if (!targetPath.endsWith('.md')) {
        pathsToTry.push(targetPath + '.md');
      }
      
      // Try as directory with index.md or README.md
      pathsToTry.push(join(targetPath, 'index.md'));
      pathsToTry.push(join(targetPath, 'README.md'));

      for (const tryPath of pathsToTry) {
        try {
          const stats = await stat(tryPath);
          if (stats.isFile()) {
            return { valid: true, type: 'internal', targetPath: tryPath };
          }
        } catch {
          // Continue to next path
        }
      }

      // None of the paths worked
      return { 
        valid: false, 
        type: 'internal',
        error: `File not found: ${path}`,
        targetPath: path
      };
    } catch (error) {
      return { 
        valid: false, 
        type: 'internal',
        error: `File not found: ${path}`,
        targetPath: path
      };
    }
  }

  /**
   * Validate asset link
   */
  async validateAssetLink(link) {
    const { url, filePath } = link;
    const [path] = url.split('#');

    try {
      let targetPath;
      
      if (path.startsWith('/')) {
        // Absolute path from public directory
        targetPath = join(DOCS_ROOT, 'public', path);
      } else {
        // Relative path from current file
        const currentDir = dirname(filePath);
        targetPath = resolve(currentDir, path);
      }

      // Check if file exists
      await stat(targetPath);
      
      return { valid: true, type: 'asset', targetPath };
    } catch (error) {
      return { 
        valid: false, 
        type: 'asset',
        error: `Asset not found: ${path}`,
        targetPath: path
      };
    }
  }

  /**
   * Validate external link with timeout and retry
   */
  async validateExternalLink(link, retryCount = 0) {
    const { url } = link;

    // Check cache
    if (this.checkedExternalLinks.has(url)) {
      return this.checkedExternalLinks.get(url);
    }

    try {
      const controller = new AbortController();
      const timeoutId = setTimeout(() => controller.abort(), CONFIG.externalTimeout);

      const response = await fetch(url, {
        method: 'HEAD', // Use HEAD to avoid downloading content
        signal: controller.signal,
        headers: {
          'User-Agent': CONFIG.userAgent
        },
        redirect: 'follow'
      });

      clearTimeout(timeoutId);

      const result = {
        valid: response.ok,
        type: 'external',
        statusCode: response.status,
        error: response.ok ? null : `HTTP ${response.status}`
      };

      // Cache result
      this.checkedExternalLinks.set(url, result);
      
      return result;
    } catch (error) {
      // Retry on failure
      if (retryCount < CONFIG.maxRetries) {
        await new Promise(resolve => setTimeout(resolve, CONFIG.retryDelay));
        return this.validateExternalLink(link, retryCount + 1);
      }

      const result = {
        valid: false,
        type: 'external',
        error: error.name === 'AbortError' ? 'Timeout' : error.message
      };

      // Cache result
      this.checkedExternalLinks.set(url, result);
      
      return result;
    }
  }

  /**
   * Validate a single link
   */
  async validateLink(link) {
    const linkType = this.classifyLink(link.url);
    
    this.results.total++;

    switch (linkType) {
      case 'internal-absolute':
      case 'internal-relative':
        this.results.internal++;
        return this.validateInternalLink(link);
      
      case 'asset':
        this.results.assets++;
        return this.validateAssetLink(link);
      
      case 'external':
        this.results.external++;
        return this.validateExternalLink(link);
      
      case 'anchor':
        // Anchor-only links are assumed valid
        return { valid: true, type: 'anchor' };
      
      default:
        return { 
          valid: false, 
          type: 'unknown',
          error: `Unknown link type: ${link.url}`
        };
    }
  }

  /**
   * Find all markdown files recursively
   */
  async findMarkdownFiles(dir) {
    const files = [];
    const entries = await readdir(dir, { withFileTypes: true });

    for (const entry of entries) {
      const fullPath = join(dir, entry.name);
      
      if (entry.isDirectory()) {
        // Skip node_modules and hidden directories
        if (entry.name === 'node_modules' || entry.name.startsWith('.')) {
          continue;
        }
        files.push(...await this.findMarkdownFiles(fullPath));
      } else if (entry.isFile() && entry.name.endsWith('.md')) {
        files.push(fullPath);
      }
    }

    return files;
  }

  /**
   * Validate all links in a file
   */
  async validateFile(filePath) {
    const content = await readFile(filePath, 'utf-8');
    const links = this.extractLinks(content, filePath);
    const results = [];

    for (const link of links) {
      const result = await this.validateLink(link);
      
      if (!result.valid) {
        const relativePath = relative(DOCS_ROOT, filePath);
        this.results.broken.push({
          file: relativePath,
          line: link.line,
          text: link.text,
          url: link.url,
          error: result.error,
          type: result.type
        });
      }

      results.push({ link, result });
    }

    return results;
  }

  /**
   * Run validation on all markdown files
   */
  async validate() {
    console.log('🔍 Tapestry Documentation Link Validator\n');
    console.log(`Scanning: ${DOCS_ROOT}\n`);

    const files = await this.findMarkdownFiles(DOCS_ROOT);
    console.log(`Found ${files.length} markdown files\n`);

    let processedFiles = 0;
    for (const file of files) {
      const relativePath = relative(DOCS_ROOT, file);
      process.stdout.write(`\rValidating: ${relativePath}${' '.repeat(50)}`);
      await this.validateFile(file);
      processedFiles++;
    }

    process.stdout.write('\r' + ' '.repeat(100) + '\r');
    
    this.printResults();
    
    return this.results.broken.length;
  }

  /**
   * Print validation results
   */
  printResults() {
    console.log('📊 Validation Results\n');
    console.log(`Total links checked: ${this.results.total}`);
    console.log(`  - Internal links: ${this.results.internal}`);
    console.log(`  - External links: ${this.results.external}`);
    console.log(`  - Asset references: ${this.results.assets}`);
    console.log();

    if (this.results.broken.length === 0) {
      console.log('✅ All links are valid!\n');
      return;
    }

    // Deduplicate broken links (same file + line + url)
    const uniqueBroken = [];
    const seen = new Set();
    
    for (const broken of this.results.broken) {
      const key = `${broken.file}:${broken.line}:${broken.url}`;
      if (!seen.has(key)) {
        seen.add(key);
        uniqueBroken.push(broken);
      }
    }

    // Count by type
    const brokenByType = {
      internal: uniqueBroken.filter(b => b.type === 'internal').length,
      external: uniqueBroken.filter(b => b.type === 'external').length,
      asset: uniqueBroken.filter(b => b.type === 'asset').length
    };

    console.log(`❌ Found ${uniqueBroken.length} broken link(s):`);
    if (brokenByType.internal > 0) console.log(`   - ${brokenByType.internal} internal link(s)`);
    if (brokenByType.external > 0) console.log(`   - ${brokenByType.external} external link(s)`);
    if (brokenByType.asset > 0) console.log(`   - ${brokenByType.asset} asset(s)`);
    console.log();

    // Group by file
    const byFile = new Map();
    for (const broken of uniqueBroken) {
      if (!byFile.has(broken.file)) {
        byFile.set(broken.file, []);
      }
      byFile.get(broken.file).push(broken);
    }

    for (const [file, links] of byFile) {
      console.log(`📄 ${file}`);
      for (const link of links) {
        console.log(`   Line ${link.line}: [${link.text}](${link.url})`);
        console.log(`   └─ ${link.error}`);
      }
      console.log();
    }
  }
}

// Run validator
const validator = new LinkValidator();
const exitCode = await validator.validate();
process.exit(exitCode > 0 ? 1 : 0);
