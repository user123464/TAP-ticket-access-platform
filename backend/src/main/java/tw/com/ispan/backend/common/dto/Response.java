package tw.com.ispan.backend.common.dto;

/**
 * 全域通用的 API 回傳格式 (Record 版本)
 * 前端接到的 JSON 永遠會長這樣：
 * {
 * "success": true/false,
 * "message": "提示訊息",
 * "data": { ...各種 DTO... }
 * }
 */
public record Response(
        boolean success,
        String message,
        Object data) {
    // 1. 成功：有訊息，也有資料要回傳 (例如：查詢場地明細)
    public static Response success(String message, Object data) {
        return new Response(true, message, data);
    }

    // 2. 成功：只有訊息，不需要回傳資料 (例如：單純的新增或刪除成功)
    public static Response success(String message) {
        return new Response(true, message, null);
    }

    // 3. 失敗：回傳錯誤訊息給前端的 Swal.fire 顯示
    public static Response error(String message) {
        return new Response(false, message, null);
    }
}
