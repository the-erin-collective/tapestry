#!/usr/bin/env node

/**
 * Validation script for TypeDoc configuration
 * Validates that entry points exist and output directory is writable
 * Requirements: 11.1, 11.2
 */

import { readFileSync, accessSync, constants, mkdirSync } from 'fs';
import { resolve, dirname } from 'path';
import { fileURLToPath } from 'url';

const __filename = fileURLToPath(import.meta.url);
const __dirname = dirname(__filename);
const projectRoot = resolve(__dirname, '..');

/**
 * Validates TypeDoc configuration
 * @returns {boolean} True if validation passes
 */
function validateTypeDocConfig() {
  let hasErrors = false;

  try {
    // Read typedoc.json
    const configPath = resolve(projectRoot, 'typedoc.json');
    const configContent = readFileSync(configPath, 'utf-8');
    const config = JSON.parse(configContent);

    console.log('Validating TypeDoc configuration...\n');

    // Validate entry points exist and are valid TypeScript files
    if (!config.entryPoints || !Array.isArray(config.entryPoints)) {
      console.error('❌ Error: entryPoints must be an array in typedoc.json');
      hasErrors = true;
    } else {
      console.log(`Checking ${config.entryPoints.length} entry point(s)...`);
      
      for (const entryPoint of config.entryPoints) {
        const absolutePath = resolve(projectRoot, entryPoint);
        
        try {
          // Check if file exists and is readable
          accessSync(absolutePath, constants.R_OK);
          
          // Verify it's a TypeScript file
          if (!entryPoint.endsWith('.ts') && !entryPoint.endsWith('.d.ts')) {
            console.error(`❌ Error: Entry point "${entryPoint}" is not a TypeScript file (.ts or .d.ts)`);
            hasErrors = true;
          } else {
            console.log(`  ✓ ${entryPoint}`);
          }
        } catch (err) {
          console.error(`❌ Error: Entry point "${entryPoint}" does not exist or is not readable`);
          console.error(`   Resolved path: ${absolutePath}`);
          hasErrors = true;
        }
      }
    }

    console.log('');

    // Validate output directory is writable
    if (!config.out) {
      console.error('❌ Error: "out" directory must be specified in typedoc.json');
      hasErrors = true;
    } else {
      const outputPath = resolve(projectRoot, config.out);
      
      try {
        // Try to create the directory if it doesn't exist
        mkdirSync(outputPath, { recursive: true });
        
        // Check if directory is writable
        accessSync(outputPath, constants.W_OK);
        console.log(`✓ Output directory is writable: ${config.out}`);
      } catch (err) {
        console.error(`❌ Error: Output directory "${config.out}" is not writable`);
        console.error(`   Resolved path: ${outputPath}`);
        console.error(`   ${err.message}`);
        hasErrors = true;
      }
    }

    console.log('');

    if (hasErrors) {
      console.error('❌ TypeDoc configuration validation failed\n');
      process.exit(1);
    } else {
      console.log('✅ TypeDoc configuration is valid\n');
      return true;
    }
  } catch (err) {
    console.error('❌ Error reading or parsing typedoc.json:');
    console.error(`   ${err.message}\n`);
    process.exit(1);
  }
}

// Run validation
validateTypeDocConfig();
