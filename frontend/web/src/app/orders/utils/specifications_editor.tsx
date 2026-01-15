"use client";

import { useState, useRef, useEffect } from "react";
import { FaTrash, FaChevronLeft, FaChevronRight, FaPlus } from "react-icons/fa";

interface SpecificationsEditorProps {
  specifications: Record<string, string>;
  onUpdate: (specs: Record<string, string>) => void;
}

export default function SpecificationsEditor({
  specifications,
  onUpdate,
}: SpecificationsEditorProps) {
  const [newSpecName, setNewSpecName] = useState("");
  const [newSpecValue, setNewSpecValue] = useState("");
  const [editingKey, setEditingKey] = useState<string | null>(null);
  const scrollContainerRef = useRef<HTMLDivElement>(null);
  const [canScrollLeft, setCanScrollLeft] = useState(false);
  const [canScrollRight, setCanScrollRight] = useState(false);

  const updateScrollButtons = () => {
    if (scrollContainerRef.current) {
      const { scrollLeft, scrollWidth, clientWidth } =
        scrollContainerRef.current;
      setCanScrollLeft(scrollLeft > 0);
      setCanScrollRight(scrollLeft < scrollWidth - clientWidth - 10);
    }
  };

  useEffect(() => {
    updateScrollButtons();
    const container = scrollContainerRef.current;
    if (container) {
      container.addEventListener("scroll", updateScrollButtons);
      return () => container.removeEventListener("scroll", updateScrollButtons);
    }
  }, [specifications]);

  const handleAddSpec = () => {
    if (newSpecName.trim() && newSpecValue.trim()) {
      const updatedSpecs = {
        ...specifications,
        [newSpecName]: newSpecValue,
      };
      onUpdate(updatedSpecs);
      setNewSpecName("");
      setNewSpecValue("");
    }
  };

  const handleDeleteSpec = (key: string) => {
    const updatedSpecs = { ...specifications };
    delete updatedSpecs[key];
    onUpdate(updatedSpecs);
  };

  const handleUpdateValue = (key: string, newValue: string) => {
    const updatedSpecs = {
      ...specifications,
      [key]: newValue,
    };
    onUpdate(updatedSpecs);
    setEditingKey(null);
  };

  const scroll = (direction: "left" | "right") => {
    if (scrollContainerRef.current) {
      const scrollAmount = 300;
      scrollContainerRef.current.scrollBy({
        left: direction === "left" ? -scrollAmount : scrollAmount,
        behavior: "smooth",
      });
    }
  };

  return (
    <div className="space-y-6">
      <div className="bg-linear-to-r from-primary-50 to-primary-100 p-6 rounded-2xl border-2 border-primary-200">
        <h4 className="text-xs font-bold text-primary-800 mb-4 uppercase tracking-widest">
          Dodaj nową specyfikację
        </h4>
        <div className="grid grid-cols-1 sm:grid-cols-2 gap-3 mb-3">
          <input
            type="text"
            placeholder="Nazwa specyfikacji (np. Rozdzielczość)"
            value={newSpecName}
            onChange={(e) => setNewSpecName(e.target.value)}
            className="px-4 py-3 bg-white border-2 border-transparent focus:border-primary-300 rounded-xl outline-none text-sm font-medium text-black"
          />
          <input
            type="text"
            placeholder="Wartość (np. 4K)"
            value={newSpecValue}
            onChange={(e) => setNewSpecValue(e.target.value)}
            className="px-4 py-3 bg-white border-2 border-transparent focus:border-primary-300 rounded-xl outline-none text-sm font-medium text-black"
          />
        </div>
        <button
          onClick={handleAddSpec}
          disabled={!newSpecName.trim() || !newSpecValue.trim()}
          className="w-full flex items-center justify-center gap-2 px-4 py-3 bg-primary-300 text-primary-900 rounded-xl font-bold hover:bg-primary-400 disabled:opacity-30 transition-all uppercase tracking-widest text-xs"
        >
          <FaPlus size={14} />
          Dodaj specyfikację
        </button>
      </div>

      <div className="bg-gray-50 rounded-2xl border-2 border-gray-100 p-4">
        <div className="flex items-center justify-between mb-4">
          <h4 className="text-xs font-bold text-primary-800 uppercase tracking-widest">
            Specyfikacje ({Object.keys(specifications).length})
          </h4>
          <div className="flex gap-2">
            <button
              onClick={() => scroll("left")}
              disabled={!canScrollLeft}
              className="p-2 bg-white border-2 border-gray-200 rounded-lg text-gray-600 hover:bg-gray-100 disabled:opacity-30 transition-all"
            >
              <FaChevronLeft size={16} />
            </button>
            <button
              onClick={() => scroll("right")}
              disabled={!canScrollRight}
              className="p-2 bg-white border-2 border-gray-200 rounded-lg text-gray-600 hover:bg-gray-100 disabled:opacity-30 transition-all"
            >
              <FaChevronRight size={16} />
            </button>
          </div>
        </div>

        {Object.keys(specifications).length === 0 ? (
          <div className="text-center py-8 text-gray-400">
            <p className="text-sm">Brak dodanych specyfikacji</p>
          </div>
        ) : (
          <div
            ref={scrollContainerRef}
            className="overflow-x-auto flex gap-3 pb-2 scroll-smooth"
            style={{ scrollBehavior: "smooth" }}
          >
            {Object.entries(specifications).map(([key, value]) => (
              <div
                key={key}
                className="shrink-0 bg-white rounded-xl border-2 border-gray-200 p-4 min-w-[250px] hover:border-primary-300 transition-all group"
              >
                <div className="flex items-start justify-between mb-3">
                  <div className="flex-1">
                    <p className="text-[10px] font-bold text-gray-400 uppercase tracking-widest mb-1">
                      {key}
                    </p>
                    {editingKey === key ? (
                      <input
                        autoFocus
                        type="text"
                        defaultValue={value}
                        onBlur={(e) => handleUpdateValue(key, e.target.value)}
                        onKeyDown={(e) => {
                          if (e.key === "Enter") {
                            handleUpdateValue(key, e.currentTarget.value);
                          }
                        }}
                        className="w-full px-2 py-1 bg-primary-50 border-2 border-primary-300 rounded text-sm font-bold text-black outline-none"
                      />
                    ) : (
                      <p
                        onClick={() => setEditingKey(key)}
                        className="text-sm font-bold text-gray-800 cursor-pointer hover:text-primary-600 transition-colors"
                      >
                        {value}
                      </p>
                    )}
                  </div>
                  <button
                    onClick={() => handleDeleteSpec(key)}
                    className="text-gray-400 hover:text-red-500 transition-colors opacity-0 group-hover:opacity-100 ml-2 shrink-0"
                  >
                    <FaTrash size={14} />
                  </button>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}
