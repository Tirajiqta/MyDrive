import { Loader2 } from "lucide-react";

export function Spinner({ className = "" }: { className?: string }) {
  return (
    <Loader2
      className={`animate-spin text-indigo-600 ${className || "w-8 h-8"}`}
    />
  );
}

export function PageSpinner() {
  return (
    <div className="flex items-center justify-center min-h-screen">
      <Spinner className="w-10 h-10" />
    </div>
  );
}
