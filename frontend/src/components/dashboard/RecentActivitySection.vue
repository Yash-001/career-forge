<template>
  <section class="activity-section" aria-labelledby="activity-heading" data-testid="activity-section">
    <div class="card-header">
      <h2 class="card-title" id="activity-heading">Recent Activity</h2>
    </div>

    <!-- Loading -->
    <div v-if="activityLoading" class="activity-loading" aria-busy="true" aria-label="Loading activity" data-testid="activity-loading">
      <div v-for="n in 4" :key="n" class="skeleton" style="height: 36px; border-radius: var(--radius)" />
    </div>

    <!-- Error -->
    <div v-else-if="activityError" role="alert" class="activity-error" data-testid="activity-error">
      <span class="activity-error__msg">{{ activityError }}</span>
      <button class="btn btn-ghost btn-sm" type="button" @click="$emit('retry')">Retry</button>
    </div>

    <!-- Empty -->
    <div v-else-if="activity.length === 0" class="empty-state" data-testid="activity-empty">
      No activity yet. Start by creating a resume or tracking an application.
    </div>

    <!-- Feed -->
    <ul v-else class="activity-list" aria-label="Recent activity feed" data-testid="activity-list">
      <li
        v-for="(entry, i) in activity"
        :key="i"
        class="activity-item"
        :data-testid="`activity-item-${entry.type.toLowerCase()}`"
      >
        <span class="activity-icon" aria-hidden="true">{{ iconFor(entry.type) }}</span>
        <div class="activity-body">
          <RouterLink
            :to="entry.linkPath"
            class="activity-label"
            :aria-label="`${entry.label}: ${entry.subLabel}`"
          >{{ entry.label }}</RouterLink>
          <span class="activity-sublabel">{{ entry.subLabel }}</span>
        </div>
        <time
          class="activity-time"
          :datetime="entry.occurredAt"
          :title="formatFull(entry.occurredAt)"
        >{{ formatRelative(entry.occurredAt) }}</time>
      </li>
    </ul>
  </section>
</template>

<script setup lang="ts">
import { RouterLink } from 'vue-router'
import type { ActivityEntry } from '@/api/dashboard'

defineProps<{
  activity: ActivityEntry[]
  activityLoading: boolean
  activityError: string | null
}>()

defineEmits<{ retry: [] }>()

const ICONS: Record<string, string> = {
  RESUME_UPDATED:    '📝',
  VERSION_CREATED:   '🔖',
  APPLICATION_ADDED: '📋',
  PDF_EXPORTED:      '📄',
}

function iconFor(type: string): string {
  return ICONS[type] ?? '🔔'
}

function formatRelative(iso: string): string {
  const diff = Date.now() - new Date(iso).getTime()
  const mins = Math.floor(diff / 60_000)
  if (mins < 1) return 'just now'
  if (mins < 60) return `${mins}m ago`
  const hrs = Math.floor(mins / 60)
  if (hrs < 24) return `${hrs}h ago`
  const days = Math.floor(hrs / 24)
  if (days < 7) return `${days}d ago`
  return new Date(iso).toLocaleDateString(undefined, { month: 'short', day: 'numeric' })
}

function formatFull(iso: string): string {
  return new Date(iso).toLocaleString(undefined, {
    year: 'numeric', month: 'short', day: 'numeric',
    hour: '2-digit', minute: '2-digit',
  })
}
</script>

<style scoped>
.activity-section {
  display: flex;
  flex-direction: column;
}

.activity-loading {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
  padding: 0.25rem 0;
}

.activity-error {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding: 0.5rem 0;
}

.activity-error__msg {
  font-size: 0.875rem;
  color: var(--color-error-text);
}

.activity-list {
  list-style: none;
  display: flex;
  flex-direction: column;
}

.activity-item {
  display: grid;
  grid-template-columns: 1.5rem 1fr auto;
  align-items: center;
  gap: 0.625rem;
  padding: 0.5rem 0;
  border-bottom: 1px solid var(--color-border);
}

.activity-item:last-child {
  border-bottom: none;
}

.activity-icon {
  font-size: 0.875rem;
  text-align: center;
  flex-shrink: 0;
}

.activity-body {
  display: flex;
  flex-direction: column;
  gap: 0.0625rem;
  min-width: 0;
}

.activity-label {
  font-size: 0.8125rem;
  font-weight: 500;
  color: var(--color-text);
  text-decoration: none;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.activity-label:hover {
  color: var(--color-primary);
  text-decoration: underline;
}

.activity-label:focus-visible {
  outline: 2px solid var(--color-primary);
  outline-offset: 2px;
  border-radius: 2px;
}

.activity-sublabel {
  font-size: 0.75rem;
  color: var(--color-text-muted);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.activity-time {
  font-size: 0.6875rem;
  color: var(--color-text-muted);
  white-space: nowrap;
  flex-shrink: 0;
}
</style>
