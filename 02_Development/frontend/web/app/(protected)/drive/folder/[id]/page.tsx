"use client";

import { use, useEffect, useState } from "react";
import { foldersApi, FolderResponse } from "@/lib/api";
import { DriveView } from "@/components/drive/DriveView";
import { PageSpinner } from "@/components/ui/Spinner";

interface Params {
  id: string;
}

export default function FolderPage({ params }: { params: Promise<Params> }) {
  const { id } = use(params);
  const folderId = parseInt(id, 10);
  const [folder, setFolder] = useState<FolderResponse | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    foldersApi
      .list(null)
      .then((all) => {
        const found = all.find((f) => f.id === folderId);
        setFolder(found ?? null);
      })
      .catch(() => {})
      .finally(() => setLoading(false));
  }, [folderId]);

  if (loading) return <PageSpinner />;

  const folderName =
    folder?.translatedName || folder?.canonicalName || `Folder ${folderId}`;

  return (
    <div className="flex flex-col h-full">
      <div className="flex items-center justify-between px-8 py-5 bg-white border-b border-gray-100">
        <h1 className="text-xl font-semibold text-gray-900">{folderName}</h1>
      </div>
      <DriveView
        folderId={folderId}
        breadcrumb={[{ id: folderId, name: folderName }]}
      />
    </div>
  );
}
