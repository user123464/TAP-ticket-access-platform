package tw.com.ispan.backend.ticket.entity;

public enum TicketStatus {
    RESERVED, // 硬體保留
    AVAILABLE, // 可售
    LOCKED, // 鎖定中
    SOLD; // 已售出
}
