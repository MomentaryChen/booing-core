/** Flat 16:9-friendly SVG thumbs for dashboard module cards (consistent stroke). */

export function AppointmentsModuleArt({ className }) {
  return (
    <svg className={className} viewBox="0 0 640 360" fill="none" xmlns="http://www.w3.org/2000/svg" aria-hidden>
      <rect width="640" height="360" fill="#F1F5F9" />
      <rect x="48" y="56" width="544" height="248" rx="12" fill="#fff" stroke="#CBD5E1" strokeWidth="2" />
      <rect x="80" y="96" width="72" height="52" rx="6" fill="#EEF2FF" stroke="#A5B4FC" strokeWidth="2" />
      <rect x="168" y="96" width="72" height="52" rx="6" fill="#F8FAFC" stroke="#CBD5E1" strokeWidth="2" />
      <rect x="256" y="96" width="72" height="52" rx="6" fill="#F8FAFC" stroke="#CBD5E1" strokeWidth="2" />
      <path d="M80 188h480" stroke="#E2E8F0" strokeWidth="2" />
      <circle cx="112" cy="232" r="10" fill="#6366F1" fillOpacity="0.35" />
      <rect x="136" y="224" width="160" height="16" rx="4" fill="#94A3B8" fillOpacity="0.35" />
      <rect x="136" y="248" width="120" height="12" rx="4" fill="#CBD5E1" />
    </svg>
  );
}

export function ScheduleModuleArt({ className }) {
  return (
    <svg className={className} viewBox="0 0 640 360" fill="none" xmlns="http://www.w3.org/2000/svg" aria-hidden>
      <rect width="640" height="360" fill="#F8FAFC" />
      <circle cx="320" cy="160" r="88" stroke="#A5B4FC" strokeWidth="3" fill="#EEF2FF" />
      <path d="M320 96v32M320 192v32M256 160h32M352 160h32" stroke="#6366F1" strokeWidth="3" strokeLinecap="round" />
      <circle cx="320" cy="160" r="8" fill="#6366F1" fillOpacity="0.6" />
      <rect x="200" y="268" width="240" height="44" rx="10" fill="#fff" stroke="#CBD5E1" strokeWidth="2" />
      <rect x="220" y="284" width="120" height="12" rx="4" fill="#94A3B8" fillOpacity="0.3" />
    </svg>
  );
}

/** Flat illustration for system dashboard header (dark panel). */
export function SystemHeaderArt({ className }) {
  return (
    <svg className={className} viewBox="0 0 320 200" fill="none" xmlns="http://www.w3.org/2000/svg" aria-hidden>
      <rect width="320" height="200" rx="12" fill="rgba(148, 163, 184, 0.08)" />
      <rect x="24" y="40" width="120" height="72" rx="8" fill="rgba(99, 102, 241, 0.12)" stroke="rgba(129, 140, 248, 0.35)" strokeWidth="1.5" />
      <rect x="160" y="40" width="136" height="20" rx="6" fill="rgba(148, 163, 184, 0.2)" />
      <rect x="160" y="72" width="96" height="14" rx="4" fill="rgba(148, 163, 184, 0.12)" />
      <rect x="24" y="128" width="272" height="40" rx="8" fill="rgba(15, 23, 42, 0.35)" stroke="rgba(148, 163, 184, 0.2)" />
      <circle cx="48" cy="148" r="6" fill="rgba(163, 230, 53, 0.5)" />
      <rect x="64" y="142" width="100" height="8" rx="2" fill="rgba(148, 163, 184, 0.25)" />
    </svg>
  );
}
