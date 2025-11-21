# GitHub Pages Deployment Guide

This guide explains how to deploy the Linkforge landing page to GitHub Pages.

## Quick Setup

### 1. Enable GitHub Pages

1. Go to your repository: `https://github.com/ylcn91/url-short`
2. Click **Settings** → **Pages**
3. Under **Source**, select:
   - Source: **GitHub Actions**
4. Click **Save**

### 2. Push Changes

The GitHub Actions workflow is already configured. Simply push to the main branch or the current branch:

```bash
git add .
git commit -m "feat: add GitHub Pages deployment"
git push
```

### 3. Access Your Site

After the workflow completes (3-5 minutes), your site will be available at:

**https://ylcn91.github.io/url-short/**

## What Gets Deployed

The deployment includes:
- ✅ Landing page (Hero, Features, Pricing)
- ✅ Login page
- ✅ Signup page
- ✅ All static assets (CSS, JS, images)

**Note:** The dashboard pages require the backend API, so they won't work on GitHub Pages. Use the full Docker deployment for the complete application.

## Configuration

### Base Path

The site is configured with basePath `/url-short` for GitHub Pages:

```javascript
// next.config.js
basePath: process.env.NODE_ENV === 'production' ? '/url-short' : ''
```

### Custom Domain (Optional)

To use a custom domain like `linkforge.io`:

1. **Add CNAME file:**
   ```bash
   echo "linkforge.io" > frontend/public/CNAME
   ```

2. **Configure DNS:**
   Add these DNS records:
   ```
   Type  Name  Value
   A     @     185.199.108.153
   A     @     185.199.109.153
   A     @     185.199.110.153
   A     @     185.199.111.153
   ```

3. **Update GitHub Pages settings:**
   - Go to Settings → Pages
   - Enter your custom domain
   - Enable "Enforce HTTPS"

4. **Update next.config.js:**
   ```javascript
   basePath: '' // Remove basePath for custom domain
   ```

## Deployment Workflow

The workflow is triggered by:
- ✅ Push to `main` branch
- ✅ Push to current development branch
- ✅ Changes to `frontend/**` directory
- ✅ Manual trigger via GitHub Actions tab

### Workflow Steps

```yaml
1. Checkout code
2. Setup Node.js 20
3. Install dependencies (npm ci)
4. Build Next.js site (npm run build)
5. Add .nojekyll file
6. Upload artifact
7. Deploy to GitHub Pages
```

### Build Time

Typical build time: **3-5 minutes**

## Local Testing

Test the production build locally:

```bash
cd frontend

# Build for production
npm run build

# The output will be in the 'out' directory
ls -la out/

# Serve locally (requires http-server)
npx http-server out -p 3000
# or
npx serve out -p 3000
```

Visit `http://localhost:3000/url-short/` to test.

## Troubleshooting

### Build Fails

**Error: Module not found**
```bash
cd frontend
rm -rf node_modules package-lock.json
npm install
npm run build
```

**Error: Image optimization**
- Make sure `images.unoptimized: true` is set in `next.config.js`

### 404 on Routes

**Symptom:** Links work on landing page but 404 on refresh

**Solution:** GitHub Pages doesn't support Next.js dynamic routing. Use hash routing or full deployment with backend.

### CSS Not Loading

**Symptom:** Blank page or unstyled content

**Solution:** Check basePath configuration:
```javascript
// next.config.js
basePath: '/url-short' // Must match repo name
```

### Deployment Doesn't Update

1. Check GitHub Actions tab for errors
2. Clear GitHub Pages cache:
   - Settings → Pages → Clear cache
3. Force rebuild:
   ```bash
   git commit --allow-empty -m "chore: trigger rebuild"
   git push
   ```

## Monitoring

### Check Deployment Status

1. **GitHub Actions Tab**
   - See build logs
   - Check for errors
   - View deployment URL

2. **Deployment History**
   - Settings → Pages
   - View deployment history
   - See success/failure status

### Analytics (Optional)

Add Google Analytics to track visits:

1. **Create GA4 property**

2. **Add to layout.tsx:**
   ```tsx
   // frontend/src/app/layout.tsx
   <Script
     src={`https://www.googletagmanager.com/gtag/js?id=G-XXXXXXXXXX`}
     strategy="afterInteractive"
   />
   ```

## Performance

### Lighthouse Scores (Target)

- ✅ Performance: 90+
- ✅ Accessibility: 95+
- ✅ Best Practices: 95+
- ✅ SEO: 90+

### Optimizations Applied

- Static site generation (SSG)
- Image optimization disabled (required for static export)
- Minified CSS/JS
- Tree-shaking
- Code splitting

## Updating the Site

### Content Changes

Edit the landing page:
```bash
# Edit content
vim frontend/src/app/page.tsx

# Test locally
cd frontend && npm run dev

# Build and push
git add .
git commit -m "feat: update landing page content"
git push
```

### Design Changes

Update styling:
```bash
# Edit Tailwind classes or components
vim frontend/src/app/globals.css
vim frontend/src/components/ui/*.tsx

# Test and push
npm run build
git add . && git commit -m "style: update design" && git push
```

## Alternative: Vercel Deployment

For better performance and automatic previews:

1. **Import to Vercel:**
   - Go to https://vercel.com
   - Import `ylcn91/url-short`
   - Set root directory to `frontend`

2. **Configure:**
   ```bash
   Framework Preset: Next.js
   Build Command: npm run build
   Output Directory: .next
   Install Command: npm install
   ```

3. **Environment Variables:**
   ```
   NEXT_PUBLIC_API_URL=https://api.linkforge.io
   ```

4. **Deploy:**
   - Automatic on push to main
   - Preview deployments for PRs
   - Custom domain support

## Security

### Content Security Policy

Add CSP headers in `next.config.js`:

```javascript
async headers() {
  return [
    {
      source: '/:path*',
      headers: [
        {
          key: 'Content-Security-Policy',
          value: "default-src 'self'; script-src 'self' 'unsafe-inline'"
        },
      ],
    },
  ]
}
```

### HTTPS

- ✅ GitHub Pages enforces HTTPS automatically
- ✅ No additional configuration needed

## Support

### Documentation
- Next.js Static Export: https://nextjs.org/docs/app/building-your-application/deploying/static-exports
- GitHub Pages: https://docs.github.com/en/pages

### Issues
- GitHub Issues: https://github.com/ylcn91/url-short/issues
- Discussions: https://github.com/ylcn91/url-short/discussions

---

**Last Updated:** November 20, 2025
