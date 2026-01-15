"use client";

import { useRef, useEffect, useState } from "react";
import { FaChevronLeft, FaChevronRight } from "react-icons/fa";

interface SpecificationsDisplayProps {
  specifications: Record<string, string>;
}

export default function SpecificationsDisplay({
  specifications,
}: SpecificationsDisplayProps) {
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
      window.addEventListener("resize", updateScrollButtons);
      return () => {
        container.removeEventListener("scroll", updateScrollButtons);
        window.removeEventListener("resize", updateScrollButtons);
      };
    }
  }, [specifications]);

  const scroll = (direction: "left" | "right") => {
    if (scrollContainerRef.current) {
      const scrollAmount = 300;
      scrollContainerRef.current.scrollBy({
        left: direction === "left" ? -scrollAmount : scrollAmount,
        behavior: "smooth",
      });
    }
  };

  if (Object.keys(specifications).length === 0) {
    return null;
  }

  return (
    <div className="mb-10">
      <div className="flex items-center justify-between mb-4">
        <h4 className="font-bold uppercase text-[10px] tracking-[0.2em] text-primary-900">
          Specyfikacje
        </h4>
        <div className="flex gap-2">
          <button
            onClick={() => scroll("left")}
            disabled={!canScrollLeft}
            className="p-2 bg-gray-100 border border-gray-200 rounded-lg text-gray-600 hover:bg-gray-200 disabled:opacity-30 transition-all"
          >
            <FaChevronLeft size={14} />
          </button>
          <button
            onClick={() => scroll("right")}
            disabled={!canScrollRight}
            className="p-2 bg-gray-100 border border-gray-200 rounded-lg text-gray-600 hover:bg-gray-200 disabled:opacity-30 transition-all"
          >
            <FaChevronRight size={14} />
          </button>
        </div>
      </div>

      <div
        ref={scrollContainerRef}
        className="overflow-x-auto flex gap-3 pb-2 scroll-smooth"
        style={{ scrollBehavior: "smooth" }}
      >
        {Object.entries(specifications).map(([key, value]) => (
          <div
            key={key}
            className="shrink-0 bg-gray-50 rounded-xl border border-gray-200 p-4 min-w-[200px]"
          >
            <p className="text-[9px] font-bold text-gray-400 uppercase tracking-widest mb-2">
              {key}
            </p>
            <p className="text-sm font-bold text-gray-800">{value}</p>
          </div>
        ))}
      </div>
    </div>
  );
}
