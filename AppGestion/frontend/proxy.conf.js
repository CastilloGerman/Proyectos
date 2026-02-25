const PROXY_CONFIG = {
  "/api": {
    target: "http://localhost:8081",
    secure: false,
    changeOrigin: true,
    logLevel: "debug",
    // Asegurar que el header Authorization se reenvíe al backend
    onProxyReq: (proxyReq, req) => {
      if (req.headers.authorization) {
        proxyReq.setHeader("Authorization", req.headers.authorization);
      }
    }
  }
};

module.exports = PROXY_CONFIG;
