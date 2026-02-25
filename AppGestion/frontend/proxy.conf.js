const PROXY_CONFIG = {
  "/api": {
    target: "http://localhost:8081",
    secure: false,
    changeOrigin: true,
    logLevel: "debug",
    // Reescribir /api/presupuestos -> /presupuestos (backend sin context-path)
    pathRewrite: { "^/api": "" },
    // Asegurar que Authorization y demás headers se reenvíen
    onProxyReq: (proxyReq, req) => {
      if (req.headers.authorization) {
        proxyReq.setHeader("Authorization", req.headers.authorization);
      }
    }
  }
};

module.exports = PROXY_CONFIG;
