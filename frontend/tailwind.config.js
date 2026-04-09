/** @type {import('tailwindcss').Config} */
export default {
  important: true,
  content: [
    "./index.html",
    "./src/**/*.{js,jsx}",
  ],
  corePlugins: {
    preflight: false,
  },
  theme: {
    extend: {
      colors: {
        primary: {
          DEFAULT: "#6366f1",
          muted: "#818cf8",
          foreground: "#ffffff",
        },
        surface: {
          DEFAULT: "#0f172a",
          raised: "#1e293b",
          card: "#1e293b",
          border: "#334155",
        },
      },
      borderRadius: {
        card: "12px",
      },
      boxShadow: {
        card: "0 1px 2px rgba(0, 0, 0, 0.45)",
        "card-hover": "0 12px 40px -12px rgba(99, 102, 241, 0.25), 0 8px 24px -8px rgba(0, 0, 0, 0.5)",
      },
      fontFamily: {
        sans: [
          "Inter",
          "ui-sans-serif",
          "system-ui",
          "-apple-system",
          "Segoe UI",
          "Roboto",
          "Noto Sans TC",
          "sans-serif",
        ],
      },
    },
  },
  plugins: [],
};
