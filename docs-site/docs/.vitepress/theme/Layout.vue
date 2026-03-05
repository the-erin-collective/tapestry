<template>
  <div>
    <!-- Skip link for keyboard navigation -->
    <a href="#main-content" class="skip-link">Skip to main content</a>

    <DefaultTheme.Layout>

      <!-- MAIN CONTENT ANCHOR -->
      <template #doc-before>
        <span id="main-content"></span>
      </template>

      <div class="thread-divider"></div> 

    </DefaultTheme.Layout>
  </div>
</template>

<script setup lang="ts">
import DefaultTheme from 'vitepress/theme'
</script>

<style scoped>
.hero-actions {
  display: flex;
  gap: 12px;
}
</style>