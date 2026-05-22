"use client";

import { use, useEffect, useState } from "react";
import { publicSharesApi, ShareInfoResponse, formatDate } from "@/lib/api";
import { Cloud, File, Folder, AlertCircle } from "lucide-react";
import Link from "next/link";
import { Spinner } from "@/components/ui/Spinner";

interface Params {
  token: string;
}

export default function PublicSharePage({
  params,
}: {
  params: Promise<Params>;
}) {
  const { token } = use(params);
  const [share, setShare] = useState<ShareInfoResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    publicSharesApi
      .get(token)
      .then(setShare)
      .catch((err) =>
        setError(err instanceof Error ? err.message : "Share not found")
      )
      .finally(() => setLoading(false));
  }, [token]);

  const expired =
    share?.expiresAt && new Date(share.expiresAt) < new Date();

  return (
    <div className="min-h-screen bg-gray-50 flex flex-col">
      <header className="bg-white border-b border-gray-100 px-8 py-4">
        <Link href="/" className="flex items-center gap-2.5 w-fit">
          <div className="w-8 h-8 bg-indigo-600 rounded-lg flex items-center justify-center">
            <Cloud className="w-5 h-5 text-white" />
          </div>
          <span className="font-bold text-lg text-gray-900">MyDrive</span>
        </Link>
      </header>

      <main className="flex-1 flex items-center justify-center p-8">
        {loading ? (
          <Spinner className="w-10 h-10" />
        ) : error ? (
          <div className="bg-white rounded-2xl border border-gray-100 shadow-sm p-10 text-center max-w-md w-full">
            <div className="w-14 h-14 bg-red-50 rounded-full flex items-center justify-center mx-auto mb-4">
              <AlertCircle className="w-7 h-7 text-red-500" />
            </div>
            <h2 className="font-semibold text-gray-900 text-lg mb-2">
              Share not found
            </h2>
            <p className="text-sm text-gray-500 mb-6">{error}</p>
            <Link
              href="/"
              className="text-indigo-600 text-sm font-medium hover:underline"
            >
              Go to MyDrive
            </Link>
          </div>
        ) : share ? (
          <div className="bg-white rounded-2xl border border-gray-100 shadow-sm p-10 text-center max-w-md w-full">
            <div
              className={`w-16 h-16 rounded-2xl flex items-center justify-center mx-auto mb-5 ${
                share.itemType === "FOLDER" ? "bg-amber-50" : "bg-indigo-50"
              }`}
            >
              {share.itemType === "FOLDER" ? (
                <Folder className="w-8 h-8 text-amber-500" />
              ) : (
                <File className="w-8 h-8 text-indigo-500" />
              )}
            </div>

            <h2 className="font-semibold text-gray-900 text-xl mb-1">
              Shared {share.itemType === "FOLDER" ? "Folder" : "File"}
            </h2>

            {expired ? (
              <div className="mt-4 flex items-center justify-center gap-2 text-sm text-red-600 bg-red-50 px-4 py-3 rounded-xl">
                <AlertCircle className="w-4 h-4" />
                This share link has expired
              </div>
            ) : (
              <div className="mt-6 flex flex-col gap-3 text-sm text-left bg-gray-50 rounded-xl p-4">
                <Row label="Type" value={share.itemType} />
                <Row label="Permission" value={share.permissionLevel} />
                <Row
                  label="Shared on"
                  value={formatDate(share.sharedDate)}
                />
                {share.expiresAt && (
                  <Row
                    label="Expires"
                    value={formatDate(share.expiresAt)}
                  />
                )}
              </div>
            )}

            <Link
              href="/login"
              className="mt-6 inline-block px-6 py-2.5 bg-indigo-600 text-white text-sm font-medium rounded-xl hover:bg-indigo-700 transition-colors"
            >
              Sign in to access
            </Link>
          </div>
        ) : null}
      </main>
    </div>
  );
}

function Row({ label, value }: { label: string; value: string }) {
  return (
    <div className="flex justify-between">
      <span className="text-gray-500">{label}</span>
      <span className="font-medium text-gray-800">{value}</span>
    </div>
  );
}
