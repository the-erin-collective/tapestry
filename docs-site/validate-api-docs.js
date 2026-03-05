/**
 * Validation script for API documentation completeness
 * 
 * This script validates:
 * 1. All exported symbols have documentation
 * 2. Cross-reference links are generated correctly
 * 3. Categories are applied when @category tags present
 */

import fs from 'fs';
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
};

const results = {
  passed: [],
  failed: [],
  warnings: [],
};

function log(message, color = 'reset') {
  console.log(`${colors[color]}${message}${colors.reset}`);
}

function checkFileExists(filePath) {
  return fs.existsSync(filePath);
}

function readFile(filePath) {
  return fs.readFileSync(filePath, 'utf-8');
}

/**
 * Validation 1: Check that all exported symbols have documentation
 */
function validateExportedSymbols() {
  log('\n=== Validation 1: Exported Symbols Documentation ===', 'blue');
  
  const sourceFile = path.join(__dirname, '../src/main/resources/mikel/mikel-0.32.0/index.d.ts');
  const apiDir = path.join(__dirname, 'docs/api');
  
  if (!checkFileExists(sourceFile)) {
    results.failed.push('Source file not found: ' + sourceFile);
    log('✗ Source file not found', 'red');
    return;
  }
  
  const sourceContent = readFile(sourceFile);
  
  // Extract exported type aliases
  const exportedTypes = [];
  const typeRegex = /export type (\w+)/g;
  let match;
  while ((match = typeRegex.exec(sourceContent)) !== null) {
    exportedTypes.push(match[1]);
  }
  
  // Extract default export
  const defaultExportRegex = /declare const (\w+):/;
  const defaultMatch = sourceContent.match(defaultExportRegex);
  if (defaultMatch) {
    exportedTypes.push(defaultMatch[1]);
  }
  
  log(`Found ${exportedTypes.length} exported symbols in source`, 'blue');
  
  // Check each exported symbol has documentation
  const missingDocs = [];
  for (const typeName of exportedTypes) {
    const docPath = path.join(apiDir, 'type-aliases', `${typeName}.md`);
    const varPath = path.join(apiDir, 'variables', `${typeName === 'mikel' ? 'default' : typeName}.md`);
    
    if (!checkFileExists(docPath) && !checkFileExists(varPath)) {
      missingDocs.push(typeName);
    } else {
      results.passed.push(`Documentation exists for: ${typeName}`);
      log(`✓ ${typeName}`, 'green');
    }
  }
  
  if (missingDocs.length > 0) {
    results.failed.push(`Missing documentation for: ${missingDocs.join(', ')}`);
    log(`✗ Missing documentation for: ${missingDocs.join(', ')}`, 'red');
  } else {
    log('✓ All exported symbols have documentation', 'green');
  }
}

/**
 * Validation 2: Check cross-reference links are generated correctly
 */
function validateCrossReferences() {
  log('\n=== Validation 2: Cross-Reference Links ===', 'blue');
  
  const apiDir = path.join(__dirname, 'docs/api');
  const typeAliasesDir = path.join(apiDir, 'type-aliases');
  const variablesDir = path.join(apiDir, 'variables');
  
  const allFiles = [
    ...fs.readdirSync(typeAliasesDir).map(f => path.join(typeAliasesDir, f)),
    ...fs.readdirSync(variablesDir).map(f => path.join(variablesDir, f)),
  ].filter(f => f.endsWith('.md'));
  
  const brokenLinks = [];
  const validLinks = [];
  
  for (const filePath of allFiles) {
    const content = readFile(filePath);
    const fileName = path.basename(filePath);
    
    // Extract all markdown links [text](url)
    const linkRegex = /\[([^\]]+)\]\(([^)]+)\)/g;
    let match;
    
    while ((match = linkRegex.exec(content)) !== null) {
      const linkText = match[1];
      const linkUrl = match[2];
      
      // Skip external links
      if (linkUrl.startsWith('http')) continue;
      
      // Skip anchor-only links
      if (linkUrl.startsWith('#')) continue;
      
      // Resolve relative link
      const linkPath = path.resolve(path.dirname(filePath), linkUrl.split('#')[0]);
      
      if (!checkFileExists(linkPath)) {
        brokenLinks.push({
          file: fileName,
          link: linkUrl,
          text: linkText,
        });
      } else {
        validLinks.push({
          file: fileName,
          link: linkUrl,
        });
      }
    }
  }
  
  if (brokenLinks.length > 0) {
    results.failed.push(`Found ${brokenLinks.length} broken cross-reference links`);
    log(`✗ Found ${brokenLinks.length} broken links:`, 'red');
    brokenLinks.forEach(({ file, link, text }) => {
      log(`  ${file}: [${text}](${link})`, 'red');
    });
  } else {
    results.passed.push(`All ${validLinks.length} cross-reference links are valid`);
    log(`✓ All ${validLinks.length} cross-reference links are valid`, 'green');
  }
}

/**
 * Validation 3: Check categories are applied when @category tags present
 */
