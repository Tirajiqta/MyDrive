import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  async rewrites() {
    return [
      {
        source: "/proxy/:path*",
        destination: "http://localhost:8083/:path*",
      },
    ];
  },
};

export default nextConfig;
