// Custom VitePress theme extending the default theme
// This allows us to customize styling while maintaining VitePress functionality

import DefaultTheme from 'vitepress/theme'
import Layout from './Layout.vue'
import { enhanceApp } from './enhanceApp'
import './style.css'

export default {
  extends: DefaultTheme,
  Layout,
  enhanceApp,
  // Custom enhancements can be added here in the future
  // For example: custom Vue components, layout overrides, etc.
}
