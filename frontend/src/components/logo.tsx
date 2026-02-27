import { cn } from "@/lib/utils";

interface LogoProps {
  className?: string;
  size?: "sm" | "default" | "lg";
  showText?: boolean;
}

function LogoMark({ className }: { className?: string }) {
  return (
    <svg
      viewBox="0 0 28 28"
      fill="none"
      xmlns="http://www.w3.org/2000/svg"
      className={cn("shrink-0", className)}
    >
      <rect width="28" height="28" rx="7" className="fill-foreground" />
      {/* Stylized "//" representing short URLs */}
      <path
        d="M11.5 8L8.5 20"
        className="stroke-background"
        strokeWidth="2.5"
        strokeLinecap="round"
      />
      <path
        d="M16.5 8L13.5 20"
        className="stroke-background"
        strokeWidth="2.5"
        strokeLinecap="round"
      />
      {/* Small arrow indicating shortening */}
      <path
        d="M19 14H22M22 14L20 12M22 14L20 16"
        className="stroke-background"
        strokeWidth="1.5"
        strokeLinecap="round"
        strokeLinejoin="round"
        opacity="0.6"
      />
    </svg>
  );
}

export function Logo({ className, size = "default", showText = true }: LogoProps) {
  const sizes = {
    sm: { icon: "h-5 w-5", text: "text-sm" },
    default: { icon: "h-6 w-6", text: "text-base" },
    lg: { icon: "h-8 w-8", text: "text-xl" },
  };

  return (
    <span className={cn("inline-flex items-center gap-2", className)}>
      <LogoMark className={sizes[size].icon} />
      {showText && (
        <span className={cn("font-semibold tracking-tight", sizes[size].text)}>
          urlshort
        </span>
      )}
    </span>
  );
}

export { LogoMark };
