"use client";

import { useState, useEffect } from "react";
import { signingApi, SignatureInfo } from "@/lib/api";
import { X, ShieldCheck, Download, Loader2, AlertCircle, CheckCircle2 } from "lucide-react";

interface SignDocumentModalProps {
  fileId: number;
  fileName: string;
  onClose: () => void;
}

export function SignDocumentModal({ fileId, fileName, onClose }: SignDocumentModalProps) {
  const [signatures, setSignatures] = useState<SignatureInfo[]>([]);
  const [signing, setSigning] = useState(false);
  const [newSig, setNewSig] = useState<SignatureInfo | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    signingApi.getSignatures(fileId).then(setSignatures).catch(() => {});
  }, [fileId]);

  async function handleSign() {
    setSigning(true);
    setError(null);
    try {
      const sig = await signingApi.sign(fileId);
      setNewSig(sig);
      setSignatures((prev) => [sig, ...prev]);
    } catch {
      setError("Signing failed. Make sure the file is a PDF.");
    } finally {
      setSigning(false);
    }
  }

  return (
    <div className="fixed inset-0 z-50 bg-black/50 flex items-center justify-center p-4">
      <div className="bg-white rounded-2xl shadow-2xl w-full max-w-lg">
        {/* Header */}
        <div className="flex items-center justify-between px-6 py-4 border-b border-gray-100">
          <div className="flex items-center gap-2.5">
            <ShieldCheck className="w-5 h-5 text-indigo-600" />
            <h2 className="text-lg font-semibold text-gray-900">Sign Document</h2>
          </div>
          <button
            onClick={onClose}
            className="p-1.5 rounded-lg text-gray-400 hover:text-gray-700 hover:bg-gray-100 transition-colors"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        <div className="px-6 py-5 space-y-5">
          {/* File info */}
          <div className="bg-gray-50 rounded-xl p-4">
            <p className="text-sm text-gray-500 mb-1">Document to sign</p>
            <p className="text-sm font-medium text-gray-900 truncate">{fileName}</p>
          </div>

          {/* eIDAS info */}
          <div className="bg-indigo-50 rounded-xl p-4 space-y-1.5">
            <p className="text-xs font-semibold text-indigo-700 uppercase tracking-wide">
              eIDAS – Advanced Electronic Signature
            </p>
            <p className="text-xs text-indigo-600">
              Produces a <strong>PAdES-B-B</strong> signature embedded in the PDF, compliant
              with the eIDAS Regulation (EU) No 910/2014. For a Qualified Electronic Signature
              (QES), replace the demo certificate with one issued by a qualified trust service
              provider (QTSP).
            </p>
          </div>

          {/* Error */}
          {error && (
            <div className="flex items-center gap-2 text-sm text-red-600 bg-red-50 rounded-xl px-4 py-3">
              <AlertCircle className="w-4 h-4 flex-shrink-0" />
              {error}
            </div>
          )}

          {/* New signature success */}
          {newSig && (
            <div className="flex items-center gap-2 text-sm text-green-700 bg-green-50 rounded-xl px-4 py-3">
              <CheckCircle2 className="w-4 h-4 flex-shrink-0" />
              Signed successfully!
              <a
                href={signingApi.downloadUrl(newSig.id)}
                download
                className="ml-auto text-xs bg-green-600 text-white px-3 py-1.5 rounded-lg flex items-center gap-1 hover:bg-green-700 transition-colors"
              >
                <Download className="w-3.5 h-3.5" />
                Download
              </a>
            </div>
          )}

          {/* Sign button */}
          <button
            onClick={handleSign}
            disabled={signing}
            className="w-full flex items-center justify-center gap-2 bg-indigo-600 hover:bg-indigo-700 disabled:bg-indigo-300 text-white py-2.5 rounded-xl text-sm font-medium transition-colors"
          >
            {signing ? (
              <>
                <Loader2 className="w-4 h-4 animate-spin" />
                Signing…
              </>
            ) : (
              <>
                <ShieldCheck className="w-4 h-4" />
                Sign with PAdES-B-B
              </>
            )}
          </button>

          {/* Previous signatures */}
          {signatures.length > 0 && (
            <div>
              <p className="text-xs font-semibold text-gray-500 uppercase tracking-wide mb-2">
                Signature history
              </p>
              <div className="space-y-2">
                {signatures.map((sig) => (
                  <div
                    key={sig.id}
                    className="flex items-center justify-between bg-gray-50 rounded-xl px-4 py-3"
                  >
                    <div>
                      <p className="text-sm font-medium text-gray-800">{sig.signerName}</p>
                      <p className="text-xs text-gray-500">
                        {sig.signatureLevel} · {new Date(sig.signedAt).toLocaleString()}
                      </p>
                    </div>
                    <a
                      href={signingApi.downloadUrl(sig.id)}
                      download
                      className="text-indigo-600 hover:text-indigo-800 transition-colors"
                    >
                      <Download className="w-4 h-4" />
                    </a>
                  </div>
                ))}
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
