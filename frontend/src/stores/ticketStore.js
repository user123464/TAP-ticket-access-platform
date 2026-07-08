import { defineStore } from 'pinia';
import axios from '@/plugins/axios';

export const useTicketStore = defineStore('ticket', {
  state: () => {
    return {
      tickets: [],
      totalRevenue: 0
    };
  },
  actions: {
    async fetchTickets() {
      try {
        const res = await axios.get('/api/tickets');
        this.tickets = res.data;
      } catch (err) {
        console.error("無法取得門票訂單資料:", err);
      }
    },
    async checkInTicket(qrCodeHash) {
      try {
        const res = await axios.post(`/api/checkin?qrCodeHash=${qrCodeHash}`);
        const message = res.data.message;
        await this.fetchTickets();
        return { success: true, message };
      } catch (err) {
        return { success: false, message: err.response?.data?.message || '核銷失敗！' };
      }
    },
    async refundTicket(tOrderId, tDetailId) {
      try {
        const res = await axios.post(`/api/${tOrderId}/refund/${tDetailId}`);
        const message = res.data.message;
        await this.fetchTickets();
        return { success: true, message };
      } catch (err) {
        return { success: false, message: err.response?.data?.message || '退票失敗！' };
      }
    },
    async fetchRevenue() {
      try {
        const res = await axios.get('/api/revenue/ticket');
        this.totalRevenue = res.data.totalRevenue || 0;
      } catch (err) {
        let sum = 0;
        this.tickets.forEach(order => {
          if (order.orderDetailTickets) {
            order.orderDetailTickets.forEach(detail => {
              if (detail.itemStatus === 'NORMAL') {
                sum += Number(detail.unitPrice || 0);
              }
            });
          }
        });
        this.totalRevenue = sum;
      }
    }
  }
});
