#!/usr/bin/env node

/**
 * Fix Vue template syntax conflicts in API documentation
 * 
 * VitePress uses Vue and interprets {{ }} as template syntax.
 * This script escapes these patterns in the API docs to prevent build errors.
 */

import { readFileSync, writeFileSync, readdirSync, statSync } from 'fs';
import { join } from 'path';

function getAllMarkdownFiles(dir, fileList = []) {
  const files = readdirSync(dir);
  
  for (const file of files) {
    const filePath = join(dir, file);
    const stat = statSync(filePath);
    
    if (stat.isDirectory()) {
      getAllMarkdownFiles(filePath, fileList);
    } else if (file.endsWith('.md')) {
      fileList.push(filePath);
    }
  }
  
  return fileList;
}

const files = getAllMarkdownFiles('docs/api');

console.log(`Found ${files.length} markdown files`);

let fixedCount = 0;

for (const file of files) {
  let content = readFileSync(file, 'utf8');
  const original = content;
  
  // Check if file has {{ patterns
  if (content.includes('{{')) {
    console.log(`Processing ${file}...`);
    
    // Simple approach: escape ALL {{ and }} regardless of context
    // VitePress will render the HTML entities correctly
    content = content
      .replace(/\{\{/g, '&#123;&#123;')
      .replace(/\}\}/g, '&#125;&#125;');
    
    if (content !== original) {
      writeFileSync(file, content, 'utf8');
      fixedCount++;
      console.log(`✓ Fixed ${file}`);
    } else {
      console.log(`⚠ No changes made to ${file}`);
    }
  }
}

console.log(`\n✅ Fixed ${fixedCount} file(s)`);
