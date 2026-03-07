import { defineConfig } from 'vitepress'

// https://vitepress.dev/reference/site-config
export default defineConfig({
  title: "Tapestry",
  description: "TypeScript-first modding framework for Minecraft",
  base: '/tapestry/',

  // Ignore dead links for now - will be fixed in link validation task
  ignoreDeadLinks: true,

  // Build output configuration
  outDir: '.vitepress/dist',
  cacheDir: '.vitepress/cache',

  // Asset optimization
  vite: {
    build: {
      // Optimize assets for production
      assetsInlineLimit: 4096, // Inline assets smaller than 4KB
      chunkSizeWarningLimit: 1000, // Warn on chunks larger than 1MB
    },
    // Optimize asset handling
    assetsInclude: ['**/*.svg', '**/*.png', '**/*.jpg', '**/*.jpeg', '**/*.gif'],
  },

  // Favicon configuration
  head: [
    ['link', { rel: 'icon', type: 'image/svg+xml', href: '/tapestry/favicon.svg' }],
    ['link', { rel: 'icon', type: 'image/png', sizes: '32x32', href: '/tapestry/favicon-32x32.png' }],
    ['link', { rel: 'icon', type: 'image/png', sizes: '16x16', href: '/tapestry/favicon-16x16.png' }],
    ['link', { rel: 'shortcut icon', href: '/tapestry/favicon.ico' }]
  ],

  themeConfig: {
    // https://vitepress.dev/reference/default-theme-config
    logo: '/logo.png',

    nav: [
      { text: 'Download the latest version', link: 'https://github.com/alizzycraft/tapestry/releases/latest' },
      { text: '', link: '' },
      { text: '', link: '' },
      { text: '', link: '' },
      { text: '', link: '' },
      { text: '', link: '' },
      { text: '', link: '' },
      { text: '', link: '' },
      { text: '', link: '' },
      { text: '', link: '' },
      { text: '', link: '' },
      { text: 'Guides', link: '/guide/getting-started' },
      { text: 'API', link: '/api/' },
      { text: 'Mods', link: '/mods/' }
    ],

    sidebar: {
      '/guide/': [
        {
          text: 'Introduction',
          items: [
            { text: 'Getting Started', link: '/guide/getting-started' },
            { text: 'Core Concepts', link: '/guide/core-concepts' },
            { text: 'Architecture', link: '/guide/architecture' }
          ]
        },
        {
          text: 'Lifecycle',
          items: [
            { text: 'Lifecycle Phases', link: '/guide/lifecycle-phases' },
            { text: 'Extensions', link: '/guide/extensions' }
          ]
        }
      ],

      '/api/': [
        {
          text: 'API Reference',
          items: [
            { text: 'Overview', link: '/api/index' },
            {
              text: 'Core',
              items: [
                { text: 'Mod Definition', link: '/api/Namespace.mod' },
                { text: 'Configuration', link: '/api/Namespace.config' }
              ]
            },
            {
              text: 'Integration',
              items: [
                { text: 'RPC Bridge', link: '/api/Namespace.rpc' }
              ]
            },
            {
              text: 'Gameplay',
              items: [
                { text: 'Worldgen', link: '/api/Namespace.worldgen' }
              ]
            },
            {
              text: 'State & Environment',
              items: [
                { text: 'Reactive State', link: '/api/Interface.State' },
                { text: 'Side Awareness', link: '/api/Namespace.env' }
              ]
            },
            {
              text: 'Utilities',
              items: [
                { text: 'Scheduler', link: '/api/Namespace.scheduler' },
                { text: 'Runtime Logging', link: '/api/Namespace.runtime' }
              ]
            }
          ]
        }
      ]
    },

    socialLinks: [
      { icon: 'github', link: 'https://github.com/alizzycraft/tapestry' },
      { icon: 'patreon', link: 'https://www.patreon.com/cw/lizzyrosa' }
    ],

    editLink: {
      pattern: 'https://github.com/alizzycraft/tapestry/edit/docs/docs-site/docs/:path',
      text: 'Edit this page on GitHub'
    },

    search: {
      provider: 'local',
      options: {
        detailedView: true,
        translations: {
          button: {
            buttonText: 'Search documentation',
            buttonAriaLabel: 'Search documentation'
          },
          modal: {
            displayDetails: 'Display detailed list',
            resetButtonTitle: 'Reset search',
            backButtonTitle: 'Close search',
            noResultsText: 'No results for',
            footer: {
              selectText: 'to select',
              selectKeyAriaLabel: 'enter',
              navigateText: 'to navigate',
              navigateUpKeyAriaLabel: 'up arrow',
              navigateDownKeyAriaLabel: 'down arrow',
              closeText: 'to close',
              closeKeyAriaLabel: 'escape'
            }
          }
        }
      }
    }
  }
})
