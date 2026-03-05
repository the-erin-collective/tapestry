#!/usr/bin/env node

/**
 * Generate favicon.ico from PNG files
 * 
 * This script creates a multi-resolution ICO file from the generated PNG favicons.
 */

import pngToIco from 'png-to-ico';
import { writeFileSync } from 'fs';
import { fileURLToPath } from 'url';
import { dirname, join } from 'path';

const __filename = fileURLToPath(import.meta.url);
const __dirname = dirname(__filename);
const publicDir = join(__dirname, '../docs/public');

console.log('Generating favicon.ico from PNG files...');

try {
  const buf = await pngToIco([
    join(publicDir, 'favicon-16x16.png'),
    join(publicDir, 'favicon-32x32.png')
  ]);
  
  writeFileSync(join(publicDir, 'favicon.ico'), buf);
  console.log('✓ Generated favicon.ico (16x16, 32x32)');
  console.log('\n✓ All favicon files are ready!');
} catch (error) {
  console.error('✗ Error generating favicon.ico:', error.message);
  process.exit(1);
}
