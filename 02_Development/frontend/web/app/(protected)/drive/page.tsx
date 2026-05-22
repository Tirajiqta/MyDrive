import { DriveView } from "@/components/drive/DriveView";

export default function DrivePage() {
  return (
    <div className="flex flex-col h-full">
      <div className="flex items-center justify-between px-8 py-5 bg-white border-b border-gray-100">
        <h1 className="text-xl font-semibold text-gray-900">My Drive</h1>
      </div>
      <DriveView folderId={null} breadcrumb={[]} />
    </div>
  );
}
