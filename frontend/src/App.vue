<template>
  <AppHeader v-if="showHeader" />
  <RouterView />
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import AppHeader from '@/components/AppHeader.vue'

const route = useRoute()
const noHeaderRoutes = new Set(['login', 'register'])
const showHeader = computed(() => !noHeaderRoutes.has(route.name as string))
</script>
