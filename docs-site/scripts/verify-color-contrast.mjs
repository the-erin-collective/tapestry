#!/usr/bin/env node

/**
 * Color Contrast Verification Script
 * 
 * Verifies that color combinations in the theme meet WCAG 2.1 Level AA requirements:
 * - Normal text: 4.5:1 contrast ratio
 * - Large text (18pt+ or 14pt+ bold): 3:1 contrast ratio
 * - Interactive elements: 3:1 contrast ratio
 */

import fs from 'fs/promises';
import path from 'path';
import { fileURLToPath } from 'url';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

// WCAG 2.1 Level AA requirements
const WCAG_AA_NORMAL_TEXT = 4.5;
const WCAG_AA_LARGE_TEXT = 3.0;
const WCAG_AA_UI_COMPONENTS = 3.0;

/**
 * Convert hex color to RGB
 */
function hexToRgb(hex) {
  const result = /^#?([a-f\d]{2})([a-f\d]{2})([a-f\d]{2})$/i.exec(hex);
  return result ? {
    r: parseInt(result[1], 16),
    g: parseInt(result[2], 16),
    b: parseInt(result[3], 16)
  } : null;
}

/**
 * Calculate relative luminance
 * https://www.w3.org/TR/WCAG21/#dfn-relative-luminance
 */
function getLuminance(r, g, b) {
  const [rs, gs, bs] = [r, g, b].map(c => {
    c = c / 255;
    return c <= 0.03928 ? c / 12.92 : Math.pow((c + 0.055) / 1.055, 2.4);
  });
  return 0.2126 * rs + 0.7152 * gs + 0.0722 * bs;
}

/**
 * Calculate contrast ratio between two colors
 * https://www.w3.org/TR/WCAG21/#dfn-contrast-ratio
 */
function getContrastRatio(color1, color2) {
  const rgb1 = hexToRgb(color1);
  const rgb2 = hexToRgb(color2);
  
  if (!rgb1 || !rgb2) {
    throw new Error(`Invalid color format: ${color1} or ${color2}`);
  }
  
  const lum1 = getLuminance(rgb1.r, rgb1.g, rgb1.b);
  const lum2 = getLuminance(rgb2.r, rgb2.g, rgb2.b);
  
  const lighter = Math.max(lum1, lum2);
  const darker = Math.min(lum1, lum2);
  
  return (lighter + 0.05) / (darker + 0.05);
}

/**
 * Extract color values from CSS
 */
async function extractColors() {
  const cssPath = path.join(__dirname, '../docs/.vitepress/theme/style.css');
  const cssContent = await fs.readFile(cssPath, 'utf-8');
  
  const colors = {};
  
  // Extract CSS variables from :root
  const rootMatch = cssContent.match(/:root\s*{([^}]+)}/);
  if (rootMatch) {
    const rootContent = rootMatch[1];
    const colorRegex = /--vp-c-([^:]+):\s*(#[0-9a-fA-F]{6})/g;
    let match;
    
    while ((match = colorRegex.exec(rootContent)) !== null) {
      colors[match[1]] = match[2];
    }
  }
  
  return colors;
}

/**
 * Test color combinations
 */
