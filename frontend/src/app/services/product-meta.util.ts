export interface ProductVariant {
  size: string;
  color: string;
  priceDelta: number;
  stockDelta: number;
}

export interface ParsedProductMeta {
  description: string;
  category: string;
  sku: string;
  width: number;
  height: number;
  depth: number;
  dimensionUnit: 'cm' | 'mm' | 'm';
  weight: number;
  images: string[];
  variants: ProductVariant[];
}

export const PRODUCT_META_SPLITTER = '\n\n---product-meta---\n';

export function buildProductDescriptionWithMeta(input: {
  description?: string;
  category?: string;
  sku?: string;
  width?: number;
  height?: number;
  depth?: number;
  dimensionUnit?: 'cm' | 'mm' | 'm';
  weight?: number;
  images?: string[];
  variants?: ProductVariant[];
}): string {
  const cleanDescription = String(input.description || '').trim();

  const meta = {
    sku: String(input.sku || '').trim(),
    // ✅ Ne pas forcer 'OTHER' — garder la vraie catégorie ou laisser vide
    category: String(input.category || '').trim(),
    dimensions: {
      width: Number(input.width || 0),
      height: Number(input.height || 0),
      depth: Number(input.depth || 0),
      unit: (input.dimensionUnit || 'cm') as 'cm' | 'mm' | 'm'
    },
    weight: Number(input.weight || 0),
    images: Array.isArray(input.images)
      ? input.images.filter(url => !!String(url || '').trim())
      : [],
    variants: Array.isArray(input.variants)
      ? input.variants
          .map(variant => ({
            size: String(variant.size || '').trim(),
            color: String(variant.color || '').trim(),
            priceDelta: Number(variant.priceDelta || 0),
            stockDelta: Number(variant.stockDelta || 0)
          }))
          .filter(variant => !!variant.size || !!variant.color)
      : []
  };

  return `${cleanDescription}${PRODUCT_META_SPLITTER}${JSON.stringify(meta)}`;
}

export function parseProductDescription(rawDescription: string): ParsedProductMeta {
  const [plainDescription, rawMeta] = String(rawDescription || '').split(PRODUCT_META_SPLITTER);
  let parsedMeta: any = {};

  if (rawMeta) {
    try {
      parsedMeta = JSON.parse(rawMeta);
    } catch {
      parsedMeta = {};
    }
  }

  const width  = Number(parsedMeta?.dimensions?.width  || 0);
  const height = Number(parsedMeta?.dimensions?.height || 0);
  const depth  = Number(parsedMeta?.dimensions?.depth  || 0);
  const unit   = (parsedMeta?.dimensions?.unit || 'cm') as 'cm' | 'mm' | 'm';

  const images = Array.isArray(parsedMeta?.images)
    ? parsedMeta.images
        .map((url: unknown) => String(url || '').trim())
        .filter((url: string) => !!url)
    : [];

  const variants = Array.isArray(parsedMeta?.variants)
    ? parsedMeta.variants.map((variant: any) => ({
        size: String(variant?.size || '').trim(),
        color: String(variant?.color || '').trim(),
        priceDelta: Number(variant?.priceDelta || 0),
        stockDelta: Number(variant?.stockDelta || 0)
      }))
    : [];

  return {
    description: String(plainDescription || '').trim(),
    // ✅ Retourne '' si pas de catégorie encodée
    // Le composant utilisera categoryName du backend à la place
    category: String(parsedMeta?.category || ''),
    sku: String(parsedMeta?.sku || ''),
    width,
    height,
    depth,
    dimensionUnit: unit,
    weight: Number(parsedMeta?.weight || 0),
    images,
    variants
  };
}

export function parseCsvImages(raw: string): string[] {
  return String(raw || '')
    .split(',')
    .map(part => part.trim())
    .filter(part => !!part);
}

export function parseVariantRows(raw: string): ProductVariant[] {
  return String(raw || '')
    .split('\n')
    .map(line => line.trim())
    .filter(line => !!line)
    .map(line => {
      const [size = '', color = '', priceDelta = '0', stockDelta = '0'] =
        line.split('|').map(part => part.trim());
      return {
        size,
        color,
        priceDelta: Number(priceDelta || 0),
        stockDelta: Number(stockDelta || 0)
      };
    })
    .filter(variant => !!variant.size || !!variant.color);
}

export function toVariantRows(variants: ProductVariant[]): string {
  return (variants || [])
    .map(v => `${v.size}|${v.color}|${v.priceDelta}|${v.stockDelta}`)
    .join('\n');
}