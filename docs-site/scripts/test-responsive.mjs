#!/usr/bin/env node

/**
 * Test script to verify responsive breakpoints in theme CSS
 * 
 * This script validates that:
 * 1. CSS media queries are properly defined for mobile, tablet, and desktop
 * 2. Breakpoints match the requirements (320px-768px, 768px-1024px, 1024px+)
 * 3. Layout adaptations are present for each breakpoint
 */

import fs from 'fs/promises';
import path from 'path';
import { fileURLToPath } from 'url';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

// ANSI color codes for output
const colors = {
  reset: '\x1b[0m',
  green: '\x1b[32m',
  red: '\x1b[31m',
  yellow: '\x1b[33m',
  blue: '\x1b[34m',
  cyan: '\x1b[36m'
};

function log(message, color = 'reset') {
  console.log(`${colors[color]}${message}${colors.reset}`);
}

async function testResponsiveBreakpoints() {
  log('\n🔍 Testing Responsive Breakpoints\n', 'cyan');
  
  const cssPath = path.join(__dirname, '../docs/.vitepress/theme/style.css');
  
  try {
    const cssContent = await fs.readFile(cssPath, 'utf-8');
    
    const tests = [
      {
        name: 'Mobile breakpoint (max-width: 768px)',
        pattern: /@media\s*\(max-width:\s*768px\)/,
        required: true
      },
      {
        name: 'Small mobile breakpoint (max-width: 480px)',
        pattern: /@media\s*\(max-width:\s*480px\)/,
        required: false
      },
      {
        name: 'Tablet breakpoint (768px - 1024px)',
        pattern: /@media\s*\(min-width:\s*768px\)\s*and\s*\(max-width:\s*1024px\)/,
        required: true
      },
      {
        name: 'Desktop breakpoint (min-width: 1024px)',
        pattern: /@media\s*\(min-width:\s*1024px\)/,
        required: true
      },
      {
        name: 'Large desktop breakpoint (min-width: 1440px)',
        pattern: /@media\s*\(min-width:\s*1440px\)/,
        required: false
      },
      {
        name: 'Landscape mobile orientation',
        pattern: /@media\s*\(max-width:\s*768px\)\s*and\s*\(orientation:\s*landscape\)/,
        required: false
      }
    ];
    
    let passed = 0;
    let failed = 0;
    let warnings = 0;
    
    for (const test of tests) {
      const found = test.pattern.test(cssContent);
      
      if (found) {
        log(`✓ ${test.name}`, 'green');
        passed++;
      } else if (test.required) {
        log(`✗ ${test.name} - MISSING (required)`, 'red');
        failed++;
      } else {
        log(`⚠ ${test.name} - not found (optional)`, 'yellow');
        warnings++;
      }
    }
    
    // Test for specific responsive adaptations
    log('\n🔍 Testing Layout Adaptations\n', 'cyan');
    
    const adaptations = [
      {
        name: 'Mobile font size adjustments',
        pattern: /--vp-font-size-h1:\s*2rem/,
        context: 'mobile'
      },
      {
        name: 'Mobile content padding',
        pattern: /\.vp-doc\s*\{[^}]*padding:\s*24px/,
        context: 'mobile'
      },
      {
        name: 'Feature cards grid (mobile)',
        pattern: /\.vp-features\s*\{[^}]*grid-template-columns:\s*1fr/,
        context: 'mobile'
      },
      {
        name: 'Feature cards grid (tablet)',
        pattern: /\.vp-features\s*\{[^}]*grid-template-columns:\s*repeat\(2,\s*1fr\)/,
        context: 'tablet'
      },
      {
        name: 'Feature cards grid (desktop)',
        pattern: /\.vp-features\s*\{[^}]*grid-template-columns:\s*repeat\(3,\s*1fr\)/,
        context: 'desktop'
      },
      {
        name: 'Hero section mobile adjustments',
        pattern: /\.vp-hero\s*\{[^}]*padding:\s*2rem\s+1rem/,
        context: 'mobile'
      },
      {
        name: 'Sidebar width (desktop)',
        pattern: /\.vp-sidebar\s*\{[^}]*width:\s*320px/,
        context: 'desktop'
      }
    ];
    
    for (const adaptation of adaptations) {
      const found = adaptation.pattern.test(cssContent);
      
      if (found) {
        log(`✓ ${adaptation.name} (${adaptation.context})`, 'green');
        passed++;
      } else {
        log(`⚠ ${adaptation.name} (${adaptation.context}) - pattern not found`, 'yellow');
        warnings++;
      }
    }
    
    // Summary
    log('\n' + '='.repeat(60), 'blue');
    log('Test Summary:', 'cyan');
    log(`  Passed: ${passed}`, 'green');
    if (failed > 0) {
      log(`  Failed: ${failed}`, 'red');
    }
    if (warnings > 0) {
      log(`  Warnings: ${warnings}`, 'yellow');
    }
    log('='.repeat(60) + '\n', 'blue');
    
    if (failed > 0) {
      log('❌ Responsive breakpoint tests FAILED', 'red');
      process.exit(1);
    } else {
      log('✅ Responsive breakpoint tests PASSED', 'green');
      log('\nNote: Manual testing on different screen sizes is still recommended.', 'yellow');
      process.exit(0);
    }
    
  } catch (error) {
    log(`\n❌ Error reading CSS file: ${error.message}`, 'red');
    process.exit(1);
  }
}

// Run tests
testResponsiveBreakpoints();
