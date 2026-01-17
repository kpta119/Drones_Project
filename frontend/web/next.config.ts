import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  output: "standalone",
  async rewrites() {
    return [
      {
        source: "/api/:path*",                    // Your frontend calls this
        destination: "http://127.0.0.1:8080/api/:path*", // Proxy target URL
      },
    ];
  },
};

export default nextConfig;
