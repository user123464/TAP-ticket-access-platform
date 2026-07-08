<script setup>
/**
 * FinanceChart.vue — 財務折線圖（vue-chartjs / Chart.js）
 *
 * P0 為空殼元件：接收 labels / data props 渲染深色折線圖，
 * P1 財務結算模組對接 API 後直接餵入真實數據即可。
 */
import { computed } from "vue";
import { Line } from "vue-chartjs";
import {
  Chart as ChartJS,
  Title,
  Tooltip,
  Legend,
  LineElement,
  PointElement,
  CategoryScale,
  LinearScale,
  Filler,
} from "chart.js";

ChartJS.register(Title, Tooltip, Legend, LineElement, PointElement, CategoryScale, LinearScale, Filler);

const props = defineProps({
  label: {
    type: String,
    default: "金額",
  },
  labels: {
    type: Array,
    default: () => [],
  },
  data: {
    type: Array,
    default: () => [],
  },
  height: {
    type: Number,
    default: 280,
  },
  showLegend: {
    type: Boolean,
    default: true,
  },
});

const chartData = computed(() => ({
  labels: props.labels,
  datasets: [
    {
      label: props.label,
      data: props.data,
      borderColor: "#e57346", // --tap-primary
      backgroundColor: "rgba(229, 115, 70, 0.15)",
      pointBackgroundColor: "#e57346",
      tension: 0.35,
      fill: true,
    },
  ],
}));

// 深色主題的軸線 / 文字配色
const chartOptions = computed(() => ({
  responsive: true,
  maintainAspectRatio: false,
  plugins: {
    legend: {
      display: props.showLegend,
      position: "bottom",
      align: "start",
      labels: { color: "#94a3b8" }, // --tap-text-secondary
    },
  },
  scales: {
    x: {
      ticks: { color: "#94a3b8" },
      grid: { color: "rgba(71, 85, 105, 0.35)" }, // --tap-border 半透明
    },
    y: {
      ticks: { color: "#94a3b8" },
      grid: { color: "rgba(71, 85, 105, 0.35)" },
    },
  },
}));
</script>

<template>
  <div :style="{ height: height + 'px' }">
    <Line :data="chartData" :options="chartOptions" />
  </div>
</template>
