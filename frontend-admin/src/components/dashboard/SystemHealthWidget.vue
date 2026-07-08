<template>
  <div class="card border shadow-sm rounded-4 h-100 system-health-widget">
    <div class="card-body d-flex flex-column h-100">
      <!-- Title & Header -->
      <div class="d-flex justify-content-between align-items-center mb-3">
        <div class="fw-bold d-flex align-items-center">
          <i class="bi bi-activity text-primary me-2"></i>
          <span>系統健康度</span>
          <span class="ms-2 badge bg-success-subtle text-success small-badge d-flex align-items-center" style="font-size: 0.7rem; font-weight: normal; padding: 0.2rem 0.4rem;">
            <span class="spinner-grow spinner-grow-sm text-success me-1" style="width: 6px; height: 6px;" role="status"></span>
            已連線
          </span>
        </div>
        <a v-if="grafanaUrl" :href="grafanaUrl" target="_blank" title="前往 Grafana" class="btn-wrench-link">
          <i class="bi bi-wrench"></i>
        </a>
      </div>

      <!-- 3x2 Grid for Metrics -->
      <div class="row g-2 flex-grow-1 align-items-stretch">
        <!-- Metric 1: API P95 Response Time -->
        <div class="col-6">
          <div class="metric-card p-2 rounded d-flex flex-column justify-content-between h-100">
            <div class="metric-header d-flex align-items-center justify-content-between">
              <span class="metric-title text-tap-secondary">P95 延遲</span>
              <i class="bi bi-stopwatch text-primary"></i>
            </div>
            <div class="metric-value-container mt-1">
              <span class="fs-4 fw-bold" :class="p95ColorClass">{{ p95ResponseTime }}</span>
              <span class="metric-unit ms-1 text-tap-secondary">ms</span>
            </div>
          </div>
        </div>

        <!-- Metric 2: API QPS -->
        <div class="col-6">
          <div class="metric-card p-2 rounded d-flex flex-column justify-content-between h-100">
            <div class="metric-header d-flex align-items-center justify-content-between">
              <span class="metric-title text-tap-secondary">QPS 請求量</span>
              <i class="bi bi-lightning-charge-fill text-warning"></i>
            </div>
            <div class="metric-value-container mt-1">
              <span class="fs-4 fw-bold text-light">{{ qpsValue }}</span>
              <span class="metric-unit ms-1 text-tap-secondary">req/s</span>
            </div>
          </div>
        </div>

        <!-- Metric 3: RabbitMQ Queue -->
        <div class="col-6">
          <div class="metric-card p-2 rounded d-flex flex-column justify-content-between h-100">
            <div class="metric-header d-flex align-items-center justify-content-between">
              <span class="metric-title text-tap-secondary">MQ 佇列</span>
              <i class="bi bi-box-seam-fill text-info"></i>
            </div>
            <div class="metric-value-container mt-1">
              <span class="fs-4 fw-bold" :class="mqColorClass">{{ mqMessages }}</span>
              <span class="metric-unit ms-1 text-tap-secondary">筆</span>
            </div>
          </div>
        </div>

        <!-- Metric 4: Redis Cache -->
        <div class="col-6">
          <div class="metric-card p-2 rounded d-flex flex-column justify-content-between h-100">
            <div class="metric-header d-flex align-items-center justify-content-between">
              <span class="metric-title text-tap-secondary">Redis 快取</span>
              <i class="bi bi-database-fill-check text-success"></i>
            </div>
            <div class="metric-value-container mt-1">
              <span class="fs-4 fw-bold text-light">{{ redisMemoryMB }}</span>
              <span class="metric-unit ms-1 text-tap-secondary">MB</span>
            </div>
          </div>
        </div>

        <!-- Metric 5: JVM Memory -->
        <div class="col-6">
          <div class="metric-card p-2 rounded d-flex flex-column justify-content-between h-100">
            <div class="metric-header d-flex align-items-center justify-content-between">
              <span class="metric-title text-tap-secondary">JVM 記憶體</span>
              <i class="bi bi-cpu text-danger"></i>
            </div>
            <div class="mt-1">
              <div class="d-flex align-items-baseline">
                <span class="fs-5 fw-bold text-light">{{ ramUsage !== null ? ramUsage + '%' : '--%' }}</span>
              </div>
              <div class="progress dark-progress-track mt-1" style="height: 6px;">
                <div class="progress-bar progress-bar-striped progress-bar-animated"
                     :class="getProgressBarColorClass(ramUsage)" role="progressbar"
                     :style="{ width: (ramUsage || 0) + '%' }"></div>
              </div>
            </div>
          </div>
        </div>

        <!-- Metric 6: Server CPU Load -->
        <div class="col-6">
          <div class="metric-card p-2 rounded d-flex flex-column justify-content-between h-100">
            <div class="metric-header d-flex align-items-center justify-content-between">
              <span class="metric-title text-tap-secondary">CPU 負載</span>
              <i class="bi bi-cpu-fill text-secondary"></i>
            </div>
            <div class="mt-1">
              <div class="d-flex align-items-baseline">
                <span class="fs-5 fw-bold text-light">{{ cpuUsage !== null ? cpuUsage + '%' : '--%' }}</span>
              </div>
              <div class="progress dark-progress-track mt-1" style="height: 6px;">
                <div class="progress-bar progress-bar-striped progress-bar-animated"
                     :class="getProgressBarColorClass(cpuUsage)" role="progressbar"
                     :style="{ width: (cpuUsage || 0) + '%' }"></div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, computed } from 'vue';
