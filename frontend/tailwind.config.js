/** @type {import('tailwindcss').Config} */
export default {
  content: ["./index.html", "./src/**/*.{vue,js,ts,jsx,tsx}"],
  theme: {
    extend: {
      colors: {
        brand: {
          DEFAULT: "#ee4d2d",
          dark: "#d7401f",
          soft: "#fdece7",
        },
      },
    },
  },
  plugins: [],
};
