/** @type {import('next').NextConfig} */
const isGitHubPages = process.env.GITHUB_PAGES === 'true';

const nextConfig = {
  reactStrictMode: true,

  // Static export only for GitHub Pages deployment
  ...(isGitHubPages && { output: 'export', trailingSlash: true }),

  basePath: isGitHubPages ? '/url-short' : '',

  images: {
    unoptimized: true,
    domains: ['localhost'],
  },
};

module.exports = nextConfig;
