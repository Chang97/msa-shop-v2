export const isPhone = (v) => /^01[0-9]-?\d{3,4}-?\d{4}$/.test(v || '');
export const isPostcode = (v) => /^\d{5}$/.test(v || '');