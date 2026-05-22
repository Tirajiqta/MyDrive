"use client";

import Link from "next/link";
import { ChevronRight, Home } from "lucide-react";

interface BreadcrumbItem {
  id: number | null;
  name: string;
}

export function Breadcrumb({ items }: { items: BreadcrumbItem[] }) {
  return (
    <nav className="flex items-center gap-1 px-8 py-3 text-sm text-gray-500">
      <Link
        href="/drive"
        className="flex items-center gap-1 hover:text-indigo-600 transition-colors"
      >
        <Home className="w-4 h-4" />
        <span>My Drive</span>
      </Link>
      {items.map((item, i) => (
        <span key={item.id ?? i} className="flex items-center gap-1">
          <ChevronRight className="w-4 h-4 text-gray-300" />
          {i === items.length - 1 ? (
            <span className="text-gray-900 font-medium">{item.name}</span>
          ) : (
            <Link
              href={item.id ? `/drive/folder/${item.id}` : "/drive"}
              className="hover:text-indigo-600 transition-colors"
            >
              {item.name}
            </Link>
          )}
        </span>
      ))}
    </nav>
  );
}
