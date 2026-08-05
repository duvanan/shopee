<script setup lang="ts">
export type IconName =
  | "search"
  | "plus"
  | "edit"
  | "trash"
  | "lock"
  | "unlock"
  | "chevron-down"
  | "chevron-right"
  | "close"
  | "check"
  | "shield"
  | "users"
  | "clipboard"
  | "filter"
  | "arrow-left"
  | "key"
  | "logout"
  | "grid"
  | "box"
  | "tag";

defineProps<{ name: IconName }>();
</script>

<template>
  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.75" stroke-linecap="round" stroke-linejoin="round">
    <template v-if="name === 'search'">
      <circle cx="10" cy="10" r="6" />
      <line x1="15" y1="15" x2="20" y2="20" />
    </template>
    <template v-else-if="name === 'plus'">
      <line x1="12" y1="5" x2="12" y2="19" />
      <line x1="5" y1="12" x2="19" y2="12" />
    </template>
    <path v-else-if="name === 'edit'" d="M16.5 3.5a2.121 2.121 0 0 1 3 3L7 19l-4 1 1-4L16.5 3.5Z" />
    <template v-else-if="name === 'trash'">
      <path d="M4 7h16M9 7V4h6v3m-7 0 1 13h8l1-13" />
    </template>
    <template v-else-if="name === 'lock'">
      <rect x="5" y="11" width="14" height="9" rx="2" />
      <path d="M8 11V7a4 4 0 1 1 8 0v4" />
    </template>
    <template v-else-if="name === 'unlock'">
      <rect x="5" y="11" width="14" height="9" rx="2" />
      <path d="M8 11V7a4 4 0 0 1 7-2.6" />
    </template>
    <polyline v-else-if="name === 'chevron-down'" points="6 9 12 15 18 9" />
    <polyline v-else-if="name === 'chevron-right'" points="9 6 15 12 9 18" />
    <template v-else-if="name === 'close'">
      <line x1="6" y1="6" x2="18" y2="18" />
      <line x1="6" y1="18" x2="18" y2="6" />
    </template>
    <polyline v-else-if="name === 'check'" points="5 13 9 17 19 7" />
    <path v-else-if="name === 'shield'" d="M12 3l7 3v6c0 5-3.5 7.5-7 9-3.5-1.5-7-4-7-9V6l7-3Z" />
    <template v-else-if="name === 'users'">
      <circle cx="9" cy="8" r="3" />
      <path d="M3 20c0-3.5 2.5-6 6-6s6 2.5 6 6" />
      <circle cx="17" cy="9" r="2.5" />
      <path d="M15.3 14.2c2.4 0.4 4.2 2.4 4.2 5.8" />
    </template>
    <template v-else-if="name === 'clipboard'">
      <rect x="6" y="4" width="12" height="17" rx="2" />
      <rect x="9" y="2.5" width="6" height="3" rx="1" />
      <line x1="9" y1="11" x2="15" y2="11" />
      <line x1="9" y1="15" x2="13" y2="15" />
    </template>
    <path v-else-if="name === 'filter'" d="M4 5h16l-6 7v6l-4 2v-8L4 5Z" />
    <template v-else-if="name === 'arrow-left'">
      <line x1="19" y1="12" x2="5" y2="12" />
      <polyline points="11 6 5 12 11 18" />
    </template>
    <template v-else-if="name === 'key'">
      <circle cx="8" cy="15" r="3.5" />
      <line x1="10.5" y1="12.5" x2="19" y2="4" />
      <line x1="15" y1="9" x2="17.5" y2="6.5" />
    </template>
    <template v-else-if="name === 'logout'">
      <path d="M9 4H5a1 1 0 0 0-1 1v14a1 1 0 0 0 1 1h4" />
      <polyline points="15 16 20 12 15 8" />
      <line x1="20" y1="12" x2="9" y2="12" />
    </template>
    <template v-else-if="name === 'grid'">
      <rect x="3.5" y="3.5" width="7" height="7" rx="1.5" />
      <rect x="13.5" y="3.5" width="7" height="7" rx="1.5" />
      <rect x="3.5" y="13.5" width="7" height="7" rx="1.5" />
      <rect x="13.5" y="13.5" width="7" height="7" rx="1.5" />
    </template>
    <template v-else-if="name === 'box'">
      <path d="M3.5 8 12 3.5 20.5 8 12 12.5 3.5 8Z" />
      <path d="M3.5 8v9L12 21.5 20.5 17V8" />
      <line x1="12" y1="12.5" x2="12" y2="21.5" />
    </template>
    <template v-else-if="name === 'tag'">
      <path d="M11 3.5H5.5a2 2 0 0 0-2 2V11a2 2 0 0 0 .6 1.4l8.5 8.5a2 2 0 0 0 2.8 0l5.6-5.6a2 2 0 0 0 0-2.8l-8.5-8.5a2 2 0 0 0-1.4-.6Z" />
      <circle cx="8.5" cy="8.5" r="1.25" />
    </template>
  </svg>
</template>
