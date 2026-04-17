# UI Design System — TMS

## Core Philosophy

The TMS UI must feel **modern, robust, and premium**. Every screen should give users a polished, professional experience — not a generic enterprise tool. Think of it as a product people enjoy using, not just a form they fill out.

## Brand Color Palette

```css
:root {
  /* Primary — Deep Navy */
  --color-primary-900: #0A1628;
  --color-primary-800: #0F2240;
  --color-primary-700: #152E54;
  --color-primary-600: #1B3A68;
  --color-primary-500: #1E4D8C;
  --color-primary-400: #2A6CB8;
  --color-primary-300: #4A8FD4;
  --color-primary-200: #7BB3E8;
  --color-primary-100: #B8D6F5;
  --color-primary-50:  #E8F2FC;

  /* Accent — Warm Amber */
  --color-accent-700: #B45309;
  --color-accent-600: #D97706;
  --color-accent-500: #F59E0B;
  --color-accent-400: #FBBF24;
  --color-accent-300: #FCD34D;
  --color-accent-200: #FDE68A;
  --color-accent-100: #FEF3C7;

  /* Neutral — Cool Grays */
  --color-neutral-900: #111827;
  --color-neutral-800: #1F2937;
  --color-neutral-700: #374151;
  --color-neutral-600: #4B5563;
  --color-neutral-500: #6B7280;
  --color-neutral-400: #9CA3AF;
  --color-neutral-300: #D1D5DB;
  --color-neutral-200: #E5E7EB;
  --color-neutral-100: #F3F4F6;
  --color-neutral-50:  #F9FAFB;

  /* Semantic */
  --color-success: #059669;
  --color-success-light: #D1FAE5;
  --color-warning: #D97706;
  --color-warning-light: #FEF3C7;
  --color-danger: #DC2626;
  --color-danger-light: #FEE2E2;
  --color-info: #2563EB;
  --color-info-light: #DBEAFE;

  /* Surfaces */
  --surface-primary: #FFFFFF;
  --surface-secondary: #F8FAFD;
  --surface-elevated: #FFFFFF;
  --surface-dark: #0A1628;
  --surface-dark-secondary: #0F2240;

  /* Shadows */
  --shadow-sm: 0 1px 2px rgba(10, 22, 40, 0.06);
  --shadow-md: 0 4px 12px rgba(10, 22, 40, 0.08);
  --shadow-lg: 0 12px 32px rgba(10, 22, 40, 0.12);
  --shadow-xl: 0 20px 48px rgba(10, 22, 40, 0.16);
  --shadow-glow-accent: 0 0 24px rgba(245, 158, 11, 0.2);
}
```

## Typography

```css
:root {
  --font-display: 'Clash Display', 'Satoshi', sans-serif;
  --font-body: 'General Sans', 'Outfit', sans-serif;
  --font-mono: 'JetBrains Mono', 'Fira Code', monospace;
}
```

- Load via Google Fonts or Fontshare
- Never use Arial, Inter, Roboto, or system-ui — they feel generic

## Motion & Animations

```css
:root {
  --transition-fast: 150ms cubic-bezier(0.4, 0, 0.2, 1);
  --transition-base: 200ms cubic-bezier(0.4, 0, 0.2, 1);
  --transition-slow: 300ms cubic-bezier(0.4, 0, 0.2, 1);
  --transition-spring: 500ms cubic-bezier(0.34, 1.56, 0.64, 1);
}
```

- Page transitions: staggered fade-up reveals (60ms delay increments)
- Card hover: `translateY(-2px)` + shadow expansion + amber glow
- Button press: `scale(0.97)` + smooth color transition (200ms)
- Sidebar nav: slide-in amber indicator bar from left on active
- Status changes: smooth color morph (300ms ease-in-out)
- Skeleton shimmer: navy-to-light gradient sweep
- Use `@angular/animations` for route transitions and enter/leave

## Layout

```
┌──────────────────────────────────────────────────────┐
│  Top Header (fixed, full-width)                      │
│  Logo | Global Search | Bell | Role Switcher | Avatar│
├────────────┬─────────────────────────────────────────┤
│            │                                         │
│  Sidebar   │  Main Content Area                      │
│  (dark     │  ┌───────────────────────────────────┐  │
│   navy,    │  │ Breadcrumb                        │  │
│   collaps- │  ├───────────────────────────────────┤  │
│   ible)    │  │ Page Content                      │  │
│            │  └───────────────────────────────────┘  │
│            │  [Sticky Action Bar at bottom of forms] │
└────────────┴─────────────────────────────────────────┘
```

- Sidebar: deep navy gradient, collapses to 48px icon rail on toggle or < 1024px, state persisted in localStorage
- Main content: light surface with subtle warm tint, dot-grid background on dashboards
- Cards: white, 12px border-radius, `--shadow-md`, 3px amber left-border on active/highlighted
- Noise texture overlay at 3–5% opacity on dark surfaces for depth

## Component Patterns

### Buttons
- Primary: solid `--color-accent-500`, dark text, bold, uppercase tracking
- Secondary: outlined `--color-primary-500`, transparent bg
- Danger: solid `--color-danger`, white text
- Success: solid `--color-success`, white text
- All: 8px border-radius, 12px 24px padding

### Tables
- Striped rows alternating `--surface-primary` / `--surface-secondary`
- Header: `--color-primary-800` bg, white text, uppercase tracking-wider
- Row hover: light amber tint (`--color-accent-100`)
- Sticky header on scroll

### Forms
- Inputs: 8px border-radius, 1px `--color-neutral-300` border
- Focus: 2px `--color-accent-500` border + `--shadow-glow-accent`
- Error: `--color-danger` border + light red bg
- Labels: semibold, `--color-neutral-700`

### Status Badges (TMS-specific)
```
PENDING              → amber bg, dark text
APPROVED             → green bg, white text
REJECTED             → red bg, white text
CLARIFICATION_REQUESTED → blue bg, white text
AUTO_APPROVED        → neutral bg, white text, italic label
```

### Cards
- White bg, 12px radius, `--shadow-md`
- Hover: lift + `--shadow-lg` + subtle amber glow
- Active/highlighted: 3px amber left-border accent

## Modern UI Principles (MANDATORY)

These apply to every screen in the TMS:

1. **No blank white flashes** — skeleton screens for all async loads
2. **Generous whitespace** — let content breathe, avoid cramped layouts
3. **Depth and texture** — use shadows, subtle gradients, noise overlays on dark surfaces
4. **Micro-interactions** — every button, card, and nav item responds to hover/focus/press
5. **Consistent amber accents** — active states, highlights, CTAs always use amber
6. **Navy dominance** — sidebar and headers are deep navy; content area is light
7. **Smooth transitions** — no jarring page jumps; all state changes animate
8. **Empty states** — every list/table has a designed empty state with illustration + CTA
9. **Error states** — distinct from empty states; always include a retry action
10. **Mobile-first responsive** — works on 320px mobile up to 1920px desktop

## Do NOT Use
- Generic fonts: Arial, Inter, Roboto, system-ui
- Purple gradients on white backgrounds
- Cookie-cutter Material Design defaults without customization
- Flat, lifeless layouts with no depth or texture
- Evenly distributed color palettes — commit to navy dominance with amber accents
- Disabled/locked UI elements for unauthorized sections — hide them entirely
