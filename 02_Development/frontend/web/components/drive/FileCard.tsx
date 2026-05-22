"use client";

import {
  File,
  FileImage,
  FileText,
  FileVideo,
  MoreVertical,
  MoveRight,
  Pencil,
  Share2,
  ShieldCheck,
  SquarePen,
  Trash2,
} from "lucide-react";
import { useState, useRef, useEffect } from "react";
import { FileResponse, formatBytes, formatDate } from "@/lib/api";

interface FileCardProps {
  file: FileResponse;
  onRename: (file: FileResponse) => void;
  onDelete: (file: FileResponse) => void;
  onShare: (file: FileResponse) => void;
  onMove: (file: FileResponse) => void;
  onEdit: (file: FileResponse) => void;
  onSign: (file: FileResponse) => void;
}

function FileIcon({ mimeType }: { mimeType: string }) {
  if (mimeType?.startsWith("image/"))
    return <FileImage className="w-6 h-6 text-blue-500" />;
  if (mimeType?.startsWith("video/"))
    return <FileVideo className="w-6 h-6 text-purple-500" />;
  if (mimeType?.includes("pdf") || mimeType?.startsWith("text/"))
    return <FileText className="w-6 h-6 text-red-500" />;
  return <File className="w-6 h-6 text-gray-500" />;
}

function iconBg(mimeType: string) {
  if (mimeType?.startsWith("image/")) return "bg-blue-50";
  if (mimeType?.startsWith("video/")) return "bg-purple-50";
  if (mimeType?.includes("pdf") || mimeType?.startsWith("text/"))
    return "bg-red-50";
  return "bg-gray-50";
}

function isEditable(mimeType: string) {
  return (
    mimeType?.startsWith("text/") ||
    mimeType === "application/json" ||
    mimeType === "application/xml"
  );
}

export function FileCard({
  file,
  onRename,
  onDelete,
  onShare,
  onMove,
  onEdit,
  onSign,
}: FileCardProps) {
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
      <div className="flex items-center gap-3 pr-6">
        <div
          className={`w-10 h-10 ${iconBg(file.type)} rounded-lg flex items-center justify-center flex-shrink-0`}
        >
          <FileIcon mimeType={file.type} />
        </div>
        <div className="min-w-0">
          <p className="text-sm font-medium text-gray-900 truncate">
            {file.originalFileName}
          </p>
          <p className="text-xs text-gray-400 mt-0.5">
            {formatBytes(file.size)} · {formatDate(file.uploadDate)}
          </p>
        </div>
      </div>

      {/* Menu */}
      <div ref={menuRef} className="absolute right-3 top-3">
        <button
          onClick={() => setMenuOpen((v) => !v)}
          className="p-1.5 rounded-lg text-gray-400 hover:text-gray-600 hover:bg-gray-100 opacity-0 group-hover:opacity-100 transition-all"
        >
          <MoreVertical className="w-4 h-4" />
        </button>
        {menuOpen && (
          <div className="absolute right-0 top-8 w-44 bg-white border border-gray-100 rounded-xl shadow-lg z-20 py-1">
            {isEditable(file.type) && (
              <MenuItem
                icon={<SquarePen className="w-4 h-4 text-indigo-500" />}
                label="Edit (Live)"
                onClick={() => { onEdit(file); setMenuOpen(false); }}
              />
            )}
            {file.type === "application/pdf" && (
              <MenuItem
                icon={<ShieldCheck className="w-4 h-4 text-green-600" />}
                label="Sign (eIDAS)"
                onClick={() => { onSign(file); setMenuOpen(false); }}
              />
            )}
            <MenuItem
              icon={<Pencil className="w-4 h-4" />}
              label="Rename"
              onClick={() => { onRename(file); setMenuOpen(false); }}
            />
            <MenuItem
              icon={<Share2 className="w-4 h-4" />}
              label="Share"
              onClick={() => { onShare(file); setMenuOpen(false); }}
            />
            <MenuItem
              icon={<MoveRight className="w-4 h-4" />}
              label="Move"
              onClick={() => { onMove(file); setMenuOpen(false); }}
            />
            <MenuItem
              icon={<Trash2 className="w-4 h-4 text-red-500" />}
              label="Delete"
              onClick={() => { onDelete(file); setMenuOpen(false); }}
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
