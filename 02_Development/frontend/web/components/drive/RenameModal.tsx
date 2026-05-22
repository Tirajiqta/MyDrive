"use client";

import { useState, useEffect } from "react";
import { toast } from "sonner";
import { foldersApi, filesApi, FolderResponse, FileResponse } from "@/lib/api";
import { Modal } from "@/components/ui/Modal";
import { Input } from "@/components/ui/Input";
import { Button } from "@/components/ui/Button";

type Item =
  | { kind: "folder"; data: FolderResponse }
  | { kind: "file"; data: FileResponse };

interface RenameModalProps {
  open: boolean;
  onClose: () => void;
  item: Item | null;
  onRenamed: () => void;
}

export function RenameModal({
  open,
  onClose,
  item,
  onRenamed,
}: RenameModalProps) {
  const [name, setName] = useState("");
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    if (item) {
      setName(
        item.kind === "folder"
          ? item.data.canonicalName
          : item.data.originalFileName
      );
    }
  }, [item]);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!name.trim() || !item) return;
    setLoading(true);
    try {
      if (item.kind === "folder") {
        await foldersApi.update(item.data.id, {
          canonicalName: name.trim(),
          parentId: item.data.parentId,
        });
      } else {
        await filesApi.update(item.data.id, { name: name.trim() });
      }
      toast.success("Renamed successfully");
      onRenamed();
      onClose();
    } catch (err) {
      toast.error(err instanceof Error ? err.message : "Rename failed");
    } finally {
      setLoading(false);
    }
  };

  return (
    <Modal open={open} onClose={onClose} title="Rename" maxWidth="sm">
      <form onSubmit={handleSubmit} className="flex flex-col gap-4">
        <Input
          label="New name"
          value={name}
          onChange={(e) => setName(e.target.value)}
          autoFocus
          onFocus={(e) => e.target.select()}
        />
        <div className="flex justify-end gap-3">
          <Button variant="secondary" type="button" onClick={onClose}>
            Cancel
          </Button>
          <Button type="submit" loading={loading} disabled={!name.trim()}>
            Rename
          </Button>
        </div>
      </form>
    </Modal>
  );
}
