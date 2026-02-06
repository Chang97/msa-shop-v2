// Minimal Pinia persist plugin to honor `persist` option without external deps
export default ({ store, options }) => {
  const persist = options.persist;
  if (!persist || persist.enabled === false) return;

  const strategies = Array.isArray(persist.strategies)
    ? persist.strategies
    : [{ key: store.$id, storage: sessionStorage }];

  strategies.forEach(({ key = store.$id, storage = sessionStorage, paths }) => {
    if (!storage || typeof storage.getItem !== 'function') return;

    try {
      const cached = storage.getItem(key);
      if (cached) {
        const parsed = JSON.parse(cached);
        if (parsed && typeof parsed === 'object') {
          store.$patch(parsed);
        }
      }
    } catch (_) {
      /* ignore hydrate errors */
    }

    store.$subscribe((_, state) => {
      try {
        const toSave = paths && Array.isArray(paths)
          ? paths.reduce((acc, path) => {
              acc[path] = state[path];
              return acc;
            }, {})
          : state;
        storage.setItem(key, JSON.stringify(toSave));
      } catch (_) {
        /* ignore persist errors */
      }
    });
  });
};

