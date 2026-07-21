import { PictureOutlined } from "@ant-design/icons";

export function ProductImage({ src, alt, className }: { src?: string; alt: string; className: string }) {
  if (!src) {
    return <span className={className} role="img" aria-label={`${alt} 暂无图片`}><PictureOutlined /></span>;
  }
  return (
    <span
      className={className}
      role="img"
      aria-label={alt}
      style={{ backgroundImage: `url(${JSON.stringify(src)})` }}
    />
  );
}
