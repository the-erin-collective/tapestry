#!/usr/bin/env node

/**
 * Generate favicon files from SVG source
 * 
 * This script converts the SVG logo and favicon to various formats needed for web deployment:
 * - PNG logo (120x120) for the site header
 * - ICO favicon (16x16, 32x32) for browser tabs
 * - PNG favicon (32x32) as fallback
 * 
 * Usage: node scripts/generate-favicons.mjs
 */

import { readFileSync, writeFileSync } from 'fs';
import { fileURLToPath } from 'url';
import { dirname, join } from 'path';

const __filename = fileURLToPath(import.meta.url);
const __dirname = dirname(__filename);
const publicDir = join(__dirname, '../docs/public');

console.log('Generating favicon files from SVG sources...');

// For now, we'll create a simple note about manual conversion
// In a production environment, you would use a library like 'sharp' or 'svg2img'
const instructions = `
# Favicon Generation Instructions

The SVG files have been created in docs/public/:
- logo.svg (120x120) - Main logo for the site
- favicon.svg (32x32) - Simplified favicon

To generate the required PNG and ICO files, you can use one of these methods:

## Method 1: Using sharp (Node.js)
\`\`\`bash
npm install --save-dev sharp
\`\`\`

Then run this script with sharp integration.

## Method 2: Using ImageMagick (Command line)
\`\`\`bash
# Generate PNG logo
convert -background none docs/public/logo.svg -resize 120x120 docs/public/logo.png

# Generate PNG favicon
convert -background none docs/public/favicon.svg -resize 32x32 docs/public/favicon-32x32.png

# Generate ICO favicon (multiple sizes)
convert -background none docs/public/favicon.svg -define icon:auto-resize=16,32 docs/public/favicon.ico
\`\`\`

## Method 3: Using online tools
- Visit https://realfavicongenerator.net/
- Upload the favicon.svg file
- Download the generated favicon package
- Place files in docs/public/

## Method 4: Using Inkscape (GUI)
- Open favicon.svg in Inkscape
- Export as PNG with desired dimensions
- Use an online ICO converter for the .ico file

For now, the SVG files will work in modern browsers. The PNG and ICO files are recommended for broader compatibility.
`;

console.log(instructions);

// Check if sharp is available
try {
  const sharp = await import('sharp');
  console.log('\n✓ Sharp is available! Generating PNG and ICO files...\n');
  
  // Generate logo.png (120x120)
  const logoSvg = readFileSync(join(publicDir, 'logo.svg'));
  await sharp.default(logoSvg)
    .resize(120, 120)
    .png()
    .toFile(join(publicDir, 'logo.png'));
  console.log('✓ Generated logo.png (120x120)');
  
  // Generate favicon PNG (32x32)
  const faviconSvg = readFileSync(join(publicDir, 'favicon.svg'));
  await sharp.default(faviconSvg)
    .resize(32, 32)
    .png()
    .toFile(join(publicDir, 'favicon-32x32.png'));
  console.log('✓ Generated favicon-32x32.png');
  
  // Generate favicon PNG (16x16)
  await sharp.default(faviconSvg)
    .resize(16, 16)
    .png()
    .toFile(join(publicDir, 'favicon-16x16.png'));
  console.log('✓ Generated favicon-16x16.png');
  
  console.log('\n✓ All favicon files generated successfully!');
  console.log('\nNote: For .ico file generation, use ImageMagick or an online converter.');
  console.log('Command: convert favicon-16x16.png favicon-32x32.png favicon.ico');
  
} catch (error) {
  console.log('\n⚠ Sharp is not installed. SVG files have been created.');
  console.log('To generate PNG and ICO files, install sharp:');
  console.log('  npm install --save-dev sharp');
  console.log('Then run this script again.\n');
  console.log('Alternatively, use one of the methods described above.');
}
