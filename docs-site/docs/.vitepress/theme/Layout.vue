<template>
  <div>
    <!-- Use default layout for all pages -->
    <DefaultTheme.Layout>
      <!-- Custom navbar content -->
      <template #nav-bar-content-after>
        <div class="download-link-container">
          <a href="https://github.com/alizzycraft/tapestry/releases/latest" 
             class="download-link" 
             target="_blank"
             rel="noopener noreferrer">
            Download the latest version here
          </a>
        </div>
      </template>

      <!-- MAIN CONTENT ANCHOR -->
      <template #doc-before>

      </template>

      <!-- Can inject stuff before footer here -->
    </DefaultTheme.Layout>
  </div>
</template>

<script setup lang="ts">
import DefaultTheme from 'vitepress/theme'
</script>
