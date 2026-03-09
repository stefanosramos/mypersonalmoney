function syncTransferSelects() {

  const from = document.getElementById('transfer-from');
  const to = document.getElementById('transfer-to');

  const fromValue = from.value;
  const toValue = to.value;

  Array.from(from.options).forEach(opt => opt.disabled = false);
  Array.from(to.options).forEach(opt => opt.disabled = false);

  if (fromValue) {
    const opt = Array.from(to.options).find(o => o.value === fromValue);
    if (opt) opt.disabled = true;
  }

  if (toValue) {
    const opt = Array.from(from.options).find(o => o.value === toValue);
    if (opt) opt.disabled = true;
  }

  if (fromValue && toValue && fromValue === toValue) {
    to.value = "";
  }
}