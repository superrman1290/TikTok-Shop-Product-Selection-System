import { render, screen } from "@testing-library/react";
import { ProductImage } from "./product-image";

describe("ProductImage", () => {
  it("renders a real product URL as an accessible image surface", () => {
    render(<ProductImage src="https://example.com/product.jpg" alt="Portable Blender" className="media" />);
    const image = screen.getByRole("img", { name: "Portable Blender" });
    expect(image).toHaveStyle({ backgroundImage: 'url("https://example.com/product.jpg")' });
  });

  it("renders an explicit fallback state", () => {
    render(<ProductImage alt="Portable Blender" className="fallback" />);
    expect(screen.getByRole("img", { name: "Portable Blender 暂无图片" })).toBeInTheDocument();
  });
});
