"use client";

import { formatBytes } from "@/lib/api";
import { UserResponse } from "@/lib/api";

export function StorageBar({ user }: { user: UserResponse }) {
  const used = user.currentStorageUsedBytes;
  const limit = user.storageLimitGB * 1024 * 1024 * 1024;
  const pct = Math.min((used / limit) * 100, 100);

  return (
    <div className="px-4 py-3">
      <div className="flex justify-between text-xs text-gray-400 mb-1.5">
        <span>Storage</span>
        <span>
          {formatBytes(used)} / {user.storageLimitGB} GB
        </span>
      </div>
      <div className="w-full bg-gray-700 rounded-full h-1.5 overflow-hidden">
        <div
          className={`h-full rounded-full transition-all ${
            pct > 90 ? "bg-red-500" : pct > 70 ? "bg-yellow-400" : "bg-indigo-500"
          }`}
          style={{ width: `${pct}%` }}
        />
      </div>
    </div>
  );
}
