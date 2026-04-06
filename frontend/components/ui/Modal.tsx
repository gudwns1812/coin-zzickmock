import { X } from "lucide-react";
import React, { useEffect, useState } from "react";
import { createPortal } from "react-dom";

interface ModalProps {
  children: React.ReactNode;
  isOpen: boolean;
  onClose: () => void;
  hasBackdropBlur?: boolean;
  hasCloseButton?: boolean;
  isClickOutsideClose?: boolean;
  isEscapeClose?: boolean;
}

const Modal = ({
  children,
  isOpen,
  onClose,
  hasBackdropBlur = true,
  hasCloseButton = true,
  isClickOutsideClose = true,
  isEscapeClose = false,
}: ModalProps) => {
  const [visible, setVisible] = useState(false);
  const [mounted, setMounted] = useState(false);

  useEffect(() => {
    if (isEscapeClose) {
      const handleEscapeClose = (event: KeyboardEvent) => {
        if (event.key === "Escape") {
          onClose();
        }
      };
      window.addEventListener("keydown", handleEscapeClose);
      return () => {
        window.removeEventListener("keydown", handleEscapeClose);
      };
    }
  }, [isEscapeClose]);

  useEffect(() => {
    setMounted(true);
  }, []);

  useEffect(() => {
    if (isOpen) {
      setVisible(true);
      if (mounted) document.body.style.overflow = "hidden";
    } else {
      setVisible(false);
      if (mounted) document.body.style.overflow = "";
    }
    return () => {
      if (mounted) document.body.style.overflow = "";
    };
  }, [isOpen, mounted]);

  if (!mounted) return null;
  if (!isOpen) return null;

  return createPortal(
    <div
      className="fixed inset-0 z-50 flex items-center justify-center"
      aria-modal="true"
      tabIndex={-1}
    >
      <div
        className="absolute inset-0 bg-black/50"
        onClick={isClickOutsideClose ? onClose : () => {}}
      />
      <div className="relative">
        <div
          className={`z-40 bg-white rounded-main p-main-2 min-w-[400px] max-w-[80vw] max-h-[90vh] shadow-color transition-opacity duration-500 overflow-y-scroll ${
            visible ? "opacity-100" : "opacity-0"
          }`}
        >
          {hasCloseButton && (
            <button
              className="absolute top-4 right-4 z-50"
              onClick={onClose}
              aria-label="닫기"
            >
              <X size={20} color="black" />
            </button>
          )}
          <div className="overflow-y-auto">{children}</div>
          {hasBackdropBlur && (
            <div className="pointer-events-none absolute left-0 bottom-0 h-[200px] bg-gradient-to-t from-white/90 to-transparent" />
          )}
        </div>
      </div>
    </div>,
    document.body
  );
};

export default Modal;
