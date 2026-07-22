import { createWriteStream, mkdirSync } from "node:fs";
import { once } from "node:events";
import { resolve } from "node:path";

const markets = [
  ["US", "USD"], ["GB", "GBP"], ["TH", "THB"], ["VN", "VND"],
  ["PH", "PHP"], ["MY", "MYR"], ["SG", "SGD"], ["ID", "IDR"],
];
const lifecycles = ["NEW", "GROWTH", "EXPLOSIVE", "DECLINE", "MATURE"];
const productHeader = "external_product_id,platform,market,title,category_external_id,category_name,shop_external_id,shop_name,currency,current_price,original_price,image_url,product_url,rating,review_count,listed_at,collected_at\n";
const statHeader = "external_product_id,platform,market,stat_date,price,sales_volume,sales_amount,total_sales_volume,video_count,creator_count,shop_count,similar_product_count,top10_shop_sales_share,top10_creator_sales_share,negative_review_rate,review_count,rating\n";

const options = parseArgs(process.argv.slice(2));
const output = resolve(options.output ?? (options.profile === "performance" ? "tmp/performance-data" : "tmp/functional-data"));
mkdirSync(output, { recursive: true });

const random = mulberry32(options.seed);
const products = createWriteStream(resolve(output, "products.csv"), { encoding: "utf8" });
const stats = createWriteStream(resolve(output, "product-stats.csv"), { encoding: "utf8" });
await write(products, productHeader);
await write(stats, statHeader);

for (let index = 1; index <= options.products; index += 1) {
  const [market, currency] = markets[(index - 1) % markets.length];
  const lifecycle = lifecycles[(index - 1) % lifecycles.length];
  const category = (index - 1) % 40 + 1;
  const shop = (index - 1) % Math.max(120, Math.ceil(options.products / 20)) + 1;
  const basePrice = 8 + random() * 92;
  const rating = 3 + random() * 2;
  const reviewCount = Math.floor(random() * 8000);
  const id = `mock-${String(index).padStart(8, "0")}`;
  const compact = options.profile === "performance";
  await write(products, [
    id, "TIKTOK_SHOP", market, csv(compact ? `P${index}` : `Mock ${lifecycle} Product ${index}`), `c-${category}`,
    csv(`Category ${category}`), `s-${shop}`, csv(`Shop ${shop}`), currency, money(basePrice),
    money(basePrice * 1.2), compact ? "" : `https://picsum.photos/seed/${id}/640/640`,
    compact ? "" : `https://example.com/products/${id}`, rating.toFixed(2), reviewCount,
    "2026-05-01T08:00:00Z", "2026-07-21T08:00:00Z",
  ].join(",") + "\n");

  let cumulativeSales = Math.floor(random() * 5000);
  for (let day = options.days - 1; day >= 0; day -= 1) {
    const date = new Date(Date.UTC(2026, 6, 21 - day));
    const phase = (options.days - day) / Math.max(options.days, 1);
    const multiplier = lifecycleMultiplier(lifecycle, phase);
    const sales = Math.max(0, Math.floor((20 + random() * 180) * multiplier));
    cumulativeSales += sales;
    const dailyPrice = Math.max(1, basePrice * (1 + (random() - 0.5) * 0.08));
    await write(stats, [
      id, "TIKTOK_SHOP", market, date.toISOString().slice(0, 10), money(dailyPrice), sales,
      money(dailyPrice * sales), cumulativeSales, Math.floor(5 + random() * 120),
      Math.floor(3 + random() * 80), Math.floor(2 + random() * 18), Math.floor(10 + random() * 100),
      (30 + random() * 60).toFixed(4), (30 + random() * 60).toFixed(4),
      (1 + random() * 12).toFixed(4), reviewCount, rating.toFixed(2),
    ].join(",") + "\n");
  }
}

products.end();
stats.end();
await Promise.all([once(products, "finish"), once(stats, "finish")]);
process.stdout.write(JSON.stringify({ ...options, output }, null, 2) + "\n");

function parseArgs(args) {
  const values = { products: 120, days: 30, seed: 20260721, profile: "functional" };
  for (let index = 0; index < args.length; index += 2) {
    const key = args[index]?.replace(/^--/, "");
    const value = args[index + 1];
    if (!key || value === undefined) throw new Error("Arguments must use --name value pairs");
    if (["products", "days", "seed"].includes(key)) values[key] = Number(value);
    else if (["profile", "output"].includes(key)) values[key] = value;
    else throw new Error(`Unknown argument: --${key}`);
  }
  if (!Number.isInteger(values.products) || values.products < 1) throw new Error("--products must be a positive integer");
  if (!Number.isInteger(values.days) || values.days < 0 || values.days > 365) throw new Error("--days must be between 0 and 365");
  if (!Number.isInteger(values.seed)) throw new Error("--seed must be an integer");
  if (!["functional", "performance"].includes(values.profile)) throw new Error("--profile must be functional or performance");
  return values;
}

async function write(stream, value) {
  if (!stream.write(value)) await once(stream, "drain");
}

function csv(value) {
  return `"${String(value).replaceAll('"', '""')}"`;
}

function money(value) {
  return value.toFixed(2);
}

function lifecycleMultiplier(lifecycle, phase) {
  if (lifecycle === "NEW") return 0.25 + phase * 0.5;
  if (lifecycle === "GROWTH") return 0.5 + phase * 1.2;
  if (lifecycle === "EXPLOSIVE") return phase > 0.65 ? 2.5 : 0.6;
  if (lifecycle === "DECLINE") return 1.8 - phase * 1.3;
  return 1 + Math.sin(phase * Math.PI * 2) * 0.08;
}

function mulberry32(seed) {
  return function seededRandom() {
    let value = seed += 0x6D2B79F5;
    value = Math.imul(value ^ value >>> 15, value | 1);
    value ^= value + Math.imul(value ^ value >>> 7, value | 61);
    return ((value ^ value >>> 14) >>> 0) / 4294967296;
  };
}
