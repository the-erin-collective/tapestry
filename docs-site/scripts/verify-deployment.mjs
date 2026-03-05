#!/usr/bin/env node

/**
 * Deployment Verification Script
 * 
 * Verifies that the deployed documentation site is accessible and all critical
 * pages and assets are loading correctly. This script checks:
 * - Root page (landing page)
 * - Guide pages (getting started, core concepts, architecture, lifecycle, extensions)
 * - API reference page
 * - Static assets (logo, favicon, diagrams)
 * 
 * Usage:
 *   node scripts/verify-deployment.mjs <base-url>
 * 
 * Example:
 *   node scripts/verify-deployment.mjs https://your-org.github.io/tapestry
 */

import { fileURLToPath } from 'url';

const __filename = fileURLToPath(import.meta.url);

/**
 * Fetch a URL and check if it returns a successful response
 */
async function checkUrl(url, description) {
  try {
    const response = await fetch(url, {
      method: 'HEAD',
      headers: {
        'User-Agent': 'Tapestry-Docs-Verification/1.0'
      }
    });
    
    const success = response.ok;
    const status = response.status;
    
    return {
      url,
      description,
      status,
      success,
      error: null
    };
  } catch (error) {
    return {
      url,
      description,
      status: null,
      success: false,
      error: error.message
    };
  }
}

/**
 * Define all URLs to check
 */
function getUrlsToCheck(baseUrl) {
  // Remove trailing slash from base URL
  const base = baseUrl.replace(/\/$/, '');
  
  return [
    // Root page
    { url: `${base}/`, description: 'Root page (landing page)' },
    
    // Guide pages
    { url: `${base}/guide/getting-started`, description: 'Getting Started guide' },
    { url: `${base}/guide/core-concepts`, description: 'Core Concepts guide' },
    { url: `${base}/guide/architecture`, description: 'Architecture guide' },
    { url: `${base}/guide/lifecycle-phases`, description: 'Lifecycle Phases guide' },
    { url: `${base}/guide/extensions`, description: 'Extensions guide' },
    
    // API reference
    { url: `${base}/api/`, description: 'API Reference' },
    
    // Static assets
    { url: `${base}/logo.png`, description: 'Logo image' },
    { url: `${base}/favicon.ico`, description: 'Favicon' },
    { url: `${base}/diagrams/architecture.svg`, description: 'Architecture diagram' },
    { url: `${base}/diagrams/lifecycle.svg`, description: 'Lifecycle diagram' },
    { url: `${base}/diagrams/extension-flow.svg`, description: 'Extension flow diagram' }
  ];
}

/**
 * Print results
 */
function printResults(results) {
  console.log('\n=== Deployment Verification ===\n');
  
  let passCount = 0;
  let failCount = 0;
  
  for (const result of results) {
    const icon = result.success ? '✓' : '✗';
    const color = result.success ? '\x1b[32m' : '\x1b[31m';
    const reset = '\x1b[0m';
    
    console.log(`${color}${icon}${reset} ${result.description}`);
    console.log(`  URL: ${result.url}`);
    
    if (result.success) {
      console.log(`  Status: ${result.status}`);
      passCount++;
    } else {
      if (result.error) {
        console.log(`  Error: ${result.error}`);
      } else {
        console.log(`  Status: ${result.status}`);
      }
      failCount++;
    }
    console.log('');
  }
  
  console.log('=== Summary ===');
  console.log(`Passed: ${passCount}`);
  console.log(`Failed: ${failCount}`);
  console.log(`Total: ${results.length}\n`);
  
  if (failCount > 0) {
    console.log('\x1b[31m⚠ Some pages or assets are not accessible.\x1b[0m');
    console.log('Please check the deployment and ensure all files are published correctly.\n');
    return false;
  } else {
    console.log('\x1b[32m✓ All pages and assets are accessible!\x1b[0m\n');
    return true;
  }
}

/**
 * Main function
 */
async function main() {
  const args = process.argv.slice(2);
  
  if (args.length === 0) {
    console.error('Error: Base URL is required');
    console.error('Usage: node scripts/verify-deployment.mjs <base-url>');
    console.error('Example: node scripts/verify-deployment.mjs https://your-org.github.io/tapestry');
    process.exit(1);
  }
  
  const baseUrl = args[0];
  
  console.log(`Verifying deployment at: ${baseUrl}\n`);
  
  const urlsToCheck = getUrlsToCheck(baseUrl);
  console.log(`Checking ${urlsToCheck.length} URLs...\n`);
  
  const results = [];
  
  for (const { url, description } of urlsToCheck) {
    const result = await checkUrl(url, description);
    results.push(result);
  }
  
  const success = printResults(results);
  process.exit(success ? 0 : 1);
}

main();
