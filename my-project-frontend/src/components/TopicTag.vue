<script setup>
import {useStore} from "@/store";

const  store =useStore()

defineProps({
    type:Number
})
</script>

<template>
    <div class="topic-type"
         :style="{
            color: store.findTypeById(type).color +'EE',
            'border-color':store.findTypeById(type).color +'77',
            'background':store.findTypeById(type).color +'25'
         }">
        {{store.findTypeById(type).name}}
    </div>
</template>

<style scoped>
  .topic-type{
      display: inline-block;
      border:  solid 0.5px grey;
      border-radius: 5px;
      font-size: small;
      padding: 0 5px;
  }
</style>