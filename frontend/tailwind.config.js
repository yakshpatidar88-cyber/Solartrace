/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
  ],
  darkMode: 'class',
  theme: {
    extend: {
      colors: {
        grid: {
          dark: '#0B1120',
          card: '#1E293B',
          accent: '#10B981', // Solar Emerald
          warning: '#F59E0B',
          critical: '#EF4444',
          info: '#3B82F6'
        }
      }
    },
  },
  plugins: [],
}
