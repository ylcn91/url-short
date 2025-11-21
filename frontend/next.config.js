/** @type {import('next').NextConfig} */
const nextConfig = {
  reactStrictMode: true,

  // GitHub Pages configuration
  output: 'export',
  basePath: process.env.NODE_ENV === 'production' ? '/url-short' : '',

  images: {
    unoptimized: true, // Required for static export
    domains: ['localhost'],
  },

  // Note: rewrites don't work with static export
  // API calls will use NEXT_PUBLIC_API_URL directly from environment
};

module.exports = nextConfig;
