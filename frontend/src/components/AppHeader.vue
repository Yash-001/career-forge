<template>
  <header class="app-header">
    <div class="app-header__inner">
      <RouterLink to="/" class="app-header__logo">
        <span class="app-header__logo-icon">⚡</span>
        CareerForge
      </RouterLink>

      <nav class="app-header__nav" aria-label="Main navigation">
        <template v-if="auth.isAuthenticated">
          <RouterLink to="/dashboard" class="app-header__link">Dashboard</RouterLink>
          <RouterLink to="/resumes" class="app-header__link">Resumes</RouterLink>
          <RouterLink to="/applications" class="app-header__link">Applications</RouterLink>
          <RouterLink to="/profile" class="app-header__link">Profile</RouterLink>
          <RouterLink to="/billing" class="app-header__link">Billing</RouterLink>
          <button class="btn btn-ghost btn-sm" @click="handleLogout">Sign out</button>
        </template>
        <template v-else>
          <RouterLink to="/login" class="app-header__link">Sign in</RouterLink>
          <RouterLink to="/register" class="btn btn-primary btn-sm">Get started</RouterLink>
        </template>
      </nav>

      <button
        class="app-header__menu-toggle"
        :aria-expanded="menuOpen"
        aria-controls="mobile-nav"
        aria-label="Toggle navigation"
        @click="menuOpen = !menuOpen"
      >
        <span class="app-header__hamburger" />
      </button>
    </div>

    <nav v-if="menuOpen" id="mobile-nav" class="app-header__mobile-nav" aria-label="Mobile navigation" @keydown.esc="menuOpen = false">
      <template v-if="auth.isAuthenticated">
        <RouterLink to="/dashboard" class="app-header__mobile-link" @click="menuOpen = false">Dashboard</RouterLink>
        <RouterLink to="/resumes" class="app-header__mobile-link" @click="menuOpen = false">Resumes</RouterLink>
        <RouterLink to="/applications" class="app-header__mobile-link" @click="menuOpen = false">Applications</RouterLink>
        <RouterLink to="/profile" class="app-header__mobile-link" @click="menuOpen = false">Profile</RouterLink>
        <RouterLink to="/billing" class="app-header__mobile-link" @click="menuOpen = false">Billing</RouterLink>
        <button class="app-header__mobile-link app-header__mobile-link--btn" @click="handleLogout">Sign out</button>
      </template>
      <template v-else>
        <RouterLink to="/login" class="app-header__mobile-link" @click="menuOpen = false">Sign in</RouterLink>
        <RouterLink to="/register" class="app-header__mobile-link" @click="menuOpen = false">Get started</RouterLink>
      </template>
    </nav>
  </header>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()
const router = useRouter()
const menuOpen = ref(false)

function handleLogout() {
  auth.logout()
  menuOpen.value = false
  router.push('/')
}
</script>

<style scoped>
.app-header {
  position: sticky;
  top: 0;
  z-index: 100;
  background: var(--color-surface);
  border-bottom: 1px solid var(--color-border);
  box-shadow: var(--shadow-sm);
}

.app-header__inner {
  max-width: 1100px;
  margin: 0 auto;
  padding: 0 1.5rem;
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.app-header__logo {
  display: flex;
  align-items: center;
  gap: 0.4rem;
  font-size: 1.125rem;
  font-weight: 700;
  color: var(--color-text);
  text-decoration: none;
  letter-spacing: -0.01em;
}

.app-header__logo-icon {
  font-size: 1.25rem;
}

.app-header__nav {
  display: flex;
  align-items: center;
  gap: 0.25rem;
}

.app-header__link {
  padding: 0.4rem 0.75rem;
  border-radius: var(--radius);
  font-size: 0.875rem;
  font-weight: 500;
  color: var(--color-text-muted);
  text-decoration: none;
  transition: color 0.15s, background 0.15s;
}

.app-header__link:hover,
.app-header__link.router-link-active {
  color: var(--color-text);
  background: var(--color-bg);
}

.app-header__menu-toggle {
  display: none;
  background: none;
  border: none;
  cursor: pointer;
  padding: 0.5rem;
}

.app-header__hamburger,
.app-header__hamburger::before,
.app-header__hamburger::after {
  display: block;
  width: 20px;
  height: 2px;
  background: var(--color-text);
  border-radius: 2px;
  transition: transform 0.2s;
}

.app-header__hamburger {
  position: relative;
}

.app-header__hamburger::before,
.app-header__hamburger::after {
  content: '';
  position: absolute;
  left: 0;
}

.app-header__hamburger::before { top: -6px; }
.app-header__hamburger::after  { top: 6px; }

.app-header__mobile-nav {
  display: flex;
  flex-direction: column;
  border-top: 1px solid var(--color-border);
  padding: 0.5rem 1.5rem 1rem;
  background: var(--color-surface);
}

.app-header__mobile-link {
  padding: 0.65rem 0;
  font-size: 0.9375rem;
  font-weight: 500;
  color: var(--color-text);
  text-decoration: none;
  border-bottom: 1px solid var(--color-border);
}

.app-header__mobile-link:last-child {
  border-bottom: none;
}

.app-header__mobile-link--btn {
  background: none;
  border: none;
  cursor: pointer;
  text-align: left;
  font-family: inherit;
  color: var(--color-text-muted);
}

@media (max-width: 640px) {
  .app-header__nav { display: none; }
  .app-header__menu-toggle { display: block; }
}
</style>
