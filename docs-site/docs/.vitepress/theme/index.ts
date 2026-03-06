// Custom VitePress theme extending the default theme
// This allows us to customize styling while maintaining VitePress functionality

import DefaultTheme from 'vitepress/theme'
import Layout from './Layout.vue'
import { enhanceApp } from './enhanceApp'
import './style.css'

// Fix bfcache (back-forward cache) issue when navigating between TWILA and Tapestry sites
// When browser restores page from cache, router state can be stale, causing 404s
// Multiple strategies to prevent bfcache issues:
if (typeof window !== 'undefined') {
  // Strategy 1: Force reload when page is restored from bfcache
  window.addEventListener('pageshow', (event) => {
    if (event.persisted) {
      location.reload()
    }
  })
  
  // Strategy 2: Prevent bfcache by adding unload handler
  window.addEventListener('unload', () => {
    // Empty handler prevents bfcache in some browsers
  })
  
  // Strategy 3: Disable bfcache via Cache-Control meta tag (added in config)
}

export default {
  extends: DefaultTheme,
  Layout,
  enhanceApp,
  // Custom enhancements can be added here in the future
  // For example: custom Vue components, layout overrides, etc.
}
