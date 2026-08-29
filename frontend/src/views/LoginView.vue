<template>
  <div class="auth-page">
    <div class="auth-card card">
      <div class="auth-card__header">
        <RouterLink to="/" class="auth-card__logo">⚡ CareerForge</RouterLink>
        <h1 class="auth-card__title">Sign in to your account</h1>
      </div>

      <form class="form" novalidate @submit.prevent="handleSubmit">
        <div class="field">
          <label for="email">Email address</label>
          <input
            id="email"
            v-model="email"
            type="email"
            autocomplete="email"
            placeholder="you@example.com"
            :class="{ error: errors.email }"
            :aria-invalid="!!errors.email || undefined"
            :aria-describedby="errors.email ? 'err-email' : undefined"
            :disabled="auth.authLoading"
          />
          <span v-if="errors.email" id="err-email" class="field-error" role="alert">{{ errors.email }}</span>
        </div>

        <div class="field">
          <label for="password">Password</label>
          <input
            id="password"
            v-model="password"
            type="password"
            autocomplete="current-password"
            placeholder="••••••••"
            :class="{ error: errors.password }"
            :aria-invalid="!!errors.password || undefined"
            :aria-describedby="errors.password ? 'err-password' : undefined"
            :disabled="auth.authLoading"
          />
          <span v-if="errors.password" id="err-password" class="field-error" role="alert">{{ errors.password }}</span>
        </div>

        <div v-if="auth.authError" class="api-error" role="alert">{{ auth.authError }}</div>

        <button type="submit" class="btn btn-primary" style="width:100%" :disabled="auth.authLoading">
          <span v-if="auth.authLoading" class="spinner" aria-hidden="true" />
          {{ auth.authLoading ? 'Signing in…' : 'Sign in' }}
        </button>
      </form>

      <p class="auth-card__footer">
        Don't have an account?
        <RouterLink to="/register">Create one free</RouterLink>
      </p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()
const router = useRouter()

const email = ref('')
const password = ref('')
const errors = reactive({ email: '', password: '' })

function validate(): boolean {
  errors.email = ''
  errors.password = ''
  if (!email.value) errors.email = 'Email is required.'
  else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email.value)) errors.email = 'Enter a valid email.'
  if (!password.value) errors.password = 'Password is required.'
  return !errors.email && !errors.password
}

async function handleSubmit() {
  if (!validate()) return
  const ok = await auth.login(email.value, password.value)
  if (ok) router.push('/resumes')
}
</script>

<style scoped>
.auth-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 2rem 1rem;
  background: var(--color-bg);
}

.auth-card {
  width: 100%;
  max-width: 420px;
}

.auth-card__header {
  text-align: center;
  margin-bottom: 1.75rem;
}

.auth-card__logo {
  display: inline-block;
  font-size: 1.125rem;
  font-weight: 700;
  color: var(--color-text);
  text-decoration: none;
  margin-bottom: 1.25rem;
}

.auth-card__title {
  font-size: 1.375rem;
  font-weight: 700;
  color: var(--color-text);
  margin: 0;
}

.auth-card__footer {
  margin-top: 1.25rem;
  text-align: center;
  font-size: 0.875rem;
  color: var(--color-text-muted);
}

.auth-card__footer a {
  color: var(--color-primary);
  text-decoration: none;
  font-weight: 500;
}

.auth-card__footer a:hover {
  text-decoration: underline;
}
</style>
