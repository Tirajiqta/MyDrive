"use client";

import { useState } from "react";
import { toast } from "sonner";
import { foldersApi } from "@/lib/api";
import { Modal } from "@/components/ui/Modal";
import { Input } from "@/components/ui/Input";
import { Button } from "@/components/ui/Button";

interface CreateFolderModalProps {
  open: boolean;
  onClose: () => void;
  parentId?: number | null;
  onCreated: () => void;
}

export function CreateFolderModal({
  open,
  onClose,
  parentId,
  onCreated,
}: CreateFolderModalProps) {
  const [name, setName] = useState("");
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!name.trim()) return;
    setLoading(true);
    try {
      await foldersApi.create({ canonicalName: name.trim(), parentId });
      toast.success("Folder created");
      setName("");
      onCreated();
      onClose();
    } catch (err) {
      toast.error(err instanceof Error ? err.message : "Failed to create folder");
    } finally {
      setLoading(false);
    }
  };

  return (
    <Modal open={open} onClose={onClose} title="New Folder" maxWidth="sm">
      <form onSubmit={handleSubmit} className="flex flex-col gap-4">
        <Input
          label="Folder name"
          value={name}
          onChange={(e) => setName(e.target.value)}
          placeholder="Untitled folder"
          autoFocus
        />
        <div className="flex justify-end gap-3">
          <Button variant="secondary" type="button" onClick={onClose}>
            Cancel
          </Button>
          <Button type="submit" loading={loading} disabled={!name.trim()}>
            Create
          </Button>
        </div>
      </form>
    </Modal>
  );
}
