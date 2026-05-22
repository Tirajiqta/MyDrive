"use client";

import { useEffect, useRef, useState, useCallback } from "react";
import { useEditor, EditorContent } from "@tiptap/react";
import StarterKit from "@tiptap/starter-kit";
import Placeholder from "@tiptap/extension-placeholder";
import { Client } from "@stomp/stompjs";
import SockJS from "sockjs-client";
import { documentsApi, API_BASE_URL } from "@/lib/api";
import { X, Users, Wifi, WifiOff } from "lucide-react";

interface DocumentEditorProps {
  fileId: number;
  fileName: string;
  editorUsername: string;
  onClose: () => void;
}

interface EditMessage {
  editorUsername: string;
  content: string;
  version: number;
}

export function DocumentEditor({
  fileId,
  fileName,
  editorUsername,
  onClose,
}: DocumentEditorProps) {
  const stompClient = useRef<Client | null>(null);
  const [connected, setConnected] = useState(false);
  const [activeEditors, setActiveEditors] = useState<string[]>([]);
  const versionRef = useRef(0);
  const isRemoteUpdate = useRef(false);

  const editor = useEditor({
    extensions: [
      StarterKit,
      Placeholder.configure({ placeholder: "Start typing…" }),
    ],
    content: "",
    onUpdate: ({ editor }) => {
      if (isRemoteUpdate.current) return;
      debouncedSend(editor.getHTML());
    },
  });

  const sendTimeout = useRef<ReturnType<typeof setTimeout> | null>(null);

  const debouncedSend = useCallback(
    (content: string) => {
      if (sendTimeout.current) clearTimeout(sendTimeout.current);
      sendTimeout.current = setTimeout(() => {
        if (!stompClient.current?.connected) return;
        versionRef.current += 1;
        const msg: EditMessage = {
          editorUsername,
          content,
          version: versionRef.current,
        };
        stompClient.current.publish({
          destination: `/app/document/${fileId}/edit`,
          body: JSON.stringify(msg),
        });
      }, 300);
    },
    [editorUsername, fileId]
  );

  useEffect(() => {
    // Load saved content first
    documentsApi.getContent(fileId).then((data) => {
      if (data.content && editor) {
        isRemoteUpdate.current = true;
        editor.commands.setContent(data.content);
        isRemoteUpdate.current = false;
      }
    });

    // Connect to STOMP WebSocket
    const client = new Client({
      webSocketFactory: () => new SockJS(`${API_BASE_URL}/ws`),
      reconnectDelay: 5000,
      onConnect: () => {
        setConnected(true);
        client.subscribe(`/topic/document/${fileId}`, (frame) => {
          const msg: EditMessage = JSON.parse(frame.body);
          if (msg.editorUsername === editorUsername) return;

          // Track active editors
          setActiveEditors((prev) =>
            prev.includes(msg.editorUsername)
              ? prev
              : [...prev, msg.editorUsername]
          );

          // Apply remote content only if newer
          if (msg.version > versionRef.current && editor) {
            versionRef.current = msg.version;
            isRemoteUpdate.current = true;
            const { from, to } = editor.state.selection;
            editor.commands.setContent(msg.content, false);
            editor.commands.setTextSelection({ from, to });
            isRemoteUpdate.current = false;
          }
        });

        // Announce presence
        client.publish({
          destination: `/app/document/${fileId}/edit`,
          body: JSON.stringify({
            editorUsername,
            content: editor?.getHTML() ?? "",
            version: versionRef.current,
          }),
        });
      },
      onDisconnect: () => setConnected(false),
    });

    stompClient.current = client;
    client.activate();

    return () => {
      client.deactivate();
    };
  }, [fileId, editorUsername, editor]);

  return (
    <div className="fixed inset-0 z-50 bg-black/50 flex items-center justify-center p-4">
      <div className="bg-white rounded-2xl shadow-2xl w-full max-w-4xl h-[80vh] flex flex-col">
        {/* Header */}
        <div className="flex items-center justify-between px-6 py-4 border-b border-gray-100">
          <div className="flex items-center gap-3">
            <h2 className="text-lg font-semibold text-gray-900 truncate max-w-sm">
              {fileName}
            </h2>
            <span className="text-xs bg-indigo-50 text-indigo-600 px-2 py-0.5 rounded-full">
              Live Editing
            </span>
          </div>

          <div className="flex items-center gap-3">
            {/* Connection status */}
            <div className="flex items-center gap-1.5 text-xs text-gray-500">
              {connected ? (
                <Wifi className="w-3.5 h-3.5 text-green-500" />
              ) : (
                <WifiOff className="w-3.5 h-3.5 text-red-400" />
              )}
              {connected ? "Connected" : "Reconnecting…"}
            </div>

            {/* Active editors */}
            {activeEditors.length > 0 && (
              <div className="flex items-center gap-1.5 text-xs text-gray-500">
                <Users className="w-3.5 h-3.5" />
                {activeEditors.join(", ")} also editing
              </div>
            )}

            <button
              onClick={onClose}
              className="p-1.5 rounded-lg text-gray-400 hover:text-gray-700 hover:bg-gray-100 transition-colors"
            >
              <X className="w-5 h-5" />
            </button>
          </div>
        </div>

        {/* Toolbar */}
        <div className="flex items-center gap-1 px-4 py-2 border-b border-gray-100 bg-gray-50">
          {[
            { label: "B", action: () => editor?.chain().focus().toggleBold().run(), active: editor?.isActive("bold") },
            { label: "I", action: () => editor?.chain().focus().toggleItalic().run(), active: editor?.isActive("italic") },
            { label: "S", action: () => editor?.chain().focus().toggleStrike().run(), active: editor?.isActive("strike") },
            { label: "H1", action: () => editor?.chain().focus().toggleHeading({ level: 1 }).run(), active: editor?.isActive("heading", { level: 1 }) },
            { label: "H2", action: () => editor?.chain().focus().toggleHeading({ level: 2 }).run(), active: editor?.isActive("heading", { level: 2 }) },
            { label: "• List", action: () => editor?.chain().focus().toggleBulletList().run(), active: editor?.isActive("bulletList") },
            { label: "1. List", action: () => editor?.chain().focus().toggleOrderedList().run(), active: editor?.isActive("orderedList") },
          ].map((btn) => (
            <button
              key={btn.label}
              onClick={btn.action}
              className={`px-2.5 py-1 text-xs rounded-md font-medium transition-colors ${
                btn.active
                  ? "bg-indigo-100 text-indigo-700"
                  : "text-gray-600 hover:bg-gray-200"
              }`}
            >
              {btn.label}
            </button>
          ))}
        </div>

        {/* Editor area */}
        <div className="flex-1 overflow-y-auto p-6">
          <EditorContent
            editor={editor}
            className="prose max-w-none min-h-full focus:outline-none"
          />
        </div>
      </div>
    </div>
  );
}
