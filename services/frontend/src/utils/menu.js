export function resolveMenuTarget(menu) {
  if (!menu || typeof menu !== 'object') {
    return '';
  }

  const candidates = [
    menu.target,
    menu.url,
    menu.URL,
    menu.path,
    menu.href
  ];

  for (const candidate of candidates) {
    if (typeof candidate === 'string' && candidate.trim().length > 0) {
      return candidate.trim();
    }
  }

  return '';
}
