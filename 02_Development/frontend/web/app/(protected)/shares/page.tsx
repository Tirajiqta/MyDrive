"use client";

import { useState, useEffect } from "react";
import { toast } from "sonner";
import { sharesApi, ShareListItemResponse, formatDate } from "@/lib/api";
import { Header } from "@/components/layout/Header";
import { Button } from "@/components/ui/Button";
import { Spinner } from "@/components/ui/Spinner";
import { Link2, Trash2, File, Folder, AlertCircle } from "lucide-react";

export default function SharesPage() {
  const [shares, setShares] = useState<ShareListItemResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [revoking, setRevoking] = useState<number | null>(null);

  const load = async () => {
    try {
      const data = await sharesApi.list();
      setShares(data);
    } catch {
      toast.error("Failed to load shares");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { load(); }, []);

  const handleRevoke = async (share: ShareListItemResponse) => {
    setRevoking(share.shareId);
    try {
      await sharesApi.revoke(share.shareId);
      toast.success("Share revoked");
      setShares((prev) => prev.filter((s) => s.shareId !== share.shareId));
    } catch (err) {
      toast.error(err instanceof Error ? err.message : "Revoke failed");
    } finally {
      setRevoking(null);
    }
  };

  const active = shares.filter((s) => !s.revoked);
  const revoked = shares.filter((s) => s.revoked);

  return (
    <div className="flex flex-col h-full">
      <Header title="Shared Links" />

      <div className="px-8 py-6 flex-1 overflow-y-auto">
        {loading ? (
          <div className="flex justify-center pt-16">
            <Spinner />
          </div>
        ) : shares.length === 0 ? (
          <div className="flex flex-col items-center justify-center pt-24 text-center">
            <div className="w-16 h-16 bg-gray-100 rounded-2xl flex items-center justify-center mb-4">
              <Link2 className="w-8 h-8 text-gray-400" />
            </div>
            <p className="text-gray-500 font-medium">No shared links yet</p>
            <p className="text-gray-400 text-sm mt-1">
              Share a file or folder from your drive to see it here.
            </p>
          </div>
        ) : (
          <div className="flex flex-col gap-8">
            {active.length > 0 && (
              <section>
                <p className="text-xs font-semibold text-gray-400 uppercase tracking-wide mb-3">
                  Active ({active.length})
                </p>
                <div className="bg-white rounded-xl border border-gray-100 overflow-hidden divide-y divide-gray-50">
                  {active.map((s) => (
                    <ShareRow
                      key={s.shareId}
                      share={s}
                      onRevoke={handleRevoke}
                      revoking={revoking === s.shareId}
                    />
                  ))}
                </div>
              </section>
            )}

            {revoked.length > 0 && (
              <section>
                <p className="text-xs font-semibold text-gray-400 uppercase tracking-wide mb-3">
                  Revoked ({revoked.length})
                </p>
                <div className="bg-white rounded-xl border border-gray-100 overflow-hidden divide-y divide-gray-50 opacity-60">
                  {revoked.map((s) => (
                    <ShareRow
                      key={s.shareId}
                      share={s}
                      onRevoke={handleRevoke}
                      revoking={false}
                    />
                  ))}
                </div>
              </section>
            )}
          </div>
        )}
      </div>
    </div>
  );
}

function ShareRow({
  share,
  onRevoke,
  revoking,
}: {
  share: ShareListItemResponse;
  onRevoke: (s: ShareListItemResponse) => void;
  revoking: boolean;
}) {
  const expired =
    share.expiresAt && new Date(share.expiresAt) < new Date();

  return (
    <div className="flex items-center gap-4 px-5 py-4">
      <div className="w-9 h-9 bg-gray-50 rounded-lg flex items-center justify-center flex-shrink-0">
        {share.itemType === "FOLDER" ? (
          <Folder className="w-5 h-5 text-amber-500" />
        ) : (
          <File className="w-5 h-5 text-indigo-500" />
        )}
      </div>

      <div className="flex-1 min-w-0">
        <div className="flex items-center gap-2">
          <span className="text-sm font-medium text-gray-900 truncate">
            {share.itemType} #{share.itemId}
          </span>
          <span
            className={`px-2 py-0.5 rounded-full text-xs font-medium
              ${share.permissionLevel === "VIEW"
                ? "bg-gray-100 text-gray-600"
                : "bg-blue-100 text-blue-700"
              }`}
          >
            {share.permissionLevel}
          </span>
          {(expired || share.revoked) && (
            <span className="px-2 py-0.5 rounded-full text-xs font-medium bg-red-100 text-red-600 flex items-center gap-1">
              <AlertCircle className="w-3 h-3" />
              {share.revoked ? "Revoked" : "Expired"}
            </span>
          )}
        </div>
        <p className="text-xs text-gray-400 mt-0.5">
          Created {formatDate(share.sharedDate)}
          {share.expiresAt && ` · Expires ${formatDate(share.expiresAt)}`}
        </p>
      </div>

      {!share.revoked && (
        <Button
          variant="ghost"
          size="sm"
          onClick={() => onRevoke(share)}
          loading={revoking}
          className="text-red-500 hover:text-red-600 hover:bg-red-50"
        >
          <Trash2 className="w-4 h-4" />
        </Button>
      )}
    </div>
  );
}
