# Linkforge Frontend - Quick Start Guide

## Prerequisites
- Node.js 18+ or 20+
- Backend API running on port 8080

## Installation

```bash
cd frontend
npm install
```

## Environment Setup

Create `.env.local`:

```bash
NEXT_PUBLIC_API_URL=http://localhost:8080/api
NEXT_PUBLIC_APP_URL=http://localhost:3000
NEXT_PUBLIC_SHORT_DOMAIN=short.link
```

## Run Development Server

```bash
npm run dev
```

Access at: **http://localhost:3000**

## Build for Production

```bash
npm run build
npm start
```

## Available Scripts

```bash
npm run dev         # Start development server
npm run build       # Build for production
npm start           # Start production server
npm run lint        # Run ESLint
npm run type-check  # Run TypeScript checks
```

## Default Credentials

Use the backend's default test user or create a new account via signup.

## Project Structure

```
src/
├── app/              # Pages (Next.js App Router)
├── components/       # React components
│   ├── ui/          # shadcn/ui components
│   ├── layouts/     # Layout components
│   └── analytics/   # Chart components
├── lib/             # Utilities and API client
└── stores/          # State management
```

## Key Features

- Landing page with pricing
- User authentication (login/signup)
- Link management (create, edit, delete)
- Analytics dashboard with charts
- QR code generation
- Workspace settings
- API key management
- Account settings

## Tech Stack

- Next.js 14 (App Router)
- React 18
- TypeScript
- Tailwind CSS
- shadcn/ui
- TanStack Query
- Zustand
- Recharts

## Documentation

- `FRONTEND_STATUS.md` - Complete technical documentation
- `FRONTEND_IMPLEMENTATION_COMPLETE.md` - Implementation summary
- `.env.example` - Environment variables reference

## Docker

```bash
docker build -t linkforge-frontend .
docker run -p 3000:3000 linkforge-frontend
```

## Support

For detailed documentation, see `FRONTEND_STATUS.md`
