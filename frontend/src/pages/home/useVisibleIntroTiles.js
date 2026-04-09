import { useMemo } from "react";
import { useLocation } from "react-router-dom";
import { getStoredAccessToken } from "../../services/api/client";
import { isMerchantAuthRequired } from "../../services/merchant/merchantAuth";
import { useNavigation } from "../../navigation/NavigationContext";
import { INTRO_TILES } from "./introTiles";

/**
 * Resolves which intro destination cards to show. When merchant auth is required and the user
 * has a token, tiles are filtered by navigation {@code routeKeys}; while navigation is loading,
 * {@code awaitingNavigation} is true and {@code visibleTiles} is empty.
 */
export function useVisibleIntroTiles() {
  const location = useLocation();
  const { routeKeys, loading } = useNavigation();

  return useMemo(() => {
    if (!isMerchantAuthRequired()) {
      return { visibleTiles: INTRO_TILES, awaitingNavigation: false };
    }
    if (!getStoredAccessToken()) {
      return { visibleTiles: INTRO_TILES, awaitingNavigation: false };
    }
    if (loading) {
      return { visibleTiles: [], awaitingNavigation: true };
    }
    return {
      visibleTiles: INTRO_TILES.filter((tile) => routeKeys.includes(tile.routeKey)),
      awaitingNavigation: false,
    };
  }, [routeKeys, loading, location.pathname]);
}
