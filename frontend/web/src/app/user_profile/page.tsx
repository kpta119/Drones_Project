
"use client";

import Image from "next/image";

export default function ProfilePage() {
    return (
        <div className="min-h-screen flex justify-center">
            <div className="w-full max-w-5xl bg-white shadow-lg rounded-lg p-6">
                {/* Header */}
                <div className="flex items-center space-x-6 mb-6">
                    <div className="w-24 h-24 relative">
                        <Image
                            src="/dron.png" // replace with actual image or avatar
                            alt="Profile Avatar"
                            fill
                            className="rounded-full object-cover"
                        />
                    </div>
                    <div>
                        <h1 className="text-2xl font-bold">John Doe</h1>
                        <p className="text-gray-500">johndoe@example.com</p>
                    </div>
                </div>
                <div className="space-y-4">
                    <div className="bg-gray-50 p-4 rounded">
                        <h2 className="font-semibold text-lg mb-1">About Me</h2>
                        <p className="text-gray-600">
                            This is a placeholder bio. You can update it with user information.
                        </p>
                    </div>

                </div>

            </div>
        </div>
    );
}
