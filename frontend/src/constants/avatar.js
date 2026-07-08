export const AVATAR_COLORS = ['#1e293b', '#e57346', '#f59e0b', '#0d9488', '#ffedd5'];

export function getOrgDefaultLogo(name) {
  const trimmed = (name || '').trim();
  const char = trimmed ? trimmed.charAt(0).toUpperCase() : 'O';
  const bgColor = '#e57346'; // 品牌主色暖橘
  const textColor = '#ffffff'; // 白色字體
  const svg = `
    <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100" width="100" height="100">
      <rect width="100" height="100" fill="${bgColor}"/>
      <text x="50" y="50" dominant-baseline="central" text-anchor="middle" font-size="50" font-family="system-ui, -apple-system, sans-serif" font-weight="bold" fill="${textColor}">${char}</text>
    </svg>
  `.trim();
  return `data:image/svg+xml;utf8,${encodeURIComponent(svg)}`;
}
