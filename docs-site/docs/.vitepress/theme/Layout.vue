<template>
  <div>
    <!-- Skip link for keyboard navigation -->
    <a href="#main-content" class="skip-link">Skip to main content</a>

    <!-- Use default layout for all pages -->
    <DefaultTheme.Layout>
      <!-- MAIN CONTENT ANCHOR -->
      <template #doc-before>
        <span id="main-content"></span>
      </template>

      <!-- Can inject stuff before footer here -->
    </DefaultTheme.Layout>
  </div>
</template>

<script setup lang="ts">
import DefaultTheme from 'vitepress/theme'
</script>

<style scoped>
.skip-link {
  position: absolute;
  top: -40px;
  left: 0;
  background: #000;
  color: white;
  padding: 8px;
  text-decoration: none;
  z-index: 100;
}

.skip-link:focus {
  top: 0;
}
</style>
