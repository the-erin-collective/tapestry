// Custom VitePress theme extending the default theme
// This allows us to customize styling while maintaining VitePress functionality

import DefaultTheme from 'vitepress/theme'
import Layout from './Layout.vue'
import { enhanceApp } from './enhanceApp'
import './style.css'

// Fix bfcache (back-forward cache) issue when navigating between TWILA and Tapestry sites
// When browser restores page from cache, router state can be stale, causing 404s
// Force reload when page is restored from bfcache to reinitialize router
if (typeof window !== 'undefined') {
  window.addEventListener('pageshow', (event) => {
    if (event.persisted) {
      location.reload()
    }
  })
}

export default {
  extends: DefaultTheme,
  Layout,
  enhanceApp,
  // Custom enhancements can be added here in the future
  // For example: custom Vue components, layout overrides, etc.
}
