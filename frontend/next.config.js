/** @type {import('next').NextConfig} */
const isGitHubPages = process.env.GITHUB_PAGES === 'true';

const nextConfig = {
  reactStrictMode: true,

  // Static export for GitHub Pages, standalone for Docker
  output: isGitHubPages ? 'export' : 'standalone',
  ...(isGitHubPages && { trailingSlash: true }),

  basePath: isGitHubPages ? '/url-short' : '',

  images: {
    unoptimized: true,
    domains: ['localhost'],
  },
};

module.exports = nextConfig;
