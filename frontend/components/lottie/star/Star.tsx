import React from "react";
import Lottie from "lottie-react";
import starJson from "./star.json";

const Star = () => {
  return (
    <Lottie
      animationData={starJson}
      loop={false}
      style={{
        // width: 20, height: 20,
        transform: "scale(2)",
      }}
    />
  );
};

export default Star;
