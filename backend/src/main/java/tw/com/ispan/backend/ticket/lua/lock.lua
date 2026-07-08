-- ==========================================
-- 傳入參數說明：
-- KEYS[1]: 場次狀態的 Hash Key (例如: "Session:2:Status")
-- ARGV[1]: 買票的使用者 ID (例如: "U001")
-- ARGV[2]: 鎖定的 TTL 秒數 (例如: "900")
-- ARGV[3] 到 ARGV[N]: 陣列，裡面裝著消費者要買的所有 Ticket IDs
-- ==========================================

-- 🌟 階段一：【驗證】先檢查是不是「所有」座位都還是可售狀態
-- 使用 for 迴圈，從第 3 個 ARGV 開始跑到最後一個 (#ARGV 代表取得陣列長度)
for i = 3, #ARGV do
    local ticketId = ARGV[i]

    -- 去 Hash 裡面撈出這張票的狀態 (等同於 redis 指令：HGET Session:2:Status 101)
    local status = redis.call('HGET', KEYS[1], ticketId)

    -- 防呆：如果票不存在，或者狀態不是 '1' (AVAILABLE)，直接中斷！
    -- 回傳 0 代表「搶票失敗」，此時因為還沒做任何寫入，所以達成了完美 rollback
    if not status or status ~= '1' then
        return 0
    end
end

-- 🌟 階段二：【寫入】全部驗證通過，開始霸道上鎖
for i = 3, #ARGV do
    local ticketId = ARGV[i]
    -- 利用字串串接 (..) 組合出過期鎖的 Key (例如: "Ticket:Lock:101")
    local lockkey = "Ticket:Lock:" .. ticketId

    -- 1. 更新 Hash 盤：把這張票的狀態改為 '2' (LOCKED)
    redis.call('HSET', KEYS[1], ticketId, '2')

    -- 2. 建立過期鎖：設定 Value 為 userId，並押上 EX (過期秒數)
    -- 等同於 redis 指令：SET Ticket:Lock:101 U001 EX 900
    redis.call('SET', lockkey, ARGV[1], 'EX', ARGV[2])
end

-- 🌟 階段三：【收工】
return 1 -- 回傳 1 代表「搶票成功
