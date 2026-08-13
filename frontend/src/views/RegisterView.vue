<template>
  <div class="auth-page">
    <div class="auth-card card">
      <div class="auth-card__header">
        <RouterLink to="/" class="auth-card__logo">⚡ CareerForge</RouterLink>
        <h1 class="auth-card__title">Create your free account</h1>
        <p class="auth-card__subtitle">Start building tailored resumes in minutes.</p>
      </div>

      <form class="form" novalidate @submit.prevent="handleSubmit">
        <div class="form-row">
          <div class="field">
            <label for="firstName">First name</label>
            <input
              id="firstName"
              v-model="firstName"
              type="text"
              autocomplete="given-name"
              placeholder="Jane"
              :class="{ error: errors.firstName }"
              :disabled="auth.authLoading"
            />
            <span v-if="errors.firstName" class="field-error">{{ errors.firstName }}</span>
          </div>
          <div class="field">
            <label for="lastName">Last name</label>
            <input
              id="lastName"
              v-model="lastName"
              type="text"
              autocomplete="family-name"
              placeholder="Smith"
              :class="{ error: errors.lastName }"
              :disabled="auth.authLoading"
            />
            <span v-if="errors.lastName" class="field-error">{{ errors.lastName }}</span>
          </div>
        </div>

        <div class="field">
          <label for="email">Email address</label>
          <input
            id="email"
            v-model="email"
            type="email"
            autocomplete="email"
            placeholder="you@example.com"
            :class="{ error: errors.email }"
            :disabled="auth.authLoading"
          />
          <span v-if="errors.email" class="field-error">{{ errors.email }}</span>
        </div>

        <div class="field">
          <label for="password">Password</label>
          <input
            id="password"
            v-model="password"
            type="password"
            autocomplete="new-password"
            placeholder="8+ characters, at least one number"
            :class="{ error: errors.password }"
            :disabled="auth.authLoading"
          />
          <span v-if="errors.password" class="field-error">{{ errors.password }}</span>
        </div>

        <div v-if="auth.authError" class="api-error" role="alert">{{ auth.authError }}</div>

        <button type="submit" class="btn btn-primary" style="width:100%" :disabled="auth.authLoading">
          <span v-if="auth.authLoading" class="spinner" aria-hidden="true" />
          {{ auth.authLoading ? 'Creating account…' : 'Create free account' }}
        </button>
      </form>

      <p class="auth-card__footer">
        Already have an account?
        <RouterLink to="/login">Sign in</RouterLink>
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

const firstName = ref('')
const lastName = ref('')
const email = ref('')
const password = ref('')
const errors = reactive({ firstName: '', lastName: '', email: '', password: '' })

function validate(): boolean {
  errors.firstName = ''
  errors.lastName = ''
  errors.email = ''
  errors.password = ''
  if (!firstName.value.trim()) errors.firstName = 'First name is required.'
  if (!lastName.value.trim()) errors.lastName = 'Last name is required.'
  if (!email.value) errors.email = 'Email is required.'
  else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email.value)) errors.email = 'Enter a valid email.'
  if (!password.value) errors.password = 'Password is required.'
  else if (password.value.length < 8) errors.password = 'Password must be at least 8 characters.'
  else if (!/\d/.test(password.value)) errors.password = 'Password must contain at least one number.'
  return !errors.firstName && !errors.lastName && !errors.email && !errors.password
}

async function handleSubmit() {
  if (!validate()) return
  const ok = await auth.register(firstName.value.trim(), lastName.value.trim(), email.value, password.value)
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
  margin: 0 0 0.375rem;
}

.auth-card__subtitle {
  font-size: 0.875rem;
  color: var(--color-text-muted);
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
