<template>
  <span class="status-badge" :class="`status-badge--${status.toLowerCase()}`" :aria-label="`Status: ${label}`">
    <span class="status-badge__dot" aria-hidden="true" />
    {{ label }}
  </span>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { ApplicationStatus } from '@/api/application'

const props = defineProps<{ status: ApplicationStatus }>()

const LABELS: Record<ApplicationStatus, string> = {
  APPLIED: 'Applied',
  INTERVIEW: 'Interview',
  OFFER: 'Offer',
  REJECTED: 'Rejected',
}

const label = computed(() => LABELS[props.status])
</script>

<style scoped>
.status-badge {
  display: inline-flex;
  align-items: center;
  gap: 0.35rem;
  padding: 0.2rem 0.6rem;
  border-radius: 999px;
  font-size: 0.75rem;
  font-weight: 600;
  border: 1px solid transparent;
}

.status-badge__dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  flex-shrink: 0;
}

/* Applied — blue */
.status-badge--applied {
  background: #eff6ff;
  color: #1d4ed8;
  border-color: #bfdbfe;
}
.status-badge--applied .status-badge__dot { background: #1d4ed8; }

/* Interview — amber */
.status-badge--interview {
  background: #fffbeb;
  color: #92400e;
  border-color: #fde68a;
}
.status-badge--interview .status-badge__dot { background: #d97706; }

/* Offer — green */
.status-badge--offer {
  background: #f0fdf4;
  color: #166534;
  border-color: #bbf7d0;
}
.status-badge--offer .status-badge__dot { background: #16a34a; }

/* Rejected — red */
.status-badge--rejected {
  background: #fef2f2;
  color: #991b1b;
  border-color: #fecaca;
}
.status-badge--rejected .status-badge__dot { background: #dc2626; }
</style>
