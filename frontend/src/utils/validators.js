// 台灣常用格式驗證工具：統一編號、身分證字號、電話號碼
// 皆為純函式，回傳結構固定 { valid, message }，方便表單即時顯示提示文字

/**
 * 統一編號（8 碼）檢驗
 * 採財政部標準邏輯乘數演算法，含第 7 碼為 7 的特例。
 */
export function validateTaxId(value) {
  const v = (value || '').trim();
  if (!v) return { valid: true, message: '' };
  if (!/^\d{8}$/.test(v)) {
    return { valid: false, message: '統一編號必須為 8 位數字' };
  }
  const weights = [1, 2, 1, 2, 1, 2, 4, 1];
  const digits = v.split('').map(Number);
  let sum = 0;
  for (let i = 0; i < 8; i++) {
    const product = digits[i] * weights[i];
    sum += Math.floor(product / 10) + (product % 10); // 邏輯乘積：取各位數字相加
  }
  const ok = sum % 5 === 0 || (digits[6] === 7 && (sum + 1) % 5 === 0);
  return ok
    ? { valid: true, message: '' }
    : { valid: false, message: '統一編號檢核碼錯誤，請確認是否輸入正確' };
}

// 身分證字號首碼英文字母對應數值
const ID_LETTER_MAP = {
  A: 10, B: 11, C: 12, D: 13, E: 14, F: 15, G: 16, H: 17, I: 34, J: 18,
  K: 19, L: 20, M: 21, N: 22, O: 35, P: 23, Q: 24, R: 25, S: 26, T: 27,
  U: 28, V: 29, W: 32, X: 30, Y: 31, Z: 33,
};

/**
 * 身分證字號檢驗（1 碼英文 + 性別碼 1/2 + 8 碼數字，末碼為檢查碼）
 */
export function validateTwId(value) {
  const v = (value || '').trim().toUpperCase();
  if (!v) return { valid: true, message: '' };
  if (!/^[A-Z][12]\d{8}$/.test(v)) {
    return { valid: false, message: '請輸入正確格式' };
  }
  const n = ID_LETTER_MAP[v[0]];
  let sum = Math.floor(n / 10) + (n % 10) * 9;
  for (let i = 1; i <= 8; i++) {
    sum += Number(v[i]) * (9 - i);
  }
  sum += Number(v[9]);
  return sum % 10 === 0
    ? { valid: true, message: '' }
    : { valid: false, message: '身分證字號檢核碼錯誤，請確認是否輸入正確' };
}

/**
 * 電話號碼檢驗（寬鬆清洗法，手機與市話通用）
 *  - 先移除空白、括號、連字號等非數字字元
 *  - 須為 0 開頭，總長度 9~10 碼（涵蓋手機 09xxxxxxxx 與各地區市話）
 * 不強制使用者輸入 - 分隔符號，正確時不回傳任何提示文字
 */
export function validatePhone(value) {
  const v = (value || '').trim();
  if (!v) return { valid: true, message: '' };

  const clean = v.replace(/\D/g, '');
  if (/^0\d{8,9}$/.test(clean)) {
    return { valid: true, message: '' };
  }

  return { valid: false, message: '請輸入正確格式' };
}
