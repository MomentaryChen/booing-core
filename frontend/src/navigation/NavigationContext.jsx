import { createContext, useCallback, useContext, useEffect, useMemo, useState } from "react";
import { useLocation } from "react-router-dom";
import { api, getStoredAccessToken } from "../services/api/client";
import { isMerchantAuthRequired } from "../services/merchant/merchantAuth";
import { ALL_TOP_ROUTE_KEYS } from "./routeKeys";

const NavigationContext = createContext({
  routeKeys: [],
  items: [],
  loading: false,
  error: null,
  hasRouteKey: () => false,
  refresh: async () => {},
});

export function NavigationProvider({ children }) {
  const location = useLocation();
  const [routeKeys, setRouteKeys] = useState([]);
  const [items, setItems] = useState([]);
  const [loading, setLoading] = useState(
    () => isMerchantAuthRequired() && !!getStoredAccessToken()
  );
  const [error, setError] = useState(null);

  const refresh = useCallback(async () => {
    if (!isMerchantAuthRequired()) {
      setRouteKeys(ALL_TOP_ROUTE_KEYS);
      setItems([]);
      setLoading(false);
      setError(null);
      return;
    }
    if (!getStoredAccessToken()) {
      setRouteKeys([]);
      setItems([]);
      setLoading(false);
      setError(null);
      return;
    }
    setLoading(true);
    setError(null);
    try {
      const data = await api("/me/navigation");
      const keys = Array.isArray(data.routeKeys) ? data.routeKeys : [];
      setRouteKeys(keys);
      setItems(Array.isArray(data.items) ? data.items : []);
    } catch (e) {
      setRouteKeys([]);
      setItems([]);
      setError(e.message || "navigation failed");
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    refresh();
  }, [refresh, location.pathname]);

  const hasRouteKey = useCallback(
    (key) => (Array.isArray(routeKeys) ? routeKeys.includes(key) : false),
    [routeKeys]
  );

  const value = useMemo(
    () => ({
      routeKeys,
      items,
      loading,
      error,
      hasRouteKey,
      refresh,
    }),
    [routeKeys, items, loading, error, hasRouteKey, refresh]
  );

  return <NavigationContext.Provider value={value}>{children}</NavigationContext.Provider>;
}

export function useNavigation() {
  return useContext(NavigationContext);
}