function validateCategories() {
  log('\n=== Validation 3: Category Organization ===', 'blue');
  
  const sourceFile = path.join(__dirname, '../src/main/resources/mikel/mikel-0.32.0/index.d.ts');
  const globalsFile = path.join(__dirname, 'docs/api/globals.md');
  
  if (!checkFileExists(sourceFile) || !checkFileExists(globalsFile)) {
    results.failed.push('Required files not found for category validation');
    log('✗ Required files not found', 'red');
    return;
  }
  
  const sourceContent = readFile(sourceFile);
  const globalsContent = readFile(globalsFile);
  
  // Extract categories from source
  const categoryMap = new Map();
  const categoryRegex = /@category\s+(\w+)/g;
  const typeNameRegex = /export type (\w+)/g;
  
  let currentType = null;
  const lines = sourceContent.split('\n');
  
  for (const line of lines) {
    const typeMatch = line.match(/export type (\w+)/);
    if (typeMatch) {
      currentType = typeMatch[1];
    }
    
    const categoryMatch = line.match(/@category\s+(\w+)/);
    if (categoryMatch && currentType) {
      categoryMap.set(currentType, categoryMatch[1]);
    }
  }
  
  // Also check for default export category
  const defaultCategoryMatch = sourceContent.match(/declare const \w+:[\s\S]*?@category\s+(\w+)/);
  if (defaultCategoryMatch) {
    categoryMap.set('default', defaultCategoryMatch[1]);
  }
  
  log(`Found ${categoryMap.size} types with @category tags`, 'blue');
  
  // Check if categories are reflected in globals.md
  const categoriesInGlobals = new Set();
  const categoryHeaderRegex = /^### (.+)$/gm;
  let match;
  
  while ((match = categoryHeaderRegex.exec(globalsContent)) !== null) {
    categoriesInGlobals.add(match[1]);
  }
  
  log(`Found ${categoriesInGlobals.size} category sections in globals.md`, 'blue');
  
  // Validate each type appears under correct category
  const missingCategories = [];
  const correctCategories = [];
  
  for (const [typeName, category] of categoryMap.entries()) {
    if (!categoriesInGlobals.has(category)) {
      missingCategories.push(`${typeName} (@category ${category})`);
    } else {
      // Check if type is listed under that category
      const categorySection = globalsContent.split(`### ${category}`)[1];
      if (categorySection) {
        const typeLink = typeName === 'mikel' ? 'default' : typeName;
        if (categorySection.includes(typeLink)) {
          correctCategories.push(`${typeName} → ${category}`);
          log(`✓ ${typeName} correctly categorized as ${category}`, 'green');
        } else {
          results.warnings.push(`${typeName} has @category ${category} but not listed under that section`);
          log(`⚠ ${typeName} has @category ${category} but not listed under that section`, 'yellow');
        }
      }
    }
  }
  
  if (missingCategories.length > 0) {
    results.failed.push(`Categories not reflected in globals.md: ${missingCategories.join(', ')}`);
    log(`✗ Missing category sections: ${missingCategories.join(', ')}`, 'red');
  }
  
  if (correctCategories.length > 0) {
    results.passed.push(`${correctCategories.length} types correctly categorized`);
  }
  
  // Check that all expected categories exist
  const expectedCategories = ['Core', 'Helpers', 'Functions', 'Partials', 'Utilities'];
  const foundCategories = Array.from(categoriesInGlobals);
  
  log('\nExpected categories:', 'blue');
  expectedCategories.forEach(cat => {
    if (foundCategories.includes(cat)) {
      log(`✓ ${cat}`, 'green');
    } else {
      log(`⚠ ${cat} (not found)`, 'yellow');
      results.warnings.push(`Expected category "${cat}" not found in globals.md`);
    }
  });
}

/**
 * Additional validation: Check that README.md exists
 */
function validateReadme() {
  log('\n=== Additional: README.md Check ===', 'blue');
  
  const readmePath = path.join(__dirname, 'docs/api/README.md');
  
  if (checkFileExists(readmePath)) {
    const content = readFile(readmePath);
    if (content.length > 100) {
      results.passed.push('API README.md exists with content');
      log('✓ API README.md exists with substantial content', 'green');
    } else {
      results.warnings.push('API README.md exists but appears empty');
      log('⚠ API README.md exists but appears empty', 'yellow');
    }
  } else {
    results.failed.push('API README.md not found');
    log('✗ API README.md not found', 'red');
  }
}

/**
 * Run all validations
 */
function runValidations() {
  log('╔════════════════════════════════════════════════════════╗', 'blue');
  log('║   API Documentation Completeness Validation Report    ║', 'blue');
  log('╚════════════════════════════════════════════════════════╝', 'blue');
  
  validateExportedSymbols();
  validateCrossReferences();
  validateCategories();
  validateReadme();
  
  // Print summary
  log('\n╔════════════════════════════════════════════════════════╗', 'blue');
  log('║                    Summary Report                      ║', 'blue');
  log('╚════════════════════════════════════════════════════════╝', 'blue');
  
  log(`\n✓ Passed: ${results.passed.length}`, 'green');
  log(`✗ Failed: ${results.failed.length}`, results.failed.length > 0 ? 'red' : 'green');
  log(`⚠ Warnings: ${results.warnings.length}`, results.warnings.length > 0 ? 'yellow' : 'green');
  
  if (results.failed.length > 0) {
    log('\nFailed checks:', 'red');
    results.failed.forEach(msg => log(`  • ${msg}`, 'red'));
  }
  
  if (results.warnings.length > 0) {
    log('\nWarnings:', 'yellow');
    results.warnings.forEach(msg => log(`  • ${msg}`, 'yellow'));
  }
  
  log('\n' + '='.repeat(60), 'blue');
  
  // Exit with appropriate code
  if (results.failed.length > 0) {
    log('\n❌ Validation FAILED', 'red');
    process.exit(1);
  } else if (results.warnings.length > 0) {
    log('\n⚠️  Validation PASSED with warnings', 'yellow');
    process.exit(0);
  } else {
    log('\n✅ Validation PASSED', 'green');
    process.exit(0);
  }
}

// Run the validations
runValidations();
