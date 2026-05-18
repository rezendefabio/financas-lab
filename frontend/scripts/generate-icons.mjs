// Gera os icones PNG do PWA a partir do SVG fonte em public/icons/icon.svg.
// Uso: node scripts/generate-icons.mjs
// Requer o pacote `sharp` (disponivel como dependencia transitiva).
import sharp from "sharp";
import { fileURLToPath } from "node:url";
import { dirname, join } from "node:path";

const root = join(dirname(fileURLToPath(import.meta.url)), "..");
const source = join(root, "public", "icons", "icon.svg");
const sizes = [192, 512];

for (const size of sizes) {
  const out = join(root, "public", "icons", `icon-${size}.png`);
  await sharp(source).resize(size, size).png().toFile(out);
  console.log(`gerado: icons/icon-${size}.png`);
}