import api from '@/plugins/axios.js';

// 讀取 Vite 環境變數 (.env)
const grafanaUrl = import.meta.env.VITE_GRAFANA_URL;

// 響應式變數
const p95ResponseTime = ref(0);
const qpsValue = ref(0);
const mqMessages = ref(0);
const redisMemoryMB = ref(0);
const cpuUsage = ref(null);
const ramUsage = ref(null);

let pollingTimer = null;

const fetchMetrics = async () => {
    const queryUrl = "/api/admin/system/health/prometheus";

    try {
        // P95 回應時間
        const p95Res = await api.get(queryUrl, {
            params: { query: 'histogram_quantile(0.95, sum(rate(http_server_requests_seconds_bucket[1m])) by (le))' }
        });
        if (p95Res.data.status === 'success' && p95Res.data.data.result.length > 0) {
            const rawP95 = p95Res.data.data.result[0].value[1];
            p95ResponseTime.value = isNaN(rawP95) ? 0 : Math.round(parseFloat(rawP95) * 1000);
        }

        // QPS
        const qpsRes = await api.get(queryUrl, {
            params: { query: 'sum(rate(http_server_requests_seconds_count[1m]))' }
        });
        if (qpsRes.data.status === 'success' && qpsRes.data.data.result.length > 0) {
            const rawQps = qpsRes.data.data.result[0].value[1];
            qpsValue.value = parseFloat(rawQps).toFixed(1);
        }

        // RabbitMQ 訊息佇列
        const mqRes = await api.get(queryUrl, {
            params: { query: 'sum(rabbitmq_queue_messages_ready)' }
        });
        if (mqRes.data.status === 'success' && mqRes.data.data.result.length > 0) {
            mqMessages.value = parseInt(mqRes.data.data.result[0].value[1], 10);
        }

        // Redis 記憶體快取量
        const redisMemRes = await api.get(queryUrl, {
            params: { query: 'redis_memory_used_bytes' }
        });
        if (redisMemRes.data.status === 'success' && redisMemRes.data.data.result.length > 0) {
            const rawRedisMem = redisMemRes.data.data.result[0].value[1];
            redisMemoryMB.value = (parseFloat(rawRedisMem) / 1024 / 1024).toFixed(1);
        }

        // JVM 記憶體
        const ramRes = await api.get(queryUrl, {
            params: { query: 'jvm_memory_used_bytes{area="heap"}' }
        });
        if (ramRes.data.status === 'success' && ramRes.data.data.result.length > 0) {
            const usedBytes = parseFloat(ramRes.data.data.result[0].value[1]);
            const maxBytes = 42949672960; // 40GB
            ramUsage.value = ((usedBytes / maxBytes) * 100).toFixed(1);
        }

        // CPU 負載
        const cpuRes = await api.get(queryUrl, {
            params: { query: 'system_cpu_usage' }
        });
        if (cpuRes.data.status === 'success' && cpuRes.data.data.result.length > 0) {
            const rawCpu = cpuRes.data.data.result[0].value[1];
            cpuUsage.value = (parseFloat(rawCpu) * 100).toFixed(1);
        }
    } catch (error) {
        console.error("Failed to fetch Prometheus metrics via proxy:", error);
    }
};

const p95ColorClass = computed(() => {
  if (p95ResponseTime.value < 500) return 'text-success';
  if (p95ResponseTime.value < 1000) return 'text-warning';
  return 'text-danger';
});

const mqColorClass = computed(() => {
  if (mqMessages.value === 0) return 'text-light';
  if (mqMessages.value <= 1000) return 'text-warning';
  return 'text-danger';
});

const getProgressBarColorClass = (value) => {
    if (value === null || value === -1) return 'bg-secondary';
    const num = parseFloat(value);
    if (num >= 90) return 'bg-danger';
    if (num >= 80) return 'bg-warning';
    return 'bg-primary';
};

onMounted(() => {
    fetchMetrics();
    pollingTimer = setInterval(fetchMetrics, 10000);
});

onUnmounted(() => {
    if (pollingTimer) clearInterval(pollingTimer);
});
</script>

<style scoped>
.system-health-widget {
  min-height: 292px; /* 與隔壁大財務卡片同等高度或完美貼合 */
}

.btn-wrench-link {
  color: var(--tap-primary);
  border: 1px solid var(--tap-primary);
  border-radius: 4px;
  background-color: transparent;
  transition: all 0.2s ease-in-out;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
}

.btn-wrench-link i {
  font-size: 0.75rem;
}

.btn-wrench-link:hover {
  background-color: rgba(229, 115, 70, 0.15);
  color: var(--tap-primary);
}

.metric-card {
  background-color: var(--tap-bg-base);
  border: 1px solid var(--tap-border);
  transition: transform 0.2s ease, box-shadow 0.2s ease;
  min-height: 72px;
}

.metric-card:hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 8px rgba(0, 0, 0, 0.2);
}

.metric-title {
  font-size: 0.75rem;
  font-weight: 500;
}

.metric-unit {
  font-size: 0.7rem;
}

.metric-header i {
  font-size: 0.9rem;
}

.dark-progress-track {
  background-color: #334155;
  border-radius: 4px;
  overflow: hidden;
}
</style>
