/**
 * Landing Page Verification Tests
 * 
 * Validates: Requirements 2.5
 * 
 * These tests verify that the landing page displays correctly with all
 * required elements (hero layout, CTAs, features).
 */

import { describe, it } from 'node:test';
import assert from 'node:assert';
import { readFileSync, existsSync } from 'node:fs';
import { join } from 'node:path';

describe('Landing Page Display Verification', () => {
  const distPath = join(process.cwd(), 'docs/.vitepress/dist/index.html');
  
  it('should have built index.html file', () => {
    assert.ok(existsSync(distPath), 'Built index.html should exist');
  });

  const html = existsSync(distPath) ? readFileSync(distPath, 'utf-8') : '';

  describe('Hero Section', () => {
    it('should display Tapestry name', () => {
      assert.ok(html.includes('Tapestry'), 'Hero should contain Tapestry name');
    });

    it('should display tagline', () => {
      assert.ok(
        html.includes('TypeScript-first Minecraft modding'),
        'Hero should contain main text'
      );
    });

    it('should display explicit lifecycle tagline', () => {
      assert.ok(
        html.includes('Explicit lifecycle, hard boundaries, deterministic behavior'),
        'Hero should contain tagline'
      );
    });

    it('should display logo image', () => {
      assert.ok(
        html.includes('logo.png') && html.includes('Tapestry Logo'),
        'Hero should contain logo image with alt text'
      );
    });
  });

  describe('Call-to-Action Buttons', () => {
    it('should have Getting Started CTA', () => {
      assert.ok(
        html.includes('Getting Started') && html.includes('/guide/getting-started'),
        'Should have Getting Started CTA linking to guide'
      );
    });

    it('should have API Reference CTA', () => {
      assert.ok(
        html.includes('API Reference') && html.includes('/api/'),
        'Should have API Reference CTA linking to API docs'
      );
    });

    it('should have GitHub CTA', () => {
      assert.ok(
        html.includes('View on GitHub') && html.includes('github.com'),
        'Should have GitHub CTA linking to repository'
      );
    });
  });

  describe('Features Section', () => {
    it('should display Explicit Lifecycle feature', () => {
      assert.ok(
        html.includes('Explicit Lifecycle') && 
        html.includes('Strict phase model prevents running mod logic at the wrong time'),
        'Should display Explicit Lifecycle feature'
      );
    });

    it('should display TypeScript First feature', () => {
      assert.ok(
        html.includes('TypeScript First') && 
        html.includes('first-class citizen with full type safety'),
        'Should display TypeScript First feature'
      );
    });

    it('should display Hard Boundaries feature', () => {
      assert.ok(
        html.includes('Hard Boundaries') && 
        html.includes('Illegal operations fail immediately'),
        'Should display Hard Boundaries feature'
      );
    });

    it('should display Deterministic Behavior feature', () => {
      assert.ok(
        html.includes('Deterministic Behavior') && 
        html.includes('Predictable execution order'),
        'Should display Deterministic Behavior feature'
      );
    });

    it('should display Extension System feature', () => {
      assert.ok(
        html.includes('Extension System') && 
        html.includes('Powerful extension API'),
        'Should display Extension System feature'
      );
    });

    it('should display Developer Experience feature', () => {
      assert.ok(
        html.includes('Developer Experience') && 
        html.includes('Rich tooling, comprehensive documentation'),
        'Should display Developer Experience feature'
      );
    });
  });

  describe('Layout Structure', () => {
    it('should use VitePress home layout', () => {
      assert.ok(
        html.includes('VPHome') && html.includes('VPHero'),
        'Should use VitePress home layout components'
      );
    });

    it('should have features grid', () => {
      assert.ok(
        html.includes('VPFeatures') && html.includes('VPHomeFeatures'),
        'Should have features section with grid layout'
      );
    });

    it('should have proper semantic structure', () => {
      assert.ok(
        html.includes('<header') && html.includes('<div class="VPContent'),
        'Should have proper semantic HTML structure'
      );
    });
  });
});
