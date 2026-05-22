"use client";

import { ReactNode } from "react";

interface HeaderProps {
  title: string;
  actions?: ReactNode;
}

export function Header({ title, actions }: HeaderProps) {
  return (
    <div className="flex items-center justify-between px-8 py-5 bg-white border-b border-gray-100">
      <h1 className="text-xl font-semibold text-gray-900">{title}</h1>
      {actions && <div className="flex items-center gap-3">{actions}</div>}
    </div>
  );
}
