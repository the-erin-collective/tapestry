#!/usr/bin/env node

/**
 * Test script to verify VitePress local search functionality
 * 
 * This script:
 * 1. Checks that the search index (hashmap.json) was generated
 * 2. Validates the search index structure
 * 3. Tests that search queries return relevant results
 * 4. Verifies keyboard navigation metadata is present
 */

import fs from 'fs/promises'
import path from 'path'
import { fileURLToPath } from 'url'

const __filename = fileURLToPath(import.meta.url)
const __dirname = path.dirname(__filename)

const DIST_DIR = path.join(__dirname, '../docs/.vitepress/dist')
const SEARCH_INDEX_PATH = path.join(DIST_DIR, 'hashmap.json')

class SearchTester {
  constructor() {
    this.errors = []
    this.warnings = []
    this.searchIndex = null
  }

  error(message) {
    this.errors.push(message)
    console.error(`❌ ${message}`)
  }

  warn(message) {
    this.warnings.push(message)
    console.warn(`⚠️  ${message}`)
  }

  success(message) {
    console.log(`✅ ${message}`)
  }

  info(message) {
    console.log(`ℹ️  ${message}`)
  }

  async checkSearchIndexExists() {
    this.info('Checking if search index exists...')
    
    try {
      await fs.access(SEARCH_INDEX_PATH)
      this.success('Search index file (hashmap.json) exists')
      return true
    } catch (error) {
      this.error('Search index file (hashmap.json) not found')
      return false
    }
  }

  async loadSearchIndex() {
    this.info('Loading search index...')
    
    try {
      const content = await fs.readFile(SEARCH_INDEX_PATH, 'utf-8')
      this.searchIndex = JSON.parse(content)
      this.success(`Search index loaded (${Object.keys(this.searchIndex).length} entries)`)
      return true
    } catch (error) {
      this.error(`Failed to load search index: ${error.message}`)
      return false
    }
  }

  validateSearchIndexStructure() {
    this.info('Validating search index structure...')
    
    if (!this.searchIndex || typeof this.searchIndex !== 'object') {
      this.error('Search index is not a valid object')
      return false
    }

    const entries = Object.entries(this.searchIndex)
    if (entries.length === 0) {
      this.error('Search index is empty')
      return false
    }

    this.success(`Search index contains ${entries.length} page(s)`)

    // Check structure of first few entries
    let validEntries = 0
    for (const [key, value] of entries.slice(0, 5)) {
      if (typeof value === 'string') {
        validEntries++
      }
    }

    if (validEntries > 0) {
      this.success('Search index entries have valid structure')
      return true
    } else {
      this.error('Search index entries have invalid structure')
      return false
    }
  }

  testSearchQueries() {
    this.info('Testing search queries...')
    
    const testQueries = [
      { query: 'typescript', expectedPages: ['getting-started', 'core-concepts'] },
      { query: 'lifecycle', expectedPages: ['lifecycle-phases', 'core-concepts'] },
      { query: 'extension', expectedPages: ['extensions'] },
      { query: 'api', expectedPages: ['api'] },
      { query: 'architecture', expectedPages: ['architecture'] }
    ]

    let passedQueries = 0
    
    for (const { query, expectedPages } of testQueries) {
      const results = this.searchInIndex(query)
      
      if (results.length === 0) {
        this.warn(`Query "${query}" returned no results`)
        continue
      }

      // Check if any expected pages are in results
      const foundExpected = expectedPages.some(expected => 
        results.some(result => result.toLowerCase().includes(expected))
      )

      if (foundExpected) {
        this.success(`Query "${query}" returned relevant results (${results.length} matches)`)
        passedQueries++
      } else {
        this.warn(`Query "${query}" returned ${results.length} results but none matched expected pages: ${expectedPages.join(', ')}`)
      }
    }

    if (passedQueries >= testQueries.length * 0.6) {
      this.success(`Search queries test passed (${passedQueries}/${testQueries.length} queries returned relevant results)`)
      return true
    } else {
      this.error(`Search queries test failed (only ${passedQueries}/${testQueries.length} queries returned relevant results)`)
      return false
    }
  }

  searchInIndex(query) {
    const results = []
    const lowerQuery = query.toLowerCase()

    for (const [key, content] of Object.entries(this.searchIndex)) {
      if (key.toLowerCase().includes(lowerQuery) || 
          (typeof content === 'string' && content.toLowerCase().includes(lowerQuery))) {
        results.push(key)
      }
    }

    return results
  }

  async verifyConfigHasSearch() {
    this.info('Verifying VitePress config has search enabled...')
    
    const configPath = path.join(__dirname, '../docs/.vitepress/config.mts')
    
    try {
      const configContent = await fs.readFile(configPath, 'utf-8')
      
      if (configContent.includes("provider: 'local'")) {
        this.success('VitePress config has local search enabled')
        return true
      } else {
        this.error('VitePress config does not have local search enabled')
        return false
      }
    } catch (error) {
      this.error(`Failed to read VitePress config: ${error.message}`)
      return false
    }
  }

  async verifyKeyboardShortcuts() {
    this.info('Verifying keyboard shortcut configuration...')
    
    const configPath = path.join(__dirname, '../docs/.vitepress/config.mts')
    
    try {
      const configContent = await fs.readFile(configPath, 'utf-8')
      
      // Check for keyboard navigation translations
      const hasNavigationConfig = configContent.includes('navigateText') || 
                                   configContent.includes('selectText') ||
                                   configContent.includes('closeText')
      
      if (hasNavigationConfig) {
        this.success('Keyboard navigation configuration found in config')
        return true
      } else {
        this.warn('Keyboard navigation configuration not explicitly defined (using defaults)')
        return true // Not a failure, just using defaults
      }
    } catch (error) {
      this.error(`Failed to verify keyboard shortcuts: ${error.message}`)
      return false
    }
  }

  printSummary() {
    console.log('\n' + '='.repeat(60))
    console.log('SEARCH FUNCTIONALITY TEST SUMMARY')
    console.log('='.repeat(60))
    
    if (this.errors.length === 0 && this.warnings.length === 0) {
      console.log('✅ All tests passed!')
    } else {
      if (this.errors.length > 0) {
        console.log(`\n❌ Errors: ${this.errors.length}`)
        this.errors.forEach(err => console.log(`   - ${err}`))
      }
      
      if (this.warnings.length > 0) {
        console.log(`\n⚠️  Warnings: ${this.warnings.length}`)
        this.warnings.forEach(warn => console.log(`   - ${warn}`))
      }
    }
    
    console.log('='.repeat(60) + '\n')
    
    return this.errors.length === 0
  }

  async run() {
    console.log('🔍 Testing VitePress Search Functionality\n')

    // Run all tests
    const configOk = await this.verifyConfigHasSearch()
    const indexExists = await this.checkSearchIndexExists()
    
    if (!indexExists) {
      this.error('Cannot continue tests without search index. Run "npm run docs:build" first.')
      return this.printSummary()
    }

    const indexLoaded = await this.loadSearchIndex()
    if (!indexLoaded) {
      return this.printSummary()
    }

    this.validateSearchIndexStructure()
    this.testSearchQueries()
    await this.verifyKeyboardShortcuts()

    return this.printSummary()
  }
}

// Run the tests
const tester = new SearchTester()
const success = await tester.run()

process.exit(success ? 0 : 1)
