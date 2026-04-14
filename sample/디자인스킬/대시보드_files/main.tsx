import __vite__cjsImport0_react_jsxDevRuntime from "/node_modules/.vite/deps/react_jsx-dev-runtime.js?v=d80b28b5"; const jsxDEV = __vite__cjsImport0_react_jsxDevRuntime["jsxDEV"];
import __vite__cjsImport1_react from "/node_modules/.vite/deps/react.js?v=d80b28b5"; const StrictMode = __vite__cjsImport1_react["StrictMode"];
import __vite__cjsImport2_reactDom_client from "/node_modules/.vite/deps/react-dom_client.js?v=03a58a00"; const createRoot = __vite__cjsImport2_reactDom_client["createRoot"];
import { QueryClient, QueryClientProvider } from "/node_modules/.vite/deps/@tanstack_react-query.js?v=0efab9b4";
import App from "/src/App.tsx";
import "/src/index.css";
const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      retry: 1,
      staleTime: 1e3 * 60 * 5
    }
  }
});
const rootEl = document.getElementById("root");
if (!rootEl) throw new Error("root element not found");
createRoot(rootEl).render(
  /* @__PURE__ */ jsxDEV(StrictMode, { children: /* @__PURE__ */ jsxDEV(QueryClientProvider, { client: queryClient, children: /* @__PURE__ */ jsxDEV(App, {}, void 0, false, {
    fileName: "C:/dev/2026/claude/work-log-ai/frontend/src/main.tsx",
    lineNumber: 22,
    columnNumber: 7
  }, this) }, void 0, false, {
    fileName: "C:/dev/2026/claude/work-log-ai/frontend/src/main.tsx",
    lineNumber: 21,
    columnNumber: 5
  }, this) }, void 0, false, {
    fileName: "C:/dev/2026/claude/work-log-ai/frontend/src/main.tsx",
    lineNumber: 20,
    columnNumber: 3
  }, this)
);

//# sourceMappingURL=data:application/json;base64,eyJ2ZXJzaW9uIjozLCJtYXBwaW5ncyI6IkFBcUJNO0FBckJOLFNBQVNBLGtCQUFrQjtBQUMzQixTQUFTQyxrQkFBa0I7QUFDM0IsU0FBU0MsYUFBYUMsMkJBQTJCO0FBQ2pELE9BQU9DLFNBQVM7QUFDaEIsT0FBTztBQUVQLE1BQU1DLGNBQWMsSUFBSUgsWUFBWTtBQUFBLEVBQ2xDSSxnQkFBZ0I7QUFBQSxJQUNkQyxTQUFTO0FBQUEsTUFDUEMsT0FBTztBQUFBLE1BQ1BDLFdBQVcsTUFBTyxLQUFLO0FBQUEsSUFDekI7QUFBQSxFQUNGO0FBQ0YsQ0FBQztBQUVELE1BQU1DLFNBQVNDLFNBQVNDLGVBQWUsTUFBTTtBQUM3QyxJQUFJLENBQUNGLE9BQVEsT0FBTSxJQUFJRyxNQUFNLHdCQUF3QjtBQUVyRFosV0FBV1MsTUFBTSxFQUFFSTtBQUFBQSxFQUNqQix1QkFBQyxjQUNDLGlDQUFDLHVCQUFvQixRQUFRVCxhQUMzQixpQ0FBQyxTQUFEO0FBQUE7QUFBQTtBQUFBO0FBQUEsU0FBSSxLQUROO0FBQUE7QUFBQTtBQUFBO0FBQUEsU0FFQSxLQUhGO0FBQUE7QUFBQTtBQUFBO0FBQUEsU0FJQTtBQUNGIiwibmFtZXMiOlsiU3RyaWN0TW9kZSIsImNyZWF0ZVJvb3QiLCJRdWVyeUNsaWVudCIsIlF1ZXJ5Q2xpZW50UHJvdmlkZXIiLCJBcHAiLCJxdWVyeUNsaWVudCIsImRlZmF1bHRPcHRpb25zIiwicXVlcmllcyIsInJldHJ5Iiwic3RhbGVUaW1lIiwicm9vdEVsIiwiZG9jdW1lbnQiLCJnZXRFbGVtZW50QnlJZCIsIkVycm9yIiwicmVuZGVyIl0sImlnbm9yZUxpc3QiOltdLCJzb3VyY2VzIjpbIm1haW4udHN4Il0sInNvdXJjZXNDb250ZW50IjpbImltcG9ydCB7IFN0cmljdE1vZGUgfSBmcm9tICdyZWFjdCdcbmltcG9ydCB7IGNyZWF0ZVJvb3QgfSBmcm9tICdyZWFjdC1kb20vY2xpZW50J1xuaW1wb3J0IHsgUXVlcnlDbGllbnQsIFF1ZXJ5Q2xpZW50UHJvdmlkZXIgfSBmcm9tICdAdGFuc3RhY2svcmVhY3QtcXVlcnknXG5pbXBvcnQgQXBwIGZyb20gJy4vQXBwJ1xuaW1wb3J0ICcuL2luZGV4LmNzcydcblxuY29uc3QgcXVlcnlDbGllbnQgPSBuZXcgUXVlcnlDbGllbnQoe1xuICBkZWZhdWx0T3B0aW9uczoge1xuICAgIHF1ZXJpZXM6IHtcbiAgICAgIHJldHJ5OiAxLFxuICAgICAgc3RhbGVUaW1lOiAxMDAwICogNjAgKiA1LFxuICAgIH0sXG4gIH0sXG59KVxuXG5jb25zdCByb290RWwgPSBkb2N1bWVudC5nZXRFbGVtZW50QnlJZCgncm9vdCcpXG5pZiAoIXJvb3RFbCkgdGhyb3cgbmV3IEVycm9yKCdyb290IGVsZW1lbnQgbm90IGZvdW5kJylcblxuY3JlYXRlUm9vdChyb290RWwpLnJlbmRlcihcbiAgPFN0cmljdE1vZGU+XG4gICAgPFF1ZXJ5Q2xpZW50UHJvdmlkZXIgY2xpZW50PXtxdWVyeUNsaWVudH0+XG4gICAgICA8QXBwIC8+XG4gICAgPC9RdWVyeUNsaWVudFByb3ZpZGVyPlxuICA8L1N0cmljdE1vZGU+LFxuKVxuIl0sImZpbGUiOiJDOi9kZXYvMjAyNi9jbGF1ZGUvd29yay1sb2ctYWkvZnJvbnRlbmQvc3JjL21haW4udHN4In0=