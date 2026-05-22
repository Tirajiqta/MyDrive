"use client";

import { Folder, MoreVertical, Pencil, Share2, Trash2 } from "lucide-react";
import Link from "next/link";
import { useState, useRef, useEffect } from "react";
import { FolderResponse } from "@/lib/api";

interface FolderCardProps {
  folder: FolderResponse;
  onRename: (folder: FolderResponse) => void;
  onDelete: (folder: FolderResponse) => void;
  onShare: (folder: FolderResponse) => void;
}

export function FolderCard({
  folder,
  onRename,
  onDelete,
  onShare,
}: FolderCardProps) {
  const [menuOpen, setMenuOpen] = useState(false);
  const menuRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (!menuOpen) return;
    const handler = (e: MouseEvent) => {
      if (!menuRef.current?.contains(e.target as Node)) setMenuOpen(false);
    };
    document.addEventListener("mousedown", handler);
    return () => document.removeEventListener("mousedown", handler);
  }, [menuOpen]);

  return (
    <div className="group relative bg-white border border-gray-100 rounded-xl p-4 hover:shadow-md hover:border-indigo-200 transition-all">
      <Link
        href={`/drive/folder/${folder.id}`}
        className="flex items-center gap-3 pr-6"
      >
        <div className="w-10 h-10 bg-amber-50 rounded-lg flex items-center justify-center flex-shrink-0">
          <Folder className="w-6 h-6 text-amber-500" />
        </div>
        <div className="min-w-0">
          <p className="text-sm font-medium text-gray-900 truncate">
            {folder.translatedName || folder.canonicalName}
          </p>
          <p className="text-xs text-gray-400 mt-0.5">Folder</p>
        </div>
      </Link>

      {/* Menu */}
      <div ref={menuRef} className="absolute right-3 top-3">
        <button
          onClick={(e) => {
            e.preventDefault();
            setMenuOpen((v) => !v);
          }}
          className="p-1.5 rounded-lg text-gray-400 hover:text-gray-600 hover:bg-gray-100 opacity-0 group-hover:opacity-100 transition-all"
        >
          <MoreVertical className="w-4 h-4" />
        </button>
        {menuOpen && (
          <div className="absolute right-0 top-8 w-40 bg-white border border-gray-100 rounded-xl shadow-lg z-20 py-1">
            <MenuItem
              icon={<Pencil className="w-4 h-4" />}
              label="Rename"
              onClick={() => { onRename(folder); setMenuOpen(false); }}
            />
            <MenuItem
              icon={<Share2 className="w-4 h-4" />}
              label="Share"
              onClick={() => { onShare(folder); setMenuOpen(false); }}
            />
            <MenuItem
              icon={<Trash2 className="w-4 h-4 text-red-500" />}
              label="Delete"
              onClick={() => { onDelete(folder); setMenuOpen(false); }}
              danger
            />
          </div>
        )}
      </div>
    </div>
  );
}

function MenuItem({
  icon,
  label,
  onClick,
  danger,
}: {
  icon: React.ReactNode;
  label: string;
  onClick: () => void;
  danger?: boolean;
}) {
  return (
    <button
      onClick={onClick}
      className={`flex items-center gap-2.5 w-full px-4 py-2 text-sm transition-colors
        ${danger ? "text-red-600 hover:bg-red-50" : "text-gray-700 hover:bg-gray-50"}`}
    >
      {icon}
      {label}
    </button>
  );
}
