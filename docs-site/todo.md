## 1. Project Initialization

If you already have a library, you can add the docs folder directly to it. If this is a fresh start, run these commands in your terminal:

```bash
# Create a directory and enter it
mkdir my-library-docs && cd my-library-docs

# Initialize your package manager
npm init -y

# Install VitePress as a dev dependency
npm add -D vitepress

```

Now, run the setup wizard. It will ask if you want to use TypeScript—**say yes**.

```bash
npx vitepress init

```

### Suggested Wizard Options:

* **Where should VitePress initialize?** `./docs`
* **Site title:** My Awesome Library
* **Site description:** A fast TS library.
* **Theme:** Default Theme (e.g., "Just the basics").
* **Use TypeScript for config?** Yes.

---

## 2. Configuration for GitHub Pages

Since you are deploying to GitHub Pages, you need to account for the repository name in your URL (unless you’re using a custom domain).

Edit your `docs/.vitepress/config.mts`:

```typescript
import { defineConfig } from 'vitepress'

export default defineConfig({
  title: "My Library",
  description: "Documentation for my TS library",
  // IMPORTANT: Set 'base' to your repo name, e.g., '/my-repo/'
  base: '/my-repo-name/', 
  
  themeConfig: {
    nav: [
      { text: 'Home', link: '/' },
      { text: 'API', link: '/api-examples' }
    ],
    sidebar: [
      {
        text: 'Guide',
        items: [
          { text: 'Getting Started', link: '/getting-started' },
          { text: 'Usage', link: '/usage' }
        ]
      }
    ],
    socialLinks: [
      { icon: 'github', link: 'https://github.com/your-username/my-repo' }
    ]
  }
})

```

---

## 3. Automated Deployment (GitHub Actions)

GitHub Pages is easiest to manage via a **GitHub Action**. Create a file at `.github/workflows/deploy.yml`:

```yaml
name: Deploy VitePress site to Pages

on:
  push:
    branches: [main]

permissions:
  contents: read
  pages: write
  id-token: write

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - name: Checkout
        uses: actions/checkout@v4
      - name: Install Node.js
        uses: actions/setup-node@v4
        with:
          node-version: 20
          cache: 'npm'
      - name: Install dependencies
        run: npm install
      - name: Build with VitePress
        run: npm run docs:build
      - name: Upload artifact
        uses: actions/upload-pages-artifact@v3
        with:
          path: docs/.vitepress/dist

  deploy:
    environment:
      name: github-pages
      url: ${{ steps.deployment.outputs.page_url }}
    needs: build
    runs-on: ubuntu-latest
    steps:
      - name: Deploy to GitHub Pages
        id: deployment
        uses: actions/deploy-pages@v4

```

---

## Why VitePress for a TS library?

* **Static Site Generation (SSG):** It’s incredibly fast because it serves static HTML but "hydrates" into a Single Page Application (SPA) for snappy navigation.
* **Native TS Support:** Since it's built on Vite, your `.ts` files and config work out of the box.
* **Markdown Extensions:** You can use custom containers (like `::: info` or `::: warning`) and even embed TS code snippets that are automatically highlighted.

> **Pro Tip:** If your library is quite large, look into **TypeDoc**. It can parse your TypeScript source code and automatically generate Markdown files that VitePress can then display as beautiful API documentation.

---

## 1. Install the "Bridge"