function testColorCombinations(colors) {
  const results = [];
  
  // Define color pairs to test (foreground, background, type)
  const testPairs = [
    // Text colors on backgrounds
    { fg: 'text-1', bg: 'bg', type: 'normal', description: 'Primary text on white background' },
    { fg: 'text-2', bg: 'bg', type: 'normal', description: 'Secondary text on white background' },
    { fg: 'text-3', bg: 'bg', type: 'normal', description: 'Tertiary text on white background' },
    { fg: 'text-1', bg: 'bg-soft', type: 'normal', description: 'Primary text on soft background' },
    { fg: 'text-2', bg: 'bg-soft', type: 'normal', description: 'Secondary text on soft background' },
    { fg: 'text-1', bg: 'bg-mute', type: 'normal', description: 'Primary text on muted background' },
    
    // Brand colors on backgrounds (links and interactive elements)
    { fg: 'brand-1', bg: 'bg', type: 'ui', description: 'Brand color on white background (links)' },
    { fg: 'brand-2', bg: 'bg', type: 'ui', description: 'Brand hover on white background' },
    { fg: 'text-code', bg: 'code-bg', type: 'normal', description: 'Inline code text on code background' },
    
    // Code blocks
    { fg: 'code-color', bg: 'code-bg', type: 'normal', description: 'Code text on code background' },
  ];
  
  for (const pair of testPairs) {
    const fgColor = colors[pair.fg];
    const bgColor = colors[pair.bg];
    
    if (!fgColor || !bgColor) {
      results.push({
        ...pair,
        status: 'skip',
        reason: `Missing color: ${!fgColor ? pair.fg : pair.bg}`,
        ratio: null
      });
      continue;
    }
    
    try {
      const ratio = getContrastRatio(fgColor, bgColor);
      const threshold = pair.type === 'normal' ? WCAG_AA_NORMAL_TEXT : 
                       pair.type === 'large' ? WCAG_AA_LARGE_TEXT : 
                       WCAG_AA_UI_COMPONENTS;
      
      const passes = ratio >= threshold;
      
      results.push({
        ...pair,
        fgColor,
        bgColor,
        ratio: ratio.toFixed(2),
        threshold: threshold.toFixed(1),
        status: passes ? 'pass' : 'fail'
      });
    } catch (error) {
      results.push({
        ...pair,
        status: 'error',
        reason: error.message,
        ratio: null
      });
    }
  }
  
  return results;
}

/**
 * Print results
 */
function printResults(results) {
  console.log('\n=== Color Contrast Verification ===\n');
  console.log('WCAG 2.1 Level AA Requirements:');
  console.log('  - Normal text: 4.5:1');
  console.log('  - Large text: 3:1');
  console.log('  - UI components: 3:1\n');
  
  let passCount = 0;
  let failCount = 0;
  let skipCount = 0;
  
  for (const result of results) {
    const icon = result.status === 'pass' ? '✓' : 
                 result.status === 'fail' ? '✗' : 
                 result.status === 'skip' ? '⊘' : '!';
    
    const color = result.status === 'pass' ? '\x1b[32m' : 
                  result.status === 'fail' ? '\x1b[31m' : 
                  '\x1b[33m';
    const reset = '\x1b[0m';
    
    console.log(`${color}${icon}${reset} ${result.description}`);
    
    if (result.status === 'pass' || result.status === 'fail') {
      console.log(`  ${result.fg} (${result.fgColor}) on ${result.bg} (${result.bgColor})`);
      console.log(`  Ratio: ${result.ratio}:1 (threshold: ${result.threshold}:1)`);
      passCount += result.status === 'pass' ? 1 : 0;
      failCount += result.status === 'fail' ? 1 : 0;
    } else {
      console.log(`  ${result.reason}`);
      skipCount++;
    }
    console.log('');
  }
  
  console.log('=== Summary ===');
  console.log(`Passed: ${passCount}`);
  console.log(`Failed: ${failCount}`);
  console.log(`Skipped: ${skipCount}`);
  console.log(`Total: ${results.length}\n`);
  
  if (failCount > 0) {
    console.log('\x1b[31m⚠ Some color combinations do not meet WCAG 2.1 Level AA requirements.\x1b[0m');
    console.log('Please adjust the colors in docs/.vitepress/theme/style.css\n');
    return false;
  } else {
    console.log('\x1b[32m✓ All color combinations meet WCAG 2.1 Level AA requirements!\x1b[0m\n');
    return true;
  }
}

/**
 * Main function
 */
async function main() {
  try {
    console.log('Extracting colors from theme CSS...');
    const colors = await extractColors();
    console.log(`Found ${Object.keys(colors).length} color definitions\n`);
    
    console.log('Testing color combinations...');
    const results = testColorCombinations(colors);
    
    const success = printResults(results);
    process.exit(success ? 0 : 1);
  } catch (error) {
    console.error('Error:', error.message);
    process.exit(1);
  }
}

main();
