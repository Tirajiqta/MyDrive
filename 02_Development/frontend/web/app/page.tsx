import Link from "next/link";
import { Cloud, Shield, Share2, Zap } from "lucide-react";

export default function HomePage() {
  return (
    <div className="min-h-screen bg-gradient-to-br from-indigo-50 via-white to-indigo-50">
      {/* Nav */}
      <header className="flex items-center justify-between px-8 py-5 max-w-6xl mx-auto">
        <div className="flex items-center gap-2.5">
          <div className="w-8 h-8 bg-indigo-600 rounded-lg flex items-center justify-center">
            <Cloud className="w-5 h-5 text-white" />
          </div>
          <span className="font-bold text-xl text-gray-900">MyDrive</span>
        </div>
        <div className="flex items-center gap-4">
          <Link
            href="/login"
            className="text-sm font-medium text-gray-600 hover:text-indigo-600 transition-colors"
          >
            Sign in
          </Link>
          <Link
            href="/register"
            className="px-4 py-2 bg-indigo-600 text-white text-sm font-medium rounded-lg hover:bg-indigo-700 transition-colors"
          >
            Get started
          </Link>
        </div>
      </header>

      {/* Hero */}
      <main className="max-w-6xl mx-auto px-8 pt-20 pb-32 text-center">
        <div className="inline-flex items-center gap-2 px-4 py-2 bg-indigo-100 rounded-full text-indigo-700 text-sm font-medium mb-8">
          <Zap className="w-4 h-4" />
          Fast, secure cloud storage
        </div>

        <h1 className="text-5xl sm:text-6xl font-bold text-gray-900 leading-tight mb-6">
          The only cloud storage
          <br />
          <span className="text-indigo-600">solution you need</span>
        </h1>

        <p className="text-lg text-gray-500 max-w-2xl mx-auto mb-10">
          Store, organize, and share your files with ease. Built for individuals
          who value simplicity, speed, and security.
        </p>

        <div className="flex items-center justify-center gap-4">
          <Link
            href="/register"
            className="px-6 py-3 bg-indigo-600 text-white font-semibold rounded-xl hover:bg-indigo-700 transition-colors shadow-lg shadow-indigo-200"
          >
            Start for free
          </Link>
          <Link
            href="/login"
            className="px-6 py-3 bg-white text-gray-700 font-semibold rounded-xl border border-gray-200 hover:bg-gray-50 transition-colors"
          >
            Sign in
          </Link>
        </div>

        {/* Features */}
        <div className="grid grid-cols-1 sm:grid-cols-3 gap-6 mt-24">
          {[
            {
              icon: <Cloud className="w-6 h-6 text-indigo-600" />,
              title: "Unlimited organisation",
              desc: "Create nested folders and organise your files exactly the way you want.",
            },
            {
              icon: <Share2 className="w-6 h-6 text-indigo-600" />,
              title: "Easy sharing",
              desc: "Share files and folders with a secure link. Set permissions and expiry dates.",
            },
            {
              icon: <Shield className="w-6 h-6 text-indigo-600" />,
              title: "Secure by default",
              desc: "JWT-based authentication with refresh tokens keeps your data safe.",
            },
          ].map((f) => (
            <div
              key={f.title}
              className="bg-white rounded-2xl p-6 border border-gray-100 shadow-sm text-left"
            >
              <div className="w-12 h-12 bg-indigo-50 rounded-xl flex items-center justify-center mb-4">
                {f.icon}
              </div>
              <h3 className="font-semibold text-gray-900 mb-2">{f.title}</h3>
              <p className="text-sm text-gray-500 leading-relaxed">{f.desc}</p>
            </div>
          ))}
        </div>
      </main>
    </div>
  );
}
