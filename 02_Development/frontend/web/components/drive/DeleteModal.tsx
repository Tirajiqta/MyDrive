"use client";

import { useState } from "react";
import { toast } from "sonner";
import { foldersApi, filesApi, FolderResponse, FileResponse } from "@/lib/api";
import { Modal } from "@/components/ui/Modal";
import { Button } from "@/components/ui/Button";
import { AlertTriangle } from "lucide-react";

type Item =
  | { kind: "folder"; data: FolderResponse }
  | { kind: "file"; data: FileResponse };

interface DeleteModalProps {
  open: boolean;
  onClose: () => void;
  item: Item | null;
  onDeleted: () => void;
}

export function DeleteModal({
  open,
  onClose,
  item,
  onDeleted,
}: DeleteModalProps) {
  const [loading, setLoading] = useState(false);

  const name =
    item?.kind === "folder"
      ? item.data.translatedName || item.data.canonicalName
      : item?.data.originalFileName;

  const handleDelete = async () => {
    if (!item) return;
    setLoading(true);
    try {
      if (item.kind === "folder") {
        await foldersApi.delete(item.data.id);
      } else {
        await filesApi.delete(item.data.id);
      }
      toast.success("Deleted successfully");
      onDeleted();
      onClose();
    } catch (err) {
      toast.error(err instanceof Error ? err.message : "Delete failed");
    } finally {
      setLoading(false);
    }
  };

  return (
    <Modal open={open} onClose={onClose} title="Delete" maxWidth="sm">
      <div className="flex flex-col gap-5">
        <div className="flex items-start gap-3">
          <AlertTriangle className="w-5 h-5 text-red-500 flex-shrink-0 mt-0.5" />
          <p className="text-sm text-gray-700">
            Are you sure you want to delete{" "}
            <span className="font-semibold">{name}</span>? This action cannot be
            undone.
          </p>
        </div>
        <div className="flex justify-end gap-3">
          <Button variant="secondary" type="button" onClick={onClose}>
            Cancel
          </Button>
          <Button variant="danger" onClick={handleDelete} loading={loading}>
            Delete
          </Button>
        </div>
      </div>
    </Modal>
  );
}
