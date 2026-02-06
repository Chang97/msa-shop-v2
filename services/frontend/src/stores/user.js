import { computed, ref, watch } from 'vue';
import { defineStore } from 'pinia';
import http, { toError } from '@/api/http';
import { resolveMenuTarget } from '@/utils/menu';

const DEFAULT_USER_STATE = {
  userId: null,
  email: '',
  loginId: '',
  userName: '',
  orgId: null,
  orgName: '',
  empNo: '',
  pstnName: '',
  tel: '',
  userStatusId: null,
  userStatusName: '',
  useYn: false
};

const createUserFieldRefs = () => {
  return Object.entries(DEFAULT_USER_STATE).reduce((acc, [key, defaultValue]) => {
    acc[key] = ref(defaultValue);
    return acc;
  }, {});
};

export const useUserStore = defineStore(
  'user',
  () => {
    const userFieldRefs = createUserFieldRefs();
    const accessToken = ref('');
    const menuTree = ref([]);
    const accessibleMenus = ref([]);
    const permissions = ref([]);
    const sessionChecked = ref(false);
    const menuList = ref([]);
    const leafMenuList = ref([]);
    const currentMenu = ref(null);
    const path = ref([]);
    const loading = ref(false);

    const roles = computed(() => {
      if (!accessToken.value) return [];
      try {
        const payload = JSON.parse(atob(accessToken.value.split('.')[1] || '')) || {};
        const rawRoles = payload.roles || payload.authorities || [];
        if (Array.isArray(rawRoles)) return rawRoles;
        if (typeof rawRoles === 'string') return rawRoles.split(',').map((r) => r.trim()).filter(Boolean);
        return [];
      } catch (_) {
        return [];
      }
    });

    const isAuthenticated = computed(
      () => Boolean(accessToken.value) || Boolean(userFieldRefs.loginId.value || userFieldRefs.userId.value)
    );

    const hasRole = (role) => roles.value.includes(role);

    const setAuthHeader = (token) => {
      if (token) {
        http.defaults.headers.common.Authorization = `Bearer ${token}`;
      } else {
        delete http.defaults.headers.common.Authorization;
      }
    };

    // hydrate auth header if token already persisted
    setAuthHeader(accessToken.value);
    watch(accessToken, (token) => setAuthHeader(token), { immediate: true });

    const leafMenus = computed(() => {
      const leaves = [];
      const traverse = (nodes) => {
        if (!Array.isArray(nodes)) {
          return;
        }
        nodes.forEach((node) => {
          const children = Array.isArray(node?.children) ? node.children : [];
          if (!children.length) {
            leaves.push(node);
          } else {
            traverse(children);
          }
        });
      };
      traverse(menuTree.value);
      return leaves;
    });

    const resetUserFields = () => {
      Object.entries(DEFAULT_USER_STATE).forEach(([key, defaultValue]) => {
        userFieldRefs[key].value = defaultValue;
      });
    };

    const applyUserSnapshot = (userSnapshot = {}) => {
      Object.keys(DEFAULT_USER_STATE).forEach((key) => {
        if (userSnapshot[key] !== undefined) {
          userFieldRefs[key].value = userSnapshot[key];
        }
      });
    };

    const normalizePermissionCodes = (values) => {
      if (!Array.isArray(values)) {
        return [];
      }
      const unique = new Set();
      values.forEach((value) => {
        if (value === undefined || value === null) {
          return;
        }
        const normalized = String(value).trim();
        if (!normalized.length) {
          return;
        }
        if (!unique.has(normalized)) {
          unique.add(normalized);
        }
      });
      return Array.from(unique);
    };

    const setPermissions = (values) => {
      permissions.value = normalizePermissionCodes(values);
    };

    const hasPermission = (required) => {
      if (required === undefined || required === null) {
        return false;
      }
      const current = permissions.value ?? [];
      if (Array.isArray(required)) {
        const normalizedList = normalizePermissionCodes(required);
        if (!normalizedList.length) {
          return false;
        }
        return normalizedList.every((code) => current.includes(code));
      }
      const normalized = String(required).trim();
      if (!normalized.length) {
        return false;
      }
      return current.includes(normalized);
    };

    const rebuildLegacyMenus = (tree, flat) => {
      menuList.value = [];
      leafMenuList.value = [];

      const assignLegacyAliases = (node) => {
        node.MENU_ID = node.menuId;
        node.MENU_CD = node.menuCode;
        node.MENU_NM = node.menuName;
        node.URL = node.url;
        node.SRT = node.srt;
        node.USE_YN = node.useYn;
        node.LVL = node.lvl;
      };

      const buildFromTree = (nodes, parentId = null, depth = 0) => {
        if (!Array.isArray(nodes)) {
          return [];
        }
        return nodes.map((node) => {
          const normalized = {
            menuId: node?.menuId ?? null,
            menuCode: node?.menuCode ?? '',
            menuName: node?.menuName ?? '',
            menuCn: node?.menuCn ?? '',
            url: node?.url ?? '',
            srt: node?.srt ?? null,
            useYn: node?.useYn ?? true,
            lvl: node?.lvl ?? depth,
            upperMenuId: parentId,
            children: []
          };
          assignLegacyAliases(normalized);
          normalized.children = buildFromTree(node?.children ?? [], normalized.menuId, depth + 1);
          if (!normalized.children.length && normalized.url) {
            leafMenuList.value.push(normalized);
          }
          return normalized;
        });
      };

      if (Array.isArray(tree) && tree.length) {
        menuList.value = buildFromTree(tree);
        return;
      }

      if (!Array.isArray(flat) || !flat.length) {
        return;
      }

      const map = new Map();
      flat.forEach((item) => {
        if (!item || item.menuId == null) return;
        const normalized = {
          menuId: item.menuId,
          menuCode: item.menuCode ?? '',
          menuName: item.menuName ?? '',
          menuCn: item.menuCn ?? '',
          url: item.url ?? '',
          srt: item.srt ?? null,
          useYn: item.useYn ?? true,
          lvl: item.lvl ?? null,
          upperMenuId: item.upperMenuId ?? null,
          children: []
        };
        assignLegacyAliases(normalized);
        map.set(normalized.menuId, normalized);
      });

      const roots = [];
      map.forEach((node) => {
        const parentId = node.upperMenuId ?? null;
        if (parentId != null && map.has(parentId)) {
          const parent = map.get(parentId);
          parent.children.push(node);
        } else {
          roots.push(node);
        }
      });

      const sortBySrt = (nodes) => {
        nodes.sort((a, b) => (a.srt ?? 0) - (b.srt ?? 0));
        nodes.forEach((child) => sortBySrt(child.children ?? []));
      };

      const assignDepth = (nodes, depth = 0, parentId = null) => {
        nodes.forEach((node) => {
          node.lvl = depth;
          node.LVL = depth;
          node.upperMenuId = parentId;
          node.children = node.children ?? [];
          if (!node.children.length && node.url) {
            leafMenuList.value.push(node);
          }
          assignDepth(node.children, depth + 1, node.menuId);
        });
      };

      sortBySrt(roots);
      assignDepth(roots);
      menuList.value = roots;
    };

    const resolveCurrentMenu = (targetPath) => {
      if (!targetPath) {
        currentMenu.value = null;
        path.value = [];
        return;
      }

      const normalizedTarget =
        typeof targetPath === 'string' && targetPath.length
          ? targetPath.startsWith('/')
            ? targetPath
            : `/${targetPath.replace(/^\/+/, '')}`
          : '';

      const search = (nodes, parents = []) => {
        for (const node of nodes) {
          const nextParents = [...parents, node];
          const resolvedPath = resolveMenuTarget(node);
          const matches = [node.url, node.URL, resolvedPath, resolvedPath?.replace(/\.do$/i, '')].some((candidate) => {
            if (typeof candidate !== 'string' || !candidate.length) {
              return false;
            }
            if (candidate === targetPath || candidate === normalizedTarget) {
              return true;
            }
            const prefixed = candidate.startsWith('/') ? candidate : `/${candidate.replace(/^\/+/, '')}`;
            return prefixed === normalizedTarget;
          });

          if (matches) {
            return nextParents;
          }
          if (Array.isArray(node.children) && node.children.length > 0) {
            const found = search(node.children, nextParents);
            if (found) return found;
          }
        }
        return null;
      };

      const chain = search(menuList.value, []);
      if (chain) {
        path.value = chain;
        currentMenu.value = chain[chain.length - 1];
      } else {
        path.value = [];
        currentMenu.value = null;
      }
    };

    const setSession = async (payload = {}, options = {}) => {
      const { fallbackLoginId = '', fallbackUserId = null, preserveExistingUser = false, user: forcedUser } = options;

      const sessionUser = payload.user ?? forcedUser;
      if (sessionUser) {
        applyUserSnapshot(sessionUser);
      } else if (!preserveExistingUser) {
        resetUserFields();
      }

      if (payload.menus !== undefined) {
        menuTree.value = Array.isArray(payload.menus) ? payload.menus : [];
      } else if (!preserveExistingUser) {
        menuTree.value = [];
      }

      if (payload.accessibleMenus !== undefined) {
        accessibleMenus.value = Array.isArray(payload.accessibleMenus) ? payload.accessibleMenus : [];
      } else if (!preserveExistingUser) {
        accessibleMenus.value = [];
      }

      if (payload.permissions !== undefined) {
        setPermissions(payload.permissions);
      } else if (!preserveExistingUser) {
        setPermissions([]);
      }

      rebuildLegacyMenus(menuTree.value, accessibleMenus.value);
      resolveCurrentMenu(window?.location?.pathname ?? '');

      if (!userFieldRefs.loginId.value && (payload.loginId || fallbackLoginId)) {
        userFieldRefs.loginId.value = payload.loginId ?? fallbackLoginId ?? '';
      }

      if (
        (userFieldRefs.userId.value === null || userFieldRefs.userId.value === undefined) &&
        (payload.userId !== undefined || fallbackUserId !== null)
      ) {
        const resolvedUserId = payload.userId ?? fallbackUserId;
        if (resolvedUserId !== undefined && resolvedUserId !== null) {
          userFieldRefs.userId.value = resolvedUserId;
        }
      }

      if (payload.accessToken) {
        accessToken.value = payload.accessToken;
        setAuthHeader(accessToken.value);
      }

      sessionChecked.value = true;
    };

    const setMenuTree = (payload = {}) => {
      menuTree.value = Array.isArray(payload.menus) ? payload.menus : [];
      accessibleMenus.value = Array.isArray(payload.accessibleMenus) ? payload.accessibleMenus : [];

      rebuildLegacyMenus(menuTree.value, accessibleMenus.value);
      resolveCurrentMenu(window?.location?.pathname ?? '');
    };

    const clearSession = () => {
      resetUserFields();
      accessToken.value = '';
      menuTree.value = [];
      accessibleMenus.value = [];
      setPermissions([]);
      menuList.value = [];
      leafMenuList.value = [];
      currentMenu.value = null;
      path.value = [];
      sessionChecked.value = true;
    };

    const markSessionChecked = () => {
      sessionChecked.value = true;
    };

    const $reset = () => {
      resetUserFields();
      menuTree.value = [];
      accessibleMenus.value = [];
      setPermissions([]);
      menuList.value = [];
      leafMenuList.value = [];
      currentMenu.value = null;
      path.value = [];
      sessionChecked.value = false;
    };

    const login = async (payload) => {
      loading.value = true;
      try {
        const { data } = await http.post('/auth/login', payload);
        await setSession(data);
        return data;
      } catch (error) {
        throw toError(error);
      } finally {
        loading.value = false;
      }
    };

    const fetchSession = async () => {
      try {
        if (!accessToken.value) {
          await tryRefresh();
        }
        const { data } = await http.get('/users/me');
        await setSession(data);
        return data;
      } catch (error) {
        clearSession();
        if (error?.status === 404 || error?.status === 204 || error?.status === 401 || error?.status === 403) {
          return null;
        }
        throw error;
      }
    };

    const logout = async () => {
      try {
        await http.post('/auth/logout');
      } finally {
        clearSession();
        setAuthHeader('');
      }
    };

    const tryRefresh = async () => {
      try {
        const { data } = await http.post('/auth/refresh');
        if (data?.accessToken) {
          await setSession(data);
          return data;
        }
      } catch (_) {
        // ignore refresh failure
      }
      return null;
    };

    return {
      ...userFieldRefs,
      accessToken,
      menuTree,
      roles,
      accessibleMenus,
      permissions,
      sessionChecked,
      menuList,
      leafMenuList,
      currentMenu,
      path,
      loading,
      isAuthenticated,
      leafMenus,
      setSession,
      setMenuTree,
      setPermissions,
      hasPermission,
      resolveCurrentMenu,
      hasRole,
      login,
      fetchSession,
      logout,
      tryRefresh,
      clearSession,
      markSessionChecked,
      $reset
    };
  },
  {
    persist: {
      enabled: true,
      strategies: [
        {
          key: 'user',
          storage: sessionStorage,
          paths: [
            ...Object.keys(DEFAULT_USER_STATE),
            'menuTree',
            'accessibleMenus',
            'permissions',
            'menuList',
            'leafMenuList',
            'currentMenu',
            'path',
            'sessionChecked',
            'accessToken'
          ]
        }
      ]
    }
  }
);
