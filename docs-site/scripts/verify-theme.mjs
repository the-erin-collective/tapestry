#!/usr/bin/env node

/**
 * Verify theme applies consistently across all pages
 * 
 * This script checks that:
 * 1. The built site contains the custom theme CSS
 * 2. All HTML pages reference the theme styles
 * 3. Color palette variables are present
 * 4. Typography settings are applied
 */

import { readFileSync, readdirSync, statSync } from 'fs';
import { join } from 'path';

const distDir = 'docs/.vitepress/dist';
const results = {
  passed: [],
  failed: [],
  warnings: []
};

console.log('🔍 Verifying theme consistency across documentation site...\n');

// Check 1: Verify theme CSS file exists and contains our custom variables
console.log('1. Checking theme CSS file...');
try {
  const cssFiles = findFiles(join(distDir, 'assets'), '.css');
  
  if (cssFiles.length === 0) {
    results.failed.push('No CSS files found in dist/assets');
  } else {
    let foundThemeVars = false;
    
    for (const cssFile of cssFiles) {
      const content = readFileSync(cssFile, 'utf8');
      
      // Check for our custom CSS variables
      if (content.includes('--vp-c-brand-1') && 
          content.includes('#14b8a6') &&
          content.includes('--vp-font-family-base')) {
        foundThemeVars = true;
        results.passed.push(`Theme variables found in ${cssFile}`);
        break;
      }
    }
    
    if (!foundThemeVars) {
      results.failed.push('Custom theme variables not found in any CSS file');
    }
  }
} catch (error) {
  results.failed.push(`Error checking CSS files: ${error.message}`);
}

// Check 2: Verify HTML pages exist and reference styles
console.log('2. Checking HTML pages...');
try {
  const htmlFiles = findFiles(distDir, '.html');
  
  if (htmlFiles.length === 0) {
    results.failed.push('No HTML files found in dist directory');
  } else {
    results.passed.push(`Found ${htmlFiles.length} HTML pages`);
    
    // Check a few key pages
    const keyPages = [
      'index.html',
      'guide/getting-started.html',
      'guide/architecture.html',
      'api/index.html'
    ];
    
    for (const page of keyPages) {
      const pagePath = join(distDir, page);
      try {
        const content = readFileSync(pagePath, 'utf8');
        
        // Check that page has CSS link
        if (content.includes('<link rel="stylesheet"') || content.includes('.css')) {
          results.passed.push(`✓ ${page} has CSS references`);
        } else {
          results.warnings.push(`⚠ ${page} may be missing CSS references`);
        }
        
        // Check for VitePress structure
        if (content.includes('class="VPNav"') || content.includes('class="vp-doc"')) {
          results.passed.push(`✓ ${page} has VitePress structure`);
        } else {
          results.warnings.push(`⚠ ${page} may be missing VitePress structure`);
        }
      } catch (error) {
        results.warnings.push(`⚠ Could not read ${page}: ${error.message}`);
      }
    }
  }
} catch (error) {
  results.failed.push(`Error checking HTML files: ${error.message}`);
}

// Check 3: Verify theme structure
console.log('3. Checking theme structure...');
try {
  // Check that our custom theme files exist in source
  const themeIndex = readFileSync('docs/.vitepress/theme/index.ts', 'utf8');
  const themeStyle = readFileSync('docs/.vitepress/theme/style.css', 'utf8');
  
  if (themeIndex.includes('DefaultTheme') && themeIndex.includes('./style.css')) {
    results.passed.push('✓ Theme index.ts properly extends DefaultTheme');
  } else {
    results.failed.push('Theme index.ts does not properly extend DefaultTheme');
  }
  
  // Check for key theme elements in style.css
  const themeChecks = [
    { name: 'Brand colors', pattern: '--vp-c-brand-1: #14b8a6' },
    { name: 'Text colors', pattern: '--vp-c-text-1: #1f2937' },
    { name: 'Background colors', pattern: '--vp-c-bg: #ffffff' },
    { name: 'Typography', pattern: '--vp-font-family-base' },
    { name: 'Dark mode', pattern: '.dark' },
    { name: 'Responsive design', pattern: '@media' },
    { name: 'Accessibility', pattern: ':focus-visible' }
  ];
  
  for (const check of themeChecks) {
    if (themeStyle.includes(check.pattern)) {
      results.passed.push(`✓ ${check.name} defined in theme`);
    } else {
      results.failed.push(`✗ ${check.name} missing from theme`);
    }
  }
} catch (error) {
  results.failed.push(`Error checking theme structure: ${error.message}`);
}

// Print results
console.log('\n' + '='.repeat(60));
console.log('VERIFICATION RESULTS');
console.log('='.repeat(60) + '\n');

if (results.passed.length > 0) {
  console.log('✅ PASSED CHECKS:');
  results.passed.forEach(msg => console.log(`   ${msg}`));
  console.log('');
}

if (results.warnings.length > 0) {
  console.log('⚠️  WARNINGS:');
  results.warnings.forEach(msg => console.log(`   ${msg}`));
  console.log('');
}

if (results.failed.length > 0) {
  console.log('❌ FAILED CHECKS:');
  results.failed.forEach(msg => console.log(`   ${msg}`));
  console.log('');
}

console.log('='.repeat(60));
console.log(`Summary: ${results.passed.length} passed, ${results.warnings.length} warnings, ${results.failed.length} failed`);
console.log('='.repeat(60) + '\n');

if (results.failed.length > 0) {
  console.log('❌ Theme verification FAILED');
  process.exit(1);
} else if (results.warnings.length > 0) {
  console.log('⚠️  Theme verification completed with warnings');
  process.exit(0);
} else {
  console.log('✅ Theme verification PASSED');
  process.exit(0);
}

// Helper function to recursively find files with a specific extension
function findFiles(dir, extension, fileList = []) {
  try {
    const files = readdirSync(dir);
    
    for (const file of files) {
      const filePath = join(dir, file);
      const stat = statSync(filePath);
      
      if (stat.isDirectory()) {
        findFiles(filePath, extension, fileList);
      } else if (file.endsWith(extension)) {
        fileList.push(filePath);
      }
    }
  } catch (error) {
    // Directory might not exist, that's okay
  }
  
  return fileList;
}
