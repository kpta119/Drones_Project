"use client";

import { useEffect, useState, useCallback } from "react";

interface User {
  id: string;
  username: string;
  role: string;
  name: string;
  surname: string;
  email: string;
  phone_number: string;
}

interface ApiResponse<T> {
  content: T[];
  page: {
    totalPages: number;
  };
}

const getRoleColor = (role: string): string => {
  const roleColors: Record<string, string> = {
    CLIENT: "bg-blue-100",
    OPERATOR: "bg-purple-100",
    ADMIN: "bg-amber-100",
    BLOCKED: "bg-red-100",
  };
  return roleColors[role] || "bg-gray-100";
};

export default function AdminUsers() {
  const [users, setUsers] = useState<User[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [searchQuery, setSearchQuery] = useState("");
  const [tempSearchQuery, setTempSearchQuery] = useState("");
  const [roleFilter, setRoleFilter] = useState("");
  const [banningId, setBanningId] = useState<string | null>(null);
  const [confirmBanId, setConfirmBanId] = useState<string | null>(null);
  const [refreshTrigger, setRefreshTrigger] = useState(0);

  const fetchUsers = useCallback(async () => {
    try {
      setLoading(true);
      const token = localStorage.getItem("token");
      let url = `/api/admin/getUsers?page=${page}&size=20`;
      if (searchQuery) url += `&query=${encodeURIComponent(searchQuery)}`;
      if (roleFilter) url += `&role=${roleFilter}`;

      const response = await fetch(url, {
        method: "GET",
        headers: {
          "X-USER-TOKEN": `Bearer ${token}`,
          "Content-Type": "application/json",
        },
      });

      if (!response.ok)
        throw new Error(`Failed to fetch users: ${response.status}`);

      const data = (await response.json()) as ApiResponse<User>;
      setUsers(data.content || []);
      setTotalPages(data.page?.totalPages || 0);
      setError("");
    } catch (err: unknown) {
      const error = err instanceof Error ? err.message : "Failed to load users";
      setError(error);
    } finally {
      setLoading(false);
    }
  }, [page, roleFilter, searchQuery]);

  useEffect(() => {
    fetchUsers();
  }, [fetchUsers, refreshTrigger]);

  const handleBanUser = async (userId: string) => {
    try {
      setBanningId(userId);
      const token = localStorage.getItem("token");

      const response = await fetch(`/api/admin/banUser/${userId}`, {
        method: "PATCH",
        headers: {
          "X-USER-TOKEN": `Bearer ${token}`,
          "Content-Type": "application/json",
        },
      });

      if (!response.ok) throw new Error("Failed to ban user");
      setConfirmBanId(null);
      setPage(0);
      setRefreshTrigger((prev) => prev + 1);
    } catch (err: unknown) {
      const error = err instanceof Error ? err.message : "Failed to ban user";
      setError(error);
    } finally {
      setBanningId(null);
    }
  };

  if (loading)
    return <div className="text-gray-600">Ładowanie użytkowników...</div>;

  return (
    <div>
      <h1 className="text-3xl font-bold mb-6">Zarządzanie Użytkownikami</h1>
      {error && <div className="text-red-600 mb-5">Błąd: {error}</div>}

      <div className="mb-5 flex gap-2.5">
        <input
          type="text"
          placeholder="Szukaj użytkownika..."
          value={tempSearchQuery}
          onKeyDown={(e) => {
            if (e.key === "Enter") {
              setPage(0);
              setSearchQuery(tempSearchQuery);
            }
          }}
          onChange={(e) => setTempSearchQuery(e.target.value)}
          className="px-2 py-2 rounded border border-gray-300 flex-1"
        />
        <select
          value={roleFilter}
          onChange={(e) => {
            setRoleFilter(e.target.value);
            setPage(0);
          }}
          className="px-2 py-2 rounded border border-gray-300"
        >
          <option value="">Wszystkie role</option>
          <option value="CLIENT">Klient</option>
          <option value="OPERATOR">Operator</option>
          <option value="ADMIN">Admin</option>
          <option value="BLOCKED">Zablokowany</option>
        </select>
      </div>

      <div className="overflow-x-auto">
        <table className="w-full border-collapse mb-5">
          <thead>
            <tr className="bg-gray-100 border-b-2 border-gray-300">
              {[
                "Użytkownik",
                "Email",
                "Imię i Nazwisko",
                "Telefon",
                "Rola",
                "Akcje",
              ].map((col) => (
                <th
                  key={col}
                  className="px-2.5 py-2.5 text-left border-b border-gray-300"
                >
                  {col}
                </th>
              ))}
            </tr>
          </thead>
          <tbody>
            {users.map((user) => (
              <tr key={user.id} className="border-b border-gray-300">
                <td className="px-2.5 py-2.5">{user.username}</td>
                <td className="px-2.5 py-2.5">{user.email}</td>
                <td className="px-2.5 py-2.5">
                  {user.name} {user.surname}
                </td>
                <td className="px-2.5 py-2.5">{user.phone_number}</td>
                <td className="px-2.5 py-2.5">
                  <span
                    className={`px-2 py-1 rounded ${getRoleColor(user.role)}`}
                  >
                    {user.role}
                  </span>
                </td>
                <td className="px-2.5 py-2.5">
                  {user.role !== "BLOCKED" ? (
                    <div className="flex gap-2">
                      <button
                        onClick={() =>
                          window.open(
                            `/user_profile?user_id=${user.id}`,
                            "_blank"
                          )
                        }
                        className="px-3 py-1.5 bg-blue-600 text-white border-none rounded text-xs cursor-pointer hover:bg-blue-700"
                      >
                        Profil
                      </button>
                      <button
                        onClick={() => setConfirmBanId(user.id)}
                        className="px-3 py-1.5 bg-red-500 text-white border-none rounded text-xs cursor-pointer hover:bg-red-600"
                      >
                        Zablokuj
                      </button>
                    </div>
                  ) : (
                    <span className="text-gray-600">Zablokowany</span>
                  )}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      <div className="flex gap-2.5 justify-center items-center">
        <button
          onClick={() => setPage(Math.max(0, page - 1))}
          disabled={page === 0}
          className="px-4 py-2 disabled:opacity-50 disabled:cursor-not-allowed cursor-pointer"
        >
          Poprzednia
        </button>
        <span>
          Strona {page + 1} z {totalPages}
        </span>
        <button
          onClick={() => setPage(Math.min(totalPages - 1, page + 1))}
          disabled={page >= totalPages - 1 || totalPages === 0}
          className="px-4 py-2 disabled:opacity-50 disabled:cursor-not-allowed cursor-pointer"
        >
          Następna
        </button>
      </div>

      {/* Modal potwierdzenia blokowania */}
      {confirmBanId && (
        <div className="fixed inset-0 bg-black/50 flex justify-center items-center z-50">
          <div className="bg-white rounded-lg p-8 max-w-sm w-11/12 shadow-lg">
            <h2 className="m-0 mb-5 text-red-500">Potwierdzenie</h2>
            <p className="mb-5 text-base">
              Czy na pewno chcesz zablokować użytkownika{" "}
              <strong>
                {users.find((u) => u.id === confirmBanId)?.username}
              </strong>
              ?
            </p>
            <p className="mb-5 text-sm text-gray-600">
              Ta akcja nie może być cofnięta. Zablokowany użytkownik nie będzie
              mógł zalogować się do systemu.
            </p>
            <div className="flex gap-2.5 justify-end">
              <button
                onClick={() => setConfirmBanId(null)}
                className="px-4 py-2 bg-gray-300 text-black border-none rounded cursor-pointer hover:bg-gray-400"
              >
                Anuluj
              </button>
              <button
                onClick={() => handleBanUser(confirmBanId)}
                disabled={banningId === confirmBanId}
                className="px-4 py-2 bg-red-500 text-white border-none rounded cursor-pointer disabled:opacity-60 disabled:cursor-not-allowed hover:bg-red-600"
              >
                {banningId === confirmBanId ? "Blokowanie..." : "Zablokuj"}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
