/**
 * Flat illustration (unDraw-style palette) for intro empty state. Decorative when paired with visible title text.
 */
export function DestinationsEmptyIllustration({ className }) {
  return (
    <svg
      className={className}
      viewBox="0 0 480 300"
      fill="none"
      xmlns="http://www.w3.org/2000/svg"
      aria-hidden
    >
      <rect width="480" height="300" rx="16" fill="#EEF2FF" />
      <path
        d="M72 208h336v32H72v-32z"
        fill="#C7D2FE"
        fillOpacity="0.45"
      />
      <circle cx="120" cy="120" r="48" fill="#A5B4FC" fillOpacity="0.5" />
      <rect x="200" y="88" width="200" height="16" rx="8" fill="#6366F1" fillOpacity="0.35" />
      <rect x="200" y="116" width="160" height="16" rx="8" fill="#94A3B8" fillOpacity="0.35" />
      <rect x="200" y="144" width="120" height="16" rx="8" fill="#94A3B8" fillOpacity="0.25" />
      <path
        d="M320 64l32 32-32 32"
        stroke="#6366F1"
        strokeWidth="6"
        strokeLinecap="round"
        strokeLinejoin="round"
        strokeOpacity="0.5"
      />
      <circle cx="360" cy="200" r="28" fill="#818CF8" fillOpacity="0.4" />
      <path
        d="M352 200l6 6 14-16"
        stroke="#fff"
        strokeWidth="4"
        strokeLinecap="round"
        strokeLinejoin="round"
      />
    </svg>
  );
}
