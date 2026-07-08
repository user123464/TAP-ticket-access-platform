<template>
    <div class="btn-group" role="group" aria-label="Basic outlined example">
        <button v-for="option in options" :key="option.value" 
                class="btn btn-outline-secondary"
                :class="{ active: selectedOption === option.value }" 
                @click="select(option.value)">
            {{ option.label }}
        </button>
    </div>

</template>

<script setup>

const props = defineProps(["options"])
const selectedOption = defineModel()

function select(option) {
    selectedOption.value = option
}

</script>
<style scoped></style>