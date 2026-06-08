"use client";

import { useState, useRef } from "react";
import { toast } from "sonner";
import { filesApi } from "@/lib/api";
import { Modal } from "@/components/ui/Modal";
import { Button } from "@/components/ui/Button";
import { Upload, File } from "lucide-react";

interface UploadModalProps {
  open: boolean;
  onClose: () => void;
  folderId: number | null;
  onUploaded: () => void;
}

export function UploadModal({
  open,
  onClose,
  folderId,
  onUploaded,
}: UploadModalProps) {
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [loading, setLoading] = useState(false);
  const [dragging, setDragging] = useState(false);
  const inputRef = useRef<HTMLInputElement>(null);

  const handleFile = (f: File) => setSelectedFile(f);

  const handleDrop = (e: React.DragEvent) => {
    e.preventDefault();
    setDragging(false);
    const f = e.dataTransfer.files[0];
    if (f) handleFile(f);
  };

  const handleUpload = async () => {
    if (!selectedFile || folderId == null) return;
    setLoading(true);
    try {
      await filesApi.upload(selectedFile, folderId);
      toast.success(`${selectedFile.name} added`);
      setSelectedFile(null);
      onUploaded();
      onClose();
    } catch (err) {
      toast.error(err instanceof Error ? err.message : "Upload failed");
    } finally {
      setLoading(false);
    }
  };

  const handleClose = () => {
    setSelectedFile(null);
    onClose();
  };

  return (
    <Modal open={open} onClose={handleClose} title="Upload File">
      <div className="flex flex-col gap-5">
        <div
          onDragOver={(e) => { e.preventDefault(); setDragging(true); }}
          onDragLeave={() => setDragging(false)}
          onDrop={handleDrop}
          onClick={() => inputRef.current?.click()}
          className={`flex flex-col items-center justify-center gap-3 border-2 border-dashed rounded-xl p-8 cursor-pointer transition-colors
            ${dragging ? "border-indigo-500 bg-indigo-50" : "border-gray-200 hover:border-indigo-400 hover:bg-gray-50"}`}
        >
          <Upload className="w-8 h-8 text-gray-400" />
          <div className="text-center">
            <p className="text-sm font-medium text-gray-700">
              Click or drag & drop to upload
            </p>
            <p className="text-xs text-gray-400 mt-1">Any file type</p>
          </div>
          <input
            ref={inputRef}
            type="file"
            className="hidden"
            onChange={(e) => {
              const f = e.target.files?.[0];
              if (f) handleFile(f);
            }}
          />
        </div>

        {selectedFile && (
          <div className="flex items-center gap-3 p-3 bg-gray-50 rounded-lg">
            <File className="w-5 h-5 text-indigo-500 flex-shrink-0" />
            <div className="min-w-0">
              <p className="text-sm font-medium truncate">{selectedFile.name}</p>
              <p className="text-xs text-gray-400">
                {(selectedFile.size / 1024).toFixed(1)} KB
              </p>
            </div>
          </div>
        )}

        {folderId == null && (
          <p className="text-xs text-amber-600 bg-amber-50 px-3 py-2 rounded-lg">
            Please open a folder before uploading files.
          </p>
        )}

        <div className="flex justify-end gap-3">
          <Button variant="secondary" onClick={handleClose}>
            Cancel
          </Button>
          <Button
            onClick={handleUpload}
            loading={loading}
            disabled={!selectedFile || folderId == null}
          >
            Upload
          </Button>
        </div>
      </div>
    </Modal>
  );
}
