<template>
  <div>
    <!-- Skip link for keyboard navigation accessibility -->
    <a href="#VPContent" class="skip-link">Skip to main content</a>
    
    <!-- Default VitePress layout with main content wrapper -->
    <DefaultTheme.Layout>
      <template #doc-before>
        <!-- Anchor point for skip link - VitePress uses VPContent as the main content container -->
        <span id="main-content" style="position: absolute; top: -1px;"></span>
      </template>
    </DefaultTheme.Layout>
  </div>
</template>

<script setup lang="ts">
import DefaultTheme from 'vitepress/theme'
</script>

<style scoped>
/* Skip link styles are defined in style.css */
</style>
