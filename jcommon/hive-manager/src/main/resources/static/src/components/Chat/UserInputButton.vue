<template>
  <button class="sc-user-input--button-icon-wrapper">
    <IconBase :color="color" width="20" height="20" :icon-name="tooltip">
      <slot />
    </IconBase>
  </button>
</template>

<script lang="ts">
import IconBase from './components/IconBase.vue'

export default {
  components: {
    IconBase
  },
  props: {
    color: {
      type: String,
      required: true
    },
    tooltip: {
      type: String,
      default: ''
    }
  }
}
</script>

<style scoped>
.sc-user-input--button-icon-wrapper {
  background: none;
  border: none;
  padding: 0px;
  margin: 0px;
  outline: none;
  cursor: pointer;
}
</style>