You’ll need TypeDoc and the plugin that specifically outputs Markdown (since VitePress doesn't read `.ts` files directly for content).

```bash
npm add -D typedoc typedoc-plugin-markdown

```

---

## 2. Configure the Homepage (3 CTAs)

VitePress has a powerful "Home" layout. You don't need to write HTML; you just configure the **Frontmatter** in `docs/index.md`.

Edit your `docs/index.md` to look like this:

```yaml
---
layout: home

hero:
  name: "MyTSLib"
  text: "The ultimate TypeScript utility"
  tagline: "Fast, type-safe, and zero dependencies."
  image:
    src: /logo.png
    alt: MyTSLib Logo
  actions:
    - theme: brand
      text: Getting Started
      link: /guide/getting-started
    - theme: alt
      text: API Reference
      link: /api/
    - theme: alt
      text: See the Code
      link: https://github.com/your-user/your-repo
---

```

---

## 3. Automate the API Generation

You want your API docs to update whenever your code changes. Add a script to your `package.json` to bridge the two:

```json
"scripts": {
  "docs:dev": "npm run gen-api && vitepress dev docs",
  "docs:build": "npm run gen-api && vitepress build docs",
  "gen-api": "typedoc --plugin typedoc-plugin-markdown --out docs/api ./src/index.ts"
}

```

### What this does:

1. **TypeDoc** looks at your `src/index.ts`.
2. It generates a folder of Markdown files at `docs/api/`.
3. **VitePress** sees those files and includes them in your site using the same CSS/theme as your tutorials.

---

## 4. Organizing the Sidebar

To keep the "Look and Feel" consistent, you’ll want a sidebar that toggles between your hand-written guides and the auto-generated API.

Update `docs/.vitepress/config.mts`:

```typescript
export default defineConfig({
  // ... other config
  themeConfig: {
    nav: [
      { text: 'Guide', link: '/guide/getting-started' },
      { text: 'API', link: '/api/' }
    ],
    sidebar: {
      // Sidebar for the "Guide" section
      '/guide/': [
        {
          text: 'Introduction',
          items: [
            { text: 'Getting Started', link: '/guide/getting-started' },
            { text: 'Architecture', link: '/guide/architecture' }
          ]
        }
      ],
      // Sidebar for the "API" section (pointing to TypeDoc output)
      '/api/': [
        {
          text: 'API Reference',
          // Note: TypeDoc-plugin-markdown usually creates an 'index.md' or 'modules.md'
          items: [
            { text: 'Exports', link: '/api/' },
            { text: 'Interfaces', link: '/api/interfaces' }
          ]
        }
      ]
    }
  }
})

```

---

## 5. Directory Structure

Your project will now look like this:

* `src/` (Your TypeScript source code)
* `docs/`
* `index.md` (The home page with 3 buttons)
* `guide/` (Your hand-written `.md` tutorials)
* `api/` (**Auto-generated** by TypeDoc—don't edit these manually!)
* `.vitepress/` (Config and themes)



---

### Pro-Tip: Visuals in Guides

Since you mentioned "big picture explanations with images," you can store your images in `docs/public/`. In your Markdown, you just reference them as `/my-diagram.png`. VitePress will handle the rest.

---

Here is how to write comments that turn into a professional-grade API reference.

---

## 1. The Anatomy of a Perfect Function Comment

The key is to provide a brief summary, a detailed explanation if needed, and at least one code example.

```typescript
/**
 * Calculates the distance between two points in a 2D plane.
 * * @remarks
 * This function uses the Pythagorean theorem and assumes a Euclidean coordinate system.
 * * @param x - The horizontal coordinate of the point.
 * @param y - The vertical coordinate of the point.
 * @returns The distance from the origin (0,0) to the point (x,y).
 * * @example
 * ```ts
 * const dist = calculateDistance(3, 4);
 * console.log(dist); // Output: 5
 * ```
 * * @category Math Utilities
 */
export function calculateDistance(x: number, y: number): number {
  return Math.sqrt(x * x + y * y);
}

```

### Why this works:

* **The first line:** Becomes the summary in the API list.
* **`@remarks`:** Used for longer, "fine print" details that shouldn't clutter the summary.
* **`@example`:** This is the most important part for your users. TypeDoc will render this in a code block.
* **`@category`:** If you have many functions, this allows VitePress to group them in the sidebar (e.g., "Math Utilities", "String Helpers").

---

## 2. Documenting Interfaces and Types

If your library takes an "Options" object (which most TS libraries do), you should document each property. TypeDoc will create a table for these.

```typescript
/**
 * Configuration options for the library logger.
 */
export interface LoggerOptions {
  /** * The minimum level of logs to display. 
   * @defaultValue 'info'
   */
  level: 'debug' | 'info' | 'warn' | 'error';

  /** * Whether to include a timestamp in the output.
   */
  timestamp?: boolean;
}

```

> **Note:** The `@defaultValue` tag is a lifesaver for users. It prevents them from having to dig through your source code to see what happens if they leave a field blank.

---

## 3. Linking Between Types

One of the coolest features of TypeDoc + VitePress is **automatic cross-linking**.

If you reference another type in your comments using curly braces or backticks, TypeDoc will often turn it into a clickable link to that type's documentation page.

```typescript
/**
 * Process data using a {@link LoggerOptions} configuration.
 */
export function processData(options: LoggerOptions) {
  // ...
}

```

---

## 4. Handling Generics and Deprecations

If your library is highly reusable, you’re likely using generics. You can document what the generic type represents using `@template`.

```typescript
/**
 * @template T - The type of the data being wrapped.
 * @deprecated Use `NewWrapper` instead. This will be removed in v2.0.
 */
export interface Wrapper<T> {
  value: T;
}

```

* **`@template`**: Explains the generic.
* **`@deprecated`**: Will visually strike out the function/type in the generated docs and add a warning, letting users know they should migrate.

---

## 5. Pro-Tip: The "See Also" Tag

If you want to link to a tutorial you wrote in your `docs/guide/` folder from an API page, use the `@see` tag.

```typescript
/**
 * @see {@link https://my-docs.com/guide/getting-started | Getting Started Guide}
 */

```