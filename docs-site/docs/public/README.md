# Static Assets

This directory contains static assets for the Tapestry documentation site.

## Logo and Favicon Files

### Logo Files
- **logo.svg** (120x120px) - Vector logo for the site header and landing page
- **logo.png** (120x120px) - Raster version of the logo for broader compatibility

### Favicon Files
- **favicon.svg** (32x32px) - Vector favicon for modern browsers
- **favicon.ico** (16x16, 32x32) - Multi-resolution ICO file for browser tabs
- **favicon-16x16.png** (16x16px) - Small PNG favicon
- **favicon-32x32.png** (32x32px) - Standard PNG favicon

## Logo Design

The Tapestry logo reflects the framework's core principles:

- **Circular elements**: Represent the explicit lifecycle phases
- **Geometric structure**: Represents hard boundaries and TypeScript's type safety
- **Central "T"**: Stands for Tapestry
- **Color palette**: Teal (#14b8a6) for lifecycle flow, blue (#1e40af) for TypeScript

## Regenerating Assets

If you need to regenerate the PNG and ICO files from the SVG sources:

```bash
npm run gen-favicons
```

This will:
1. Convert logo.svg to logo.png (120x120)
2. Convert favicon.svg to PNG files (16x16, 32x32)
3. Generate favicon.ico from the PNG files

## Adding New Assets

When adding new static assets:

1. Place files in this directory or appropriate subdirectories (e.g., `diagrams/`)
2. Optimize images before committing:
   - SVG: Use SVGO or similar tool
   - PNG: Use pngquant or similar tool
   - Keep file sizes reasonable (<200KB for images)
3. Reference assets in markdown using absolute paths: `/logo.png`, `/diagrams/architecture.svg`
4. Include descriptive alt text for accessibility

## Asset Guidelines

- **Format**: Prefer SVG for diagrams and icons (scalable, small file size)
- **Size**: Keep individual files under 200KB
- **Optimization**: Always optimize images before committing
- **Naming**: Use kebab-case for file names (e.g., `lifecycle-diagram.svg`)
- **Accessibility**: Provide meaningful alt text when embedding in markdown
