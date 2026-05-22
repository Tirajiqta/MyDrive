"use client";

import { useState, useEffect, useCallback } from "react";
import { toast } from "sonner";
import { foldersApi, filesApi, FolderResponse, FileResponse } from "@/lib/api";
import { Spinner } from "@/components/ui/Spinner";
import { Button } from "@/components/ui/Button";
import { FolderCard } from "@/components/drive/FolderCard";
import { FileCard } from "@/components/drive/FileCard";
import { Breadcrumb } from "@/components/drive/Breadcrumb";
import { CreateFolderModal } from "@/components/drive/CreateFolderModal";
import { RenameModal } from "@/components/drive/RenameModal";
import { DeleteModal } from "@/components/drive/DeleteModal";
import { ShareModal } from "@/components/drive/ShareModal";
import { UploadModal } from "@/components/drive/UploadModal";
import { MoveModal } from "@/components/drive/MoveModal";
import { DocumentEditor } from "@/components/drive/DocumentEditor";
import { SignDocumentModal } from "@/components/drive/SignDocumentModal";
import { FolderPlus, Upload, FolderOpen } from "lucide-react";

type RenameItem =
  | { kind: "folder"; data: FolderResponse }
  | { kind: "file"; data: FileResponse };

type DeleteItem =
  | { kind: "folder"; data: FolderResponse }
  | { kind: "file"; data: FileResponse };

type ShareItem =
  | { kind: "folder"; data: FolderResponse }
  | { kind: "file"; data: FileResponse };

interface DriveViewProps {
  folderId: number | null;
  breadcrumb?: { id: number | null; name: string }[];
}

function getCurrentUsername(): string {
  if (typeof window === "undefined") return "unknown";
  try {
    const u = localStorage.getItem("user");
    if (u) return JSON.parse(u).username ?? "unknown";
  } catch {}
  return "unknown";
}

export function DriveView({ folderId, breadcrumb = [] }: DriveViewProps) {
  const [folders, setFolders] = useState<FolderResponse[]>([]);
  const [files, setFiles] = useState<FileResponse[]>([]);
  const [loading, setLoading] = useState(true);

  // Modals
  const [createFolderOpen, setCreateFolderOpen] = useState(false);
  const [uploadOpen, setUploadOpen] = useState(false);
  const [renameItem, setRenameItem] = useState<RenameItem | null>(null);
  const [deleteItem, setDeleteItem] = useState<DeleteItem | null>(null);
  const [shareItem, setShareItem] = useState<ShareItem | null>(null);
  const [moveFile, setMoveFile] = useState<FileResponse | null>(null);
  const [editFile, setEditFile] = useState<FileResponse | null>(null);
  const [signFile, setSignFile] = useState<FileResponse | null>(null);

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const folderList = await foldersApi.list(folderId);
      setFolders(folderList.filter((f) => !f.isDeleted));
      if (folderId != null) {
        const fileList = await filesApi.list(folderId);
        setFiles(fileList.filter((f) => !f.isDeleted));
      } else {
        setFiles([]);
      }
    } catch {
      toast.error("Failed to load files");
    } finally {
      setLoading(false);
    }
  }, [folderId]);

  useEffect(() => {
    load();
  }, [load]);

  const isEmpty = !loading && folders.length === 0 && files.length === 0;

  return (
    <div className="flex flex-col h-full">
      {/* Breadcrumb */}
      <Breadcrumb items={breadcrumb} />

      {/* Toolbar */}
      <div className="flex items-center gap-3 px-8 pb-4">
        <Button
          variant="secondary"
          size="sm"
          onClick={() => setCreateFolderOpen(true)}
        >
          <FolderPlus className="w-4 h-4" />
          New folder
        </Button>
        <Button
          variant="secondary"
          size="sm"
          onClick={() => setUploadOpen(true)}
          disabled={folderId == null}
        >
          <Upload className="w-4 h-4" />
          Upload
        </Button>
        {folderId == null && (
          <span className="text-xs text-gray-400">
            Open a folder to upload files
          </span>
        )}
      </div>

      {/* Content */}
      <div className="flex-1 px-8 pb-8 overflow-y-auto">
        {loading ? (
          <div className="flex justify-center pt-16">
            <Spinner />
          </div>
        ) : isEmpty ? (
          <div className="flex flex-col items-center justify-center pt-24 text-center">
            <div className="w-16 h-16 bg-gray-100 rounded-2xl flex items-center justify-center mb-4">
              <FolderOpen className="w-8 h-8 text-gray-400" />
            </div>
            <p className="text-gray-500 font-medium">This folder is empty</p>
            <p className="text-gray-400 text-sm mt-1">
              Create a folder or upload a file to get started.
            </p>
          </div>
        ) : (
          <div className="flex flex-col gap-6">
            {folders.length > 0 && (
              <section>
                <p className="text-xs font-semibold text-gray-400 uppercase tracking-wide mb-3">
                  Folders
                </p>
                <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-3">
                  {folders.map((f) => (
                    <FolderCard
                      key={f.id}
                      folder={f}
                      onRename={(d) => setRenameItem({ kind: "folder", data: d })}
                      onDelete={(d) => setDeleteItem({ kind: "folder", data: d })}
                      onShare={(d) => setShareItem({ kind: "folder", data: d })}
                    />
                  ))}
                </div>
              </section>
            )}

            {files.length > 0 && (
              <section>
                <p className="text-xs font-semibold text-gray-400 uppercase tracking-wide mb-3">
                  Files
                </p>
                <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-3">
                  {files.map((f) => (
                    <FileCard
                      key={f.id}
                      file={f}
                      onRename={(d) => setRenameItem({ kind: "file", data: d })}
                      onDelete={(d) => setDeleteItem({ kind: "file", data: d })}
                      onShare={(d) => setShareItem({ kind: "file", data: d })}
                      onMove={(d) => setMoveFile(d)}
                      onEdit={(d) => setEditFile(d)}
                      onSign={(d) => setSignFile(d)}
                    />
                  ))}
                </div>
              </section>
            )}
          </div>
        )}
      </div>

      {/* Modals */}
      <CreateFolderModal
        open={createFolderOpen}
        onClose={() => setCreateFolderOpen(false)}
        parentId={folderId}
        onCreated={load}
      />
      <UploadModal
        open={uploadOpen}
        onClose={() => setUploadOpen(false)}
        folderId={folderId}
        onUploaded={load}
      />
      <RenameModal
        open={renameItem != null}
        onClose={() => setRenameItem(null)}
        item={renameItem}
        onRenamed={load}
      />
      <DeleteModal
        open={deleteItem != null}
        onClose={() => setDeleteItem(null)}
        item={deleteItem}
        onDeleted={load}
      />
      <ShareModal
        open={shareItem != null}
        onClose={() => setShareItem(null)}
        item={shareItem}
      />
      <MoveModal
        open={moveFile != null}
        onClose={() => setMoveFile(null)}
        file={moveFile}
        onMoved={load}
      />

      {/* Live document editor */}
      {editFile && (
        <DocumentEditor
          fileId={editFile.id}
          fileName={editFile.originalFileName}
          editorUsername={getCurrentUsername()}
          onClose={() => setEditFile(null)}
        />
      )}

      {/* eIDAS document signing */}
      {signFile && (
        <SignDocumentModal
          fileId={signFile.id}
          fileName={signFile.originalFileName}
          onClose={() => setSignFile(null)}
        />
      )}
    </div>
  );
}
