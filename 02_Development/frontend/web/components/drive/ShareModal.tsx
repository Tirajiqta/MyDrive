"use client";

import { useState } from "react";
import { toast } from "sonner";
import { sharesApi, FolderResponse, FileResponse } from "@/lib/api";
import { Modal } from "@/components/ui/Modal";
import { Button } from "@/components/ui/Button";
import { Copy, Check } from "lucide-react";

type Item =
  | { kind: "folder"; data: FolderResponse }
  | { kind: "file"; data: FileResponse };

interface ShareModalProps {
  open: boolean;
  onClose: () => void;
  item: Item | null;
}

export function ShareModal({ open, onClose, item }: ShareModalProps) {
  const [permission, setPermission] = useState<"VIEW" | "EDIT">("VIEW");
  const [expiresAt, setExpiresAt] = useState("");
  const [loading, setLoading] = useState(false);
  const [shareToken, setShareToken] = useState<string | null>(null);
  const [copied, setCopied] = useState(false);

  const handleCreate = async () => {
    if (!item) return;
    setLoading(true);
    try {
      const res = await sharesApi.create({
        itemId: item.data.id,
        itemType: item.kind === "folder" ? "FOLDER" : "FILE",
        permissionLevel: permission,
        expiresAt: expiresAt || null,
      });
      setShareToken(res.token);
      toast.success("Share link created");
    } catch (err) {
      toast.error(err instanceof Error ? err.message : "Failed to create share link");
    } finally {
      setLoading(false);
    }
  };

  const shareUrl = shareToken
    ? `${window.location.origin}/share/${shareToken}`
    : null;

  const handleCopy = async () => {
    if (!shareUrl) return;
    await navigator.clipboard.writeText(shareUrl);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  const handleClose = () => {
    setShareToken(null);
    setCopied(false);
    setExpiresAt("");
    setPermission("VIEW");
    onClose();
  };

  const name =
    item?.kind === "folder"
      ? item.data.translatedName || item.data.canonicalName
      : item?.data.originalFileName;

  return (
    <Modal open={open} onClose={handleClose} title="Share" maxWidth="md">
      <div className="flex flex-col gap-5">
        <p className="text-sm text-gray-600">
          Sharing: <span className="font-medium text-gray-900">{name}</span>
        </p>

        {!shareToken ? (
          <>
            <div className="flex flex-col gap-3">
              <label className="text-sm font-medium text-gray-700">
                Permission level
              </label>
              <div className="flex gap-3">
                {(["VIEW", "EDIT"] as const).map((p) => (
                  <button
                    key={p}
                    type="button"
                    onClick={() => setPermission(p)}
                    className={`flex-1 py-2 rounded-lg border text-sm font-medium transition-colors
                      ${permission === p
                        ? "border-indigo-600 bg-indigo-50 text-indigo-700"
                        : "border-gray-200 text-gray-600 hover:bg-gray-50"
                      }`}
                  >
                    {p === "VIEW" ? "View only" : "Can edit"}
                  </button>
                ))}
              </div>
            </div>

            <div className="flex flex-col gap-1">
              <label className="text-sm font-medium text-gray-700">
                Expiry date{" "}
                <span className="text-gray-400 font-normal">(optional)</span>
              </label>
              <input
                type="datetime-local"
                value={expiresAt}
                onChange={(e) => setExpiresAt(e.target.value)}
                className="w-full px-3 py-2 rounded-lg border border-gray-300 text-sm focus:outline-none focus:border-indigo-500 focus:ring-2 focus:ring-indigo-100"
              />
            </div>

            <div className="flex justify-end gap-3">
              <Button variant="secondary" onClick={handleClose}>
                Cancel
              </Button>
              <Button onClick={handleCreate} loading={loading}>
                Create link
              </Button>
            </div>
          </>
        ) : (
          <>
            <div className="flex flex-col gap-2">
              <label className="text-sm font-medium text-gray-700">
                Share link
              </label>
              <div className="flex gap-2">
                <input
                  readOnly
                  value={shareUrl ?? ""}
                  className="flex-1 px-3 py-2 rounded-lg border border-gray-200 text-sm bg-gray-50 text-gray-600"
                />
                <button
                  onClick={handleCopy}
                  className="px-3 py-2 rounded-lg border border-gray-200 hover:bg-gray-50 transition-colors"
                >
                  {copied ? (
                    <Check className="w-4 h-4 text-green-600" />
                  ) : (
                    <Copy className="w-4 h-4 text-gray-500" />
                  )}
                </button>
              </div>
              <p className="text-xs text-gray-400">
                {copied ? "Copied!" : "Click the icon to copy the link"}
              </p>
            </div>

            <div className="flex justify-end">
              <Button onClick={handleClose}>Done</Button>
            </div>
          </>
        )}
      </div>
    </Modal>
  );
}
