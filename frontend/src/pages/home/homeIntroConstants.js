/**
 * Public overview for the repository (README / project home). Change if the remote moves.
 * Set `VITE_BOOKING_CORE_REPO_URL` in `.env` to override at build time.
 */
const fromEnv = import.meta.env.VITE_BOOKING_CORE_REPO_URL;
export const BOOKING_CORE_REPO_OVERVIEW_URL =
  typeof fromEnv === "string" && fromEnv.trim() !== ""
    ? fromEnv.trim()
    : "https://github.com/MomentaryChen/booing-core";
