"use client";

import { useState, useEffect } from "react";
import { toast } from "sonner";
import { filesApi, foldersApi, FileResponse, FolderResponse } from "@/lib/api";
import { Modal } from "@/components/ui/Modal";
import { Button } from "@/components/ui/Button";
import { Folder } from "lucide-react";

interface MoveModalProps {
  open: boolean;
  onClose: () => void;
  file: FileResponse | null;
  onMoved: () => void;
}

export function MoveModal({ open, onClose, file, onMoved }: MoveModalProps) {
  const [folders, setFolders] = useState<FolderResponse[]>([]);
  const [selected, setSelected] = useState<number | null>(null);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    if (!open) return;
    foldersApi
      .list(null)
      .then(setFolders)
      .catch(() => toast.error("Could not load folders"));
  }, [open]);

  const handleMove = async () => {
    if (!file || selected == null) return;
    setLoading(true);
    try {
      await filesApi.move(file.id, selected);
      toast.success("File moved");
      onMoved();
      onClose();
    } catch (err) {
      toast.error(err instanceof Error ? err.message : "Move failed");
    } finally {
      setLoading(false);
    }
  };

  return (
    <Modal open={open} onClose={onClose} title="Move to folder">
      <div className="flex flex-col gap-4">
        <p className="text-sm text-gray-600">
          Moving:{" "}
          <span className="font-medium">{file?.originalFileName}</span>
        </p>

        <div className="max-h-56 overflow-y-auto flex flex-col gap-1 border border-gray-100 rounded-lg p-2">
          {folders.length === 0 ? (
            <p className="text-sm text-gray-400 text-center py-4">
              No folders available
            </p>
          ) : (
            folders.map((f) => (
              <button
                key={f.id}
                type="button"
                onClick={() => setSelected(f.id)}
                className={`flex items-center gap-3 px-3 py-2.5 rounded-lg text-sm transition-colors text-left
                  ${selected === f.id
                    ? "bg-indigo-50 text-indigo-700 border border-indigo-200"
                    : "hover:bg-gray-50 text-gray-700"
                  }`}
              >
                <Folder className="w-4 h-4 text-amber-500 flex-shrink-0" />
                {f.translatedName || f.canonicalName}
              </button>
            ))
          )}
        </div>

        <div className="flex justify-end gap-3">
          <Button variant="secondary" onClick={onClose}>
            Cancel
          </Button>
          <Button
            onClick={handleMove}
            loading={loading}
            disabled={selected == null}
          >
            Move here
          </Button>
        </div>
      </div>
    </Modal>
  );
}
