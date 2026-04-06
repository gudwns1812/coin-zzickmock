"use client";

import React from "react";
import Modal from "../Modal";
import DaumPostcodeEmbed from "react-daum-postcode";

const AddressModal = ({
  isOpen,
  onClose,
  handleAddress,
}: {
  isOpen: boolean;
  onClose: () => void;
  handleAddress: (data: any) => void;
}) => {
  return (
    <Modal isOpen={isOpen} onClose={onClose} hasCloseButton={false}>
      <DaumPostcodeEmbed
        onComplete={handleAddress}
        style={{ width: "400px", height: "500px" }}
      />
    </Modal>
  );
};

export default AddressModal;
